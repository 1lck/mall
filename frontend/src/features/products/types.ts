export type ProductStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SHELF'

export interface ProductRecord {
  id: number
  productNo: string
  name: string
  categoryName: string
  price: number
  stock: number
  status: ProductStatus
  description?: string
  imageUrl?: string
  createdAt: string
  updatedAt: string
}

export interface CreateProductInput {
  name: string
  categoryName: string
  price: number
  stock: number
  description?: string
  imageUrl?: string
}

export interface UpdateProductInput {
  name: string
  categoryName: string
  price: number
  stock: number
  status: ProductStatus
  description?: string
  imageUrl?: string
}

export interface ProductEditSummary {
  id: number
  productNo: string
  createdAt: string
  updatedAt: string
}

export interface ProductImageUploadResult {
  objectKey: string
  imageUrl: string
}
