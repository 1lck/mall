const authErrorMessageMap: Array<[pattern: RegExp, message: string]> = [
  [/^Username or password is incorrect$/i, '用户名或密码错误。'],
  [/^This account has been disabled$/i, '该账号已被停用，请联系管理员。'],
  [/^Username .+ already exists$/i, '用户名已存在，请更换一个。'],
]

export function getAuthErrorMessage(message: string | undefined, fallbackMessage: string) {
  if (!message) {
    return fallbackMessage
  }

  const matchedEntry = authErrorMessageMap.find(([pattern]) => pattern.test(message))
  if (matchedEntry) {
    return matchedEntry[1]
  }

  return message
}
