import { useState, useMemo } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { upsertBudgetByCategory } from '@/api/budgets.api'
import { ApiClientError } from '@/api/client'
import type { BudgetPlanCategoryRow } from '@/types/budget.types'

const MONTHLY_MULTIPLIERS: Record<string, number> = {
  WEEKLY: 13 / 3,
  BI_WEEKLY: 13 / 6,
  MONTHLY: 1,
  QUARTERLY: 1 / 3,
  SEMI_ANNUAL: 1 / 6,
  ANNUALLY: 1 / 12,
}

const FREQUENCY_OPTIONS = [
  { code: 'WEEKLY', label: 'Weekly' },
  { code: 'BI_WEEKLY', label: 'Bi-weekly' },
  { code: 'MONTHLY', label: 'Monthly' },
  { code: 'QUARTERLY', label: 'Quarterly' },
  { code: 'SEMI_ANNUAL', label: 'Semi-Annual' },
  { code: 'ANNUALLY', label: 'Yearly' },
]

interface SetBudgetDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  categoryId: number
  categoryName: string
  existingBudgetId: number | null
  initialFrequency: string
  initialAmount: string
  currency: string
  startDate: string
  endDate: string
  onSuccess: (row: BudgetPlanCategoryRow) => void
}

export function SetBudgetDialog({
  open,
  onOpenChange,
  categoryId,
  categoryName,
  existingBudgetId,
  initialFrequency,
  initialAmount,
  currency,
  startDate,
  endDate,
  onSuccess,
}: SetBudgetDialogProps) {
  const isEditing = existingBudgetId !== null

  const [frequency, setFrequency] = useState(initialFrequency || 'MONTHLY')
  const [amount, setAmount] = useState(
    initialAmount !== '0' && initialAmount !== '0.0000' ? initialAmount : '',
  )
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const multiplier = MONTHLY_MULTIPLIERS[frequency] ?? 1
  const amountNum = parseFloat(amount)

  const monthlyEquivalent = useMemo(() => {
    if (isNaN(amountNum) || amountNum <= 0) return null
    return (amountNum * multiplier).toFixed(2)
  }, [amountNum, multiplier])

  const yearlyEquivalent = useMemo(() => {
    if (isNaN(amountNum) || amountNum <= 0) return null
    return (amountNum * 12 * multiplier).toFixed(2)
  }, [amountNum, multiplier])

  const currencySymbol =
    new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: currency || 'USD',
    })
      .formatToParts(0)
      .find((p) => p.type === 'currency')?.value ?? currency

  const isAmountInvalid = !amount || isNaN(amountNum) || amountNum <= 0
  const isSaveDisabled = isAmountInvalid || isSaving

  const handleSave = async () => {
    setError(null)
    setIsSaving(true)
    try {
      const saved = await upsertBudgetByCategory({
        categoryId,
        periodType: frequency,
        amount: amountNum.toFixed(4),
        currency: currency || 'USD',
        startDate,
        endDate,
      })

      const updatedRow: BudgetPlanCategoryRow = {
        categoryId: saved.categoryId,
        categoryName,
        budgetId: saved.id,
        budgetedAmount: saved.amount,
        actualAmount: saved.spentAmount,
        varianceAmount: saved.remainingAmount,
        percentUsed: saved.percentUsed,
        hasBudget: true,
        frequency,
        monthlyAmount: monthlyEquivalent ?? '0.00',
        yearlyAmount: yearlyEquivalent ?? '0.00',
      }

      onSuccess(updatedRow)
      onOpenChange(false)
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to save budget')
      } else {
        setError('An unexpected error occurred')
      }
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/* sm:max-w-md keeps the dialog comfortably sized on all screens */}
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          {/* Title shows the category name per design requirement 4 */}
          <DialogTitle className="truncate">{categoryName}</DialogTitle>
          <DialogDescription>
            {isEditing ? 'Update the budget for this category.' : 'Set a budget for this category.'}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {error && <ErrorAlert message={error} />}

          {/* Frequency — SelectTrigger meets 44px touch target */}
          <div className="space-y-2">
            <Label htmlFor="frequency">Frequency</Label>
            <Select value={frequency} onValueChange={setFrequency}>
              <SelectTrigger id="frequency" className="h-11">
                <SelectValue placeholder="Select frequency" />
              </SelectTrigger>
              <SelectContent>
                {FREQUENCY_OPTIONS.map((opt) => (
                  <SelectItem key={opt.code} value={opt.code}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Amount — Input meets 44px touch target via h-11 */}
          <div className="space-y-2">
            <Label htmlFor="budget-amount">
              Amount ({currency})
            </Label>
            <Input
              id="budget-amount"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="h-11"
              aria-invalid={isAmountInvalid && amount !== ''}
              aria-describedby={isAmountInvalid && amount !== '' ? 'budget-amount-error' : undefined}
            />
            {isAmountInvalid && amount !== '' && (
              <p id="budget-amount-error" className="text-xs text-destructive mt-1" role="alert">
                Please enter a positive amount.
              </p>
            )}
          </div>

          {/* Live monthly / yearly equivalents — shown as soon as a valid amount exists */}
          {monthlyEquivalent && yearlyEquivalent && (
            <div
              className="rounded-lg border border-border/60 bg-muted/40 px-4 py-3 space-y-1.5"
              aria-label="Calculated equivalents"
            >
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Monthly equivalent</span>
                <span className="font-semibold tabular-nums">
                  {currencySymbol}
                  {parseFloat(monthlyEquivalent).toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Yearly equivalent</span>
                <span className="font-semibold tabular-nums">
                  {currencySymbol}
                  {parseFloat(yearlyEquivalent).toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </span>
              </div>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          {/* Cancel — ghost/outline style, 44px touch target */}
          <Button
            variant="ghost"
            className="h-11"
            onClick={() => onOpenChange(false)}
            disabled={isSaving}
          >
            Cancel
          </Button>
          {/* Save — primary style, disabled when amount invalid or saving in progress */}
          <Button
            variant="default"
            className="h-11"
            onClick={handleSave}
            disabled={isSaveDisabled}
            aria-disabled={isSaveDisabled}
          >
            {isSaving ? 'Saving…' : isEditing ? 'Update Budget' : 'Save Budget'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
