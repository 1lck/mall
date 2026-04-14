package com.mall.modules.outbox.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import com.mall.modules.outbox.vo.OutboxEventAdminVO;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

	/** 支付类 outbox 记录使用的聚合类型。 */
	private static final String PAYMENT_AGGREGATE_TYPE = "PAYMENT";
	/** 支付成功事件类型。 */
	private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "PAYMENT_SUCCEEDED";
	/** 用于稳定制造失败的调试事件类型。 */
	private static final String UNSUPPORTED_DEBUG_EVENT_TYPE = "DEBUG_UNSUPPORTED_EVENT";

	private final OutboxEventMapper outboxEventMapper;
	private final OutboxDispatchTrigger outboxDispatchTrigger;
	private final KafkaTopicsProperties kafkaTopicsProperties;
	private final ObjectMapper objectMapper;

	public OutboxDebugApplicationService(
		OutboxEventMapper outboxEventMapper,
		OutboxDispatchTrigger outboxDispatchTrigger,
		KafkaTopicsProperties kafkaTopicsProperties,
		ObjectMapper objectMapper
	) {
		this.outboxEventMapper = outboxEventMapper;
		this.outboxDispatchTrigger = outboxDispatchTrigger;
		this.kafkaTopicsProperties = kafkaTopicsProperties;
		this.objectMapper = objectMapper;
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
	 * 构造一条已经投递成功的历史消息，方便页面观察 SENT 状态。
	 */
	private OutboxEventAdminVO createSentDemoEvent(Instant now) {
		String orderNo = "ORD-DEMO-SENT-" + shortId();
		OutboxEventEntity entity = buildPaymentEvent(orderNo, now.minusSeconds(120));
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
		String orderNo = "ORD-DEMO-FAILED-" + shortId();
		OutboxEventEntity entity = buildPaymentEvent(orderNo, now.minusSeconds(60));
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
		String orderNo = "ORD-DEMO-DEAD-" + shortId();
		OutboxEventEntity entity = buildPaymentEvent(orderNo, now.minusSeconds(300));
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
		String orderNo = "ORD-DEMO-IMMEDIATE-FAIL-" + shortId();
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setEventId(UUID.randomUUID().toString());
		entity.setAggregateType(PAYMENT_AGGREGATE_TYPE);
		entity.setAggregateId(orderNo);
		entity.setEventType(UNSUPPORTED_DEBUG_EVENT_TYPE);
		entity.setTopic(kafkaTopicsProperties.getTopics().getPaymentSucceeded());
		entity.setMessageKey(orderNo);
		entity.setPayload(objectMapper.createObjectNode()
			.put("orderNo", orderNo)
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
	 * 构造一条标准的支付成功 outbox 记录。
	 */
	private OutboxEventEntity buildPaymentEvent(String orderNo, Instant paidAt) {
		OutboxEventEntity entity = new OutboxEventEntity();
		entity.setEventId(UUID.randomUUID().toString());
		entity.setAggregateType(PAYMENT_AGGREGATE_TYPE);
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
