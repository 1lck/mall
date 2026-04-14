package com.mall.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 把 YAML 里的 {@code mall.kafka.*} 配置映射成 Java 对象。
 *
 * <p>控制器和后续 Kafka 相关组件都可以从这里读取统一的 topic 名称和连接信息，
 * 避免把配置散落在代码各处。</p>
 */
@ConfigurationProperties(prefix = "mall.kafka")
public class KafkaTopicsProperties {

	/** 是否启用 Kafka 消息能力。 */
	private boolean enabled;
	/** Kafka 服务连接地址。 */
	private String bootstrapServers;
	/** 消费者组名称。 */
	private String consumerGroup;
	/** 业务 topic 名称集合。 */
	private Topics topics = new Topics();

	/**
	 * 返回 Kafka 能力是否启用。
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * 设置 Kafka 能力是否启用。
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * 返回 Kafka 服务连接地址。
	 */
	public String getBootstrapServers() {
		return bootstrapServers;
	}

	/**
	 * 设置 Kafka 服务连接地址。
	 */
	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	/**
	 * 返回消费者组名称。
	 */
	public String getConsumerGroup() {
		return consumerGroup;
	}

	/**
	 * 设置消费者组名称。
	 */
	public void setConsumerGroup(String consumerGroup) {
		this.consumerGroup = consumerGroup;
	}

	/**
	 * 返回业务 topic 配置集合。
	 */
	public Topics getTopics() {
		return topics;
	}

	/**
	 * 设置业务 topic 配置集合。
	 */
	public void setTopics(Topics topics) {
		this.topics = topics;
	}

	/**
	 * Kafka 业务 topic 配置项。
	 */
	public static class Topics {

		// 这里把所有 topic 名称收口到一起，调用方拿配置时更清晰。
		/** 订单创建事件 topic。 */
		private String orderCreated;
		/** 订单取消事件 topic。 */
		private String orderCancelled;
		/** 支付成功事件 topic。 */
		private String paymentSucceeded;
		/** 库存锁定事件 topic。 */
		private String inventoryReserved;
		/** 库存释放事件 topic。 */
		private String inventoryReleased;

		/**
		 * 返回订单创建事件 topic。
		 */
		public String getOrderCreated() {
			return orderCreated;
		}

		/**
		 * 设置订单创建事件 topic。
		 */
		public void setOrderCreated(String orderCreated) {
			this.orderCreated = orderCreated;
		}

		/**
		 * 返回订单取消事件 topic。
		 */
		public String getOrderCancelled() {
			return orderCancelled;
		}

		/**
		 * 设置订单取消事件 topic。
		 */
		public void setOrderCancelled(String orderCancelled) {
			this.orderCancelled = orderCancelled;
		}

		/**
		 * 返回支付成功事件 topic。
		 */
		public String getPaymentSucceeded() {
			return paymentSucceeded;
		}

		/**
		 * 设置支付成功事件 topic。
		 */
		public void setPaymentSucceeded(String paymentSucceeded) {
			this.paymentSucceeded = paymentSucceeded;
		}

		/**
		 * 返回库存锁定事件 topic。
		 */
		public String getInventoryReserved() {
			return inventoryReserved;
		}

		/**
		 * 设置库存锁定事件 topic。
		 */
		public void setInventoryReserved(String inventoryReserved) {
			this.inventoryReserved = inventoryReserved;
		}

		/**
		 * 返回库存释放事件 topic。
		 */
		public String getInventoryReleased() {
			return inventoryReleased;
		}

		/**
		 * 设置库存释放事件 topic。
		 */
		public void setInventoryReleased(String inventoryReleased) {
			this.inventoryReleased = inventoryReleased;
		}
	}
}
