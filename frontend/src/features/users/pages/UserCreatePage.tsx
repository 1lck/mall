import { Button, Card, Form, Input, Select, Space, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { createUser } from '../service'
import type { CreateUserInput, UserRole } from '../types'

const roleOptions: Array<{ label: string; value: UserRole }> = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' },
]

export function UserCreatePage() {
  const navigate = useNavigate()

  const handleFinish = async (values: CreateUserInput) => {
    try {
      await createUser(values)
      message.success('用户创建成功。')
      navigate('/users')
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '用户创建失败。')
    }
  }

  return (
    <Card variant="borderless">
      <Space orientation="vertical" size={24} className="page-stack">
        <div>
          <Typography.Title level={2}>新建用户</Typography.Title>
          <Typography.Paragraph type="secondary">
            这里创建的是后台可管理账号，默认创建后立即启用。
          </Typography.Paragraph>
        </div>

        <Form<CreateUserInput> layout="vertical" onFinish={handleFinish} initialValues={{ role: 'USER' }}>
          <Form.Item<CreateUserInput>
            label="用户名"
            name="username"
            rules={[
              { required: true, message: '请输入用户名。' },
              { min: 3, message: '用户名至少 3 位。' },
            ]}
          >
            <Input placeholder="operator01" />
          </Form.Item>

          <Form.Item<CreateUserInput>
            label="昵称"
            name="nickname"
            rules={[
              { required: true, message: '请输入昵称。' },
              { min: 2, message: '昵称至少 2 位。' },
            ]}
          >
            <Input placeholder="运营同学" />
          </Form.Item>

          <Form.Item<CreateUserInput>
            label="密码"
            name="password"
            rules={[
              { required: true, message: '请输入密码。' },
              { min: 8, message: '密码至少 8 位。' },
            ]}
          >
            <Input.Password placeholder="请输入密码" />
          </Form.Item>

          <Form.Item<CreateUserInput>
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色。' }]}
          >
            <Select options={roleOptions} style={{ width: 240 }} />
          </Form.Item>

          <Space size={12}>
            <Button type="primary" htmlType="submit">
              创建用户
            </Button>
            <Button onClick={() => navigate('/users')}>返回列表</Button>
          </Space>
        </Form>
      </Space>
    </Card>
  )
}
