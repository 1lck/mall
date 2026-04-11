/** 登录请求参数 */
export type LoginParam = {
  username: string
  password: string
}

/** 登录返回结果 */
export type LoginResult = {
  token: string
  expiresIn?: number
  user?: {
    id: number
    username: string
    nickname: string
    role: string
    status: string
    createdAt: string
  }
}

/** 用户信息结果封装 */
export type UserInfoResult = {
  id: number
  username: string
  nickname: string
  role: string
  status: string
  roles: string[]
}

/** 用户信息（store中存储的） */
export type UserInfo = Pick<UserInfoResult, 'id' | 'username' | 'nickname' | 'role' | 'roles'> & {
  password: string
  token: string
  avatar: string
}

/** 后台用户信息 */
export type UmsAdmin = {
  id?: number
  username: string
  password?: string
  nickName?: string
  role: string
  status: number
  createTime?: string
}
