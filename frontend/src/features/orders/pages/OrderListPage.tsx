import { Button, Card, Flex, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { useState } from 'react'
import { useLoaderData, useNavigate, useRevalidator } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { deleteOrder, listOrders } from '../service'
import type { OrderRecord } from '../types'
import { OrderDetailDrawer } from '../components/OrderDetailDrawer'
import { formatOrderDateTime, orderStatusColors, orderStatusLabels } from '../utils'

export function OrderListPage() {
  const navigate = useNavigate()
  const revalidator = useRevalidator()
  const orders = useLoaderData() as Awaited<ReturnType<typeof listOrders>>
  const [detailOrderId, setDetailOrderId] = useState<number | null>(null)

  const handleDelete = async (orderId: number) => {
    try {
      await deleteOrder(orderId)
      message.success('订单已删除。')

      if (detailOrderId === orderId) {
        setDetailOrderId(null)
      }

      void revalidator.revalidate()
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '删除订单失败。')
    }
  }

  return (
    <>
      <Space size={20} orientation="vertical" className="page-stack">
        <Flex justify="space-between" align="center" gap={16}>
          <div>
            <Typography.Title level={2}>订单列表</Typography.Title>
            <Typography.Paragraph type="secondary">
              当前页面已接入真实订单接口，支持查看、新建、编辑和删除。
            </Typography.Paragraph>
          </div>
          <Button type="primary" size="large" onClick={() => navigate('/orders/new')}>
            新建订单
          </Button>
        </Flex>

        <Card variant="borderless">
          <Table<OrderRecord>
            rowKey="id"
            loading={revalidator.state === 'loading'}
            pagination={false}
            dataSource={orders}
            columns={[
              {
                title: '数据库 ID',
                dataIndex: 'id',
              },
              {
                title: '订单编号',
                dataIndex: 'orderNo',
              },
              {
                title: '用户 ID',
                dataIndex: 'userId',
              },
              {
                title: '总金额',
                dataIndex: 'totalAmount',
                render: (amount: number) => `¥${amount.toFixed(2)}`,
              },
              {
                title: '状态',
                dataIndex: 'status',
                render: (status: OrderRecord['status']) => (
                  <Tag color={orderStatusColors[status]}>{orderStatusLabels[status]}</Tag>
                ),
              },
              {
                title: '备注',
                dataIndex: 'remark',
                render: (remark?: string) => remark || '-',
              },
              {
                title: '创建时间',
                dataIndex: 'createdAt',
                render: (value: string) => formatOrderDateTime(value),
              },
              {
                title: '更新时间',
                dataIndex: 'updatedAt',
                render: (value: string) => formatOrderDateTime(value),
              },
              {
                title: '操作',
                key: 'actions',
                render: (_, order) => (
                  <Space size={4}>
                    <Button type="link" onClick={() => setDetailOrderId(order.id)}>
                      查看
                    </Button>
                    <Button type="link" onClick={() => navigate(`/orders/${order.id}/edit`)}>
                      编辑
                    </Button>
                    <Popconfirm
                      title="删除订单"
                      description="删除后无法恢复，请确认是否继续。"
                      okText="确认"
                      cancelText="取消"
                      onConfirm={() => handleDelete(order.id)}
                    >
                      <Button type="link" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
          />
        </Card>
      </Space>

      <OrderDetailDrawer
        open={detailOrderId !== null}
        orderId={detailOrderId}
        onClose={() => setDetailOrderId(null)}
      />
    </>
  )
}
