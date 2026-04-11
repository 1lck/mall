import type { OrderStatus } from './types'

export const orderStatusLabels: Record<OrderStatus, string> = {
  CREATED: '已创建',
  PAID: '已支付',
  CANCELLED: '已取消',
}

export const orderStatusColors: Record<OrderStatus, string> = {
  CREATED: 'blue',
  PAID: 'green',
  CANCELLED: 'default',
}

export function formatOrderDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'medium',
    timeZone: 'Asia/Shanghai',
  }).format(new Date(value))
}
