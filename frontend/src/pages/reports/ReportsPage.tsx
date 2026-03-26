import { useState } from 'react'
import { TrendingUp, TrendingDown, Activity } from 'lucide-react'
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend,
  ResponsiveContainer,
} from 'recharts'
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

function shortMonthLabel(yearMonth: string): string {
  const [year, month] = yearMonth.split('-')
  const date = new Date(parseInt(year), parseInt(month) - 1)
  return date.toLocaleDateString('en-US', { month: 'short' })
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

  const trendChartData = trend?.months.map((item) => ({
    month: shortMonthLabel(item.month),
    income: parseFloat(item.income),
    expense: parseFloat(item.expense),
    net: parseFloat(item.net),
  })) ?? []

  const categoryChartData = report?.categoryBreakdown.map((cat) => ({
    name: cat.categoryName,
    amount: parseFloat(cat.amount),
  })) ?? []

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

          {/* Category Breakdown — Horizontal Bar Chart */}
          <Card>
            <CardHeader>
              <CardTitle>Category Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              {categoryChartData.length === 0 ? (
                <p className="text-sm text-muted-foreground">No expenses for this month</p>
              ) : (
                <div
                  role="img"
                  aria-label={`Horizontal bar chart showing expense breakdown by category for ${formatMonthLabel(selectedMonth)}`}
                >
                <ResponsiveContainer
                  width="100%"
                  height={Math.max(200, categoryChartData.length * 42)}
                >
                  <BarChart
                    layout="vertical"
                    data={categoryChartData}
                    margin={{ top: 0, right: 24, bottom: 0, left: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                    <XAxis
                      type="number"
                      tick={{ fontSize: 11 }}
                      tickFormatter={(v: number) => v.toFixed(0)}
                    />
                    <YAxis
                      type="category"
                      dataKey="name"
                      tick={{ fontSize: 11 }}
                      width={110}
                    />
                    <Tooltip
                      formatter={(value: unknown) => [
                        typeof value === 'number' ? value.toFixed(2) : '',
                        'Amount',
                      ]}
                      contentStyle={{ borderRadius: '8px', fontSize: '12px' }}
                    />
                    <Bar
                      dataKey="amount"
                      fill="hsl(var(--chart-1))"
                      radius={[0, 4, 4, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
                </div>
              )}
            </CardContent>
          </Card>
        </>
      ) : null}

      {/* 6-Month Trend — Line Chart */}
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
          ) : trendChartData.length > 0 ? (
            <div role="img" aria-label="Line chart showing 6-month trend of income, expense, and net cash flow">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trendChartData} margin={{ top: 4, right: 24, bottom: 4, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} tickFormatter={(v: number) => v.toFixed(0)} />
                <Tooltip
                  formatter={(value: unknown) => [
                    typeof value === 'number' ? value.toFixed(2) : '',
                    '',
                  ]}
                  contentStyle={{ borderRadius: '8px', fontSize: '12px' }}
                />
                <Legend wrapperStyle={{ fontSize: '12px' }} />
                <Line
                  type="monotone"
                  dataKey="income"
                  name="Income"
                  stroke="hsl(var(--chart-2))"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
                <Line
                  type="monotone"
                  dataKey="expense"
                  name="Expense"
                  stroke="hsl(var(--chart-3))"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
                <Line
                  type="monotone"
                  dataKey="net"
                  name="Net"
                  stroke="hsl(var(--chart-1))"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No trend data available</p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
