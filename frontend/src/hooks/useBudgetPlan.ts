import { useState, useEffect, useCallback } from 'react'
import { getBudgetPlan } from '@/api/budgets.api'
import type { BudgetPlan } from '@/types/budget.types'

export function useBudgetPlan(startDate: string, endDate: string) {
  const [plan, setPlan] = useState<BudgetPlan | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await getBudgetPlan(startDate, endDate)
      setPlan(data)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load budget plan')
    } finally {
      setIsLoading(false)
    }
  }, [startDate, endDate])

  useEffect(() => { refresh() }, [refresh])

  return { plan, isLoading, error, refresh }
}
