import { useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, AlertTriangle, TrendingUp, TrendingDown, Activity } from 'lucide-react'
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { useBudgetPlan } from '@/hooks/useBudgetPlan'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import type { BudgetPlanCategoryRow, BudgetPlanCategoryGroup, BudgetPlanTotals } from '@/types/budget.types'

// ── Date helpers ──────────────────────────────────────────────────────────────

function toLocalDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function firstDayOfMonth(year: number, month: number) {
  return toLocalDate(new Date(year, month, 1))
}

function lastDayOfMonth(year: number, month: number) {
  return toLocalDate(new Date(year, month + 1, 0))
}

function monthLabel(year: number, month: number) {
  return new Date(year, month, 1).toLocaleString(undefined, { month: 'long', year: 'numeric' })
}

// ── Number formatting ─────────────────────────────────────────────────────────

function fmt(value: string | number): string {
  const n = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(n)) return '—'
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtSigned(value: string): string {
  const n = parseFloat(value)
  if (isNaN(n)) return '—'
  const abs = Math.abs(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return n > 0 ? `+${abs}` : n < 0 ? `-${abs}` : abs
}

// ── Variance colour ───────────────────────────────────────────────────────────

function varianceClass(value: string, hasBudget: boolean): string {
  if (!hasBudget) return 'text-muted-foreground'
  const n = parseFloat(value)
  if (n > 0) return 'text-green-600 dark:text-green-400'
  if (n < 0) return 'text-red-600 dark:text-red-400'
  return ''
}

// ── Column header strip ───────────────────────────────────────────────────────

function ColHeaders() {
  return (
    <div className="hidden sm:grid sm:grid-cols-[1fr_110px_110px_110px] gap-x-3 py-2 px-3 border-b border-border/40">
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">Category</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">Budget</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">Actual</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">Vs Budget</p>
    </div>
  )
}

// ── Single category row ───────────────────────────────────────────────────────

function CategoryRow({ row, indent = false }: { row: BudgetPlanCategoryRow; indent?: boolean }) {
  const overBudget = row.hasBudget && row.percentUsed > 100

  return (
    <div className={`py-2.5 border-b border-border/40 last:border-0 ${indent ? 'pl-8' : 'pl-3'}`}>
      {/* Desktop */}
      <div className="hidden sm:grid sm:grid-cols-[1fr_110px_110px_110px] gap-x-3 items-center">
        <div className="flex items-center gap-1.5 min-w-0">
          {overBudget && <AlertTriangle className="h-3.5 w-3.5 text-yellow-500 shrink-0" />}
          <span className="text-sm truncate">{row.categoryName}</span>
        </div>
        <p className="text-sm text-right tabular-nums">
          {row.hasBudget ? fmt(row.monthlyAmount) : '—'}
        </p>
        <p className="text-sm text-right tabular-nums">{fmt(row.actualAmount)}</p>
        <p className={`text-sm text-right font-medium tabular-nums ${varianceClass(row.varianceAmount, row.hasBudget)}`}>
          {row.hasBudget ? fmtSigned(row.varianceAmount) : '—'}
        </p>
      </div>

      {/* Mobile */}
      <div className="sm:hidden space-y-1">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 min-w-0">
            {overBudget && <AlertTriangle className="h-3.5 w-3.5 text-yellow-500 shrink-0" />}
            <span className="text-sm font-medium truncate">{row.categoryName}</span>
          </div>
          <span className={`text-sm font-medium tabular-nums ${varianceClass(row.varianceAmount, row.hasBudget)}`}>
            {row.hasBudget ? fmtSigned(row.varianceAmount) : '—'}
          </span>
        </div>
        <div className="flex gap-4 text-xs text-muted-foreground">
          <span>Budget: {row.hasBudget ? fmt(row.monthlyAmount) : '—'}</span>
          <span>Actual: {fmt(row.actualAmount)}</span>
        </div>
      </div>
    </div>
  )
}

// ── Group header row ──────────────────────────────────────────────────────────

function GroupHeaderRow({ group }: { group: BudgetPlanCategoryGroup }) {
  const variance = parseFloat(group.groupMonthlyTotal) - parseFloat(group.groupActualTotal)
  const varianceStr = variance.toFixed(4)

  return (
    <div className="py-2.5 pl-3 mt-1 bg-muted/50 rounded-sm border-l-4 border-red-400">
      {/* Desktop — same grid as ColHeaders and CategoryRow */}
      <div className="hidden sm:grid sm:grid-cols-[1fr_110px_110px_110px] gap-x-3 items-center">
        <div className="flex items-center gap-1.5 min-w-0">
          {group.alertTriggered && <AlertTriangle className="h-3.5 w-3.5 text-yellow-500 shrink-0" />}
          <span className="text-sm font-bold truncate">{group.parentCategoryName}</span>
        </div>
        <p className="text-sm font-bold text-right tabular-nums">{fmt(group.groupMonthlyTotal)}</p>
        <p className="text-sm font-bold text-right tabular-nums">{fmt(group.groupActualTotal)}</p>
        <p className={`text-sm font-bold text-right tabular-nums ${varianceClass(varianceStr, true)}`}>
          {fmtSigned(varianceStr)}
        </p>
      </div>
      {/* Mobile */}
      <div className="sm:hidden flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          {group.alertTriggered && <AlertTriangle className="h-3.5 w-3.5 text-yellow-500 shrink-0" />}
          <span className="text-sm font-bold truncate">{group.parentCategoryName}</span>
        </div>
        <div className="flex items-center gap-3 shrink-0 text-xs">
          <span className="font-bold text-foreground tabular-nums">{fmt(group.groupActualTotal)}</span>
          <span className={`font-bold tabular-nums ${varianceClass(varianceStr, true)}`}>{fmtSigned(varianceStr)}</span>
        </div>
      </div>
    </div>
  )
}

// ── Totals row ────────────────────────────────────────────────────────────────

function TotalsRow({ totals, label }: { totals: BudgetPlanTotals; label: string }) {
  return (
    <div className="py-2.5 px-3 border-t-2 border-border mt-1">
      <div className="hidden sm:grid sm:grid-cols-[1fr_110px_110px_110px] gap-x-3 items-center">
        <p className="text-sm font-bold">{label}</p>
        <p className="text-sm font-bold text-right tabular-nums">{fmt(totals.totalBudgeted)}</p>
        <p className="text-sm font-bold text-right tabular-nums">{fmt(totals.totalActual)}</p>
        <p className={`text-sm font-bold text-right tabular-nums ${varianceClass(totals.totalVariance, true)}`}>
          {fmtSigned(totals.totalVariance)}
        </p>
      </div>
      <div className="sm:hidden flex items-center justify-between">
        <p className="text-sm font-bold">{label}</p>
        <div className="flex gap-3 text-sm font-bold">
          <span className="tabular-nums">{fmt(totals.totalActual)}</span>
          <span className={`tabular-nums ${varianceClass(totals.totalVariance, true)}`}>
            {fmtSigned(totals.totalVariance)}
          </span>
        </div>
      </div>
    </div>
  )
}

// ── Summary cards ─────────────────────────────────────────────────────────────

function SummaryCards({ incomeTotals, expenseTotals, currency }: {
  incomeTotals: BudgetPlanTotals
  expenseTotals: BudgetPlanTotals
  currency: string
}) {
  const actualNet = parseFloat(incomeTotals.totalActual) - parseFloat(expenseTotals.totalActual)
  const budgetNet = parseFloat(incomeTotals.totalBudgeted) - parseFloat(expenseTotals.totalBudgeted)
  const isNetPositive = actualNet >= 0

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <Card className="border-green-500/20 bg-gradient-to-br from-green-500/8 to-emerald-500/4">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Total Income</CardTitle>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-green-500/15">
            <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p className="text-2xl font-bold text-green-600 dark:text-green-400 tabular-nums">{fmt(incomeTotals.totalActual)}</p>
          <p className="text-xs text-muted-foreground mt-0.5">Budget: {fmt(incomeTotals.totalBudgeted)}{currency ? ` · ${currency}` : ''}</p>
        </CardContent>
      </Card>

      <Card className="border-red-500/20 bg-gradient-to-br from-red-500/8 to-rose-500/4">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Total Spending</CardTitle>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-red-500/15">
            <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p className="text-2xl font-bold text-red-600 dark:text-red-400 tabular-nums">{fmt(expenseTotals.totalActual)}</p>
          <p className="text-xs text-muted-foreground mt-0.5">Budget: {fmt(expenseTotals.totalBudgeted)}{currency ? ` · ${currency}` : ''}</p>
        </CardContent>
      </Card>

      <Card className={isNetPositive
        ? 'border-green-500/30 bg-gradient-to-br from-green-500/12 to-teal-500/6'
        : 'border-red-500/30 bg-gradient-to-br from-red-500/12 to-orange-500/6'}>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Net Balance</CardTitle>
          <div className={`flex h-8 w-8 items-center justify-center rounded-lg ${isNetPositive ? 'bg-green-500/15' : 'bg-red-500/15'}`}>
            <Activity className={`h-4 w-4 ${isNetPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`} />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p className={`text-2xl font-bold tabular-nums ${isNetPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}>
            {isNetPositive ? '+' : ''}{fmt(actualNet)}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5">
            Budget net: {budgetNet >= 0 ? '+' : ''}{fmt(budgetNet)}{currency ? ` · ${currency}` : ''} · {isNetPositive ? 'Surplus' : 'Deficit'}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

// ── Budget vs Actual Summary Chart ────────────────────────────────────────────

function SummaryChart({ incomeTotals, expenseTotals }: {
  incomeTotals: BudgetPlanTotals
  expenseTotals: BudgetPlanTotals
}) {
  const data = [
    {
      name: 'Balance',
      Budget: Math.max(0, parseFloat(incomeTotals.totalBudgeted) - parseFloat(expenseTotals.totalBudgeted)),
      Actual: Math.max(0, parseFloat(incomeTotals.totalActual) - parseFloat(expenseTotals.totalActual)),
    },
    {
      name: 'Income',
      Budget: parseFloat(incomeTotals.totalBudgeted),
      Actual: parseFloat(incomeTotals.totalActual),
    },
    {
      name: 'Expense',
      Budget: parseFloat(expenseTotals.totalBudgeted),
      Actual: parseFloat(expenseTotals.totalActual),
    },
  ]

  return (
    <Card>
      <CardHeader className="pb-2 pt-5 px-5">
        <CardTitle className="text-base font-semibold">Budget vs Actual Summary</CardTitle>
      </CardHeader>
      <CardContent className="px-2 pb-4">
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data} margin={{ top: 16, right: 16, left: 0, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="name" tick={{ fontSize: 12 }} />
            <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number | undefined) => v != null && v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v ?? '')} />
            <Tooltip formatter={(v: unknown) => [typeof v === 'number' ? v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '']} />
            <Legend />
            <Bar dataKey="Budget" fill="#3b82f6" radius={[3, 3, 0, 0]} />
            <Bar dataKey="Actual" fill="#ef4444" radius={[3, 3, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  )
}

// ── Spending Comparison Chart ─────────────────────────────────────────────────

function SpendingChart({ groups }: { groups: BudgetPlanCategoryGroup[] }) {
  const data = useMemo(() =>
    groups
      .filter(g => parseFloat(g.groupMonthlyTotal) > 0 || parseFloat(g.groupActualTotal) > 0)
      .map(g => ({
        name: g.parentCategoryName.length > 12 ? g.parentCategoryName.slice(0, 12) + '…' : g.parentCategoryName,
        fullName: g.parentCategoryName,
        Budget: parseFloat(g.groupMonthlyTotal),
        Actual: parseFloat(g.groupActualTotal),
      })),
    [groups]
  )

  if (data.length === 0) return null

  return (
    <Card>
      <CardHeader className="pb-2 pt-5 px-5">
        <CardTitle className="text-base font-semibold">Spending Comparison</CardTitle>
      </CardHeader>
      <CardContent className="px-2 pb-4">
        <ResponsiveContainer width="100%" height={260}>
          <BarChart data={data} margin={{ top: 16, right: 16, left: 0, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="name" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 11 }} tickFormatter={(v: number | undefined) => v != null && v >= 1000 ? `${(v / 1000).toFixed(0)}k` : String(v ?? '')} />
            <Tooltip
              formatter={(v: unknown) => [typeof v === 'number' ? v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '']}
            />
            <Legend />
            <Bar dataKey="Budget" fill="#3b82f6" radius={[3, 3, 0, 0]} />
            <Bar dataKey="Actual" fill="#ef4444" radius={[3, 3, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export function BudgetsListPage() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())

  const startDate = useMemo(() => firstDayOfMonth(year, month), [year, month])
  const endDate = useMemo(() => lastDayOfMonth(year, month), [year, month])

  const { plan, isLoading, error } = useBudgetPlan(startDate, endDate)

  const prevMonth = () => {
    if (month === 0) { setYear(y => y - 1); setMonth(11) }
    else setMonth(m => m - 1)
  }
  const nextMonth = () => {
    if (month === 11) { setYear(y => y + 1); setMonth(0) }
    else setMonth(m => m + 1)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold">Spending Analysis</h1>
        <p className="text-muted-foreground text-sm mt-1">Budget vs actual comparison for the selected month</p>
      </div>

      {/* Month navigator */}
      <div className="flex items-center justify-center gap-2 sm:justify-start">
        <Button variant="outline" size="icon" className="h-11 w-11 shrink-0" onClick={prevMonth} aria-label="Previous month">
          <ChevronLeft className="h-5 w-5" />
        </Button>
        <span className="min-w-[180px] text-center text-base font-semibold select-none" aria-live="polite">
          {monthLabel(year, month)}
        </span>
        <Button variant="outline" size="icon" className="h-11 w-11 shrink-0" onClick={nextMonth} aria-label="Next month">
          <ChevronRight className="h-5 w-5" />
        </Button>
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="flex justify-center py-12">
          <LoadingSpinner size="lg" />
        </div>
      )}

      {/* Error */}
      {!isLoading && error && <ErrorAlert message={error} />}

      {/* Content */}
      {!isLoading && !error && plan && (
        <>
          {/* Summary cards */}
          <SummaryCards
            incomeTotals={plan.incomeTotals}
            expenseTotals={plan.expenseTotals}
            currency={plan.currency}
          />

          {/* Charts */}
          <div className="grid gap-6 lg:grid-cols-2">
            <SummaryChart incomeTotals={plan.incomeTotals} expenseTotals={plan.expenseTotals} />
            <SpendingChart groups={plan.expenseGroups} />
          </div>

          {/* Income comparison table */}
          <Card className="overflow-hidden">
            <CardHeader className="pb-0 pt-5 px-5">
              <div className="flex items-center gap-2 border-l-4 border-green-500 pl-3">
                <TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400 shrink-0" />
                <CardTitle className="text-base font-semibold text-green-700 dark:text-green-300">INCOME</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="px-5 pt-2 pb-4">
              {plan.incomeRows.length === 0 ? (
                <p className="py-6 text-center text-sm text-muted-foreground">No income categories</p>
              ) : (
                <>
                  <ColHeaders />
                  {plan.incomeRows.map(row => (
                    <CategoryRow key={row.categoryId} row={row} />
                  ))}
                  <TotalsRow totals={plan.incomeTotals} label="Income Total" />
                </>
              )}
            </CardContent>
          </Card>

          {/* Spending comparison table */}
          <Card className="overflow-hidden">
            <CardHeader className="pb-0 pt-5 px-5">
              <div className="flex items-center gap-2 border-l-4 border-red-500 pl-3">
                <TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0" />
                <CardTitle className="text-base font-semibold text-red-700 dark:text-red-300">SPENDING</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="px-5 pt-2 pb-4">
              {plan.expenseGroups.length === 0 ? (
                <p className="py-6 text-center text-sm text-muted-foreground">No expense categories</p>
              ) : (
                <>
                  <ColHeaders />
                  {plan.expenseGroups.map(group => (
                    <div key={group.parentCategoryId ?? group.parentCategoryName}>
                      <GroupHeaderRow group={group} />
                      {group.rows.map(row => (
                        <CategoryRow key={row.categoryId} row={row} indent />
                      ))}
                    </div>
                  ))}
                  <TotalsRow totals={plan.expenseTotals} label="Spending Total" />
                </>
              )}
            </CardContent>
          </Card>
        </>
      )}

    </div>
  )
}
