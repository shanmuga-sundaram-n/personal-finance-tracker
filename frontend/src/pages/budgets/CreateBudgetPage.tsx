import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { createBudget } from '@/api/budgets.api'
import { ApiClientError } from '@/api/client'
import { useCategories } from '@/hooks/useCategories'
import { BUDGET_PERIODS } from '@/constants/budget-periods'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'

const createBudgetSchema = z.object({
  categoryId: z.string().min(1, 'Category is required'),
  periodType: z.string().min(1, 'Period is required'),
  amount: z.string().min(1, 'Amount is required').refine((v) => !isNaN(Number(v)) && Number(v) > 0, 'Must be a positive number'),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().optional().or(z.literal('')),
  rolloverEnabled: z.boolean(),
  alertThresholdPct: z.string().optional().or(z.literal('')),
})

type CreateBudgetForm = z.infer<typeof createBudgetSchema>

export function CreateBudgetPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { categories } = useCategories('EXPENSE')

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateBudgetForm>({
    resolver: zodResolver(createBudgetSchema),
    defaultValues: {
      startDate: new Date().toISOString().split('T')[0],
      rolloverEnabled: false,
      periodType: 'MONTHLY',
    },
  })

  const selectedPeriod = watch('periodType')

  const activeCategories = categories.filter((c) => c.isActive)

  const onSubmit = async (data: CreateBudgetForm) => {
    setError(null)
    try {
      await createBudget({
        categoryId: parseInt(data.categoryId, 10),
        periodType: data.periodType as 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY' | 'CUSTOM',
        amount: parseFloat(data.amount),
        currency: 'USD',
        startDate: data.startDate,
        endDate: data.endDate || undefined,
        rolloverEnabled: data.rolloverEnabled,
        alertThresholdPct: data.alertThresholdPct ? parseInt(data.alertThresholdPct, 10) : undefined,
      })
      toast.success('Budget created successfully')
      navigate('/budgets')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to create budget')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Create Budget</h1>

      <Card>
        <CardHeader>
          <CardTitle>Budget Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Category</Label>
                <Select onValueChange={(v) => setValue('categoryId', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {activeCategories.map((c) => (
                      <SelectItem key={c.id} value={String(c.id)}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.categoryId && <p className="text-sm text-destructive">{errors.categoryId.message}</p>}
              </div>

              <div className="space-y-2">
                <Label>Period</Label>
                <Select
                  value={selectedPeriod}
                  onValueChange={(v) => setValue('periodType', v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select period" />
                  </SelectTrigger>
                  <SelectContent>
                    {BUDGET_PERIODS.map((p) => (
                      <SelectItem key={p.code} value={p.code}>
                        {p.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.periodType && <p className="text-sm text-destructive">{errors.periodType.message}</p>}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="amount">Amount</Label>
                <Input id="amount" type="number" step="0.01" min="0.01" placeholder="0.00" {...register('amount')} />
                {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="startDate">Start Date</Label>
                <Input id="startDate" type="date" {...register('startDate')} />
                {errors.startDate && <p className="text-sm text-destructive">{errors.startDate.message}</p>}
              </div>
            </div>

            {selectedPeriod === 'CUSTOM' && (
              <div className="space-y-2">
                <Label htmlFor="endDate">End Date</Label>
                <Input id="endDate" type="date" {...register('endDate')} />
                {errors.endDate && <p className="text-sm text-destructive">{errors.endDate.message}</p>}
              </div>
            )}

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
                {isSubmitting ? 'Creating...' : 'Create Budget'}
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
