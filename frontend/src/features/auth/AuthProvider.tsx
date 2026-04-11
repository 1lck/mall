import { useEffect, useState, type ReactNode } from 'react'

import { ApiError } from '../../shared/api/http'
import { AuthContext } from './context'
import { clearStoredAccessToken, getStoredAccessToken, storeAccessToken } from './storage'
import { getCurrentUser, login } from './service'
import type { AuthUser, LoginInput } from './types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [isInitializing, setIsInitializing] = useState(() => getStoredAccessToken() !== null)

  useEffect(() => {
    const token = getStoredAccessToken()
    if (!token) {
      return
    }

    let cancelled = false

    void getCurrentUser()
      .then((currentUser) => {
        if (!cancelled) {
          setUser(currentUser)
        }
      })
      .catch((error) => {
        if (!cancelled && error instanceof ApiError && error.status === 401) {
          clearStoredAccessToken()
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsInitializing(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const handleLogin = async (input: LoginInput) => {
    const result = await login(input)
    storeAccessToken(result.token)
    setUser(result.user)
    return result.user
  }

  const handleLogout = () => {
    clearStoredAccessToken()
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        isInitializing,
        login: handleLogin,
        logout: handleLogout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
