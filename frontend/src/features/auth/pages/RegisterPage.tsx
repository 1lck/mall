import { Button, Card, Form, Input, Space, Typography, message } from 'antd'
import { Link, Navigate, useNavigate } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { useAuth } from '../context'
import { getAuthErrorMessage } from '../errorMessages'
import { register } from '../service'
import type { RegisterInput } from '../types'

export function RegisterPage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  if (isAuthenticated) {
    return <Navigate replace to="/shop/profile" />
  }

  const handleFinish = async (values: RegisterInput) => {
    try {
      await register(values)
      message.success('注册成功，请使用新账户登录。')
      navigate('/login', { replace: true })
    } catch (error) {
      message.error(error instanceof ApiError ? getAuthErrorMessage(error.message, '注册失败。') : '注册失败。')
    }
  }

  return (
    <div className="auth-page">
      <Card variant="borderless" style={{ maxWidth: 460, margin: '64px auto' }}>
        <Space orientation="vertical" size={24} className="page-stack">
          <div>
            <Typography.Text type="secondary">Mall Account</Typography.Text>
            <Typography.Title level={2}>创建商城账户</Typography.Title>
            <Typography.Paragraph type="secondary">
              先完成注册，再用新账号登录进入个人中心和下单流程。
            </Typography.Paragraph>
          </div>

          <Form<RegisterInput> layout="vertical" onFinish={handleFinish}>
            <Form.Item<RegisterInput>
              label="用户名"
              name="username"
              rules={[
                { required: true, message: '请输入用户名。' },
                { min: 3, message: '用户名至少 3 位。' },
              ]}
            >
              <Input placeholder="alice01" autoComplete="username" />
            </Form.Item>

            <Form.Item<RegisterInput>
              label="昵称"
              name="nickname"
              rules={[
                { required: true, message: '请输入昵称。' },
                { min: 2, message: '昵称至少 2 位。' },
              ]}
            >
              <Input placeholder="Alice" autoComplete="nickname" />
            </Form.Item>

            <Form.Item<RegisterInput>
              label="密码"
              name="password"
              rules={[
                { required: true, message: '请输入密码。' },
                { min: 8, message: '密码至少 8 位。' },
              ]}
            >
              <Input.Password placeholder="请输入密码" autoComplete="new-password" />
            </Form.Item>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Button block type="primary" htmlType="submit" size="large">
                注册
              </Button>
              <Typography.Text type="secondary">
                已有账号？
                <Link to="/login">去登录</Link>
              </Typography.Text>
            </Space>
          </Form>
        </Space>
      </Card>
    </div>
  )
}
