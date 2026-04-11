import { request } from '../../shared/api/http'
import type { CreateOrderInput, OrderRecord, UpdateOrderInput } from './types'

export async function listOrders(): Promise<OrderRecord[]> {
  return request<OrderRecord[]>('/api/v1/orders')
}

export async function getOrderById(orderId: number | string): Promise<OrderRecord> {
  return request<OrderRecord>(`/api/v1/orders/${orderId}`)
}

export async function createOrder(input: CreateOrderInput): Promise<OrderRecord> {
  return request<OrderRecord>('/api/v1/orders', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function updateOrder(orderId: number | string, input: UpdateOrderInput): Promise<OrderRecord> {
  return request<OrderRecord>(`/api/v1/orders/${orderId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export async function deleteOrder(orderId: number | string): Promise<string> {
  return request<string>(`/api/v1/orders/${orderId}`, {
    method: 'DELETE',
  })
}
