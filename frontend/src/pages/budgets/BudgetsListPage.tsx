import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Trash2, Pencil, AlertTriangle } from 'lucide-react'
import { toast } from 'sonner'
import { useBudgets } from '@/hooks/useBudgets'
import { deleteBudget } from '@/api/budgets.api'
import { BUDGET_PERIODS } from '@/constants/budget-periods'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import type { Budget } from '@/types/budget.types'

function periodLabel(code: string): string {
  return BUDGET_PERIODS.find((p) => p.code === code)?.name ?? code
}

function gaugeColor(percent: number): string {
  if (percent >= 90) return '#ef4444'
  if (percent >= 70) return '#eab308'
  return '#22c55e'
}

function amountColor(percent: number): string {
  if (percent >= 90) return 'text-red-600 dark:text-red-400'
  if (percent >= 70) return 'text-yellow-600 dark:text-yellow-400'
  return 'text-green-600 dark:text-green-400'
}

function RadialProgress({ percent, size = 76 }: { percent: number; size?: number }) {
  const strokeWidth = 7
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const clamped = Math.min(percent, 100)
  const offset = circumference - (clamped / 100) * circumference
  const color = gaugeColor(percent)

  return (
    <div className="relative flex shrink-0 items-center justify-center" style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {/* Track */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-muted/60"
        />
        {/* Progress arc */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{ transition: 'stroke-dashoffset 0.5s ease' }}
        />
      </svg>
      <span
        className="absolute text-[11px] font-bold tabular-nums"
        style={{ color }}
      >
        {clamped.toFixed(0)}%
      </span>
    </div>
  )
}

export function BudgetsListPage() {
  const { budgets, isLoading, error, refresh } = useBudgets()
  const [deleteTarget, setDeleteTarget] = useState<Budget | null>(null)
  const [deleting, setDeleting] = useState(false)

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteBudget(deleteTarget.id)
      toast.success('Budget deactivated')
      setDeleteTarget(null)
      refresh()
    } catch {
      toast.error('Failed to deactivate budget')
    } finally {
      setDeleting(false)
    }
  }

  if (isLoading && budgets.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) return <ErrorAlert message={error} />

  const totalBudgeted = budgets.reduce((sum, b) => sum + parseFloat(b.amount || '0'), 0)
  const totalSpent = budgets.reduce((sum, b) => sum + parseFloat(b.spentAmount || '0'), 0)
  const currency = budgets[0]?.currency ?? ''

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Budgets</h1>
        <Button asChild>
          <Link to="/budgets/new">
            <Plus className="mr-2 h-4 w-4" />
            Add Budget
          </Link>
        </Button>
      </div>

      {budgets.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">No budgets found. Create one to start tracking your spending.</p>
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Summary stats bar */}
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardContent className="pt-6">
                <p className="text-sm text-muted-foreground">Total Budgets</p>
                <p className="mt-1 text-2xl font-bold">{budgets.length}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <p className="text-sm text-muted-foreground">Total Budgeted</p>
                <p className="mt-1 text-2xl font-bold">
                  {totalBudgeted.toFixed(2)}{currency && <span className="ml-1 text-base font-normal text-muted-foreground">{currency}</span>}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <p className="text-sm text-muted-foreground">Total Spent</p>
                <p className={`mt-1 text-2xl font-bold ${totalSpent > totalBudgeted ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'}`}>
                  {totalSpent.toFixed(2)}{currency && <span className="ml-1 text-base font-normal text-muted-foreground">{currency}</span>}
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Budget cards grid */}
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {budgets.map((budget) => (
              <Card key={budget.id} className="transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md">
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle className="text-base">{budget.categoryName}</CardTitle>
                      <Badge variant="outline" className="mt-1">
                        {periodLabel(budget.periodType)}
                      </Badge>
                    </div>
                    <div className="flex gap-1">
                      {budget.alertTriggered && (
                        <AlertTriangle className="h-5 w-5 text-yellow-500" />
                      )}
                      <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                        <Link to={`/budgets/${budget.id}/edit`}>
                          <Pencil className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                        </Link>
                      </Button>
                      <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setDeleteTarget(budget)}>
                        <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center gap-4">
                    {/* Text stats on the left */}
                    <div className="flex-1 space-y-2">
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Spent</span>
                        <span className={`font-medium ${amountColor(budget.percentUsed)}`}>
                          {budget.spentAmount} {budget.currency}
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Budget</span>
                        <span className="font-medium">
                          {budget.amount} {budget.currency}
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Remaining</span>
                        <span className={`font-medium ${Number(budget.remainingAmount) < 0 ? 'text-red-600' : ''}`}>
                          {budget.remainingAmount} {budget.currency}
                        </span>
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {budget.startDate} — {budget.endDate ?? '∞'}
                      </div>
                    </div>

                    {/* Radial gauge on the right */}
                    <RadialProgress percent={budget.percentUsed} />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </>
      )}

      {/* Delete Dialog */}
      <AlertDialog open={deleteTarget !== null} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Deactivate Budget</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to deactivate the budget for "{deleteTarget?.categoryName}"? This will stop tracking spending against this budget.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleting ? 'Deactivating...' : 'Deactivate'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
