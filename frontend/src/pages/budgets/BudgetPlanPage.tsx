import { useState, useMemo, useRef } from 'react'
import { Link } from 'react-router-dom'
import {
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  TrendingDown,
  Activity,
  Tag,
  Pencil,
  Plus,
  Trash2,
  Copy,
  Loader2,
  AlertTriangle,
} from 'lucide-react'
import { toast } from 'sonner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { useBudgetPlan } from '@/hooks/useBudgetPlan'
import { deleteBudget, copyBudgetsFromPreviousMonth } from '@/api/budgets.api'
import { SetBudgetDialog } from '@/components/budgets/SetBudgetDialog'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import type { BudgetPlanCategoryRow, BudgetPlanCategoryGroup, BudgetPlanTotals, CopyBudgetsResult } from '@/types/budget.types'

// ── Date helpers ──────────────────────────────────────────────────────────────

function toIsoDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}

function firstDayOfMonth(year: number, month: number): Date {
  return new Date(year, month, 1)
}

function lastDayOfMonth(year: number, month: number): Date {
  return new Date(year, month + 1, 0)
}

function monthLabel(year: number, month: number): string {
  return new Date(year, month, 1).toLocaleString(undefined, { month: 'long', year: 'numeric' })
}

function prevMonthLabel(year: number, month: number): string {
  // month is 0-based; new Date handles month=-1 → Dec of (year-1) automatically
  return new Date(year, month - 1, 1).toLocaleString(undefined, { month: 'long', year: 'numeric' })
}

// ── Number formatting ─────────────────────────────────────────────────────────

function fmt(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '—'
  return Math.abs(num).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

// ── Safe totals accessors ─────────────────────────────────────────────────────

/**
 * Backend will add totalMonthly / totalYearly to BudgetPlanTotals.
 * Until then, fall back to totalBudgeted (treated as monthly) and derive yearly.
 */
function getTotalsMonthly(totals: BudgetPlanTotals): string {
  const extended = totals as unknown as Record<string, unknown>
  return extended.totalMonthly as string ?? totals.totalBudgeted
}

function getTotalsYearly(totals: BudgetPlanTotals): string {
  const extended = totals as unknown as Record<string, unknown>
  const yearly = extended.totalYearly as string | undefined
  if (yearly !== undefined) return yearly
  return (parseFloat(totals.totalBudgeted) * 12).toFixed(4)
}

// ── Frequency label ────────────────────────────────────────────────────────────

const FREQUENCY_LABELS: Record<string, string> = {
  WEEKLY: 'Weekly',
  BI_WEEKLY: 'Bi-weekly',
  MONTHLY: 'Monthly',
  QUARTERLY: 'Quarterly',
  SEMI_ANNUAL: 'Semi-annual',
  ANNUALLY: 'Yearly',
  CUSTOM: 'Custom',
}

function frequencyLabel(code: string | null): string {
  if (!code) return ''
  return FREQUENCY_LABELS[code] ?? code
}

// ── Skeleton loader ───────────────────────────────────────────────────────────

function SkeletonPulse({ className }: { className?: string }) {
  return <div className={`animate-pulse rounded bg-muted ${className ?? ''}`} />
}

function SkeletonRow({ indent = false }: { indent?: boolean }) {
  return (
    <div className={`flex items-center gap-3 py-3 border-b border-border/40 last:border-0 ${indent ? 'pl-8' : ''}`}>
      <SkeletonPulse className="h-4 flex-1 max-w-[140px]" />
      <SkeletonPulse className="h-5 w-16 rounded-full" />
      <SkeletonPulse className="h-4 w-20 hidden sm:block" />
      <SkeletonPulse className="h-4 w-24 hidden sm:block" />
      <SkeletonPulse className="h-9 w-24 rounded-md ml-auto" />
    </div>
  )
}

function BudgetPlanSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true" aria-label="Loading budget plan">
      {/* Summary cards skeleton */}
      <div className="grid gap-4 sm:grid-cols-3">
        {[0, 1, 2].map((i) => (
          <Card key={i}>
            <CardContent className="pt-5 space-y-3">
              <SkeletonPulse className="h-4 w-28" />
              <SkeletonPulse className="h-7 w-36" />
              <SkeletonPulse className="h-3 w-20" />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Income section skeleton */}
      <Card>
        <CardHeader className="pb-3">
          <SkeletonPulse className="h-5 w-20" />
        </CardHeader>
        <CardContent className="space-y-1">
          {[0, 1, 2].map((i) => (
            <SkeletonRow key={i} />
          ))}
        </CardContent>
      </Card>

      {/* Expense section skeleton — with parent header rows */}
      <Card>
        <CardHeader className="pb-3">
          <SkeletonPulse className="h-5 w-24" />
        </CardHeader>
        <CardContent className="space-y-1">
          <SkeletonPulse className="h-8 w-full rounded" />
          <SkeletonRow indent />
          <SkeletonRow indent />
          <SkeletonPulse className="h-8 w-full rounded mt-2" />
          <SkeletonRow indent />
          <SkeletonRow indent />
          <SkeletonRow indent />
        </CardContent>
      </Card>
    </div>
  )
}

// ── Column header strip ───────────────────────────────────────────────────────

/**
 * Single strip rendered once per section above all rows.
 * Columns: Category | Frequency | Monthly | Yearly | Action
 * Hidden on mobile — rows use stacked layout there.
 */
function ColumnHeaderStrip() {
  return (
    <div
      className="hidden sm:grid sm:grid-cols-[1fr_100px_110px_110px_160px] sm:gap-x-3 py-2 px-3 mb-1 border-b border-border/40"
      aria-hidden="true"
    >
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
        Category
      </p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
        Frequency
      </p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">
        Monthly
      </p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">
        Yearly
      </p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">
        Action
      </p>
    </div>
  )
}

// ── Category row ──────────────────────────────────────────────────────────────

interface CategoryRowProps {
  row: BudgetPlanCategoryRow
  currency: string
  isIncome: boolean
  /** indent = true for expense child rows (pl-8) */
  indent?: boolean
  onSetBudget: (row: BudgetPlanCategoryRow) => void
  onDeleteBudget?: (budgetId: number, categoryName: string) => void
}

/**
 * Planning-only row: shows Category | Frequency | Monthly | Yearly | Action.
 * No progress bar, no actual amounts, no variance, no % used.
 */
function CategoryRow({ row, currency: _currency, isIncome, indent = false, onSetBudget, onDeleteBudget }: CategoryRowProps) {
  const amountColor = isIncome
    ? 'text-green-600 dark:text-green-400'
    : 'text-foreground'

  const freqBadgeClass = row.hasBudget
    ? isIncome
      ? 'bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300'
      : 'bg-muted text-muted-foreground'
    : 'bg-muted/60 text-muted-foreground/70'

  return (
    <div
      className={`py-3 border-b border-border/40 last:border-0 border-l-2 border-l-transparent ${indent ? 'pl-8' : 'pl-3'}`}
    >
      {/* ── Desktop layout: 5-column grid ── */}
      <div className="hidden sm:grid sm:grid-cols-[1fr_100px_110px_110px_160px] sm:gap-x-3 sm:items-center">
        {/* Col 1: Category name */}
        <div className="min-w-0">
          <span className="text-sm font-medium leading-snug truncate block">{row.categoryName}</span>
        </div>

        {/* Col 2: Frequency badge */}
        <div>
          {row.hasBudget && row.frequency ? (
            <span
              className={`inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium ${freqBadgeClass}`}
            >
              {frequencyLabel(row.frequency)}
            </span>
          ) : (
            <span className="text-sm text-muted-foreground/50">—</span>
          )}
        </div>

        {/* Col 3: Monthly amount */}
        <div className="text-right">
          <span
            className={`text-sm font-semibold tabular-nums ${row.hasBudget ? amountColor : 'text-muted-foreground/50'}`}
            aria-label={`Monthly: ${row.hasBudget ? fmt(row.monthlyAmount) : 'not set'}`}
          >
            {row.hasBudget ? fmt(row.monthlyAmount) : '—'}
          </span>
        </div>

        {/* Col 4: Yearly amount */}
        <div className="text-right">
          <span
            className={`text-sm font-semibold tabular-nums ${row.hasBudget ? 'text-foreground' : 'text-muted-foreground/50'}`}
            aria-label={`Yearly: ${row.hasBudget ? fmt(row.yearlyAmount) : 'not set'}`}
          >
            {row.hasBudget ? fmt(row.yearlyAmount) : '—'}
          </span>
        </div>

        {/* Col 5: Action button — 44px touch target */}
        <div className="flex justify-end items-center gap-2">
          {row.hasBudget && onDeleteBudget && (
            <button
              onClick={() => { if (row.budgetId != null) onDeleteBudget(row.budgetId, row.categoryName) }}
              className="h-11 w-11 flex items-center justify-center rounded text-muted-foreground hover:text-destructive hover:bg-destructive/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 transition-colors shrink-0"
              aria-label={`Remove budget for ${row.categoryName}`}
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
          <Button
            variant={row.hasBudget ? 'secondary' : 'outline'}
            size="sm"
            className="h-11 px-3 text-xs min-w-[96px]"
            onClick={() => onSetBudget(row)}
            aria-label={row.hasBudget ? `Edit budget for ${row.categoryName}` : `Set budget for ${row.categoryName}`}
          >
            {row.hasBudget ? (
              <>
                <Pencil className="h-3 w-3 mr-1.5" aria-hidden="true" />
                Edit Budget
              </>
            ) : (
              <>
                <Plus className="h-3 w-3 mr-1.5" aria-hidden="true" />
                Set Budget
              </>
            )}
          </Button>
        </div>
      </div>

      {/* ── Mobile layout: stacked ── */}
      <div className="sm:hidden">
        {/* Row 1: Category name + action button */}
        <div className="flex items-center justify-between gap-3 mb-2">
          <span className="font-medium text-sm leading-snug truncate">{row.categoryName}</span>
          <div className="flex items-center gap-2 shrink-0">
            {row.hasBudget && onDeleteBudget && (
              <button
                onClick={() => { if (row.budgetId != null) onDeleteBudget(row.budgetId, row.categoryName) }}
                className="h-11 w-11 flex items-center justify-center rounded text-muted-foreground hover:text-destructive hover:bg-destructive/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 transition-colors"
                aria-label={`Remove budget for ${row.categoryName}`}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            )}
            <Button
              variant={row.hasBudget ? 'secondary' : 'outline'}
              size="sm"
              className="h-11 px-3 text-xs"
              onClick={() => onSetBudget(row)}
              aria-label={row.hasBudget ? `Edit budget for ${row.categoryName}` : `Set budget for ${row.categoryName}`}
            >
              {row.hasBudget ? (
                <>
                  <Pencil className="h-3 w-3 mr-1" aria-hidden="true" />
                  Edit
                </>
              ) : (
                <>
                  <Plus className="h-3 w-3 mr-1" aria-hidden="true" />
                  Set
                </>
              )}
            </Button>
          </div>
        </div>

        {/* Row 2: Stats (frequency | monthly | yearly) */}
        <div className="grid grid-cols-3 gap-x-2">
          <div>
            <p className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground mb-0.5">
              Freq
            </p>
            {row.hasBudget && row.frequency ? (
              <span
                className={`inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-medium ${freqBadgeClass}`}
              >
                {frequencyLabel(row.frequency)}
              </span>
            ) : (
              <span className="text-xs text-muted-foreground/50">—</span>
            )}
          </div>
          <div>
            <p className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground mb-0.5">
              Monthly
            </p>
            <p className={`text-xs font-semibold tabular-nums ${row.hasBudget ? amountColor : 'text-muted-foreground/50'}`}>
              {row.hasBudget ? fmt(row.monthlyAmount) : '—'}
            </p>
          </div>
          <div>
            <p className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground mb-0.5">
              Yearly
            </p>
            <p className={`text-xs font-semibold tabular-nums ${row.hasBudget ? 'text-foreground' : 'text-muted-foreground/50'}`}>
              {row.hasBudget ? fmt(row.yearlyAmount) : '—'}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Pagination ────────────────────────────────────────────────────────────────

const PAGE_SIZE_OPTIONS = [5, 10, 25, 50] as const
type PageSize = (typeof PAGE_SIZE_OPTIONS)[number]

interface PaginationState {
  page: number
  pageSize: PageSize
}

function usePagination(defaultPageSize: PageSize = 10) {
  const [state, setState] = useState<PaginationState>({
    page: 1,
    pageSize: defaultPageSize,
  })

  const setPage = (page: number) => setState((s) => ({ ...s, page }))

  const setPageSize = (pageSize: PageSize) =>
    setState({ page: 1, pageSize })

  return { ...state, setPage, setPageSize }
}

interface PaginationControlsProps {
  page: number
  pageSize: PageSize
  total: number
  onPageChange: (page: number) => void
  onPageSizeChange: (size: PageSize) => void
}

function PaginationControls({
  page,
  pageSize,
  total,
  onPageChange,
  onPageSizeChange,
}: PaginationControlsProps) {
  if (total <= 10) return null

  const totalPages = Math.ceil(total / pageSize)
  const from = (page - 1) * pageSize + 1
  const to = Math.min(page * pageSize, total)

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-4 mt-2 border-t border-border/40">
      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground whitespace-nowrap">Rows per page:</span>
        <Select
          value={String(pageSize)}
          onValueChange={(val) => onPageSizeChange(Number(val) as PageSize)}
        >
          <SelectTrigger className="h-8 w-16 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PAGE_SIZE_OPTIONS.map((size) => (
              <SelectItem key={size} value={String(size)} className="text-xs">
                {size}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground tabular-nums whitespace-nowrap">
          {from}–{to} of {total}
        </span>
        <Button
          variant="outline"
          size="icon"
          className="h-11 w-11"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 1}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-11 w-11"
          onClick={() => onPageChange(page + 1)}
          disabled={page === totalPages}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}

// ── Empty section ─────────────────────────────────────────────────────────────

function EmptySection({ isIncome }: { isIncome: boolean }) {
  return (
    <div className="flex flex-col items-center gap-3 py-10 text-center">
      <div
        className={`flex h-12 w-12 items-center justify-center rounded-full ${
          isIncome ? 'bg-green-100 dark:bg-green-900/30' : 'bg-red-100 dark:bg-red-900/30'
        }`}
      >
        <Tag
          className={`h-6 w-6 ${isIncome ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'}`}
        />
      </div>
      <div>
        <p className="text-sm font-medium text-foreground">
          No {isIncome ? 'income' : 'expense'} categories yet
        </p>
        <p className="text-sm text-muted-foreground mt-0.5">
          Add categories to start planning your {isIncome ? 'income' : 'spending'}.
        </p>
      </div>
      <Button variant="outline" size="sm" asChild>
        <Link to="/categories">Manage categories</Link>
      </Button>
    </div>
  )
}

// ── Section header with inline totals ────────────────────────────────────────

interface SectionHeaderProps {
  label: string
  icon: React.ReactNode
  totals: BudgetPlanTotals
  accentClass: string       // e.g. 'border-green-500'
  titleClass: string        // e.g. 'text-green-700 dark:text-green-300'
  totalAmountClass: string  // e.g. 'text-green-600 dark:text-green-400'
}

function SectionHeader({
  label,
  icon,
  totals,
  accentClass,
  titleClass,
  totalAmountClass,
}: SectionHeaderProps) {
  const monthly = getTotalsMonthly(totals)
  const yearly = getTotalsYearly(totals)
  const hasAny = parseFloat(monthly) > 0

  return (
    <div className={`flex items-center justify-between gap-4 border-l-4 ${accentClass} pl-3`}>
      {/* Left: icon + label */}
      <div className="flex items-center gap-2">
        {icon}
        <CardTitle className={`text-base font-semibold ${titleClass}`}>
          {label}
        </CardTitle>
      </div>

      {/* Right: totals — shown only when there are budgeted amounts */}
      {hasAny && (
        <div className="flex items-center gap-3 shrink-0">
          <div className="text-right">
            <span className={`text-sm font-bold tabular-nums ${totalAmountClass}`}>
              {fmt(monthly)}
            </span>
            <span className="text-xs text-muted-foreground ml-1">/mo</span>
          </div>
          <div className="text-right hidden sm:block">
            <span className="text-sm font-bold tabular-nums text-foreground">
              {fmt(yearly)}
            </span>
            <span className="text-xs text-muted-foreground ml-1">/yr</span>
          </div>
        </div>
      )}
    </div>
  )
}

// ── Plan section (income — flat rows) ─────────────────────────────────────────

interface PlanSectionProps {
  rows: BudgetPlanCategoryRow[]
  currency: string
  isIncome: boolean
  onSetBudget: (row: BudgetPlanCategoryRow) => void
  onDeleteBudget: (budgetId: number, categoryName: string) => void
}

function PlanSection({ rows, currency, isIncome, onSetBudget, onDeleteBudget }: PlanSectionProps) {
  const { page, pageSize, setPage, setPageSize } = usePagination(10)

  const totalRows = rows.length
  const pagedRows = useMemo(() => {
    const start = (page - 1) * pageSize
    return rows.slice(start, start + pageSize)
  }, [rows, page, pageSize])

  if (totalRows === 0) {
    return <EmptySection isIncome={isIncome} />
  }

  return (
    <div>
      {/* Column header strip — desktop only, rendered once above all rows */}
      <ColumnHeaderStrip />

      {pagedRows.map((row) => (
        <CategoryRow
          key={row.categoryId}
          row={row}
          currency={currency}
          isIncome={isIncome}
          indent={false}
          onSetBudget={onSetBudget}
          onDeleteBudget={onDeleteBudget}
        />
      ))}

      <PaginationControls
        page={page}
        pageSize={pageSize}
        total={totalRows}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
      />
    </div>
  )
}

// ── Expense section (hierarchical groups) ────────────────────────────────────

interface ExpenseSectionProps {
  groups: BudgetPlanCategoryGroup[]
  currency: string
  onSetBudget: (row: BudgetPlanCategoryRow) => void
  onDeleteBudget: (budgetId: number, categoryName: string) => void
}

function ExpenseSection({ groups, currency, onSetBudget, onDeleteBudget }: ExpenseSectionProps) {
  if (groups.length === 0) {
    return <EmptySection isIncome={false} />
  }

  return (
    <div>
      {/* Single column header strip for the entire expense section */}
      <ColumnHeaderStrip />

      {groups.map((group) => (
        <div key={group.parentCategoryId ?? group.parentCategoryName}>
          {/* ── Parent header row ── */}
          {/* Visually distinct: bold text, muted background, red left border, group totals on right */}
          <div className="flex items-center justify-between gap-3 py-2.5 px-3 mt-1 bg-muted/50 rounded-sm border-l-4 border-red-500">
            <span className="text-sm font-bold text-foreground truncate">
              {group.parentCategoryName}
            </span>
            <div className="flex items-center gap-3 shrink-0 text-xs text-muted-foreground tabular-nums">
              <span>
                <span className="font-bold text-foreground tabular-nums">{fmt(group.groupMonthlyTotal)}</span>
                <span className="ml-1">/mo</span>
              </span>
              <span className="hidden sm:inline">
                <span className="font-bold text-foreground tabular-nums">{fmt(group.groupYearlyTotal)}</span>
                <span className="ml-1">/yr</span>
              </span>
            </div>
          </div>

          {/* ── Child rows — indented pl-8 ── */}
          {group.rows.map((row) => (
            <CategoryRow
              key={row.categoryId}
              row={row}
              currency={currency}
              isIncome={false}
              indent={true}
              onSetBudget={onSetBudget}
              onDeleteBudget={onDeleteBudget}
            />
          ))}
        </div>
      ))}
    </div>
  )
}

// ── Summary cards ─────────────────────────────────────────────────────────────

interface SummaryCardsProps {
  plannedIncome: number
  plannedIncomeYearly: number
  plannedSpending: number
  plannedSpendingYearly: number
  plannedNet: number
  plannedNetYearly: number
  currency: string
}

function SummaryCards({
  plannedIncome,
  plannedIncomeYearly,
  plannedSpending,
  plannedSpendingYearly,
  plannedNet,
  plannedNetYearly,
  currency,
}: SummaryCardsProps) {
  const isNetPositive = plannedNet >= 0

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {/* Planned Income card */}
      <Card className="border-green-500/20 bg-gradient-to-br from-green-500/8 to-emerald-500/4 dark:from-green-500/12 dark:to-emerald-500/4">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Planned Income</CardTitle>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-green-500/15">
            <TrendingUp className="h-4 w-4 text-green-600 dark:text-green-400" />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p className="text-2xl font-bold text-green-600 dark:text-green-400 tabular-nums">
            {fmt(plannedIncome)}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 tabular-nums">
            {fmt(plannedIncomeYearly)}/yr{currency ? ` · ${currency}` : ''}
          </p>
        </CardContent>
      </Card>

      {/* Planned Spending card */}
      <Card className="border-red-500/20 bg-gradient-to-br from-red-500/8 to-rose-500/4 dark:from-red-500/12 dark:to-rose-500/4">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Planned Spending</CardTitle>
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-red-500/15">
            <TrendingDown className="h-4 w-4 text-red-600 dark:text-red-400" />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p className="text-2xl font-bold text-red-600 dark:text-red-400 tabular-nums">
            {fmt(plannedSpending)}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 tabular-nums">
            {fmt(plannedSpendingYearly)}/yr{currency ? ` · ${currency}` : ''}
          </p>
        </CardContent>
      </Card>

      {/* Planned Net card — color-coded by sign */}
      <Card
        className={
          isNetPositive
            ? 'border-green-500/30 bg-gradient-to-br from-green-500/12 to-teal-500/6 dark:from-green-500/18 dark:to-teal-500/6'
            : 'border-red-500/30 bg-gradient-to-br from-red-500/12 to-orange-500/6 dark:from-red-500/18 dark:to-orange-500/6'
        }
      >
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 pt-4 px-4">
          <CardTitle className="text-sm font-medium text-muted-foreground">Planned Net</CardTitle>
          <div
            className={`flex h-8 w-8 items-center justify-center rounded-lg ${
              isNetPositive ? 'bg-green-500/15' : 'bg-red-500/15'
            }`}
          >
            <Activity
              className={`h-4 w-4 ${
                isNetPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
              }`}
            />
          </div>
        </CardHeader>
        <CardContent className="px-4 pb-4">
          <p
            className={`text-2xl font-bold tabular-nums ${
              isNetPositive ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'
            }`}
          >
            {isNetPositive ? '+' : ''}{fmt(plannedNet)}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 tabular-nums">
            {isNetPositive ? '+' : ''}{fmt(plannedNetYearly)}/yr · {isNetPositive ? 'Surplus' : 'Deficit'}{currency ? ` · ${currency}` : ''}
          </p>
        </CardContent>
      </Card>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export function BudgetPlanPage() {
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth())

  const startDate = useMemo(() => toIsoDate(firstDayOfMonth(year, month)), [year, month])
  const endDate = useMemo(() => toIsoDate(lastDayOfMonth(year, month)), [year, month])

  const { plan, isLoading, error, refresh } = useBudgetPlan(startDate, endDate)

  // SetBudgetDialog state
  const [dialogRow, setDialogRow] = useState<BudgetPlanCategoryRow | null>(null)

  // Delete budget state
  const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null)
  const [deleting, setDeleting] = useState(false)

  // Copy from previous month state
  const [isCopying, setIsCopying] = useState(false)
  const [showOverwriteDialog, setShowOverwriteDialog] = useState(false)
  const [pendingConflictCount, setPendingConflictCount] = useState(0)
  const [pendingCopiedCount, setPendingCopiedCount] = useState(0)
  // Ref for focus restoration after overwrite dialog closes (WCAG 2.4.3)
  const copyButtonRef = useRef<HTMLButtonElement>(null)

  const isCurrentMonth = useMemo(() => {
    const n = new Date()
    return year === n.getFullYear() && month === n.getMonth()
  }, [year, month])

  const handleDeleteBudget = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteBudget(deleteTarget.id)
      toast.success(`Budget for "${deleteTarget.name}" removed`)
      setDeleteTarget(null)
      refresh().catch(() => toast.error('Failed to refresh plan'))
    } catch {
      toast.error('Failed to remove budget')
    } finally {
      setDeleting(false)
    }
  }

  const prevMonth = () => {
    if (month === 0) { setYear(y => y - 1); setMonth(11) }
    else { setMonth(m => m - 1) }
  }

  const nextMonth = () => {
    if (month === 11) { setYear(y => y + 1); setMonth(0) }
    else { setMonth(m => m + 1) }
  }

  const handleSetBudgetSuccess = () => {
    refresh().catch(() => {
      toast.error('Failed to refresh plan')
    })
    toast.success('Budget saved')
    setDialogRow(null)
  }

  // ── Copy from previous month handlers ──────────────────────────────────────

  const handleCopyFromPreviousMonth = async () => {
    setIsCopying(true)
    try {
      const result: CopyBudgetsResult = await copyBudgetsFromPreviousMonth({
        targetYear: year,
        targetMonth: month + 1, // convert 0-based to 1-based
        overwriteExisting: false,
      })

      if (result.copiedCount === 0 && result.conflictCount === 0) {
        toast.info(`No budgets found in ${prevMonthLabel(year, month)}`)
        return
      }

      if (result.conflictCount > 0) {
        setPendingConflictCount(result.conflictCount)
        setPendingCopiedCount(result.copiedCount)
        setShowOverwriteDialog(true)
        return
      }

      // No conflicts — clean copy
      toast.success(
        `Copied ${result.copiedCount} budget${result.copiedCount !== 1 ? 's' : ''} from ${prevMonthLabel(year, month)}`
      )
      refresh().catch(() => toast.error('Failed to refresh plan'))
    } catch {
      toast.error('Failed to copy budgets. Please try again.')
    } finally {
      setIsCopying(false)
    }
  }

  const handleConfirmOverwrite = async () => {
    setShowOverwriteDialog(false)
    setIsCopying(true)
    try {
      const result: CopyBudgetsResult = await copyBudgetsFromPreviousMonth({
        targetYear: year,
        targetMonth: month + 1,
        overwriteExisting: true,
      })

      const parts: string[] = []
      if (result.copiedCount > 0)
        parts.push(`Copied ${result.copiedCount} budget${result.copiedCount !== 1 ? 's' : ''}`)
      if (result.overwrittenCount > 0)
        parts.push(`updated ${result.overwrittenCount}`)

      toast.success(
        parts.length > 0
          ? parts.join(' and ') + ` from ${prevMonthLabel(year, month)}`
          : `Budgets copied from ${prevMonthLabel(year, month)}`
      )
      refresh().catch(() => toast.error('Failed to refresh plan'))
    } catch {
      toast.error('Failed to copy budgets. Please try again.')
    } finally {
      setIsCopying(false)
      setPendingConflictCount(0)
      setPendingCopiedCount(0)
      // Return focus to copy button after overwrite completes (WCAG 2.4.3)
      setTimeout(() => copyButtonRef.current?.focus(), 0)
    }
  }

  const handleCancelOverwrite = () => {
    setShowOverwriteDialog(false)
    setPendingConflictCount(0)
    setPendingCopiedCount(0)
    // Refresh so non-conflicting budgets written during the first call are visible
    refresh()
    // Return focus to the copy button (WCAG 2.4.3 — button is disabled while dialog
    // is open, so defer until React re-enables it in the next paint)
    setTimeout(() => copyButtonRef.current?.focus(), 0)
  }

  // Planned totals — from budgeted amounts, not actuals
  const plannedIncome = plan ? parseFloat(getTotalsMonthly(plan.incomeTotals)) : 0
  const plannedIncomeYearly = plan ? parseFloat(getTotalsYearly(plan.incomeTotals)) : 0
  const plannedSpending = plan ? parseFloat(getTotalsMonthly(plan.expenseTotals)) : 0
  const plannedSpendingYearly = plan ? parseFloat(getTotalsYearly(plan.expenseTotals)) : 0
  const plannedNet = plannedIncome - plannedSpending
  const plannedNetYearly = plannedIncomeYearly - plannedSpendingYearly
  const currency = plan?.currency ?? ''

  return (
    <div className="space-y-6">

      {/* ── Page header ── */}
      <div>
        <h1 className="text-3xl font-bold">Budget Plan</h1>
        <p className="text-muted-foreground text-sm mt-1">
          Set and review your planned income and spending per category
        </p>
      </div>

      {/* ── Month navigation + copy action ── */}
      <div className="flex items-center justify-between gap-3">
        {/* Left: prev / month-label / next */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            onClick={prevMonth}
            className="h-11 w-11 shrink-0"
            aria-label="Previous month"
          >
            <ChevronLeft className="h-5 w-5" />
          </Button>
          <span
            className="min-w-[160px] sm:min-w-[180px] text-center text-base font-semibold select-none"
            aria-live="polite"
            aria-atomic="true"
          >
            {monthLabel(year, month)}
          </span>
          <Button
            variant="outline"
            size="icon"
            onClick={nextMonth}
            className="h-11 w-11 shrink-0"
            aria-label="Next month"
          >
            <ChevronRight className="h-5 w-5" />
          </Button>
        </div>

        {/* Right: copy button — only for current month */}
        {isCurrentMonth && (
          <Button
            ref={copyButtonRef}
            variant="outline"
            size="sm"
            className="h-11 gap-2 px-3 text-sm shrink-0"
            onClick={handleCopyFromPreviousMonth}
            disabled={isCopying || showOverwriteDialog || isLoading}
            aria-label="Copy budgets from previous month"
            aria-busy={isCopying}
          >
            {isCopying ? (
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            ) : (
              <Copy className="h-4 w-4" aria-hidden="true" />
            )}
            <span className="hidden sm:inline">Copy from Previous Month</span>
            <span className="sm:hidden">Copy</span>
          </Button>
        )}
      </div>

      {/* ── Loading skeleton ── */}
      {isLoading && <BudgetPlanSkeleton />}

      {/* ── Error state ── */}
      {!isLoading && error && <ErrorAlert message={error} />}

      {/* ── Content ── */}
      {!isLoading && !error && plan && (
        <>
          {/* Summary cards */}
          <SummaryCards
            plannedIncome={plannedIncome}
            plannedIncomeYearly={plannedIncomeYearly}
            plannedSpending={plannedSpending}
            plannedSpendingYearly={plannedSpendingYearly}
            plannedNet={plannedNet}
            plannedNetYearly={plannedNetYearly}
            currency={currency}
          />

          {/* Income section */}
          <Card className="overflow-hidden">
            <CardHeader className="pb-0 pt-5 px-5">
              <SectionHeader
                label="INCOME"
                icon={<TrendingUp className="h-5 w-5 text-green-600 dark:text-green-400 shrink-0" aria-hidden="true" />}
                totals={plan.incomeTotals}
                accentClass="border-green-500"
                titleClass="text-green-700 dark:text-green-300"
                totalAmountClass="text-green-600 dark:text-green-400"
              />
            </CardHeader>
            <CardContent className="px-5 pt-2 pb-5">
              <PlanSection
                rows={plan.incomeRows}
                currency={currency}
                isIncome={true}
                onSetBudget={setDialogRow}
                onDeleteBudget={(id, name) => setDeleteTarget({ id, name })}
              />
            </CardContent>
          </Card>

          {/* Spending section */}
          <Card className="overflow-hidden">
            <CardHeader className="pb-0 pt-5 px-5">
              <SectionHeader
                label="SPENDING"
                icon={<TrendingDown className="h-5 w-5 text-red-600 dark:text-red-400 shrink-0" aria-hidden="true" />}
                totals={plan.expenseTotals}
                accentClass="border-red-500"
                titleClass="text-red-700 dark:text-red-300"
                totalAmountClass="text-red-600 dark:text-red-400"
              />
            </CardHeader>
            <CardContent className="px-5 pt-2 pb-5">
              <ExpenseSection
                groups={plan.expenseGroups}
                currency={currency}
                onSetBudget={setDialogRow}
                onDeleteBudget={(id, name) => setDeleteTarget({ id, name })}
              />
            </CardContent>
          </Card>
        </>
      )}

      {/* ── Delete Budget Dialog ── */}
      <AlertDialog open={deleteTarget !== null} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove Budget</AlertDialogTitle>
            <AlertDialogDescription>
              Remove the budget for "{deleteTarget?.name}"? The category will remain and any recorded spending is preserved, but the budget target will be removed.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteBudget}
              disabled={deleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleting ? 'Removing...' : 'Remove Budget'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* ── Copy from Previous Month: Overwrite Confirmation ── */}
      <AlertDialog
        open={showOverwriteDialog}
        onOpenChange={(open) => { if (!open) handleCancelOverwrite() }}
      >
        <AlertDialogContent className="max-w-md">
          <AlertDialogHeader>
            <AlertDialogTitle>Overwrite existing budgets?</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingConflictCount} budget{pendingConflictCount !== 1 ? 's' : ''} from this
              month will be replaced with last month's amounts.
              {pendingCopiedCount > 0 && (
                <> {pendingCopiedCount} new budget{pendingCopiedCount !== 1 ? 's' : ''} will
                also be added.</>
              )}
              {' '}This cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={handleCancelOverwrite} className="h-11">
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmOverwrite}
              className="h-11 bg-destructive text-destructive-foreground hover:bg-destructive/90 focus-visible:ring-destructive"
            >
              <AlertTriangle className="h-4 w-4 mr-1.5" aria-hidden="true" />
              Yes, overwrite
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* ── SetBudgetDialog ── */}
      {dialogRow && plan && (
        <SetBudgetDialog
          open={dialogRow !== null}
          onOpenChange={(open) => { if (!open) setDialogRow(null) }}
          categoryId={dialogRow.categoryId}
          categoryName={dialogRow.categoryName}
          existingBudgetId={dialogRow.budgetId}
          initialFrequency={dialogRow.frequency ?? 'MONTHLY'}
          initialAmount={dialogRow.budgetedAmount}
          currency={plan.currency}
          startDate={startDate}
          endDate={endDate}
          onSuccess={handleSetBudgetSuccess}
        />
      )}
    </div>
  )
}
