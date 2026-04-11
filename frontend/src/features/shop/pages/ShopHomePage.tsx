import { Card, Empty, Space, Tabs, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { useLoaderData } from 'react-router-dom'

import type { ProductRecord } from '../../products/types'

function buildCategories(products: ProductRecord[]) {
  const names = Array.from(new Set(products.map((product) => product.categoryName).filter(Boolean)))
  return ['精选', ...names]
}

function buildPlaceholderTone(categoryName: string) {
  const tones = [
    'linear-gradient(135deg, #f7f3ee 0%, #efe6dc 100%)',
    'linear-gradient(135deg, #edf2f7 0%, #dce5ef 100%)',
    'linear-gradient(135deg, #f7efe8 0%, #f3dfcf 100%)',
    'linear-gradient(135deg, #f3f6eb 0%, #dce5c7 100%)',
  ]
  const seed = Array.from(categoryName).reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return tones[seed % tones.length]
}

export function ShopHomePage() {
  const products = (useLoaderData() as ProductRecord[]).filter((product) => product.status === 'ON_SALE')
  const categories = useMemo(() => buildCategories(products), [products])
  const [activeCategory, setActiveCategory] = useState('精选')

  const visibleProducts = useMemo(() => {
    if (activeCategory === '精选') {
      return products
    }

    return products.filter((product) => product.categoryName === activeCategory)
  }, [activeCategory, products])

  return (
    <Space orientation="vertical" size={28} className="shop-page">
      <section className="shop-hero">
        <div>
          <Typography.Text className="shop-eyebrow">Online Select</Typography.Text>
          <Typography.Title>发现今天值得逛的上新好物</Typography.Title>
          <Typography.Paragraph>
            用一页干净的首页，把当前在售商品直接铺到用户面前，先让浏览体验成立。
          </Typography.Paragraph>
        </div>
        <div className="shop-hero-panel">
          <span>在售商品</span>
          <strong>{products.length}</strong>
          <Typography.Text>右上角提供个人中心入口，后续可继续扩展订单、收藏和地址。</Typography.Text>
        </div>
      </section>

      <Tabs
        activeKey={activeCategory}
        onChange={setActiveCategory}
        items={categories.map((category) => ({
          key: category,
          label: category,
        }))}
      />

      {visibleProducts.length === 0 ? (
        <Card variant="borderless">
          <Empty description="当前分类下暂无在售商品" />
        </Card>
      ) : (
        <section className="shop-grid" aria-label="商品列表">
          {visibleProducts.map((product) => (
            <article key={product.id} className="shop-product-card">
              <div
                className="shop-product-visual"
                style={{ background: buildPlaceholderTone(product.categoryName) }}
              >
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} className="shop-product-image" />
                ) : null}
                <span>{product.categoryName}</span>
              </div>
              <div className="shop-product-body">
                <Typography.Text className="shop-product-category">{product.categoryName}</Typography.Text>
                <Typography.Title level={4}>{product.name}</Typography.Title>
                <Typography.Paragraph>{product.description || '正在热卖，欢迎选购。'}</Typography.Paragraph>
                <div className="shop-product-meta">
                  <strong>¥{product.price.toFixed(2)}</strong>
                  <span>现货 {product.stock} 件</span>
                </div>
              </div>
            </article>
          ))}
        </section>
      )}
    </Space>
  )
}
