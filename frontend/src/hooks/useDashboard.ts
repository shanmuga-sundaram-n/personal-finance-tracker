import { useCallback, useEffect, useState } from 'react'
import type { DashboardData } from '@/types/report.types'
import { ApiClientError } from '@/api/client'
import * as reportsApi from '@/api/reports.api'

export function useDashboard() {
  const [dashboard, setDashboard] = useState<DashboardData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await reportsApi.getDashboard()
      setDashboard(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load dashboard')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { dashboard, isLoading, error, refresh }
}
