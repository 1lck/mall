import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

import { ProductListPage } from './ProductListPage'
import { listProducts } from '../service'

function mockJsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('ProductListPage', () => {
  it('renders live product fields and the create action', async () => {
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
              productNo: 'PRD202604111620001A2B3C',
              name: 'iPhone 16',
              categoryName: '手机',
              price: 6999,
              stock: 88,
              status: 'DRAFT',
              description: '旗舰新机',
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
          path: '/',
          loader: async () => listProducts(),
          element: <ProductListPage />,
        },
      ],
      { initialEntries: ['/'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    expect(await screen.findByRole('button', { name: /新建商品/i })).toBeInTheDocument()
    expect(await screen.findByText(/iPhone 16/i)).toBeInTheDocument()
    expect(await screen.findByText(/PRD202604111620001A2B3C/i)).toBeInTheDocument()
  })
})
