import { useCallback, useEffect, useState } from 'react'
import type { Category } from '@/types/category.types'
import { ApiClientError } from '@/api/client'
import * as categoriesApi from '@/api/categories.api'

export function useCategories(type?: string) {
  const [categories, setCategories] = useState<Category[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await categoriesApi.listCategories(type)
      setCategories(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load categories')
    } finally {
      setIsLoading(false)
    }
  }, [type])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { categories, isLoading, error, refresh }
}
