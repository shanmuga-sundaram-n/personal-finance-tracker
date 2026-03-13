import { api } from './client'
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from '@/types/category.types'

const BASE = '/api/v1/categories'

export function listCategories(type?: string): Promise<Category[]> {
  const url = type ? `${BASE}?type=${encodeURIComponent(type)}` : BASE
  return api.get<Category[]>(url)
}

export function getCategory(id: number): Promise<Category> {
  return api.get<Category>(`${BASE}/${id}`)
}

export function createCategory(data: CreateCategoryRequest): Promise<Category> {
  return api.post<Category>(BASE, data)
}

export function updateCategory(id: number, data: UpdateCategoryRequest): Promise<Category> {
  return api.put<Category>(`${BASE}/${id}`, data)
}

export function deleteCategory(id: number): Promise<void> {
  return api.delete(`${BASE}/${id}`)
}
