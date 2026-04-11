import { Card, Spin } from 'antd'
import { createBrowserRouter, Navigate } from 'react-router-dom'

import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { RequireAuth } from '../features/auth/RequireAuth'
import { RequireRole } from '../features/auth/RequireRole'
import { useAuth } from '../features/auth/context'
import { ApiError } from '../shared/api/http'
import { getOrderById, listOrders } from '../features/orders/service'
import { getProductById, listProducts } from '../features/products/service'
import { listUsers } from '../features/users/service'
import { ProfilePage } from '../features/shop/pages/ProfilePage'
import { ShopHomePage } from '../features/shop/pages/ShopHomePage'
import { OrderCreatePage } from '../features/orders/pages/OrderCreatePage'
import { OrderEditPage } from '../features/orders/pages/OrderEditPage'
import { OrderListPage } from '../features/orders/pages/OrderListPage'
import { ProductCreatePage } from '../features/products/pages/ProductCreatePage'
import { ProductEditPage } from '../features/products/pages/ProductEditPage'
import { ProductListPage } from '../features/products/pages/ProductListPage'
import { UserCreatePage } from '../features/users/pages/UserCreatePage'
import { UserListPage } from '../features/users/pages/UserListPage'
import { AdminLayout } from '../layouts/AdminLayout'
import { ShopLayout } from '../layouts/ShopLayout'
import { requireAdminUser, requireSignedInUser, resolveDefaultPath } from './routeGuards'

function RootIndexRedirect() {
  const { isInitializing, user } = useAuth()

  if (isInitializing) {
    return (
      <Card variant="borderless">
        <Spin size="large" />
      </Card>
    )
  }

  return <Navigate replace to={resolveDefaultPath(user)} />
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/shop',
    element: <ShopLayout />,
    children: [
      {
        index: true,
        loader: async () => listProducts(),
        element: <ShopHomePage />,
      },
      {
        path: 'profile',
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        ),
      },
    ],
  },
  {
    path: '/',
    element: <AdminLayout />,
    children: [
      {
        index: true,
        element: <RootIndexRedirect />,
      },
      {
        path: 'products',
        loader: async () => {
          await requireAdminUser()
          return listProducts()
        },
        element: (
          <RequireRole role="ADMIN">
            <ProductListPage />
          </RequireRole>
        ),
      },
      {
        path: 'products/new',
        element: (
          <RequireRole role="ADMIN">
            <ProductCreatePage />
          </RequireRole>
        ),
      },
      {
        path: 'products/:productId/edit',
        loader: async ({ params }) => {
          await requireAdminUser()

          if (!params.productId) {
            return null
          }

          return getProductById(params.productId)
        },
        element: (
          <RequireRole role="ADMIN">
            <ProductEditPage />
          </RequireRole>
        ),
      },
      {
        path: 'orders',
        loader: async () => {
          await requireSignedInUser()
          return listOrders()
        },
        element: (
          <RequireAuth>
            <OrderListPage />
          </RequireAuth>
        ),
      },
      {
        path: 'orders/new',
        element: (
          <RequireAuth>
            <OrderCreatePage />
          </RequireAuth>
        ),
      },
      {
        path: 'orders/:orderId/edit',
        loader: async ({ params }) => {
          await requireSignedInUser()

          if (!params.orderId) {
            return null
          }

          try {
            return await getOrderById(params.orderId)
          } catch (error) {
            if (error instanceof ApiError && error.status === 404) {
              return null
            }

            throw error
          }
        },
        element: (
          <RequireAuth>
            <OrderEditPage />
          </RequireAuth>
        ),
      },
      {
        path: 'users',
        loader: async () => {
          await requireAdminUser()
          return listUsers()
        },
        element: (
          <RequireRole role="ADMIN">
            <UserListPage />
          </RequireRole>
        ),
      },
      {
        path: 'users/new',
        element: (
          <RequireRole role="ADMIN">
            <UserCreatePage />
          </RequireRole>
        ),
      },
    ],
  },
])
