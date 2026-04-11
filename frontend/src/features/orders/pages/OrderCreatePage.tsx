import { message } from 'antd'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../../auth/context'
import { ApiError } from '../../../shared/api/http'
import { OrderForm } from '../components/OrderForm'
import { createOrder } from '../service'
import type { CreateOrderInput } from '../types'

export function OrderCreatePage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const handleSubmit = async (values: CreateOrderInput) => {
    try {
      await createOrder(values)
      message.success('订单创建成功。')
      navigate('/orders')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '订单创建失败。')
    }
  }

  if (!user) {
    return null
  }

  return <OrderForm mode="create" currentUser={user} onSubmit={handleSubmit} />
}
