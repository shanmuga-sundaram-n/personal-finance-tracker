import { Link } from 'react-router-dom'
import { Plus, TrendingUp, TrendingDown, DollarSign, ArrowUpCircle, ArrowDownCircle, Activity, AlertTriangle } from 'lucide-react'
import { useDashboard } from '@/hooks/useDashboard'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

export function DashboardPage() {
  const { dashboard, isLoading, error } = useDashboard()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) {
    return <ErrorAlert message={error} />
  }

  if (!dashboard) return null

  const netCashFlowNum = parseFloat(dashboard.netCashFlow)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <Button asChild>
          <Link to="/accounts/new">
            <Plus className="mr-2 h-4 w-4" />
            Add Account
          </Link>
        </Button>
      </div>

      {/* Row 1: Net Worth */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Worth</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              <MoneyDisplay amount={dashboard.netWorth} currency={dashboard.currency} colored />
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Assets</CardTitle>
            <TrendingUp className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              <MoneyDisplay amount={dashboard.totalAssets} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Liabilities</CardTitle>
            <TrendingDown className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              <MoneyDisplay amount={dashboard.totalLiabilities} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Row 2: Current Month */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Income</CardTitle>
            <ArrowUpCircle className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              <MoneyDisplay amount={dashboard.currentMonthIncome} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Expense</CardTitle>
            <ArrowDownCircle className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              <MoneyDisplay amount={dashboard.currentMonthExpense} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Cash Flow</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${netCashFlowNum >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              <MoneyDisplay amount={dashboard.netCashFlow} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Row 3: Top Categories + Budget Alerts */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Top Expense Categories</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboard.topExpenseCategories.length === 0 ? (
              <p className="text-sm text-muted-foreground">No expenses this month</p>
            ) : (
              <div className="space-y-3">
                {dashboard.topExpenseCategories.map((cat) => {
                  const maxAmount = parseFloat(dashboard.topExpenseCategories[0].amount)
                  const catAmount = parseFloat(cat.amount)
                  const pct = maxAmount > 0 ? (catAmount / maxAmount) * 100 : 0
                  return (
                    <div key={cat.categoryId} className="space-y-1">
                      <div className="flex items-center justify-between text-sm">
                        <span>{cat.categoryName}</span>
                        <MoneyDisplay amount={cat.amount} currency={dashboard.currency} className="text-sm" />
                      </div>
                      <div className="h-2 rounded-full bg-muted">
                        <div
                          className="h-2 rounded-full bg-red-500"
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Budget Alerts</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboard.budgetAlerts.length === 0 ? (
              <p className="text-sm text-muted-foreground">All budgets on track</p>
            ) : (
              <div className="space-y-3">
                {dashboard.budgetAlerts.map((alert) => {
                  const pct = Math.min(alert.percentUsed, 100)
                  const barColor = pct >= 100 ? 'bg-red-500' : pct >= 85 ? 'bg-orange-500' : 'bg-yellow-500'
                  return (
                    <div key={alert.budgetId} className="space-y-1">
                      <div className="flex items-center justify-between text-sm">
                        <div className="flex items-center gap-1">
                          {pct >= 100 && <AlertTriangle className="h-3 w-3 text-red-500" />}
                          <span>{alert.categoryName}</span>
                        </div>
                        <span className="text-muted-foreground">
                          <MoneyDisplay amount={alert.spent} currency={dashboard.currency} className="text-sm" />
                          {' / '}
                          <MoneyDisplay amount={alert.amount} currency={dashboard.currency} className="text-sm" />
                        </span>
                      </div>
                      <div className="h-2 rounded-full bg-muted">
                        <div
                          className={`h-2 rounded-full ${barColor}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <p className="text-xs text-muted-foreground text-right">{alert.percentUsed.toFixed(0)}% used</p>
                    </div>
                  )
                })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Row 4: Recent Transactions */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Recent Transactions</CardTitle>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/transactions">View all</Link>
          </Button>
        </CardHeader>
        <CardContent>
          {dashboard.recentTransactions.length === 0 ? (
            <p className="text-sm text-muted-foreground">No transactions yet</p>
          ) : (
            <div className="space-y-2">
              {dashboard.recentTransactions.map((txn) => (
                <div
                  key={txn.id}
                  className="flex items-center justify-between rounded-md border p-2"
                >
                  <div className="flex flex-col">
                    <span className="text-sm font-medium">{txn.description || txn.categoryName}</span>
                    <div className="flex gap-2 text-xs text-muted-foreground">
                      <Badge variant="secondary" className="text-xs">{txn.categoryName}</Badge>
                      <span>{txn.date}</span>
                    </div>
                  </div>
                  <span className={`font-mono text-sm font-medium ${txn.type === 'INCOME' ? 'text-green-600' : 'text-red-600'}`}>
                    {txn.type === 'INCOME' ? '+' : '-'}
                    <MoneyDisplay amount={txn.amount} currency={txn.currency} />
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Row 5: Accounts */}
      <Card>
        <CardHeader>
          <CardTitle>Accounts</CardTitle>
        </CardHeader>
        <CardContent>
          {dashboard.accountBalances.length === 0 ? (
            <p className="py-4 text-center text-muted-foreground">
              No accounts yet.{' '}
              <Link to="/accounts/new" className="text-primary hover:underline">
                Create your first account
              </Link>
            </p>
          ) : (
            <div className="space-y-3">
              {dashboard.accountBalances.map((account) => (
                <Link
                  key={account.id}
                  to={`/accounts/${account.id}`}
                  className="flex items-center justify-between rounded-md border p-3 transition-colors hover:bg-accent"
                >
                  <div className="flex items-center gap-3">
                    <div>
                      <p className="font-medium">{account.name}</p>
                      {account.isLiability && (
                        <Badge variant="destructive" className="mt-1 text-xs">Liability</Badge>
                      )}
                    </div>
                  </div>
                  <MoneyDisplay
                    amount={account.balance}
                    currency={account.currency}
                    colored
                    className="text-lg font-semibold"
                  />
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
