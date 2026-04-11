import { message } from 'antd'
import { useNavigate } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { ProductForm } from '../components/ProductForm'
import { createProduct } from '../service'
import type { CreateProductInput } from '../types'

export function ProductCreatePage() {
  const navigate = useNavigate()

  const handleSubmit = async (values: CreateProductInput) => {
    try {
      await createProduct(values)
      message.success('商品创建成功。')
      navigate('/products')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '商品创建失败。')
    }
  }

  return <ProductForm mode="create" onSubmit={handleSubmit} />
}
