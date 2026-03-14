import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { createTransaction } from '@/api/transactions.api'
import { ApiClientError } from '@/api/client'
import { useAccounts } from '@/hooks/useAccounts'
import { useCategories } from '@/hooks/useCategories'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'

const createTransactionSchema = z.object({
  accountId: z.string().min(1, 'Account is required'),
  categoryId: z.string().min(1, 'Category is required'),
  amount: z.string().min(1, 'Amount is required').refine((v) => !isNaN(Number(v)) && Number(v) > 0, 'Must be a positive number'),
  type: z.enum(['INCOME', 'EXPENSE'], { error: 'Type is required' }),
  transactionDate: z.string().min(1, 'Date is required'),
  description: z.string().max(500).optional().or(z.literal('')),
  merchantName: z.string().max(200).optional().or(z.literal('')),
  referenceNumber: z.string().max(100).optional().or(z.literal('')),
})

type CreateTransactionForm = z.infer<typeof createTransactionSchema>

export function CreateTransactionPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { accounts } = useAccounts()
  const { categories } = useCategories()

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateTransactionForm>({
    resolver: zodResolver(createTransactionSchema),
    defaultValues: {
      transactionDate: new Date().toISOString().split('T')[0],
      type: 'EXPENSE',
    },
  })

  const selectedAccountId = watch('accountId')
  const selectedType = watch('type')
  const selectedAccount = accounts.find((a) => String(a.id) === selectedAccountId)

  // Filter categories by type matching transaction type
  const categoryTypeCode = selectedType === 'INCOME' ? 'INCOME' : 'EXPENSE'
  const filteredCategories = categories.filter((c) => c.categoryTypeCode === categoryTypeCode && c.isActive)

  const onSubmit = async (data: CreateTransactionForm) => {
    setError(null)
    try {
      await createTransaction({
        accountId: parseInt(data.accountId, 10),
        categoryId: parseInt(data.categoryId, 10),
        amount: parseFloat(data.amount),
        currency: selectedAccount?.currency ?? 'USD',
        type: data.type,
        transactionDate: data.transactionDate,
        description: data.description || undefined,
        merchantName: data.merchantName || undefined,
        referenceNumber: data.referenceNumber || undefined,
      })
      toast.success('Transaction created successfully')
      navigate('/transactions')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to create transaction')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Create Transaction</h1>

      <Card className="overflow-hidden">
        <div className="h-1 bg-gradient-to-r from-blue-600 via-blue-500/70 to-blue-400/30" />
        <CardHeader>
          <CardTitle>Transaction Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Type</Label>
                <Select
                  value={selectedType}
                  onValueChange={(v) => {
                    setValue('type', v as 'INCOME' | 'EXPENSE')
                    setValue('categoryId', '')
                  }}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="EXPENSE">Expense</SelectItem>
                    <SelectItem value="INCOME">Income</SelectItem>
                  </SelectContent>
                </Select>
                {errors.type && <p className="text-sm text-destructive">{errors.type.message}</p>}
              </div>

              <div className="space-y-2">
                <Label>Account</Label>
                <Select onValueChange={(v) => setValue('accountId', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select account" />
                  </SelectTrigger>
                  <SelectContent>
                    {accounts.filter((a) => a.isActive).map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.name} ({a.currency})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.accountId && <p className="text-sm text-destructive">{errors.accountId.message}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label>Category</Label>
              <Select onValueChange={(v) => setValue('categoryId', v)}>
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
                <Label htmlFor="amount">Amount</Label>
                <Input id="amount" type="number" step="0.01" min="0.01" placeholder="0.00" {...register('amount')} />
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
                {isSubmitting ? 'Creating...' : 'Create Transaction'}
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
