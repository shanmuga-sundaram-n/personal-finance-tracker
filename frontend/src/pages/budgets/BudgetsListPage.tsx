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

function progressColor(percent: number): string {
  if (percent >= 90) return 'bg-red-500'
  if (percent >= 70) return 'bg-yellow-500'
  return 'bg-green-500'
}

function progressTrackColor(percent: number): string {
  if (percent >= 90) return 'bg-red-100 dark:bg-red-950'
  if (percent >= 70) return 'bg-yellow-100 dark:bg-yellow-950'
  return 'bg-green-100 dark:bg-green-950'
}

function amountColor(percent: number): string {
  if (percent >= 90) return 'text-red-600 dark:text-red-400'
  if (percent >= 70) return 'text-yellow-600 dark:text-yellow-400'
  return 'text-green-600 dark:text-green-400'
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
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {budgets.map((budget) => {
            const percent = Math.min(budget.percentUsed, 100)
            return (
              <Card key={budget.id}>
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
                <CardContent className="space-y-3">
                  {/* Progress bar */}
                  <div className="space-y-1">
                    <div className={`h-3 w-full rounded-full ${progressTrackColor(budget.percentUsed)}`}>
                      <div
                        className={`h-3 rounded-full transition-all ${progressColor(budget.percentUsed)}`}
                        style={{ width: `${percent}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className={amountColor(budget.percentUsed)}>
                        {budget.spentAmount} {budget.currency} spent
                      </span>
                      <span className="text-muted-foreground">
                        {budget.amount} {budget.currency}
                      </span>
                    </div>
                  </div>

                  {/* Remaining */}
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Remaining</span>
                    <span className={`font-medium ${Number(budget.remainingAmount) < 0 ? 'text-red-600' : ''}`}>
                      {budget.remainingAmount} {budget.currency}
                    </span>
                  </div>

                  {/* Usage percentage */}
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Used</span>
                    <span className={`font-medium ${amountColor(budget.percentUsed)}`}>
                      {budget.percentUsed.toFixed(1)}%
                    </span>
                  </div>

                  {/* Period dates */}
                  <div className="text-xs text-muted-foreground">
                    {budget.startDate} — {budget.endDate ?? '∞'}
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>
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
