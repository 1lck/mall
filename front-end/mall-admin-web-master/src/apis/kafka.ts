import http from '@/utils/http'
import type { KafkaConsoleOverview, KafkaTopicMessages } from '@/types/kafka'

/** 读取 Kafka 控制台总览 */
export function getKafkaConsoleOverviewAPI() {
  return http<KafkaConsoleOverview>({
    method: 'GET',
    url: '/api/v1/admin/kafka-console/overview',
  })
}

/** 读取指定 topic 最近消息 */
export function getKafkaTopicMessagesAPI(params: {
  topic: string
  partition?: number
  limit?: number
}) {
  return http<KafkaTopicMessages>({
    method: 'GET',
    url: '/api/v1/admin/kafka-console/messages',
    params,
  })
}
