package com.mall.infrastructure.messaging.kafka;

import com.mall.modules.order.event.OrderCreatedEvent;
import com.mall.modules.order.persistence.mapper.OrderEventRecordMapper;
import com.mall.modules.payment.domain.PaymentStatus;
import com.mall.modules.payment.persistence.entity.PaymentRecordEntity;
import com.mall.modules.payment.persistence.mapper.PaymentRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第一阶段只做最小消费者。
 *
 * <p>它暂时不做库存扣减、不做支付编排，
 * 只负责帮我们确认一件事：
 * “订单创建事件已经真的从 Kafka 被消费到了。”</p>
 */
@Component
@ConditionalOnProperty(name = "mall.kafka.enabled", havingValue = "true")
public class OrderCreatedLoggingConsumer {

	private static final Logger log = LoggerFactory.getLogger(OrderCreatedLoggingConsumer.class);
	private static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";
	private final OrderEventRecordMapper orderEventRecordRepository;
	private final PaymentRecordMapper paymentRecordRepository;

	public OrderCreatedLoggingConsumer(
		OrderEventRecordMapper orderEventRecordRepository,
		PaymentRecordMapper paymentRecordRepository
	) {
		this.orderEventRecordRepository = orderEventRecordRepository;
		this.paymentRecordRepository = paymentRecordRepository;
	}

	@KafkaListener(
		topics = "${mall.kafka.topics.order-created}",
		groupId = "${mall.kafka.consumer-group}",
		containerFactory = "orderCreatedKafkaListenerContainerFactory"
	)
	@Transactional
	public void onOrderCreated(OrderCreatedEvent event, Acknowledgment acknowledgment) {
		// 直接向数据库抢占“处理资格”：
		// 插入成功的消费者才能继续处理，插入失败说明已有其他消费者抢先处理过。
		// 这样不会再出现“两个消费者都先查到空，然后都开始创建支付记录”的窗口。
		boolean claimed = orderEventRecordRepository.claimProcessing(
			ORDER_CREATED_EVENT_TYPE,
			event.orderNo()
		) == 1;
		if (!claimed) {
			// 已被别人处理过时，同样走 afterCommit ack，
			// 保持“事务结束后再提交 offset”的时序一致性。
			log.info("Kafka order created event skipped because it was already processed: orderNo={}", event.orderNo());
			KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
			return;
		}

		// 这一步开始接入一个最小支付流程：
		// 当订单创建事件第一次被消费时，先为这笔订单生成一条待支付记录。
		PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
		paymentRecord.setOrderNo(event.orderNo());
		paymentRecord.setAmount(event.totalAmount());
		paymentRecord.setStatus(PaymentStatus.PENDING);
		paymentRecordRepository.save(paymentRecord);

		// 当前真实业务仍然保持极简：
		// 先抢到处理资格，再创建支付记录，避免并发下重复执行业务副作用。
		log.info(
			"Kafka order created event consumed and payment record created: orderId={}, orderNo={}, userId={}, totalAmount={}",
			event.orderId(),
			event.orderNo(),
			event.userId(),
			event.totalAmount()
		);

		// 这里不直接 ack。
		// 只有当前事务真正提交成功之后，才会由 afterCommit 回调去确认这条 Kafka 消息。
		KafkaAcknowledgmentSupport.acknowledgeAfterCommit(acknowledgment);
	}
}
