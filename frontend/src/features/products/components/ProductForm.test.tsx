import { App as AntdApp } from 'antd'
import { render, screen } from '@testing-library/react'

import { ProductForm } from './ProductForm'

describe('ProductForm', () => {
  it('renders create mode with shared fields', () => {
    render(
      <AntdApp>
        <ProductForm mode="create" onSubmit={async () => {}} />
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /新建商品/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/商品名称/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/分类名称/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /上传商品图片/i })).toBeInTheDocument()
    expect(screen.queryByLabelText(/商品状态/i)).not.toBeInTheDocument()
  })

  it('renders edit mode with initial values and image preview', () => {
    render(
      <AntdApp>
        <ProductForm
          mode="edit"
          initialValues={{
            name: 'iPhone 16',
            categoryName: '手机',
            price: 6999,
            stock: 88,
            status: 'ON_SALE',
            description: '旗舰新机',
            imageUrl: 'http://127.0.0.1:9000/mall-product-images/product-images/2026/04/11/iphone16.png',
          }}
          onSubmit={async () => {}}
        />
      </AntdApp>,
    )

    expect(screen.getByRole('heading', { name: /编辑商品/i })).toBeInTheDocument()
    expect(screen.getByDisplayValue(/iPhone 16/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/商品状态/i)).toBeInTheDocument()
    expect(screen.getByRole('img', { name: /iPhone 16 商品图片/i })).toBeInTheDocument()
  })
})
