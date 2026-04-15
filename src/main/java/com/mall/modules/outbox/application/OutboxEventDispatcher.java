package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 统一的 outbox 扫描投递器。
 *
 * <p>它的职责非常单一：
 * 定时捞出 outbox 表里“现在可以发”的事件，投递到 Kafka，
 * 然后把发送结果再回写到 outbox 表。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class OutboxEventDispatcher {

	private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcher.class);
	/** 单次扫描允许投递的最大批次大小。 */
	private static final int DISPATCH_BATCH_SIZE = 50;
	/** 当前支持反序列化的支付成功事件类型。 */
	private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "PAYMENT_SUCCEEDED";

	private final OutboxEventMapper outboxEventRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final OutboxRetryPolicy outboxRetryPolicy;

	public OutboxEventDispatcher(
		OutboxEventMapper outboxEventRepository,
		KafkaTemplate<String, Object> kafkaTemplate,
		ObjectMapper objectMapper,
		OutboxRetryPolicy outboxRetryPolicy
	) {
		this.outboxEventRepository = outboxEventRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.outboxRetryPolicy = outboxRetryPolicy;
	}

	/**
	 * 定时扫描并投递待发送的 outbox 事件。
	 */
	@Scheduled(fixedDelayString = "${mall.outbox.dispatch-fixed-delay-ms:1000}")
	public void dispatchPendingEvents() {
		// 第一版先用最简单稳定的轮询模型：
		// 每次捞一小批，避免一次扫描把所有待发记录都压进内存。
		List<OutboxEventEntity> events =
			outboxEventRepository.findDispatchableBatch(Instant.now(), DISPATCH_BATCH_SIZE);
		for (OutboxEventEntity event : events) {
			dispatchSingleEvent(event);
		}
	}

	/**
	 * 只投递指定 id 的 outbox 事件。
	 */
	public void dispatchEventById(Long outboxEventId) {
		// 即时触发路径只关心当前这条 outbox 记录：
		// 查到了就精确投递，查不到就直接结束，不再顺手扫描整批数据。
		Optional<OutboxEventEntity> eventOptional = outboxEventRepository.findById(outboxEventId);
		if (eventOptional.isEmpty()) {
			return;
		}

		OutboxEventEntity event = eventOptional.get();
		dispatchSingleEvent(event);
	}

	/**
	 * 执行单条 outbox 事件的实际投递。
	 */
	private void dispatchSingleEvent(OutboxEventEntity event) {
		try {
			// 真正的外部副作用只发生在这里：
			// 把 outbox 里已经持久化好的消息发到 Kafka。
			kafkaTemplate.send(event.getTopic(), event.getMessageKey(), toKafkaPayload(event));
			markSent(event);
		} catch (Exception exception) {
			markFailed(event, exception);
		}
	}

	/**
	 * 把 outbox 记录里的通用 JSON 还原成具体 Kafka 消息对象。
	 */
	private Object toKafkaPayload(OutboxEventEntity event) {
		// outbox 表里只存通用 JSON，所以投递前要按 eventType 还原成具体消息对象。
		// 目前先只接支付成功事件，后面订单事件也可以继续往这里扩。
		if (PAYMENT_SUCCEEDED_EVENT_TYPE.equals(event.getEventType())) {
			return objectMapper.convertValue(event.getPayload(), PaymentSucceededEvent.class);
		}

		throw new IllegalArgumentException("暂不支持的待投递事件类型: " + event.getEventType());
	}

	/**
	 * 把 outbox 记录标记为发送成功。
	 */
	private void markSent(OutboxEventEntity event) {
		// 发送成功后把这条记录标记为 SENT，
		// 后续扫描时就不会再被重复捞出来。
		Instant sentAt = Instant.now();
		outboxEventRepository.updateDispatchResult(
			event.getId(),
			OutboxEventStatus.SENT,
			event.getRetryCount(),
			null,
			null,
			sentAt
		);
		log.info(
			"待投递消息发送成功: 事件编号={}, 事件类型={}, 消息主题={}",
			event.getEventId(),
			describeEventType(event.getEventType()),
			event.getTopic()
		);
	}

	/**
	 * 按重试策略回写发送失败结果。
	 */
	private void markFailed(OutboxEventEntity event, Exception exception) {
		// 发送失败后，统一交给重试策略决定：
		// 是继续进入 FAILED 等待下次重试，还是进入 DEAD 停止自动重试。
		String logErrorMessage = describeDispatchException(exception);
		OutboxRetryPlan retryPlan = outboxRetryPolicy.planFailure(event.getRetryCount(), Instant.now(), exception.getMessage());
		outboxEventRepository.updateDispatchResult(
			event.getId(),
			retryPlan.status(),
			retryPlan.retryCount(),
			retryPlan.nextRetryAt(),
			retryPlan.lastError(),
			null
		);
		if (retryPlan.status() == OutboxEventStatus.DEAD) {
			log.error(
				"待投递消息发送达到最大重试次数，已转入终止状态: 事件编号={}, 事件类型={}, 重试次数={}, 错误信息={}",
				event.getEventId(),
				describeEventType(event.getEventType()),
				retryPlan.retryCount(),
				logErrorMessage
			);
			return;
		}

		log.warn(
			"待投递消息发送失败，稍后会自动重试: 事件编号={}, 事件类型={}, 重试次数={}, 下次重试时间={}, 错误信息={}",
			event.getEventId(),
			describeEventType(event.getEventType()),
			retryPlan.retryCount(),
			retryPlan.nextRetryAt(),
			logErrorMessage
		);
	}

	/**
	 * 把事件类型代码转换成便于日志阅读的中文描述。
	 */
	private String describeEventType(String eventType) {
		if (PAYMENT_SUCCEEDED_EVENT_TYPE.equals(eventType)) {
			return "支付成功";
		}

		return "未知事件类型(" + eventType + ")";
	}

	/**
	 * 生成适合写入中文日志的投递失败摘要，避免第三方异常原文直接出现在日志里。
	 */
	private String describeDispatchException(Exception exception) {
		if (exception instanceof IllegalArgumentException illegalArgumentException
			&& illegalArgumentException.getMessage() != null
			&& !illegalArgumentException.getMessage().isBlank()) {
			return illegalArgumentException.getMessage();
		}

		return "消息发送过程中发生异常，请结合持久化错误详情进一步排查";
	}
}
