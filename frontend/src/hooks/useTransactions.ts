import { useCallback, useEffect, useState } from 'react'
import type { Transaction, TransactionFilters } from '@/types/transaction.types'
import { ApiClientError } from '@/api/client'
import * as transactionsApi from '@/api/transactions.api'

export function useTransactions(initialFilters: TransactionFilters = {}) {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filters, setFilters] = useState<TransactionFilters>(initialFilters)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await transactionsApi.listTransactions(filters)
      setTransactions(data.content)
      setPage(data.page)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load transactions')
    } finally {
      setIsLoading(false)
    }
  }, [filters])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { transactions, page, totalPages, totalElements, isLoading, error, filters, setFilters, refresh }
}
