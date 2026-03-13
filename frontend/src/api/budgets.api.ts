import { api } from './client'
import type { Budget, CreateBudgetRequest, UpdateBudgetRequest } from '@/types/budget.types'

const BASE = '/api/v1/budgets'

export function listBudgets(): Promise<Budget[]> {
  return api.get<Budget[]>(BASE)
}

export function getBudget(id: number): Promise<Budget> {
  return api.get<Budget>(`${BASE}/${id}`)
}

export function createBudget(data: CreateBudgetRequest): Promise<Budget> {
  return api.post<Budget>(BASE, data)
}

export function updateBudget(id: number, data: UpdateBudgetRequest): Promise<Budget> {
  return api.put<Budget>(`${BASE}/${id}`, data)
}

export function deleteBudget(id: number): Promise<void> {
  return api.delete(`${BASE}/${id}`)
}
