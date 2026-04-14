import http from '@/utils/http'
import type { CommonPage } from '@/types/common'
import type { OutboxDebugEventType, OutboxEvent, OutboxQueryParam } from '@/types/outbox'

function paginate<T>(items: T[], pageNum: number, pageSize: number): CommonPage<T> {
  const safePageNum = Math.max(pageNum, 1)
  const safePageSize = Math.max(pageSize, 1)
  const start = (safePageNum - 1) * safePageSize
  const list = items.slice(start, start + safePageSize)
  const total = items.length

  return {
    pageNum: safePageNum,
    pageSize: safePageSize,
    total,
    totalPage: Math.max(Math.ceil(total / safePageSize), 1),
    list,
  }
}

export async function getOutboxEventListAPI(params: OutboxQueryParam) {
  const res = await http<OutboxEvent[]>({
    method: 'GET',
    url: '/api/v1/admin/outbox-events',
    params: {
      status: params.status,
      keyword: params.keyword?.trim() || undefined,
      limit: 200,
    },
  })

  return {
    code: 200,
    message: 'ok',
    data: paginate(res.data, params.pageNum, params.pageSize),
    rawList: res.data,
  }
}

export function createOutboxDemoBatchAPI() {
  return http<OutboxEvent[]>({
    method: 'POST',
    url: '/api/v1/admin/outbox-debug/demo-batch',
  })
}

export function createSingleOutboxDebugEventAPI(data: {
  type: OutboxDebugEventType
  aggregateId?: string
}) {
  return http<OutboxEvent>({
    method: 'POST',
    url: '/api/v1/admin/outbox-debug/single',
    data,
  })
}
