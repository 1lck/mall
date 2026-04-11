export type OrderStatus = 'CREATED' | 'PAID' | 'CANCELLED'

export interface OrderRecord {
  id: number
  orderNo: string
  userId: number
  totalAmount: number
  status: OrderStatus
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface CreateOrderInput {
  totalAmount: number
  remark?: string
}

export interface UpdateOrderInput {
  totalAmount: number
  status: OrderStatus
  remark?: string
}

export interface EditOrderSummary {
  id: number
  orderNo: string
  userId: number
  createdAt: string
  updatedAt: string
}
