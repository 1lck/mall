import { Card, Spin } from 'antd'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { resolveDefaultPath } from '../../app/routeGuards'
import type { AuthUser } from './types'
import { useAuth } from './context'

interface RequireRoleProps {
  role: AuthUser['role']
  children: ReactNode
}

export function RequireRole({ role, children }: RequireRoleProps) {
  const { isAuthenticated, isInitializing, user } = useAuth()
  const location = useLocation()

  if (isInitializing) {
    return (
      <Card variant="borderless">
        <Spin size="large" />
      </Card>
    )
  }

  if (!isAuthenticated || !user) {
    return <Navigate replace to="/login" state={{ from: location.pathname }} />
  }

  if (user.role !== role) {
    return <Navigate replace to={resolveDefaultPath(user)} state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
