import { useCallback, useEffect, useState } from 'react'
import type { Budget } from '@/types/budget.types'
import { ApiClientError } from '@/api/client'
import * as budgetsApi from '@/api/budgets.api'

export function useBudgets() {
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await budgetsApi.listBudgets()
      setBudgets(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load budgets')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { budgets, isLoading, error, refresh }
}
