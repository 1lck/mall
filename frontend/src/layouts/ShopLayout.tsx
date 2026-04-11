import { Button, Input, Space, Typography } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'

import { useAuth } from '../features/auth/context'

const quickLinks = ['精选', '鞋类', '潮服', '数码', '美妆', '家居', '配饰']

export function ShopLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, logout, user } = useAuth()

  return (
    <div className="shop-shell">
      <header className="shop-header">
        <div className="shop-brand-block">
          <Typography.Text className="shop-brand-kicker">Mall</Typography.Text>
          <Typography.Title level={2}>潮流商城</Typography.Title>
        </div>

        <nav className="shop-quick-nav" aria-label="前台导航">
          {quickLinks.map((item) => (
            <button
              key={item}
              type="button"
              className={`shop-quick-link ${item === '精选' && location.pathname === '/shop' ? 'is-active' : ''}`}
            >
              {item}
            </button>
          ))}
        </nav>

        <Space size={12} className="shop-header-actions">
          <Input.Search placeholder="搜索灵感好物" allowClear className="shop-search" />
          {isAuthenticated && user ? <Typography.Text>{user.nickname}</Typography.Text> : null}
          <Link to="/shop/profile">个人中心</Link>
          {isAuthenticated ? (
            <Button
              type="text"
              onClick={() => {
                logout()
                navigate('/shop')
              }}
            >
              退出登录
            </Button>
          ) : (
            <Button type="primary" onClick={() => navigate('/login')}>
              登录
            </Button>
          )}
        </Space>
      </header>

      <main className="shop-main">
        <Outlet />
      </main>
    </div>
  )
}
