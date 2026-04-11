interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

const ACCESS_TOKEN_KEY = 'mall-access-token'

export class ApiError extends Error {
  status: number
  code?: string

  constructor(message: string, status: number, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function readApiResponse<T>(response: Response): Promise<ApiResponse<T> | null> {
  const contentType = response.headers.get('content-type') ?? ''

  if (!contentType.includes('application/json')) {
    return null
  }

  return (await response.json()) as ApiResponse<T>
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  const accessToken = window.localStorage.getItem(ACCESS_TOKEN_KEY)
  const isFormData = init?.body instanceof FormData

  if (init?.body && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  })

  const apiResponse = await readApiResponse<T>(response)

  if (!response.ok) {
    throw new ApiError(
      apiResponse?.message ?? `Request failed with status ${response.status}`,
      response.status,
      apiResponse?.code,
    )
  }

  if (!apiResponse) {
    throw new ApiError('Server returned an unexpected response format.', response.status)
  }

  if (!apiResponse.success) {
    throw new ApiError(apiResponse.message, response.status, apiResponse.code)
  }

  return apiResponse.data
}
