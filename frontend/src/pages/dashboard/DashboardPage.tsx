import { Link } from 'react-router-dom'
import { Plus, TrendingUp, TrendingDown, DollarSign, ArrowUpCircle, ArrowDownCircle, Activity, AlertTriangle } from 'lucide-react'
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { useDashboard } from '@/hooks/useDashboard'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { TransactionAmount } from '@/components/shared/TransactionAmount'
import { typeBadgeVariant, typeLabel, rowBorderClass } from '@/lib/transactionUtils'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

const CHART_COLORS = [
  'hsl(var(--chart-1))',
  'hsl(var(--chart-2))',
  'hsl(var(--chart-3))',
  'hsl(var(--chart-4))',
  'hsl(var(--chart-5))',
]

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
  const isPositive = netCashFlowNum >= 0

  const donutData = dashboard.topExpenseCategories.map((cat) => ({
    name: cat.categoryName,
    value: parseFloat(cat.amount),
  }))

  const donutTotal = donutData.reduce((sum, d) => sum + d.value, 0)

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
        <Card className="border-blue-500/20 bg-gradient-to-br from-blue-500/10 to-indigo-500/5 dark:from-blue-500/15 dark:to-indigo-500/5">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Worth</CardTitle>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-500/15">
              <DollarSign className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              <MoneyDisplay amount={dashboard.netWorth} currency={dashboard.currency} colored />
            </div>
          </CardContent>
        </Card>

        <Card className="border-green-500/20 bg-gradient-to-br from-green-500/10 to-emerald-500/5 dark:from-green-500/15 dark:to-emerald-500/5">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Assets</CardTitle>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-green-500/15">
              <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              <MoneyDisplay amount={dashboard.totalAssets} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>

        <Card className="border-red-500/20 bg-gradient-to-br from-red-500/10 to-rose-500/5 dark:from-red-500/15 dark:to-rose-500/5">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Liabilities</CardTitle>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-red-500/15">
              <TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400" />
            </div>
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
        <Card className="border-green-500/20 bg-gradient-to-br from-green-500/10 to-teal-500/5 dark:from-green-500/15 dark:to-teal-500/5">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Income</CardTitle>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-green-500/15">
              <ArrowUpCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600 dark:text-green-400">
              <MoneyDisplay amount={dashboard.currentMonthIncome} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>

        <Card className="border-red-500/20 bg-gradient-to-br from-red-500/10 to-orange-500/5 dark:from-red-500/15 dark:to-orange-500/5">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Monthly Expense</CardTitle>
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-red-500/15">
              <ArrowDownCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600 dark:text-red-400">
              <MoneyDisplay amount={dashboard.currentMonthExpense} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>

        <Card className={isPositive
          ? 'border-green-500/20 bg-gradient-to-br from-green-500/10 to-cyan-500/5 dark:from-green-500/15 dark:to-cyan-500/5'
          : 'border-red-500/20 bg-gradient-to-br from-red-500/10 to-orange-500/5 dark:from-red-500/15 dark:to-orange-500/5'
        }>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Net Cash Flow</CardTitle>
            <div className={`flex h-9 w-9 items-center justify-center rounded-lg ${isPositive ? 'bg-green-500/15' : 'bg-red-500/15'}`}>
              <Activity className={`h-5 w-5 ${isPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`} />
            </div>
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${isPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
              <MoneyDisplay amount={dashboard.netCashFlow} currency={dashboard.currency} />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Row 3: Top Categories (Donut) + Budget Alerts */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Top Expense Categories</CardTitle>
          </CardHeader>
          <CardContent>
            {donutData.length === 0 ? (
              <p className="text-sm text-muted-foreground">No expenses this month</p>
            ) : (
              <div className="relative h-[260px] w-full">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={donutData}
                      cx="50%"
                      cy="45%"
                      innerRadius={55}
                      outerRadius={90}
                      paddingAngle={2}
                      dataKey="value"
                    >
                      {donutData.map((_, index) => (
                        <Cell key={index} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip
                      formatter={(value: number | undefined) => [
                        typeof value === 'number' ? `${dashboard.currency} ${value.toFixed(2)}` : '',
                        'Spent',
                      ]}
                      contentStyle={{ borderRadius: '8px', fontSize: '12px' }}
                    />
                    <Legend iconSize={10} wrapperStyle={{ fontSize: '12px' }} />
                  </PieChart>
                </ResponsiveContainer>
                {/* Center total */}
                <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center pb-8">
                  <span className="text-xs text-muted-foreground">Total</span>
                  <span className="text-sm font-bold">
                    {dashboard.currency} {donutTotal.toFixed(2)}
                  </span>
                </div>
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
                  const barColor = pct >= 100
                    ? 'bg-gradient-to-r from-red-500 to-rose-400'
                    : pct >= 85
                    ? 'bg-gradient-to-r from-orange-500 to-amber-400'
                    : 'bg-gradient-to-r from-yellow-500 to-amber-400'
                  const trackColor = pct >= 100
                    ? 'bg-red-100 dark:bg-red-950'
                    : pct >= 85
                    ? 'bg-orange-100 dark:bg-orange-950'
                    : 'bg-yellow-100 dark:bg-yellow-950'
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
                      <div className={`h-2 rounded-full ${trackColor}`}>
                        <div
                          className={`h-2 rounded-full transition-all ${barColor}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <p className="text-right text-xs text-muted-foreground">{alert.percentUsed.toFixed(0)}% used</p>
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
        <CardContent className="p-0">
          {dashboard.recentTransactions.length === 0 ? (
            <p className="px-4 py-6 text-sm text-muted-foreground">No transactions yet</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b text-left text-sm text-muted-foreground">
                    <th className="px-4 py-3 font-medium">Date</th>
                    <th className="px-4 py-3 font-medium">Description</th>
                    <th className="px-4 py-3 font-medium">Category</th>
                    <th className="px-4 py-3 font-medium">Type</th>
                    <th className="px-4 py-3 text-right font-medium">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.recentTransactions.map((txn) => (
                    <tr
                      key={txn.id}
                      className={`border-b last:border-0 hover:bg-accent/50 transition-colors ${rowBorderClass(txn.type)}`}
                    >
                      <td className="px-4 py-3 text-sm">{txn.date}</td>
                      <td className="px-4 py-3 text-sm font-medium">{txn.description || txn.categoryName}</td>
                      <td className="px-4 py-3 text-sm">{txn.categoryName}</td>
                      <td className="px-4 py-3">
                        <Badge variant={typeBadgeVariant(txn.type)}>{typeLabel(txn.type)}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <TransactionAmount
                          amount={txn.amount}
                          currency={txn.currency}
                          type={txn.type}
                          className="text-sm font-medium"
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
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
              <Link to="/accounts/new" className="text-blue-600 dark:text-blue-400 hover:underline">
                Create your first account
              </Link>
            </p>
          ) : (
            <div className="space-y-2">
              {dashboard.accountBalances.map((account) => (
                <Link
                  key={account.id}
                  to={`/accounts/${account.id}`}
                  className="flex items-center justify-between rounded-xl border p-3 transition-all duration-200 hover:bg-accent hover:shadow-sm"
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
