import { afterEach, describe, expect, it, vi } from 'vitest'

import { request } from './http'

describe('request', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    window.localStorage.clear()
  })

  it('does not force json content type for FormData uploads', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          code: 'SUCCESS',
          message: 'Request succeeded',
          data: {
            imageUrl: 'http://127.0.0.1:9000/demo.png',
            objectKey: 'product-images/demo.png',
          },
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        },
      ),
    )

    const formData = new FormData()
    formData.append('file', new File(['demo'], 'demo.png', { type: 'image/png' }))

    await request('/api/v1/products/images/upload', {
      method: 'POST',
      body: formData,
    })

    const headers = new Headers(fetchSpy.mock.calls[0]?.[1]?.headers)
    expect(headers.has('Content-Type')).toBe(false)
  })
})
