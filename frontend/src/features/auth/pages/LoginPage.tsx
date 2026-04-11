import { Button, Card, Form, Input, Space, Typography, message } from 'antd'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../context'
import { getAuthErrorMessage } from '../errorMessages'
import type { LoginInput } from '../types'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, login } = useAuth()

  if (isAuthenticated) {
    return <Navigate replace to="/shop/profile" />
  }

  const redirectTo = typeof location.state?.from === 'string' ? location.state.from : '/shop/profile'

  const handleFinish = async (values: LoginInput) => {
    try {
      await login(values)
      message.success('登录成功。')
      navigate(redirectTo, { replace: true })
    } catch (error) {
      message.error(error instanceof ApiError ? getAuthErrorMessage(error.message, '登录失败。') : '登录失败。')
    }
  }

  return (
    <div className="auth-page">
      <Card variant="borderless" style={{ maxWidth: 460, margin: '64px auto' }}>
        <Space orientation="vertical" size={24} className="page-stack">
          <div>
            <Typography.Text type="secondary">Mall Account</Typography.Text>
            <Typography.Title level={2}>登录商城账户</Typography.Title>
            <Typography.Paragraph type="secondary">
              登录后可以查看个人中心，并以当前用户身份创建订单。
            </Typography.Paragraph>
          </div>

          <Form<LoginInput> layout="vertical" onFinish={handleFinish} initialValues={{ username: 'alice' }}>
            <Form.Item<LoginInput>
              label="用户名"
              name="username"
              rules={[{ required: true, message: '请输入用户名。' }]}
            >
              <Input placeholder="alice" autoComplete="username" />
            </Form.Item>

            <Form.Item<LoginInput>
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码。' }]}
            >
              <Input.Password placeholder="请输入密码" autoComplete="current-password" />
            </Form.Item>

            <Button block type="primary" htmlType="submit" size="large">
              登录
            </Button>
            <Typography.Text type="secondary">
              还没有账号？
              <Link to="/register">去注册</Link>
            </Typography.Text>
          </Form>
        </Space>
      </Card>
    </div>
  )
}
