export interface AccountBalanceSummary {
  id: number
  name: string
  balance: string
  currency: string
  isLiability: boolean
}

export interface CategorySpendingSummary {
  categoryId: number
  categoryName: string
  amount: string
}

export interface RecentTransactionSummary {
  id: number
  description: string
  amount: string
  currency: string
  type: string
  categoryName: string
  date: string
}

export interface BudgetProgressSummary {
  budgetId: number
  categoryName: string
  percentUsed: number
  amount: string
  spent: string
}

export interface DashboardData {
  netWorth: string
  totalAssets: string
  totalLiabilities: string
  currency: string
  currentMonthIncome: string
  currentMonthExpense: string
  netCashFlow: string
  accountBalances: AccountBalanceSummary[]
  topExpenseCategories: CategorySpendingSummary[]
  recentTransactions: RecentTransactionSummary[]
  budgetAlerts: BudgetProgressSummary[]
}

export interface SpendingReport {
  month: string
  totalIncome: string
  totalExpense: string
  netFlow: string
  categoryBreakdown: CategorySpendingSummary[]
}

export interface MonthlyTrendItem {
  month: string
  income: string
  expense: string
  net: string
}

export interface MonthlyTrend {
  months: MonthlyTrendItem[]
}
