import { Button, Card, Flex, Space, Table, Tag, Typography, message } from 'antd'
import { useLoaderData, useNavigate, useRevalidator } from 'react-router-dom'

import { ApiError } from '../../../shared/api/http'
import { formatOrderDateTime } from '../../orders/utils'
import { listUsers, updateUserStatus } from '../service'
import type { UserRecord, UserRole, UserStatus } from '../types'

const roleLabels: Record<UserRole, string> = {
  USER: '普通用户',
  ADMIN: '管理员',
}

const statusLabels: Record<UserStatus, string> = {
  ACTIVE: '启用中',
  DISABLED: '已停用',
}

const statusColors: Record<UserStatus, string> = {
  ACTIVE: 'green',
  DISABLED: 'red',
}

export function UserListPage() {
  const navigate = useNavigate()
  const revalidator = useRevalidator()
  const users = useLoaderData() as Awaited<ReturnType<typeof listUsers>>

  const handleToggleStatus = async (user: UserRecord) => {
    const targetStatus: UserStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'

    try {
      await updateUserStatus(user.id, targetStatus)
      message.success(targetStatus === 'ACTIVE' ? '用户已启用。' : '用户已停用。')
      void revalidator.revalidate()
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '更新用户状态失败。')
    }
  }

  return (
    <Space size={20} orientation="vertical" className="page-stack">
      <Flex justify="space-between" align="center" gap={16}>
        <div>
          <Typography.Title level={2}>用户管理</Typography.Title>
          <Typography.Paragraph type="secondary">
            当前版本支持查看后台用户、创建账户，以及切换启用状态。
          </Typography.Paragraph>
        </div>
        <Button type="primary" size="large" onClick={() => navigate('/users/new')}>
          新建用户
        </Button>
      </Flex>

      <Card variant="borderless">
        <Table<UserRecord>
          rowKey="id"
          loading={revalidator.state === 'loading'}
          pagination={false}
          dataSource={users}
          columns={[
            {
              title: '用户 ID',
              dataIndex: 'id',
            },
            {
              title: '用户名',
              dataIndex: 'username',
            },
            {
              title: '昵称',
              dataIndex: 'nickname',
            },
            {
              title: '角色',
              dataIndex: 'role',
              render: (role: UserRole) => roleLabels[role],
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: (status: UserStatus) => (
                <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>
              ),
            },
            {
              title: '创建时间',
              dataIndex: 'createdAt',
              render: (value: string) => formatOrderDateTime(value),
            },
            {
              title: '更新时间',
              dataIndex: 'updatedAt',
              render: (value: string) => formatOrderDateTime(value),
            },
            {
              title: '操作',
              key: 'actions',
              render: (_, user) => (
                <Button type="link" onClick={() => handleToggleStatus(user)}>
                  {user.status === 'ACTIVE' ? '停用' : '启用'}
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </Space>
  )
}
