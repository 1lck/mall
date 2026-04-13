package com.mall.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.application.PaymentEventPublisher;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import com.mall.modules.outbox.application.OutboxDispatchTrigger;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 支付成功事件发布器。
 *
 * <p>名字上虽然还是 publisher，但现在它不再直接发 Kafka。
 * 它真正做的是：把支付成功事件转换成一条 outbox 记录，
 * 让当前业务事务把“待发送消息”先可靠落库。</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);
	private static final String AGGREGATE_TYPE = "PAYMENT";
	private static final String EVENT_TYPE = "PAYMENT_SUCCEEDED";

	private final KafkaTopicsProperties kafkaTopicsProperties;
	private final OutboxEventMapper outboxEventRepository;
	private final OutboxDispatchTrigger outboxDispatchTrigger;
	private final ObjectMapper objectMapper;

	public KafkaPaymentEventPublisher(
		KafkaTopicsProperties kafkaTopicsProperties,
		OutboxEventMapper outboxEventRepository,
		OutboxDispatchTrigger outboxDispatchTrigger,
		ObjectMapper objectMapper
	) {
		this.kafkaTopicsProperties = kafkaTopicsProperties;
		this.outboxEventRepository = outboxEventRepository;
		this.outboxDispatchTrigger = outboxDispatchTrigger;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publishPaymentSucceeded(PaymentSucceededEvent event) {
		String topic = kafkaTopicsProperties.getTopics().getPaymentSucceeded();

		// 这里不直接 send Kafka。
		// 当前阶段先把“要发什么消息”完整写进 outbox 表，
		// 后面的扫描投递器再统一负责真正投递。
		OutboxEventEntity outboxEvent = new OutboxEventEntity();
		outboxEvent.setEventId(UUID.randomUUID().toString());
		outboxEvent.setAggregateType(AGGREGATE_TYPE);
		outboxEvent.setAggregateId(event.orderNo());
		outboxEvent.setEventType(EVENT_TYPE);
		outboxEvent.setTopic(topic);
		outboxEvent.setMessageKey(event.orderNo());
		outboxEvent.setPayload(objectMapper.valueToTree(event));
		outboxEvent.setStatus(OutboxEventStatus.PENDING);
		outboxEvent.setRetryCount(0);
		outboxEventRepository.save(outboxEvent);
		// 记录完 outbox 后立即申请一次“精确投递当前这条记录”。
		// 如果当前还在事务里，这次投递会被挂到 afterCommit；否则直接触发。
		outboxDispatchTrigger.requestDispatch(outboxEvent.getId());
		log.info(
			"Payment succeeded event saved to outbox: topic={}, orderNo={}, eventType={}",
			topic,
			event.orderNo(),
			EVENT_TYPE
		);
	}
}
