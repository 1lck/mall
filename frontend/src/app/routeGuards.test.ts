import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '../shared/api/http'
import { requireAdminUser, requireSignedInUser, resolveDefaultPath } from './routeGuards'

vi.mock('../features/auth/service', () => ({
  getCurrentUser: vi.fn(),
}))

vi.mock('../features/auth/storage', () => ({
  getStoredAccessToken: vi.fn(),
  clearStoredAccessToken: vi.fn(),
}))

async function expectRedirect(
  loader: Promise<unknown>,
  location: string,
) {
  try {
    await loader
  } catch (error) {
    expect(error).toBeInstanceOf(Response)
    expect((error as Response).headers.get('Location')).toBe(location)
    return
  }

  throw new Error('Expected redirect response')
}

describe('routeGuards', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('resolves the default path by current role', () => {
    expect(resolveDefaultPath(null)).toBe('/shop')
    expect(resolveDefaultPath({ role: 'USER' })).toBe('/orders')
    expect(resolveDefaultPath({ role: 'ADMIN' })).toBe('/products')
  })

  it('redirects to login when a protected route is opened without a token', async () => {
    const { getStoredAccessToken } = await import('../features/auth/storage')
    vi.mocked(getStoredAccessToken).mockReturnValue(null)

    await expectRedirect(requireSignedInUser(), '/login')
  })

  it('redirects non-admin users away from admin routes', async () => {
    const { getStoredAccessToken } = await import('../features/auth/storage')
    const { getCurrentUser } = await import('../features/auth/service')

    vi.mocked(getStoredAccessToken).mockReturnValue('demo-token')
    vi.mocked(getCurrentUser).mockResolvedValue({
      id: 1,
      username: 'alice',
      nickname: 'Alice',
      role: 'USER',
      status: 'ACTIVE',
      createdAt: '2026-04-11T09:00:00.000Z',
    })

    await expectRedirect(requireAdminUser(), '/orders')
  })

  it('redirects to login when the stored token has expired', async () => {
    const { getStoredAccessToken } = await import('../features/auth/storage')
    const { getCurrentUser } = await import('../features/auth/service')

    vi.mocked(getStoredAccessToken).mockReturnValue('expired-token')
    vi.mocked(getCurrentUser).mockRejectedValue(new ApiError('请先登录后再访问。', 401, 'UNAUTHORIZED'))

    await expectRedirect(requireSignedInUser(), '/login')
  })
})
