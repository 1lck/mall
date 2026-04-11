import { request } from '../../shared/api/http'
import type { AuthUser, LoginInput, LoginResult, RegisterInput } from './types'

export async function register(input: RegisterInput): Promise<AuthUser> {
  return request<AuthUser>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function login(input: LoginInput): Promise<LoginResult> {
  return request<LoginResult>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function getCurrentUser(): Promise<AuthUser> {
  return request<AuthUser>('/api/v1/auth/me')
}
