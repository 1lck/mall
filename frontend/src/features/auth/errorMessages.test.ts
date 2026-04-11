import { describe, expect, it } from 'vitest'

import { getAuthErrorMessage } from './errorMessages'

describe('getAuthErrorMessage', () => {
  it('translates common auth errors to chinese messages', () => {
    expect(getAuthErrorMessage('Username or password is incorrect', '登录失败。')).toBe('用户名或密码错误。')
    expect(getAuthErrorMessage('This account has been disabled', '登录失败。')).toBe('该账号已被停用，请联系管理员。')
    expect(getAuthErrorMessage('Username alice already exists', '注册失败。')).toBe('用户名已存在，请更换一个。')
  })

  it('falls back to the original message when no mapping exists', () => {
    expect(getAuthErrorMessage('Custom backend error', '登录失败。')).toBe('Custom backend error')
    expect(getAuthErrorMessage('', '登录失败。')).toBe('登录失败。')
  })
})
