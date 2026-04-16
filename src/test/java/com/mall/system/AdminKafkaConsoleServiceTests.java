package com.mall.system;

import com.mall.config.KafkaTopicsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminKafkaConsoleServiceTests {

	@Mock
	private KafkaConsoleProbe kafkaConsoleProbe;

	@Test
	void getOverviewShouldCombineConfiguredTopicsAndRuntimeLag() {
		KafkaTopicsProperties properties = buildKafkaProperties();
		AdminKafkaConsoleService service = new AdminKafkaConsoleService(properties, kafkaConsoleProbe);

		when(kafkaConsoleProbe.inspectTopics(List.of("mall.order.created", "mall.payment.succeeded"), "mall-learning-group"))
			.thenReturn(List.of(
				new KafkaConsoleProbe.TopicRuntimeSnapshot(
					"mall.order.created",
					true,
					List.of(
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(0, 120L, 120L),
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(1, 98L, 95L),
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(2, 40L, null)
					)
				),
				new KafkaConsoleProbe.TopicRuntimeSnapshot(
					"mall.payment.succeeded",
					true,
					List.of(
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(0, 55L, 55L),
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(1, 31L, 31L),
						new KafkaConsoleProbe.PartitionRuntimeSnapshot(2, 18L, 18L)
					)
				)
			));

		AdminKafkaConsoleOverviewResponse response = service.getOverview();

		assertThat(response.summary().topicCount()).isEqualTo(2);
		assertThat(response.summary().laggingTopicCount()).isEqualTo(1);
		assertThat(response.summary().totalLag()).isEqualTo(43L);
		assertThat(response.topics()).hasSize(2);
		assertThat(response.topics().get(0).topicName()).isEqualTo("mall.order.created");
		assertThat(response.topics().get(0).configuredPartitions()).isEqualTo(3);
		assertThat(response.topics().get(0).status()).isEqualTo("LAGGING");
		assertThat(response.topics().get(0).partitions().get(1).lag()).isEqualTo(3L);
		assertThat(response.topics().get(0).partitions().get(2).status()).isEqualTo("UNASSIGNED");
	}

	@Test
	void getTopicMessagesShouldPreserveRecentRecordsAndDetectJsonPayload() {
		KafkaTopicsProperties properties = buildKafkaProperties();
		AdminKafkaConsoleService service = new AdminKafkaConsoleService(properties, kafkaConsoleProbe);

		when(kafkaConsoleProbe.fetchRecentMessages("mall.order.created", null, 20))
			.thenReturn(List.of(
				new KafkaConsoleProbe.KafkaRecordSnapshot(
					"mall.order.created",
					1,
					99L,
					Instant.parse("2026-04-16T05:00:00Z").toEpochMilli(),
					"ORD-001",
					"{\"orderNo\":\"ORD-001\"}"
				),
				new KafkaConsoleProbe.KafkaRecordSnapshot(
					"mall.order.created",
					2,
					100L,
					Instant.parse("2026-04-16T05:01:00Z").toEpochMilli(),
					null,
					"1"
				)
			));

		AdminKafkaTopicMessagesResponse response = service.getTopicMessages("mall.order.created", null, 20);

		assertThat(response.topic()).isEqualTo("mall.order.created");
		assertThat(response.messages()).hasSize(2);
		assertThat(response.messages().get(0).valueFormat()).isEqualTo("JSON");
		assertThat(response.messages().get(1).key()).isNull();
		assertThat(response.messages().get(1).valueFormat()).isEqualTo("TEXT");
		assertThat(response.messages().get(1).partition()).isEqualTo(2);
	}

	private KafkaTopicsProperties buildKafkaProperties() {
		KafkaTopicsProperties properties = new KafkaTopicsProperties();
		properties.setEnabled(true);
		properties.setBootstrapServers("localhost:9094");
		properties.setConsumerGroup("mall-learning-group");

		KafkaTopicsProperties.Topics topics = new KafkaTopicsProperties.Topics();
		topics.setOrderCreated("mall.order.created");
		topics.setPaymentSucceeded("mall.payment.succeeded");
		properties.setTopics(topics);

		KafkaTopicsProperties.Partitions partitions = new KafkaTopicsProperties.Partitions();
		partitions.setOrderCreated(3);
		partitions.setPaymentSucceeded(3);
		properties.setPartitions(partitions);
		return properties;
	}
}
