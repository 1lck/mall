import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'

import { RegisterPage } from './RegisterPage'

describe('RegisterPage', () => {
  it('renders register form fields and login entry', () => {
    render(
      <AntdApp>
        <MemoryRouter>
          <RegisterPage />
        </MemoryRouter>
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /创建商城账户/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/用户名/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/昵称/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/密码/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /去登录/i })).toBeInTheDocument()
  })
})
