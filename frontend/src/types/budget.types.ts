export type BudgetPeriod = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY' | 'CUSTOM'

export interface Budget {
  id: number
  categoryId: number
  categoryName: string
  periodType: BudgetPeriod
  amount: string
  currency: string
  startDate: string
  endDate: string | null
  rolloverEnabled: boolean
  alertThresholdPct: number | null
  isActive: boolean
  spentAmount: string
  remainingAmount: string
  percentUsed: number
  alertTriggered: boolean
  createdAt: string
}

export interface CreateBudgetRequest {
  categoryId: number
  periodType: BudgetPeriod
  amount: number
  currency: string
  startDate: string
  endDate?: string
  rolloverEnabled: boolean
  alertThresholdPct?: number
}

export interface UpdateBudgetRequest {
  amount: number
  currency: string
  endDate?: string
  rolloverEnabled: boolean
  alertThresholdPct?: number
}
