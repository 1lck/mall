import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'

import { AuthContext, type AuthContextValue } from './context'
import { RequireRole } from './RequireRole'

function renderWithAuth(authContextValue: AuthContextValue) {
  render(
    <AntdApp>
      <AuthContext.Provider value={authContextValue}>
        <MemoryRouter initialEntries={['/users']}>
          <Routes>
            <Route
              path="/users"
              element={
                <RequireRole role="ADMIN">
                  <div>admin only</div>
                </RequireRole>
              }
            />
            <Route path="/orders" element={<div>orders page</div>} />
            <Route path="/login" element={<div>login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </AntdApp>,
  )
}

describe('RequireRole', () => {
  it('redirects non-admin users to their order page', async () => {
    renderWithAuth({
      user: {
        id: 1,
        username: 'alice',
        nickname: 'Alice',
        role: 'USER',
        status: 'ACTIVE',
        createdAt: '2026-04-11T09:00:00.000Z',
      },
      isAuthenticated: true,
      isInitializing: false,
      login: async () => {
        throw new Error('not used')
      },
      logout: () => {},
    })

    expect(await screen.findByText(/orders page/i)).toBeInTheDocument()
    expect(screen.queryByText(/admin only/i)).not.toBeInTheDocument()
  })
})
