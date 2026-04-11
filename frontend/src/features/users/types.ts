export type UserRole = 'USER' | 'ADMIN'
export type UserStatus = 'ACTIVE' | 'DISABLED'

export interface UserRecord {
  id: number
  username: string
  nickname: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

export interface CreateUserInput {
  username: string
  nickname: string
  password: string
  role: UserRole
}
