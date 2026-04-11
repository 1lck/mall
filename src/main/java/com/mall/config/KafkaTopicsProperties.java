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

	private boolean enabled;
	private String bootstrapServers;
	private String consumerGroup;
	private Topics topics = new Topics();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBootstrapServers() {
		return bootstrapServers;
	}

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public String getConsumerGroup() {
		return consumerGroup;
	}

	public void setConsumerGroup(String consumerGroup) {
		this.consumerGroup = consumerGroup;
	}

	public Topics getTopics() {
		return topics;
	}

	public void setTopics(Topics topics) {
		this.topics = topics;
	}

	public static class Topics {

		// 这里把所有 topic 名称收口到一起，调用方拿配置时更清晰。
		private String orderCreated;
		private String orderCancelled;
		private String inventoryReserved;
		private String inventoryReleased;

		public String getOrderCreated() {
			return orderCreated;
		}

		public void setOrderCreated(String orderCreated) {
			this.orderCreated = orderCreated;
		}

		public String getOrderCancelled() {
			return orderCancelled;
		}

		public void setOrderCancelled(String orderCancelled) {
			this.orderCancelled = orderCancelled;
		}

		public String getInventoryReserved() {
			return inventoryReserved;
		}

		public void setInventoryReserved(String inventoryReserved) {
			this.inventoryReserved = inventoryReserved;
		}

		public String getInventoryReleased() {
			return inventoryReleased;
		}

		public void setInventoryReleased(String inventoryReleased) {
			this.inventoryReleased = inventoryReleased;
		}
	}
}
