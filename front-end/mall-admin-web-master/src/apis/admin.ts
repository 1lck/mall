import http from '@/utils/http'
import type { LoginParam, LoginResult, UmsAdmin, UserInfoResult } from '@/types/admin'
import type { CommonPage, PageParam } from '@/types/common'

type BackendUser = {
  id: number
  username: string
  nickname: string
  role: string
  status: string
  createdAt: string
  updatedAt: string
}

function paginate<T>(items: T[], pageNum: number, pageSize: number): CommonPage<T> {
  const safePageNum = Math.max(pageNum, 1)
  const safePageSize = Math.max(pageSize, 1)
  const start = (safePageNum - 1) * safePageSize
  const list = items.slice(start, start + safePageSize)
  const total = items.length

  return {
    pageNum: safePageNum,
    pageSize: safePageSize,
    total,
    totalPage: Math.max(Math.ceil(total / safePageSize), 1),
    list,
  }
}

function mapUser(user: BackendUser): UmsAdmin {
  return {
    id: user.id,
    username: user.username,
    nickName: user.nickname,
    role: user.role,
    status: user.status === 'ACTIVE' ? 1 : 0,
    createTime: user.createdAt,
  }
}

export function adminLoginAPI(data: LoginParam) {
  return http<LoginResult>({
    method: 'POST',
    url: '/api/v1/auth/login',
    data,
  })
}

export function adminLogoutAPI() {
  return Promise.resolve({
    code: 200,
    message: 'ok',
    data: null,
  })
}

export function getAdminInfoAPI() {
  return http<Omit<UserInfoResult, 'roles'>>({
    method: 'GET',
    url: '/api/v1/auth/me',
  }).then(res => ({
    ...res,
    data: {
      ...res.data,
      roles: [res.data.role],
    },
  }))
}

export async function getAdminListAPI(params: PageParam) {
  const res = await http<BackendUser[]>({
    method: 'GET',
    url: '/api/v1/admin/users',
  })
  const keyword = params.keyword?.trim().toLowerCase()
  const filtered = res.data
    .map(mapUser)
    .filter(item => {
      if (!keyword) {
        return true
      }

      return (
        item.username.toLowerCase().includes(keyword) ||
        (item.nickName ?? '').toLowerCase().includes(keyword)
      )
    })

  return {
    code: 200,
    message: 'ok',
    data: paginate(filtered, params.pageNum, params.pageSize),
  }
}

export function adminRegisterAPI(data: UmsAdmin) {
  return http({
    method: 'POST',
    url: '/api/v1/admin/users',
    data: {
      username: data.username,
      nickname: data.nickName,
      password: data.password,
      role: data.role,
    },
  })
}

export function adminUpdateStatusByIdAPI(id: number, params: { status: number }) {
  return http({
    method: 'PATCH',
    url: `/api/v1/admin/users/${id}/status`,
    data: {
      status: params.status === 1 ? 'ACTIVE' : 'DISABLED',
    },
  })
}
