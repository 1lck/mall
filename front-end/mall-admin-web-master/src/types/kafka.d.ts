/** Kafka 分区状态 */
export type KafkaPartitionStatus = 'CAUGHT_UP' | 'LAGGING' | 'UNASSIGNED'

/** Kafka topic 状态 */
export type KafkaTopicStatus = 'HEALTHY' | 'LAGGING' | 'MISSING'

/** Kafka 控制台摘要 */
export type KafkaConsoleSummary = {
  /** 观察中的 topic 数量 */
  topicCount: number
  /** 已存在 topic 数量 */
  existingTopicCount: number
  /** 有积压的 topic 数量 */
  laggingTopicCount: number
  /** 已存在 topic 的总分区数 */
  totalPartitionCount: number
  /** 当前总 lag */
  totalLag: number
}

/** Kafka 分区视图 */
export type KafkaPartitionView = {
  /** 分区编号 */
  partition: number
  /** 末尾 offset */
  endOffset: number
  /** 已提交 offset */
  committedOffset: number | null
  /** lag */
  lag: number
  /** 状态 */
  status: KafkaPartitionStatus
}

/** Kafka topic 视图 */
export type KafkaTopicView = {
  /** topic 名称 */
  topicName: string
  /** 配置期望分区数 */
  configuredPartitions: number | null
  /** topic 是否已存在 */
  exists: boolean
  /** 实际分区数 */
  actualPartitions: number
  /** topic 状态 */
  status: KafkaTopicStatus
  /** topic 总 lag */
  totalLag: number
  /** 分区列表 */
  partitions: KafkaPartitionView[]
}

/** Kafka 控制台总览 */
export type KafkaConsoleOverview = {
  /** Kafka 是否启用 */
  enabled: boolean
  /** bootstrap servers */
  bootstrapServers: string
  /** 消费组 */
  consumerGroup: string
  /** 摘要 */
  summary: KafkaConsoleSummary
  /** topic 列表 */
  topics: KafkaTopicView[]
}

/** Kafka 消息值格式 */
export type KafkaMessageValueFormat = 'JSON' | 'TEXT' | 'EMPTY'

/** Kafka 单条消息 */
export type KafkaMessageView = {
  /** topic */
  topic: string
  /** 分区 */
  partition: number
  /** offset */
  offset: number
  /** ISO 时间戳 */
  timestamp: string
  /** key */
  key: string | null
  /** value */
  value: string | null
  /** value 格式 */
  valueFormat: KafkaMessageValueFormat
}

/** Kafka 消息预览响应 */
export type KafkaTopicMessages = {
  /** topic */
  topic: string
  /** 当前过滤分区 */
  partition: number | null
  /** 限制条数 */
  limit: number
  /** 消息列表 */
  messages: KafkaMessageView[]
}
