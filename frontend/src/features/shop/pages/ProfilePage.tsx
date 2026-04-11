import { Button, Card, Descriptions, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../../auth/context'
import { formatOrderDateTime } from '../../orders/utils'

const roleLabels = {
  USER: '普通用户',
  ADMIN: '管理员',
} as const

export function ProfilePage() {
  const navigate = useNavigate()
  const { logout, user } = useAuth()

  if (!user) {
    return null
  }

  return (
    <Card variant="borderless">
      <Space orientation="vertical" size={24} className="page-stack">
        <div>
          <Typography.Title level={2}>个人中心</Typography.Title>
          <Typography.Paragraph type="secondary">
            当前已接入基础登录态，可以查看自己的账户信息并直接跳去下单。
          </Typography.Paragraph>
        </div>

        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label="昵称">{user.nickname}</Descriptions.Item>
          <Descriptions.Item label="用户名">{user.username}</Descriptions.Item>
          <Descriptions.Item label="角色">{roleLabels[user.role]}</Descriptions.Item>
          <Descriptions.Item label="用户 ID">{user.id}</Descriptions.Item>
          <Descriptions.Item label="注册时间" span={2}>
            {formatOrderDateTime(user.createdAt)}
          </Descriptions.Item>
        </Descriptions>

        <Space size={12}>
          <Button type="primary" onClick={() => navigate('/orders/new')}>
            去创建订单
          </Button>
          <Button
            onClick={() => {
              logout()
              navigate('/shop')
            }}
          >
            退出登录
          </Button>
        </Space>
      </Space>
    </Card>
  )
}
