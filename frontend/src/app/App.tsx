import { AuthProvider } from '../features/auth/AuthProvider'
import { App as AntdApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { RouterProvider } from 'react-router-dom'

import { router } from './router'

function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#b95c2e',
          colorBgLayout: '#f5efe7',
          borderRadius: 14,
          fontFamily: '"Noto Sans SC", "PingFang SC", "Helvetica Neue", sans-serif',
        },
      }}
    >
      <AntdApp>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </AntdApp>
    </ConfigProvider>
  )
}

export default App
