export interface Category {
  id: number
  name: string
  categoryTypeCode: string
  categoryTypeName: string
  parentCategoryId: number | null
  parentCategoryName: string | null
  icon: string | null
  color: string | null
  isSystem: boolean
  isActive: boolean
  createdAt: string
}

export interface CreateCategoryRequest {
  name: string
  categoryTypeCode: string
  parentCategoryId?: number
  icon?: string
  color?: string
}

export interface UpdateCategoryRequest {
  name: string
  icon?: string
  color?: string
}
