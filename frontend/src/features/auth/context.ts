import { createContext, useContext } from 'react'

import type { AuthUser, LoginInput } from './types'

export interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  isInitializing: boolean
  login: (input: LoginInput) => Promise<AuthUser>
  logout: () => void
}

const defaultAuthContextValue: AuthContextValue = {
  user: null,
  isAuthenticated: false,
  isInitializing: false,
  login: async () => {
    throw new Error('AuthProvider is not mounted')
  },
  logout: () => {},
}

export const AuthContext = createContext<AuthContextValue>(defaultAuthContextValue)

export function useAuth() {
  return useContext(AuthContext)
}
