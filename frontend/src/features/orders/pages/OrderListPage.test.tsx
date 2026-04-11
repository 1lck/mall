import { App as AntdApp } from 'antd'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

import { OrderListPage } from './OrderListPage'
import { listOrders } from '../service'

function mockJsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('OrderListPage', () => {
  it('renders fetched orders and the create action', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/orders')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 1,
              orderNo: 'ORD202604110756302B5878',
              userId: 1001,
              totalAmount: 199.9,
              status: 'CREATED',
              remark: 'first order',
              createdAt: '2026-04-11T07:56:30.754987Z',
              updatedAt: '2026-04-11T07:56:30.754987Z',
            },
          ],
        })
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    const router = createMemoryRouter(
      [
        {
          path: '/',
          loader: async () => listOrders(),
          element: <OrderListPage />,
        },
      ],
      { initialEntries: ['/'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    expect(await screen.findByRole('button', { name: /新建订单/i })).toBeInTheDocument()
    expect(await screen.findByText(/ORD202604110756302B5878/i)).toBeInTheDocument()
  })

  it('opens the detail drawer and shows fetched order detail', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/orders')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 1,
              orderNo: 'ORD202604110756302B5878',
              userId: 1001,
              totalAmount: 199.9,
              status: 'CREATED',
              remark: 'first order',
              createdAt: '2026-04-11T07:56:30.754987Z',
              updatedAt: '2026-04-11T07:56:30.754987Z',
            },
          ],
        })
      }

      if (url.endsWith('/api/v1/orders/1')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: {
            id: 1,
            orderNo: 'ORD202604110756302B5878',
            userId: 1001,
            totalAmount: 199.9,
            status: 'CREATED',
            remark: 'first order',
            createdAt: '2026-04-11T07:56:30.754987Z',
            updatedAt: '2026-04-11T07:56:30.754987Z',
          },
        })
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    const router = createMemoryRouter(
      [
        {
          path: '/',
          loader: async () => listOrders(),
          element: <OrderListPage />,
        },
      ],
      { initialEntries: ['/'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    const user = userEvent.setup()
    await user.click(await screen.findByRole('button', { name: /查看/i }))

    const detailDialog = await screen.findByRole('dialog', { name: /订单详情/i })

    expect(detailDialog).toBeInTheDocument()
    expect(within(detailDialog).getByText(/first order/i)).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
