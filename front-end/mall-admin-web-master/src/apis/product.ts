import http from '@/utils/http'
import type { CommonPage } from '@/types/common'
import type { PmsProduct, PmsProductParam, ProductQueryParam } from '@/types/product'

type BackendProduct = {
  id: number
  productNo: string
  name: string
  categoryName: string
  price: number | string
  stock: number
  status: 'DRAFT' | 'ON_SALE' | 'OFF_SHELF'
  description?: string
  imageUrl?: string
  createdAt: string
  updatedAt: string
}

function paginate<T>(items: T[], pageNum: number, pageSize: number): CommonPage<T> {
  const safePageNum = Math.max(pageNum, 1)
  const safePageSize = Math.max(pageSize, 1)
  const start = (safePageNum - 1) * safePageSize
  const list = items.slice(start, start + safePageSize)
  const total = items.length

  return {
    pageNum: safePageNum,
    pageSize: safePageSize,
    total,
    totalPage: Math.max(Math.ceil(total / safePageSize), 1),
    list,
  }
}

function mapProduct(item: BackendProduct): PmsProduct {
  return {
    id: item.id,
    name: item.name,
    pic: item.imageUrl,
    productSn: item.productNo,
    publishStatus: item.status === 'ON_SALE' ? 1 : 0,
    price: Number(item.price),
    stock: item.stock,
    productCategoryName: item.categoryName,
    description: item.description,
    createTime: item.createdAt,
    updateTime: item.updatedAt,
    backendStatus: item.status,
  }
}

function mapRequest(data: PmsProductParam) {
  return {
    name: data.name,
    categoryName: data.productCategoryName || '',
    price: data.price,
    stock: data.stock,
    description: data.description || '',
    imageUrl: data.pic || data.imageUrl || '',
  }
}

export async function getProductListAPI(params: ProductQueryParam) {
  const res = await http<BackendProduct[]>({
    method: 'GET',
    url: '/api/v1/products',
  })
  const keyword = params.keyword?.trim().toLowerCase()
  const categoryName = params.categoryName?.trim().toLowerCase()
  const filtered = res.data
    .map(mapProduct)
    .filter(item => {
      if (params.publishStatus !== undefined && item.publishStatus !== params.publishStatus) {
        return false
      }

      if (
        keyword &&
        !item.name.toLowerCase().includes(keyword) &&
        !item.productSn.toLowerCase().includes(keyword)
      ) {
        return false
      }

      if (categoryName && !(item.productCategoryName ?? '').toLowerCase().includes(categoryName)) {
        return false
      }

      return true
    })

  return {
    code: 200,
    message: 'ok',
    data: paginate(filtered, params.pageNum, params.pageSize),
  }
}

export function productCreateAPI(data: PmsProductParam) {
  return http({
    method: 'POST',
    url: '/api/v1/products',
    data: mapRequest(data),
  })
}

export function productUpdateByIdAPI(id: number, data: PmsProductParam) {
  return http({
    method: 'PUT',
    url: `/api/v1/products/${id}`,
    data: {
      ...mapRequest(data),
      status: data.backendStatus ?? (data.publishStatus === 1 ? 'ON_SALE' : 'OFF_SHELF'),
    },
  })
}

export function getPruductUpdateInfoAPI(id: number) {
  return http<BackendProduct>({
    method: 'GET',
    url: `/api/v1/products/${id}`,
  }).then(res => ({
    ...res,
    data: {
      ...mapProduct(res.data),
      flashPromotionCount: 0,
      flashPromotionId: 0,
      flashPromotionPrice: 0,
      flashPromotionSort: 0,
      categoryName: res.data.categoryName,
      imageUrl: res.data.imageUrl,
      backendStatus: res.data.status,
    } as PmsProductParam,
  }))
}

export function productDeleteByIdAPI(id: number) {
  return http({
    method: 'DELETE',
    url: `/api/v1/products/${id}`,
  })
}
