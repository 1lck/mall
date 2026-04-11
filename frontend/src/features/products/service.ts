import { request } from '../../shared/api/http'
import type {
  CreateProductInput,
  ProductImageUploadResult,
  ProductRecord,
  UpdateProductInput,
} from './types'

export async function listProducts(): Promise<ProductRecord[]> {
  return request<ProductRecord[]>('/api/v1/products')
}

export async function getProductById(productId: number | string): Promise<ProductRecord> {
  return request<ProductRecord>(`/api/v1/products/${productId}`)
}

export async function createProduct(input: CreateProductInput): Promise<ProductRecord> {
  return request<ProductRecord>('/api/v1/products', {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export async function uploadProductImage(file: File): Promise<ProductImageUploadResult> {
  const formData = new FormData()
  formData.append('file', file)

  return request<ProductImageUploadResult>('/api/v1/products/images/upload', {
    method: 'POST',
    body: formData,
  })
}

export async function updateProduct(
  productId: number | string,
  input: UpdateProductInput,
): Promise<ProductRecord> {
  return request<ProductRecord>(`/api/v1/products/${productId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export async function deleteProduct(productId: number | string): Promise<string> {
  return request<string>(`/api/v1/products/${productId}`, {
    method: 'DELETE',
  })
}
