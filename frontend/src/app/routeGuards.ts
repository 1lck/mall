import { redirect } from 'react-router-dom'

import { getCurrentUser } from '../features/auth/service'
import { clearStoredAccessToken, getStoredAccessToken } from '../features/auth/storage'
import type { AuthUser } from '../features/auth/types'
import { ApiError } from '../shared/api/http'

export function resolveDefaultPath(user: Pick<AuthUser, 'role'> | null): string {
  if (!user) {
    return '/shop'
  }

  return user.role === 'ADMIN' ? '/products' : '/orders'
}

export async function requireSignedInUser(): Promise<AuthUser> {
  const accessToken = getStoredAccessToken()

  if (!accessToken) {
    throw redirect('/login')
  }

  try {
    return await getCurrentUser()
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      clearStoredAccessToken()
      throw redirect('/login')
    }

    throw error
  }
}

export async function requireAdminUser(): Promise<AuthUser> {
  const user = await requireSignedInUser()

  if (user.role !== 'ADMIN') {
    throw redirect(resolveDefaultPath(user))
  }

  return user
}
