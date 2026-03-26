export type BudgetPeriod = 'WEEKLY' | 'BI_WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUALLY' | 'CUSTOM'

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

export interface BudgetPlanCategoryRow {
  categoryId: number
  categoryName: string
  budgetId: number | null
  budgetedAmount: string
  actualAmount: string
  varianceAmount: string
  percentUsed: number
  hasBudget: boolean
  frequency: string | null
  monthlyAmount: string
  yearlyAmount: string
}

export interface BudgetPlanTotals {
  totalBudgeted: string
  totalActual: string
  totalVariance: string
  totalPercentUsed: number
  totalMonthly: string
  totalYearly: string
}

export interface BudgetPlanCategoryGroup {
  parentCategoryId: number | null
  parentCategoryName: string
  rows: BudgetPlanCategoryRow[]
  groupMonthlyTotal: string
  groupYearlyTotal: string
  groupActualTotal: string
  alertTriggered: boolean
}

export interface BudgetPlan {
  startDate: string
  endDate: string
  currency: string
  incomeRows: BudgetPlanCategoryRow[]
  expenseGroups: BudgetPlanCategoryGroup[]
  incomeTotals: BudgetPlanTotals
  expenseTotals: BudgetPlanTotals
}

export interface CopyBudgetsRequest {
  targetYear: number
  targetMonth: number // 1-12
  overwriteExisting: boolean
}

export interface CopyBudgetsResult {
  copiedCount: number
  skippedCount: number
  conflictCount: number
  overwrittenCount: number
}
