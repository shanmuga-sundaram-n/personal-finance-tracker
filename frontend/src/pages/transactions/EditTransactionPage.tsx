import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { getTransaction, updateTransaction } from '@/api/transactions.api'
import { ApiClientError } from '@/api/client'
import { useCategories } from '@/hooks/useCategories'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import type { Transaction } from '@/types/transaction.types'

const editTransactionSchema = z.object({
  categoryId: z.string().min(1, 'Category is required'),
  amount: z.string().min(1, 'Amount is required').refine((v) => !isNaN(Number(v)) && Number(v) > 0, 'Must be a positive number'),
  transactionDate: z.string().min(1, 'Date is required'),
  description: z.string().max(500).optional().or(z.literal('')),
  merchantName: z.string().max(200).optional().or(z.literal('')),
  referenceNumber: z.string().max(100).optional().or(z.literal('')),
})

type EditTransactionForm = z.infer<typeof editTransactionSchema>

export function EditTransactionPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [transaction, setTransaction] = useState<Transaction | null>(null)
  const [loading, setLoading] = useState(true)
  const { categories } = useCategories()

  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EditTransactionForm>({
    resolver: zodResolver(editTransactionSchema),
  })

  useEffect(() => {
    if (!id) return
    setLoading(true)
    getTransaction(parseInt(id, 10))
      .then((tx) => {
        setTransaction(tx)
        reset({
          categoryId: String(tx.categoryId),
          amount: tx.amount,
          transactionDate: tx.transactionDate,
          description: tx.description ?? '',
          merchantName: tx.merchantName ?? '',
          referenceNumber: tx.referenceNumber ?? '',
        })
      })
      .catch(() => {
        setError('Failed to load transaction')
      })
      .finally(() => setLoading(false))
  }, [id, reset])

  // Filter categories by transaction type
  const categoryTypeCode = transaction?.type === 'INCOME' ? 'INCOME' : 'EXPENSE'
  const filteredCategories = categories.filter((c) => c.categoryTypeCode === categoryTypeCode && c.isActive)

  const onSubmit = async (data: EditTransactionForm) => {
    if (!transaction) return
    setError(null)
    try {
      await updateTransaction(transaction.id, {
        categoryId: parseInt(data.categoryId, 10),
        amount: parseFloat(data.amount),
        currency: transaction.currency,
        transactionDate: data.transactionDate,
        description: data.description || undefined,
        merchantName: data.merchantName || undefined,
        referenceNumber: data.referenceNumber || undefined,
      })
      toast.success('Transaction updated successfully')
      navigate('/transactions')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to update transaction')
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

  if (!transaction) {
    return <ErrorAlert message={error ?? 'Transaction not found'} />
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Edit Transaction</h1>

      <Card className="overflow-hidden">
        <div className="h-1 bg-gradient-to-r from-blue-600 via-blue-500/70 to-blue-400/30" />
        <CardHeader>
          <CardTitle>
            {transaction.type === 'INCOME' ? 'Income' : 'Expense'} — {transaction.accountName}
          </CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="space-y-2">
              <Label>Category</Label>
              <Select
                defaultValue={String(transaction.categoryId)}
                onValueChange={(v) => setValue('categoryId', v)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {filteredCategories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.categoryId && <p className="text-sm text-destructive">{errors.categoryId.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="amount">Amount ({transaction.currency})</Label>
                <Input id="amount" type="number" step="0.01" min="0.01" {...register('amount')} />
                {errors.amount && <p className="text-sm text-destructive">{errors.amount.message}</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="transactionDate">Date</Label>
                <Input id="transactionDate" type="date" {...register('transactionDate')} />
                {errors.transactionDate && <p className="text-sm text-destructive">{errors.transactionDate.message}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description (optional)</Label>
              <Input id="description" placeholder="e.g., Weekly groceries" {...register('description')} />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="merchantName">Merchant (optional)</Label>
                <Input id="merchantName" placeholder="e.g., Whole Foods" {...register('merchantName')} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="referenceNumber">Reference # (optional)</Label>
                <Input id="referenceNumber" placeholder="e.g., INV-001" {...register('referenceNumber')} />
              </div>
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Saving...' : 'Save Changes'}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/transactions')}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  )
}
