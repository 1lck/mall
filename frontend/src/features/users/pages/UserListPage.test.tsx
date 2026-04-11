import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

import { listUsers } from '../service'
import { UserListPage } from './UserListPage'

function mockJsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  )
}

describe('UserListPage', () => {
  it('renders fetched users and the create action', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/admin/users')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 2,
              username: 'operator01',
              nickname: '运营同学',
              role: 'ADMIN',
              status: 'ACTIVE',
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
          loader: async () => listUsers(),
          element: <UserListPage />,
        },
      ],
      { initialEntries: ['/'] },
    )

    render(
      <AntdApp>
        <RouterProvider router={router} />
      </AntdApp>,
    )

    expect(await screen.findByRole('button', { name: /新建用户/i })).toBeInTheDocument()
    expect(await screen.findByText(/operator01/i)).toBeInTheDocument()
    expect(await screen.findByText(/运营同学/i)).toBeInTheDocument()
  })

  it('submits a disable action for an active user', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      const url = String(input)

      if (url.endsWith('/api/v1/admin/users')) {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: [
            {
              id: 2,
              username: 'operator01',
              nickname: '运营同学',
              role: 'ADMIN',
              status: 'ACTIVE',
              createdAt: '2026-04-11T08:20:00.000Z',
              updatedAt: '2026-04-11T08:20:00.000Z',
            },
          ],
        })
      }

      if (url.endsWith('/api/v1/admin/users/2/status') && init?.method === 'PATCH') {
        return mockJsonResponse({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: {
            id: 2,
            username: 'operator01',
            nickname: '运营同学',
            role: 'ADMIN',
            status: 'DISABLED',
            createdAt: '2026-04-11T08:20:00.000Z',
            updatedAt: '2026-04-11T08:30:00.000Z',
          },
        })
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    const router = createMemoryRouter(
      [
        {
          path: '/',
          loader: async () => listUsers(),
          element: <UserListPage />,
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
    await user.click(await screen.findByRole('button', { name: /停用/i }))

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/admin/users/2/status'),
      expect.objectContaining({
        method: 'PATCH',
      }),
    )
  })
})
