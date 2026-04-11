import { Button, Result, message } from 'antd'
import { useLoaderData, useNavigate, useParams } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { ProductForm } from '../components/ProductForm'
import { updateProduct } from '../service'
import type { ProductRecord, UpdateProductInput } from '../types'

export function ProductEditPage() {
  const navigate = useNavigate()
  const { productId = '' } = useParams()
  const product = useLoaderData() as ProductRecord | null
  const initialValues = product
    ? {
        name: product.name,
        categoryName: product.categoryName,
        price: product.price,
        stock: product.stock,
        status: product.status,
        description: product.description,
        imageUrl: product.imageUrl,
      }
    : null

  const handleSubmit = async (values: UpdateProductInput) => {
    try {
      await updateProduct(productId, values)
      message.success('商品更新成功。')
      navigate('/products')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '商品更新失败。')
    }
  }

  if (!initialValues) {
    return (
      <Result
        status="404"
        title="商品不存在"
        subTitle="当前未找到对应的商品记录。"
        extra={
          <Button type="primary" onClick={() => navigate('/products')}>
            返回商品列表
          </Button>
        }
      />
    )
  }

  const currentProduct = product as ProductRecord

  const productSummary = {
    id: currentProduct.id,
    productNo: currentProduct.productNo,
    createdAt: currentProduct.createdAt,
    updatedAt: currentProduct.updatedAt,
  }

  return (
    <ProductForm
      mode="edit"
      summary={productSummary}
      initialValues={initialValues}
      onSubmit={handleSubmit}
    />
  )
}
