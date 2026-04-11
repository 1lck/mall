import { Button, Card, Flex, Image, Popconfirm, Space, Table, Tag, Typography, message } from 'antd'
import { useNavigate, useLoaderData, useRevalidator } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { deleteProduct, listProducts } from '../service'
import type { ProductRecord, ProductStatus } from '../types'
import { formatOrderDateTime } from '../../orders/utils'

const statusColors: Record<ProductStatus, string> = {
  DRAFT: 'gold',
  ON_SALE: 'green',
  OFF_SHELF: 'default',
}

const statusLabels: Record<ProductStatus, string> = {
  DRAFT: '草稿',
  ON_SALE: '在售',
  OFF_SHELF: '已下架',
}

export function ProductListPage() {
  const navigate = useNavigate()
  const revalidator = useRevalidator()
  const products = useLoaderData() as Awaited<ReturnType<typeof listProducts>>

  const handleDelete = async (productId: number) => {
    try {
      await deleteProduct(productId)
      message.success('商品已删除。')
      void revalidator.revalidate()
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '删除商品失败。')
    }
  }

  return (
    <Space size={20} orientation="vertical" className="page-stack">
      <Flex justify="space-between" align="center" gap={16}>
        <div>
          <Typography.Title level={2}>商品列表</Typography.Title>
          <Typography.Paragraph type="secondary">
            当前页面已接入真实商品接口，支持新建、编辑和删除。
          </Typography.Paragraph>
        </div>
        <Button type="primary" size="large" onClick={() => navigate('/products/new')}>
          新建商品
        </Button>
      </Flex>

      <Card variant="borderless">
        <Table<ProductRecord>
          rowKey="id"
          loading={revalidator.state === 'loading'}
          pagination={false}
          dataSource={products}
          columns={[
            {
              title: '商品图片',
              dataIndex: 'imageUrl',
              render: (imageUrl?: string, product?: ProductRecord) =>
                imageUrl ? (
                  <Image
                    src={imageUrl}
                    alt={product?.name ?? '商品图片'}
                    width={56}
                    height={56}
                    style={{ objectFit: 'cover', borderRadius: 12 }}
                  />
                ) : (
                  '-'
                ),
            },
            {
              title: '商品编号',
              dataIndex: 'productNo',
            },
            {
              title: '商品名称',
              dataIndex: 'name',
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: (status: ProductStatus) => (
                <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>
              ),
            },
            {
              title: '价格',
              dataIndex: 'price',
              render: (price: number) => `¥${price.toFixed(2)}`,
            },
            {
              title: '库存',
              dataIndex: 'stock',
              render: (stock: number) => stock,
            },
            {
              title: '分类',
              dataIndex: 'categoryName',
            },
            {
              title: '更新时间',
              dataIndex: 'updatedAt',
              render: (value: string) => formatOrderDateTime(value),
            },
            {
              title: '操作',
              key: 'actions',
              render: (_, product) => (
                <Space size={4}>
                  <Button type="link" onClick={() => navigate(`/products/${product.id}/edit`)}>
                    编辑
                  </Button>
                  <Popconfirm
                    title="删除商品"
                    description="当前只会删除本地模拟数据，后续可接入真实删除接口。"
                    onConfirm={() => handleDelete(product.id)}
                    okText="确认"
                    cancelText="取消"
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
      <Card variant="borderless">
        <Typography.Paragraph type="secondary">
          商品创建时默认由后端落成草稿状态，编辑页可以修改为在售或已下架。
        </Typography.Paragraph>
      </Card>
    </Space>
  )
}
