import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { CommonResult } from '@/types/common'

const http = axios.create({
  baseURL: import.meta.env.VITE_BASE_SERVER_URL,
  timeout: 5000,
})

function normalizeResponse<T>(payload: unknown): CommonResult<T> {
  if (
    payload &&
    typeof payload === 'object' &&
    'success' in payload &&
    'data' in payload
  ) {
    const mallPayload = payload as { success: boolean; message?: string; data: T }
    return {
      code: mallPayload.success ? 200 : 500,
      message: mallPayload.message ?? '',
      data: mallPayload.data,
    }
  }

  return payload as CommonResult<T>
}

http.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    const token = userStore.userInfo.token
    if (token) {
      config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error),
)

http.interceptors.response.use(
  response => {
    const res = normalizeResponse(response.data)
    if (res.code !== 200) {
      ElMessage({
        message: res.message,
        type: 'error',
        duration: 3000,
      })
      return Promise.reject(new Error(res.message))
    }

    return res as unknown as typeof response
  },
  error => {
    const status = error.response?.status
    const payload = error.response?.data ? normalizeResponse(error.response.data) : null
    const message = payload?.message ?? error.message

    if (status === 401) {
      ElMessageBox.confirm('登录状态已失效，可以重新登录后再继续操作。', '确定登出', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        const userStore = useUserStore()
        userStore.fedLogout()
        location.reload()
      })
    }

    ElMessage({
      message,
      type: 'error',
      duration: 3000,
    })
    return Promise.reject(error)
  },
)

export default http
