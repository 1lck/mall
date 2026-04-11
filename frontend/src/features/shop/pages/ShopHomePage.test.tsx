import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

import { ShopHomePage } from './ShopHomePage'
import { listProducts } from '../../products/service'
import { ShopLayout } from '../../../layouts/ShopLayout'

function mockJsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('ShopHomePage', () => {
  it('renders category tabs, personal center entry, and on-sale products only', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/products')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 1,
              productNo: 'PRD1',
              name: 'iPhone 16',
              categoryName: '数码',
              price: 6999,
              stock: 88,
              status: 'ON_SALE',
              description: '旗舰新机',
              imageUrl: 'http://127.0.0.1:9000/mall-product-images/product-images/2026/04/11/iphone16.png',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
            {
              id: 2,
              productNo: 'PRD2',
              name: '运动夹克',
              categoryName: '潮服',
              price: 499,
              stock: 16,
              status: 'ON_SALE',
              description: '轻量防风',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
            {
              id: 3,
              productNo: 'PRD3',
              name: '测试草稿商品',
              categoryName: '数码',
              price: 1,
              stock: 1,
              status: 'DRAFT',
              description: '不应展示',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
          ],
        })
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    const router = createMemoryRouter(
      [
        {
          path: '/shop',
          element: <ShopLayout />,
          children: [
            {
              index: true,
              loader: async () => listProducts(),
              element: <ShopHomePage />,
            },
          ],
        },
      ],
      { initialEntries: ['/shop'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    expect(await screen.findByRole('link', { name: /个人中心/i })).toBeInTheDocument()
    expect(await screen.findByRole('tab', { name: /精选/i })).toBeInTheDocument()
    expect(await screen.findByRole('tab', { name: /数码/i })).toBeInTheDocument()
    expect(await screen.findByRole('tab', { name: /潮服/i })).toBeInTheDocument()
    expect(await screen.findByText(/iPhone 16/i)).toBeInTheDocument()
    expect(await screen.findByRole('img', { name: /iPhone 16/i })).toBeInTheDocument()
    expect(await screen.findByText(/运动夹克/i)).toBeInTheDocument()
    expect(screen.queryByText(/测试草稿商品/i)).not.toBeInTheDocument()
  })

  it('filters products by category tab', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/products')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 1,
              productNo: 'PRD1',
              name: 'iPhone 16',
              categoryName: '数码',
              price: 6999,
              stock: 88,
              status: 'ON_SALE',
              description: '旗舰新机',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
            {
              id: 2,
              productNo: 'PRD2',
              name: '运动夹克',
              categoryName: '潮服',
              price: 499,
              stock: 16,
              status: 'ON_SALE',
              description: '轻量防风',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
          ],
        })
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    const router = createMemoryRouter(
      [
        {
          path: '/shop',
          element: <ShopLayout />,
          children: [
            {
              index: true,
              loader: async () => listProducts(),
              element: <ShopHomePage />,
            },
          ],
        },
      ],
      { initialEntries: ['/shop'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    const user = userEvent.setup()
    await user.click(await screen.findByRole('tab', { name: /数码/i }))

    expect(await screen.findByText(/iPhone 16/i)).toBeInTheDocument()
    expect(screen.queryByText(/运动夹克/i)).not.toBeInTheDocument()
  })
})
