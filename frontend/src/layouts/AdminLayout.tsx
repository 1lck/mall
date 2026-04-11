import { Layout, Menu, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { Button, Space } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'

import { useAuth } from '../features/auth/context'

function buildMenuItems(isAdmin: boolean): MenuProps['items'] {
  const items: MenuProps['items'] = [
    {
      key: '/orders',
      label: '订单管理',
    },
  ]

  if (isAdmin) {
    items.push(
      {
        key: '/products',
        label: '商品管理',
      },
      {
        key: '/users',
        label: '用户管理',
      },
    )
  }

  return items
}

function getSelectedMenuKey(pathname: string) {
  if (pathname.startsWith('/orders')) {
    return '/orders'
  }

  if (pathname.startsWith('/products')) {
    return '/products'
  }

  if (pathname.startsWith('/users')) {
    return '/users'
  }

  return pathname
}

export function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, logout, user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  return (
    <Layout className="admin-layout">
      <Layout.Sider breakpoint="lg" collapsedWidth={0} theme="light" width={248}>
        <div className="admin-brand">
          <Typography.Text className="admin-brand-eyebrow">商城后台</Typography.Text>
          <Typography.Title level={4}>运营控制台</Typography.Title>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[getSelectedMenuKey(location.pathname)]}
          items={buildMenuItems(isAdmin)}
          onClick={({ key }) => navigate(key)}
        />
      </Layout.Sider>
      <Layout>
        <Layout.Header className="admin-header">
          <Space size={16} style={{ width: '100%', justifyContent: 'space-between' }}>
            <Typography.Title level={3}>商城管理工作台</Typography.Title>
            <Space size={12}>
              {isAuthenticated && user ? <Typography.Text>{user.nickname}</Typography.Text> : null}
              {isAuthenticated ? (
                <>
                  <Link to="/shop/profile">个人中心</Link>
                  <Button type="text" onClick={logout}>
                    退出登录
                  </Button>
                </>
              ) : (
                <Button type="primary" onClick={() => navigate('/login')}>
                  登录
                </Button>
              )}
            </Space>
          </Space>
        </Layout.Header>
        <Layout.Content className="admin-content">
          <Outlet />
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
