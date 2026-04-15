package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.dto.OutboxDebugEventType;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Outbox 调试数据应用服务。
 *
 * <p>这个服务只在本地调试开关打开时生效，
 * 用来快速生成几类典型的 outbox 记录，方便观察页面、重试逻辑和断点调试。
 * 它不会参与正式业务流程。</p>
 */
@Service
@ConditionalOnProperty(name = {"mall.outbox.debug.enabled", "mall.kafka.enabled"}, havingValue = "true")
public class OutboxDebugApplicationService {

	private static final Logger log = LoggerFactory.getLogger(OutboxDebugApplicationService.class);
	/** 调试接口直接发送 Kafka 时使用的默认支付金额。 */
	private static final BigDecimal DEFAULT_DEBUG_PAYMENT_AMOUNT = new BigDecimal("99.90");
	/** 调试接口等待 Kafka send 完成的最长时间，避免请求线程无限卡住。 */
	private static final long DIRECT_SEND_WAIT_SECONDS = 5;
	/** 调试页生成的 outbox 消息统一打上专门聚合类型，便于后续清理。 */
	private static final String DEBUG_AGGREGATE_TYPE = "PAYMENT_DEBUG";
	/** 支付成功事件类型。 */
	private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "PAYMENT_SUCCEEDED";
	/** 用于稳定制造失败的调试事件类型。 */
	private static final String UNSUPPORTED_DEBUG_EVENT_TYPE = "DEBUG_UNSUPPORTED_EVENT";

	private final OutboxEventMapper outboxEventMapper;
	private final OutboxDispatchTrigger outboxDispatchTrigger;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final KafkaTopicsProperties kafkaTopicsProperties;
	private final ObjectMapper objectMapper;

	public OutboxDebugApplicationService(
		OutboxEventMapper outboxEventMapper,
		OutboxDispatchTrigger outboxDispatchTrigger,
		KafkaTemplate<String, Object> kafkaTemplate,
		KafkaTopicsProperties kafkaTopicsProperties,
		ObjectMapper objectMapper
	) {
		this.outboxEventMapper = outboxEventMapper;
		this.outboxDispatchTrigger = outboxDispatchTrigger;
		this.kafkaTemplate = kafkaTemplate;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
		this.objectMapper = objectMapper;
	}

	/**
	 * 直接往 Kafka 发送一条支付成功调试消息。
	 *
	 * <p>这个入口专门用于练习消费者链路，不会落 outbox。
	 * 方法会等待 send future 完成后再返回，确保前端收到成功提示时，
	 * 这条消息已经真正交给 Kafka 客户端发送流程。</p>
	 *
	 * @param orderNo 要发送的订单号
	 * @param amount 调试支付金额；为空时使用默认值
	 * @return 实际发送出去的支付成功事件内容
	 */
	public PaymentSucceededEvent sendPaymentSucceededMessage(String orderNo, BigDecimal amount) {
		BigDecimal resolvedAmount = amount == null ? DEFAULT_DEBUG_PAYMENT_AMOUNT : amount;
		PaymentSucceededEvent event = new PaymentSucceededEvent(orderNo, resolvedAmount, Instant.now());

		try {
			kafkaTemplate.send(
				kafkaTopicsProperties.getTopics().getPaymentSucceeded(),
				orderNo,
				event
			).get(DIRECT_SEND_WAIT_SECONDS, TimeUnit.SECONDS);
			log.info("调试接口已直接发送支付成功 Kafka 消息: orderNo={}, amount={}", orderNo, resolvedAmount);
			return event;
		} catch (Exception exception) {
			throw new BusinessException(
				ErrorCode.INTERNAL_ERROR,
				"调试支付成功消息发送失败: " + exception.getMessage()
			);
		}
	}

	/**
	 * 一次性生成一组常见的 outbox 演示数据。
	 *
	 * <p>当前会生成：
	 * 1. 一条已经发送成功的历史消息
	 * 2. 一条等待下一轮扫描重试的失败消息
	 * 3. 一条已经进入 DEAD 的死信消息
	 * 4. 一条会在精确投递时立即失败的调试消息</p>
	 *
	 * @return 刚生成出来的 outbox 记录，方便前端立即展示
	 */
	public List<OutboxEventAdminVO> createDemoBatch() {
		Instant now = Instant.now();
		List<OutboxEventAdminVO> result = new ArrayList<>();
		result.add(createSentDemoEvent(now));
		result.add(createFailedDemoEvent(now));
		result.add(createDeadDemoEvent(now));
		result.add(createImmediateFailDemoEvent(now));
		return result;
	}

	/**
	 * 按指定调试类型生成单条 outbox 消息。
	 *
	 * @param type 调试场景类型
	 * @param aggregateId 可选聚合标识，为空时自动生成
	 * @return 刚创建的单条调试消息
	 */
	public OutboxEventAdminVO createSingleEvent(OutboxDebugEventType type, String aggregateId) {
		Instant now = Instant.now();
		String resolvedAggregateId = resolveAggregateId(type, aggregateId);
		if (type == OutboxDebugEventType.SENT) {
			return createSentDemoEvent(now, resolvedAggregateId);
		}
		if (type == OutboxDebugEventType.FAILED) {
			return createFailedDemoEvent(now, resolvedAggregateId);
		}
		if (type == OutboxDebugEventType.DEAD) {
			return createDeadDemoEvent(now, resolvedAggregateId);
		}
		if (type == OutboxDebugEventType.IMMEDIATE_FAIL) {
			return createImmediateFailDemoEvent(now, resolvedAggregateId);
		}

		throw new IllegalArgumentException("Unsupported debug event type: " + type);
	}

	/**
	 * 清理当前调试功能生成的历史 outbox 数据。
	 *
	 * <p>为了避免误删真实业务消息，这里只删除两类记录：
	 * 1. 当前新版本调试功能打过 DEBUG 聚合标记的消息
	 * 2. 早期使用 ORD-DEMO-* 前缀生成的历史演示数据</p>
	 *
	 * @return 实际删除的记录条数
	 */
	public int cleanupDebugEvents() {
		return outboxEventMapper.deleteDebugEvents(DEBUG_AGGREGATE_TYPE, "ORD-DEMO-%");
	}

	/**
	 * 构造一条已经投递成功的历史消息，方便页面观察 SENT 状态。
	 */
	private OutboxEventAdminVO createSentDemoEvent(Instant now) {
		return createSentDemoEvent(now, "ORD-DEMO-SENT-" + shortId());
	}

	/**
	 * 构造一条已经投递成功的历史消息，并允许指定聚合标识。
	 */
	private OutboxEventAdminVO createSentDemoEvent(Instant now, String aggregateId) {
		OutboxEventEntity entity = buildPaymentEvent(aggregateId, now.minusSeconds(120));
		entity.setStatus(OutboxEventStatus.SENT);
		entity.setRetryCount(0);
		entity.setSentAt(now.minusSeconds(90));
		outboxEventMapper.save(entity);
		return toAdminVO(entity);
	}

	/**
	 * 构造一条待重试消息，方便观察 FAILED 与 nextRetryAt。
	 */
	private OutboxEventAdminVO createFailedDemoEvent(Instant now) {
		return createFailedDemoEvent(now, "ORD-DEMO-FAILED-" + shortId());
	}

	/**
	 * 构造一条待重试消息，并允许指定聚合标识。
	 */
	private OutboxEventAdminVO createFailedDemoEvent(Instant now, String aggregateId) {
		OutboxEventEntity entity = buildPaymentEvent(aggregateId, now.minusSeconds(60));
		entity.setStatus(OutboxEventStatus.FAILED);
		entity.setRetryCount(1);
		entity.setNextRetryAt(now.plusSeconds(10));
		entity.setLastError("模拟 Kafka 不可用，等待下次自动重试");
		outboxEventMapper.save(entity);
		return toAdminVO(entity);
	}

	/**
	 * 构造一条已经超过最大重试次数的死信消息。
	 */
	private OutboxEventAdminVO createDeadDemoEvent(Instant now) {
		return createDeadDemoEvent(now, "ORD-DEMO-DEAD-" + shortId());
	}

	/**
	 * 构造一条死信消息，并允许指定聚合标识。
	 */
	private OutboxEventAdminVO createDeadDemoEvent(Instant now, String aggregateId) {
		OutboxEventEntity entity = buildPaymentEvent(aggregateId, now.minusSeconds(300));
		entity.setStatus(OutboxEventStatus.DEAD);
		entity.setRetryCount(4);
		entity.setLastError("模拟连续多次投递失败，已进入 DEAD");
		outboxEventMapper.save(entity);
		return toAdminVO(entity);
	}

	/**
	 * 构造一条会被精确投递路径立即打成失败的消息。
	 *
	 * <p>这里故意写入一个当前投递器不支持的 eventType，
	 * 这样 afterCommit 或直接触发投递时会稳定抛错，便于你观察失败回写链路。</p>
	 */
	private OutboxEventAdminVO createImmediateFailDemoEvent(Instant now) {
		return createImmediateFailDemoEvent(now, "ORD-DEMO-IMMEDIATE-FAIL-" + shortId());
	}

	/**
	 * 构造一条会被即时投递路径稳定打成失败的消息，并允许指定聚合标识。
	 */
	private OutboxEventAdminVO createImmediateFailDemoEvent(Instant now, String aggregateId) {
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setEventId(UUID.randomUUID().toString());
		entity.setAggregateType(DEBUG_AGGREGATE_TYPE);
		entity.setAggregateId(aggregateId);
		entity.setEventType(UNSUPPORTED_DEBUG_EVENT_TYPE);
		entity.setTopic(kafkaTopicsProperties.getTopics().getPaymentSucceeded());
		entity.setMessageKey(aggregateId);
		entity.setPayload(objectMapper.createObjectNode()
			.put("orderNo", aggregateId)
			.put("message", "这是一条专门用于演示即时失败的调试消息"));
		entity.setStatus(OutboxEventStatus.PENDING);
		entity.setRetryCount(0);
		outboxEventMapper.save(entity);
		outboxDispatchTrigger.requestDispatch(entity.getId());
		return outboxEventMapper.findById(entity.getId())
			.map(this::toAdminVO)
			.orElseGet(() -> toAdminVO(entity));
	}

	/**
	 * 规范化外部传入的聚合标识，空值时按不同调试类型自动生成。
	 */
	private String resolveAggregateId(OutboxDebugEventType type, String aggregateId) {
		if (aggregateId != null && !aggregateId.isBlank()) {
			return aggregateId.trim();
		}

		if (type == OutboxDebugEventType.SENT) {
			return "ORD-DEMO-SENT-" + shortId();
		}
		if (type == OutboxDebugEventType.FAILED) {
			return "ORD-DEMO-FAILED-" + shortId();
		}
		if (type == OutboxDebugEventType.DEAD) {
			return "ORD-DEMO-DEAD-" + shortId();
		}
		return "ORD-DEMO-IMMEDIATE-FAIL-" + shortId();
	}

	/**
	 * 构造一条标准的支付成功 outbox 记录。
	 */
	private OutboxEventEntity buildPaymentEvent(String orderNo, Instant paidAt) {
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setEventId(UUID.randomUUID().toString());
		entity.setAggregateType(DEBUG_AGGREGATE_TYPE);
		entity.setAggregateId(orderNo);
		entity.setEventType(PAYMENT_SUCCEEDED_EVENT_TYPE);
		entity.setTopic(kafkaTopicsProperties.getTopics().getPaymentSucceeded());
		entity.setMessageKey(orderNo);
		entity.setPayload(objectMapper.valueToTree(new PaymentSucceededEvent(
			orderNo,
			new BigDecimal("99.90"),
			paidAt
		)));
		return entity;
	}

	/**
	 * 把新生成的 outbox 实体转成后台页面可直接展示的结构。
	 */
	private OutboxEventAdminVO toAdminVO(OutboxEventEntity entity) {
		return new OutboxEventAdminVO(
			entity.getId(),
			entity.getEventId(),
			entity.getAggregateType(),
			entity.getAggregateId(),
			entity.getEventType(),
			entity.getTopic(),
			entity.getMessageKey(),
			entity.getStatus(),
			entity.getRetryCount(),
			entity.getNextRetryAt(),
			entity.getLastError(),
			entity.getSentAt(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}

	/**
	 * 生成一个短后缀，方便本地多次点按钮时仍能快速区分不同调试数据。
	 */
	private String shortId() {
		return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
