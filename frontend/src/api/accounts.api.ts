import { api } from './client'
import type { Account, CreateAccountRequest, UpdateAccountRequest, NetWorth } from '@/types/account.types'

const BASE = '/api/v1/accounts'

export function listAccounts(): Promise<Account[]> {
  return api.get<Account[]>(BASE)
}

export function getAccount(id: number): Promise<Account> {
  return api.get<Account>(`${BASE}/${id}`)
}

export function createAccount(data: CreateAccountRequest): Promise<Account> {
  return api.post<Account>(BASE, data)
}

export function updateAccount(id: number, data: UpdateAccountRequest): Promise<Account> {
  return api.put<Account>(`${BASE}/${id}`, data)
}

export function deleteAccount(id: number): Promise<void> {
  return api.delete(`${BASE}/${id}`)
}

export function getNetWorth(): Promise<NetWorth> {
  return api.get<NetWorth>(`${BASE}/net-worth`)
}
