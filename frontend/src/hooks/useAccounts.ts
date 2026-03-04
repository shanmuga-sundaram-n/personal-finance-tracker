import { useCallback, useEffect, useState } from 'react'
import type { Account, NetWorth } from '@/types/account.types'
import { ApiClientError } from '@/api/client'
import * as accountsApi from '@/api/accounts.api'

export function useAccounts() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await accountsApi.listAccounts()
      setAccounts(data)
    } catch (e) {
      // 401 is handled globally (auto-redirect to login), don't show error
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load accounts')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { accounts, isLoading, error, refresh }
}

export function useNetWorth() {
  const [netWorth, setNetWorth] = useState<NetWorth | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const data = await accountsApi.getNetWorth()
      setNetWorth(data)
    } catch (e) {
      if (e instanceof ApiClientError && e.status === 401) return
      setError(e instanceof Error ? e.message : 'Failed to load net worth')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { netWorth, isLoading, error, refresh }
}
