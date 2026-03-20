import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { createTransfer } from '@/api/transactions.api'
import { ApiClientError } from '@/api/client'
import { useAccounts } from '@/hooks/useAccounts'
import { useCategories } from '@/hooks/useCategories'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'

const createTransferSchema = z
  .object({
    fromAccountId: z.string().min(1, 'Source account is required'),
    toAccountId: z.string().min(1, 'Destination account is required'),
    categoryId: z.string().min(1, 'Category is required'),
    amount: z.string().min(1, 'Amount is required').refine((v) => !isNaN(Number(v)) && Number(v) > 0, 'Must be a positive number'),
    transactionDate: z.string().min(1, 'Date is required'),
    description: z.string().max(500).optional().or(z.literal('')),
  })
  .refine((data) => data.fromAccountId !== data.toAccountId, {
    message: 'Source and destination accounts must be different',
    path: ['toAccountId'],
  })

type CreateTransferForm = z.infer<typeof createTransferSchema>

export function CreateTransferPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { accounts } = useAccounts()
  const { categories } = useCategories('TRANSFER')

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateTransferForm>({
    resolver: zodResolver(createTransferSchema),
    defaultValues: {
      transactionDate: new Date().toISOString().split('T')[0],
    },
  })

  const selectedFromId = watch('fromAccountId')
  const fromAccount = accounts.find((a) => String(a.id) === selectedFromId)

  const onSubmit = async (data: CreateTransferForm) => {
    setError(null)
    try {
      await createTransfer({
        fromAccountId: parseInt(data.fromAccountId, 10),
        toAccountId: parseInt(data.toAccountId, 10),
        categoryId: parseInt(data.categoryId, 10),
        amount: parseFloat(data.amount),
        currency: fromAccount?.currency ?? 'USD',
        transactionDate: data.transactionDate,
        description: data.description || undefined,
      })
      toast.success('Transfer created successfully')
      navigate('/transactions')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to create transfer')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  const activeAccounts = accounts.filter((a) => a.isActive)

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Create Transfer</h1>

      <Card className="overflow-hidden">
        <div className="h-1 bg-gradient-to-r from-blue-600 via-blue-500/70 to-blue-400/30" />
        <CardHeader>
          <CardTitle>Transfer Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>From Account</Label>
                <Select onValueChange={(v) => setValue('fromAccountId', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select source" />
                  </SelectTrigger>
                  <SelectContent>
                    {activeAccounts.map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.name} ({a.currency})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.fromAccountId && <p className="text-sm text-destructive">{errors.fromAccountId.message}</p>}
              </div>

              <div className="space-y-2">
                <Label>To Account</Label>
                <Select onValueChange={(v) => setValue('toAccountId', v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select destination" />
                  </SelectTrigger>
                  <SelectContent>
                    {activeAccounts.map((a) => (
                      <SelectItem key={a.id} value={String(a.id)}>
                        {a.name} ({a.currency})
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {errors.toAccountId && <p className="text-sm text-destructive">{errors.toAccountId.message}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label>Category</Label>
              <Select onValueChange={(v) => setValue('categoryId', v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {categories.filter((c) => c.isActive).map((c) => (
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
              <Input id="description" placeholder="e.g., Monthly savings transfer" {...register('description')} />
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creating...' : 'Create Transfer'}
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
