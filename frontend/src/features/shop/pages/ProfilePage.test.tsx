import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

import { AuthContext, type AuthContextValue } from '../../auth/context'
import { ProfilePage } from './ProfilePage'

describe('ProfilePage', () => {
  it('renders current user profile information', () => {
    const authContextValue: AuthContextValue = {
      user: {
        id: 1,
        username: 'alice01',
        nickname: 'alice',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2026-04-11T09:00:00.000Z',
      },
      isAuthenticated: true,
      isInitializing: false,
      login: async () => ({
        id: 1,
        username: 'alice01',
        nickname: 'alice',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2026-04-11T09:00:00.000Z',
      }),
      logout: () => {},
    }

    render(
      <AntdApp>
        <AuthContext.Provider value={authContextValue}>
          <MemoryRouter>
            <ProfilePage />
          </MemoryRouter>
        </AuthContext.Provider>
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /个人中心/i })).toBeInTheDocument()
    expect(screen.getByText(/^alice$/)).toBeInTheDocument()
    expect(screen.getByText(/^alice01$/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /退出登录/i })).toBeInTheDocument()
  })
})
