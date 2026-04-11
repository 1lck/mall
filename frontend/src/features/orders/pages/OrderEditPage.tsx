import { Button, Result, message } from 'antd'
import { useLoaderData, useNavigate, useParams } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { OrderForm } from '../components/OrderForm'
import { updateOrder } from '../service'
import type { OrderRecord, UpdateOrderInput } from '../types'

export function OrderEditPage() {
  const navigate = useNavigate()
  const { orderId = '' } = useParams()
  const order = useLoaderData() as OrderRecord | null

  const handleSubmit = async (values: UpdateOrderInput) => {
    try {
      await updateOrder(orderId, values)
      message.success('订单更新成功。')
      navigate('/orders')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '订单更新失败。')
    }
  }

  if (!order) {
    return (
      <Result
        status="404"
        title="订单不存在"
        subTitle="当前未找到对应的订单记录。"
        extra={
          <Button type="primary" onClick={() => navigate('/orders')}>
            返回订单列表
          </Button>
        }
      />
    )
  }

  return (
    <OrderForm
      mode="edit"
      orderSummary={{
        id: order.id,
        orderNo: order.orderNo,
        userId: order.userId,
        createdAt: order.createdAt,
        updatedAt: order.updatedAt,
      }}
      initialValues={{
        totalAmount: order.totalAmount,
        status: order.status,
        remark: order.remark,
      }}
      onSubmit={handleSubmit}
    />
  )
}
