import { api } from './client'
import type { Budget, BudgetPlan, CreateBudgetRequest, CopyBudgetsRequest, CopyBudgetsResult, UpdateBudgetRequest } from '@/types/budget.types'

const BASE = '/api/v1/budgets'

export interface UpsertBudgetByCategoryRequest {
  categoryId: number
  periodType: string
  amount: string
  currency: string
  startDate: string
  endDate: string
}

export function listBudgets(): Promise<Budget[]> {
  return api.get<{ content: Budget[] }>(BASE).then(data => data.content)
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

export function getBudgetPlan(startDate: string, endDate: string): Promise<BudgetPlan> {
  return api.get<BudgetPlan>(`${BASE}/plan?startDate=${startDate}&endDate=${endDate}`)
}

export function upsertBudgetByCategory(data: UpsertBudgetByCategoryRequest): Promise<Budget> {
  return api.post<Budget>(`${BASE}/upsert-by-category`, data)
}

export function copyBudgetsFromPreviousMonth(data: CopyBudgetsRequest): Promise<CopyBudgetsResult> {
  return api.post<CopyBudgetsResult>(`${BASE}/copy-from-previous-month`, data)
}
