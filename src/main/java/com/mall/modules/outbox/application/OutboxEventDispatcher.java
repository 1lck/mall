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
	private static final int DISPATCH_BATCH_SIZE = 50;
	private static final long RETRY_DELAY_SECONDS = 30L;
	private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "PAYMENT_SUCCEEDED";

	private final OutboxEventMapper outboxEventRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;

	public OutboxEventDispatcher(
		OutboxEventMapper outboxEventRepository,
		KafkaTemplate<String, Object> kafkaTemplate,
		ObjectMapper objectMapper
	) {
		this.outboxEventRepository = outboxEventRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

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

	private Object toKafkaPayload(OutboxEventEntity event) {
		// outbox 表里只存通用 JSON，所以投递前要按 eventType 还原成具体消息对象。
		// 目前先只接支付成功事件，后面订单事件也可以继续往这里扩。
		if (PAYMENT_SUCCEEDED_EVENT_TYPE.equals(event.getEventType())) {
			return objectMapper.convertValue(event.getPayload(), PaymentSucceededEvent.class);
		}

		throw new IllegalArgumentException("Unsupported outbox event type: " + event.getEventType());
	}

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
			"Outbox event dispatched successfully: eventId={}, eventType={}, topic={}",
			event.getEventId(),
			event.getEventType(),
			event.getTopic()
		);
	}

	private void markFailed(OutboxEventEntity event, Exception exception) {
		// 第一版先用固定退避时间：
		// 发失败就进入 FAILED，并约定 30 秒后再重试一次。
		int nextRetryCount = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
		Instant nextRetryAt = Instant.now().plusSeconds(RETRY_DELAY_SECONDS);
		outboxEventRepository.updateDispatchResult(
			event.getId(),
			OutboxEventStatus.FAILED,
			nextRetryCount,
			nextRetryAt,
			exception.getMessage(),
			null
		);
		log.warn(
			"Outbox event dispatch failed and will retry later: eventId={}, eventType={}, retryCount={}, message={}",
			event.getEventId(),
			event.getEventType(),
			nextRetryCount,
			exception.getMessage()
		);
	}
}
