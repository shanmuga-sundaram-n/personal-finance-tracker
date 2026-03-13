import { useCallback, useEffect, useState } from 'react'
import type { SpendingReport, MonthlyTrend } from '@/types/report.types'
import { ApiClientError } from '@/api/client'
import * as reportsApi from '@/api/reports.api'

export function useSpendingReport(month: string) {
  const [report, setReport] = useState<SpendingReport | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await reportsApi.getSpendingReport(month)
      setReport(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load spending report')
    } finally {
      setIsLoading(false)
    }
  }, [month])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { report, isLoading, error, refresh }
}

export function useTrend(months: number = 6) {
  const [trend, setTrend] = useState<MonthlyTrend | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await reportsApi.getTrend(months)
      setTrend(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load trend data')
    } finally {
      setIsLoading(false)
    }
  }, [months])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { trend, isLoading, error, refresh }
}
