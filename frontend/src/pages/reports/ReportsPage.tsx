import { useState } from 'react'
import { TrendingUp, TrendingDown, Activity } from 'lucide-react'
import { useSpendingReport, useTrend } from '@/hooks/useReports'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

function getCurrentMonth(): string {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

function formatMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-')
  const date = new Date(parseInt(year), parseInt(month) - 1)
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long' })
}

export function ReportsPage() {
  const [selectedMonth, setSelectedMonth] = useState(getCurrentMonth())
  const { report, isLoading: reportLoading, error: reportError } = useSpendingReport(selectedMonth)
  const { trend, isLoading: trendLoading, error: trendError } = useTrend(6)

  const handlePrevMonth = () => {
    const [year, month] = selectedMonth.split('-').map(Number)
    const d = new Date(year, month - 2)
    setSelectedMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }

  const handleNextMonth = () => {
    const [year, month] = selectedMonth.split('-').map(Number)
    const d = new Date(year, month)
    setSelectedMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Reports</h1>

      {/* Month Selector */}
      <div className="flex items-center gap-4">
        <Button variant="outline" size="sm" onClick={handlePrevMonth}>Previous</Button>
        <span className="text-lg font-medium">{formatMonthLabel(selectedMonth)}</span>
        <Button variant="outline" size="sm" onClick={handleNextMonth}>Next</Button>
      </div>

      {/* Monthly Summary */}
      {reportLoading ? (
        <div className="flex items-center justify-center py-8">
          <LoadingSpinner size="lg" />
        </div>
      ) : reportError ? (
        <ErrorAlert message={reportError} />
      ) : report ? (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Income</CardTitle>
                <TrendingUp className="h-4 w-4 text-green-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">
                  <MoneyDisplay amount={report.totalIncome} />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Expenses</CardTitle>
                <TrendingDown className="h-4 w-4 text-red-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-red-600">
                  <MoneyDisplay amount={report.totalExpense} />
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Net Flow</CardTitle>
                <Activity className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">
                  <MoneyDisplay amount={report.netFlow} colored />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Category Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle>Category Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              {report.categoryBreakdown.length === 0 ? (
                <p className="text-sm text-muted-foreground">No expenses for this month</p>
              ) : (
                <div className="space-y-3">
                  {report.categoryBreakdown.map((cat) => {
                    const maxAmount = parseFloat(report.categoryBreakdown[0].amount)
                    const catAmount = parseFloat(cat.amount)
                    const pct = maxAmount > 0 ? (catAmount / maxAmount) * 100 : 0
                    return (
                      <div key={cat.categoryId} className="space-y-1">
                        <div className="flex items-center justify-between text-sm">
                          <span>{cat.categoryName}</span>
                          <MoneyDisplay amount={cat.amount} className="text-sm" />
                        </div>
                        <div className="h-2 rounded-full bg-muted">
                          <div
                            className="h-2 rounded-full bg-primary"
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
        </>
      ) : null}

      {/* 6-Month Trend */}
      <Card>
        <CardHeader>
          <CardTitle>6-Month Trend</CardTitle>
        </CardHeader>
        <CardContent>
          {trendLoading ? (
            <div className="flex items-center justify-center py-8">
              <LoadingSpinner />
            </div>
          ) : trendError ? (
            <ErrorAlert message={trendError} />
          ) : trend && trend.months.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="py-2 text-left font-medium">Month</th>
                    <th className="py-2 text-right font-medium">Income</th>
                    <th className="py-2 text-right font-medium">Expenses</th>
                    <th className="py-2 text-right font-medium">Net</th>
                  </tr>
                </thead>
                <tbody>
                  {trend.months.map((item) => (
                    <tr key={item.month} className="border-b last:border-0">
                      <td className="py-2">{formatMonthLabel(item.month)}</td>
                      <td className="py-2 text-right text-green-600">
                        <MoneyDisplay amount={item.income} />
                      </td>
                      <td className="py-2 text-right text-red-600">
                        <MoneyDisplay amount={item.expense} />
                      </td>
                      <td className="py-2 text-right">
                        <MoneyDisplay amount={item.net} colored />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No trend data available</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
