import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'

import App from './App'

describe('App shell', () => {
  it('lands on the shop home by default for anonymous visitors', async () => {
    window.history.replaceState({}, '', '/')
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
      const url = String(input)

      if (url.endsWith('/api/v1/products')) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              success: true,
              code: 'SUCCESS',
              message: 'Request succeeded',
              data: [],
            }),
            {
              status: 200,
              headers: { 'Content-Type': 'application/json' },
            },
          ),
        )
      }

      throw new Error(`Unexpected request: ${url}`)
    })

    render(<App />)

    expect(await screen.findByRole('heading', { name: /发现今天值得逛的上新好物/i })).toBeInTheDocument()
  })
})
