import { api } from './client'
import type {
  CreateTransactionRequest,
  CreateTransferRequest,
  Transaction,
  TransactionFilters,
  TransactionPage,
  TransferResult,
  UpdateTransactionRequest,
} from '@/types/transaction.types'

const BASE = '/api/v1/transactions'

export function listTransactions(filters: TransactionFilters = {}): Promise<TransactionPage> {
  const params = new URLSearchParams()
  if (filters.accountId) params.set('accountId', String(filters.accountId))
  if (filters.categoryId) params.set('categoryId', String(filters.categoryId))
  if (filters.type) params.set('type', filters.type)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  if (filters.page !== undefined) params.set('page', String(filters.page))
  if (filters.size !== undefined) params.set('size', String(filters.size))
  const qs = params.toString()
  return api.get<TransactionPage>(qs ? `${BASE}?${qs}` : BASE)
}

export function getTransaction(id: number): Promise<Transaction> {
  return api.get<Transaction>(`${BASE}/${id}`)
}

export function createTransaction(data: CreateTransactionRequest): Promise<Transaction> {
  return api.post<Transaction>(BASE, data)
}

export function createTransfer(data: CreateTransferRequest): Promise<TransferResult> {
  return api.post<TransferResult>(`${BASE}/transfers`, data)
}

export function updateTransaction(id: number, data: UpdateTransactionRequest): Promise<Transaction> {
  return api.put<Transaction>(`${BASE}/${id}`, data)
}

export function deleteTransaction(id: number): Promise<void> {
  return api.delete(`${BASE}/${id}`)
}
