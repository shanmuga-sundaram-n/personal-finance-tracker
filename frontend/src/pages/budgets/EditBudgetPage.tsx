import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { getBudget, updateBudget } from '@/api/budgets.api'
import { ApiClientError } from '@/api/client'
import { BUDGET_PERIODS } from '@/constants/budget-periods'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import type { Budget } from '@/types/budget.types'

const editBudgetSchema = z.object({
  amount: z.string().min(1, 'Amount is required').refine((v) => !isNaN(Number(v)) && Number(v) > 0, 'Must be a positive number'),
  endDate: z.string().optional().or(z.literal('')),
  rolloverEnabled: z.boolean(),
  alertThresholdPct: z.string().optional().or(z.literal('')),
})

type EditBudgetForm = z.infer<typeof editBudgetSchema>

export function EditBudgetPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [budget, setBudget] = useState<Budget | null>(null)
  const [loading, setLoading] = useState(true)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EditBudgetForm>({
    resolver: zodResolver(editBudgetSchema),
  })

  useEffect(() => {
    if (!id) return
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true)
    getBudget(parseInt(id, 10))
      .then((b) => {
        setBudget(b)
        reset({
          amount: b.amount,
          endDate: b.endDate ?? '',
          rolloverEnabled: b.rolloverEnabled,
          alertThresholdPct: b.alertThresholdPct != null ? String(b.alertThresholdPct) : '',
        })
      })
      .catch(() => {
        setError('Failed to load budget')
      })
      .finally(() => setLoading(false))
  }, [id, reset])

  const periodLabel = BUDGET_PERIODS.find((p) => p.code === budget?.periodType)?.name ?? budget?.periodType

  const onSubmit = async (data: EditBudgetForm) => {
    if (!budget) return
    setError(null)
    try {
      await updateBudget(budget.id, {
        amount: parseFloat(data.amount),
        currency: budget.currency,
        endDate: data.endDate || undefined,
        rolloverEnabled: data.rolloverEnabled,
        alertThresholdPct: data.alertThresholdPct ? parseInt(data.alertThresholdPct, 10) : undefined,
      })
      toast.success('Budget updated successfully')
      navigate('/budgets')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to update budget')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (!budget) {
    return <ErrorAlert message={error ?? 'Budget not found'} />
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Edit Budget</h1>

      <Card className="overflow-hidden">
        <div className="h-1 bg-gradient-to-r from-blue-600 via-blue-500/70 to-blue-400/30" />
        <CardHeader>
          <CardTitle>
            {budget.categoryName} — {periodLabel}
          </CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="amount">Amount ({budget.currency})</Label>
                <Input id="amount" type="number" step="0.01" min="0.01" {...register('amount')} />
                {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="endDate">End Date</Label>
                <Input id="endDate" type="date" {...register('endDate')} />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="flex items-center gap-3 pt-2">
                <input
                  type="checkbox"
                  id="rolloverEnabled"
                  className="h-4 w-4 rounded border-gray-300"
                  {...register('rolloverEnabled')}
                />
                <Label htmlFor="rolloverEnabled" className="cursor-pointer">Enable Rollover</Label>
              </div>

              <div className="space-y-2">
                <Label htmlFor="alertThresholdPct">Alert Threshold % (optional)</Label>
                <Input
                  id="alertThresholdPct"
                  type="number"
                  min="1"
                  max="100"
                  placeholder="e.g., 80"
                  {...register('alertThresholdPct')}
                />
              </div>
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Saving...' : 'Save Changes'}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/budgets')}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  )
}
