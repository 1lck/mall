import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'

import { OrderForm } from './OrderForm'

describe('OrderForm', () => {
  it('renders create mode fields', () => {
    render(
      <AntdApp>
        <OrderForm
          mode="create"
          currentUser={{
            id: 1,
            username: 'alice',
            nickname: 'Alice',
            role: 'USER',
            status: 'ACTIVE',
            createdAt: '2026-04-11T09:00:00.000Z',
          }}
          onSubmit={async () => {}}
        />
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /新建订单/i })).toBeInTheDocument()
    expect(screen.getByText(/^Alice$/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/用户 ID/i)).not.toBeInTheDocument()
    expect(screen.getByLabelText(/订单总金额/i)).toBeInTheDocument()
  })

  it('renders the current user role label in create mode', () => {
    render(
      <AntdApp>
        <OrderForm
          mode="create"
          currentUser={{
            id: 3,
            username: 'admin',
            nickname: 'admin',
            role: 'ADMIN',
            status: 'ACTIVE',
            createdAt: '2026-04-11T12:35:47.410701Z',
          }}
          onSubmit={async () => {}}
        />
      </AntdApp>,
    )

    expect(screen.getByText('管理员')).toBeInTheDocument()
  })

  it('renders edit mode fields with initial values', () => {
    render(
      <AntdApp>
        <OrderForm
          mode="edit"
          orderSummary={{
            id: 1,
            orderNo: 'ORD202604110756302B5878',
            userId: 1001,
            createdAt: '2026-04-11T07:56:30.754987Z',
            updatedAt: '2026-04-11T07:56:30.754987Z',
          }}
          initialValues={{
            totalAmount: 299.5,
            status: 'PAID',
            remark: 'updated order',
          }}
          onSubmit={async () => {}}
        />
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /编辑订单/i })).toBeInTheDocument()
    expect(screen.getByText(/ORD202604110756302B5878/i)).toBeInTheDocument()
    expect(screen.getByDisplayValue('299.50')).toBeInTheDocument()
  })
})
