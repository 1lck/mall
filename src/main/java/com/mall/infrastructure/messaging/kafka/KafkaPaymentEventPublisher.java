package com.mall.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.config.KafkaTopicsProperties;
import com.mall.modules.payment.application.PaymentEventPublisher;
import com.mall.modules.payment.event.PaymentSucceededEvent;
import com.mall.modules.outbox.domain.OutboxEventStatus;
import com.mall.modules.outbox.persistence.entity.OutboxEventEntity;
import com.mall.modules.outbox.persistence.mapper.OutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 基于 Kafka 的支付事件发布器。
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaPaymentEventPublisher.class);
	private static final String AGGREGATE_TYPE = "PAYMENT";
	private static final String EVENT_TYPE = "PAYMENT_SUCCEEDED";

	private final KafkaTopicsProperties kafkaTopicsProperties;
	private final OutboxEventMapper outboxEventRepository;
	private final ObjectMapper objectMapper;

	public KafkaPaymentEventPublisher(
		KafkaTopicsProperties kafkaTopicsProperties,
		OutboxEventMapper outboxEventRepository,
		ObjectMapper objectMapper
	) {
		this.kafkaTopicsProperties = kafkaTopicsProperties;
		this.outboxEventRepository = outboxEventRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publishPaymentSucceeded(PaymentSucceededEvent event) {
		String topic = kafkaTopicsProperties.getTopics().getPaymentSucceeded();
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
		log.info(
			"Payment succeeded event saved to outbox: topic={}, orderNo={}, eventType={}",
			topic,
			event.orderNo(),
			EVENT_TYPE
		);
	}
}
