export interface AuthUser {
  id: number
  username: string
  nickname: string
  role: 'USER' | 'ADMIN'
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
}

export interface LoginInput {
  username: string
  password: string
}

export interface RegisterInput {
  username: string
  nickname: string
  password: string
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}
