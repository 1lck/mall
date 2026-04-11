import { Alert, Descriptions, Drawer, Empty, Skeleton, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'

import { ApiError } from '../../../shared/api/http'
import { getOrderById } from '../service'
import type { OrderRecord } from '../types'
import { formatOrderDateTime, orderStatusColors, orderStatusLabels } from '../utils'

interface OrderDetailDrawerProps {
  open: boolean
  orderId: number | null
  onClose: () => void
}

export function OrderDetailDrawer({ open, orderId, onClose }: OrderDetailDrawerProps) {
  const [order, setOrder] = useState<OrderRecord | null>(null)
  const [loading, setLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function loadOrderDetail(targetOrderId: number, isActive: () => boolean) {
    setLoading(true)
    setErrorMessage(null)
    setOrder(null)

    try {
      const result = await getOrderById(targetOrderId)

      if (!isActive()) {
        return
      }

      setOrder(result)
    } catch (error) {
      if (!isActive()) {
        return
      }

      if (error instanceof ApiError && error.status === 404) {
        setErrorMessage('订单不存在或已被删除。')
        return
      }

      setErrorMessage(error instanceof Error ? error.message : '订单详情加载失败。')
    } finally {
      if (isActive()) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    if (!open || orderId === null) {
      return
    }

    let active = true
    void loadOrderDetail(orderId, () => active)

    return () => {
      active = false
    }
  }, [open, orderId])

  return (
    <Drawer title="订单详情" open={open} size="large" onClose={onClose} destroyOnHidden>
      {loading ? <Skeleton active paragraph={{ rows: 8 }} /> : null}

      {!loading && errorMessage ? <Alert type="warning" message={errorMessage} showIcon /> : null}

      {!loading && !errorMessage && !order ? <Empty description="暂无订单详情" /> : null}

      {!loading && !errorMessage && order ? (
        <Descriptions column={1} bordered size="middle">
          <Descriptions.Item label="数据库 ID">{order.id}</Descriptions.Item>
          <Descriptions.Item label="订单编号">{order.orderNo}</Descriptions.Item>
          <Descriptions.Item label="用户 ID">{order.userId}</Descriptions.Item>
          <Descriptions.Item label="订单总金额">¥{order.totalAmount.toFixed(2)}</Descriptions.Item>
          <Descriptions.Item label="订单状态">
            <Tag color={orderStatusColors[order.status]}>{orderStatusLabels[order.status]}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="备注">
            <Typography.Text>{order.remark || '无'}</Typography.Text>
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">{formatOrderDateTime(order.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{formatOrderDateTime(order.updatedAt)}</Descriptions.Item>
        </Descriptions>
      ) : null}
    </Drawer>
  )
}
