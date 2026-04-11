import { request } from '../../shared/api/http'
import type { CreateUserInput, UserRecord, UserStatus } from './types'

export async function listUsers(): Promise<UserRecord[]> {
  return request<UserRecord[]>('/api/v1/admin/users')
}

export async function createUser(input: CreateUserInput): Promise<UserRecord> {
  return request<UserRecord>('/api/v1/admin/users', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function updateUserStatus(userId: number | string, status: UserStatus): Promise<UserRecord> {
  return request<UserRecord>(`/api/v1/admin/users/${userId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
}
