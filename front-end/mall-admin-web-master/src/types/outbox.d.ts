/** outbox 投递状态 */
export type OutboxEventStatus = 'PENDING' | 'SENT' | 'FAILED' | 'DEAD'

/** outbox 调试消息类型 */
export type OutboxDebugEventType = 'SENT' | 'FAILED' | 'DEAD' | 'IMMEDIATE_FAIL'

/** outbox 观察页记录 */
export type OutboxEvent = {
  /** outbox 主键 */
  id: number
  /** 消息唯一 id */
  eventId: string
  /** 聚合类型 */
  aggregateType: string
  /** 聚合实例 id */
  aggregateId: string
  /** 事件类型 */
  eventType: string
  /** Kafka topic */
  topic: string
  /** Kafka message key */
  messageKey: string
  /** 当前投递状态 */
  status: OutboxEventStatus
  /** 已重试次数 */
  retryCount: number
  /** 下次允许重试时间 */
  nextRetryAt?: string
  /** 最近一次失败原因 */
  lastError?: string
  /** 成功投递时间 */
  sentAt?: string
  /** 创建时间 */
  createdAt: string
  /** 更新时间 */
  updatedAt: string
}

/** outbox 观察页查询参数 */
export type OutboxQueryParam = {
  /** 当前页码 */
  pageNum: number
  /** 每页条数 */
  pageSize: number
  /** 状态筛选 */
  status?: OutboxEventStatus
  /** 关键字筛选 */
  keyword?: string
}
