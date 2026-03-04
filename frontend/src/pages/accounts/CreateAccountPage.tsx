import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { createAccount } from '@/api/accounts.api'
import { ApiClientError } from '@/api/client'
import { ACCOUNT_TYPES } from '@/constants/account-types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'

const createAccountSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  accountTypeCode: z.string().min(1, 'Account type is required'),
  initialBalance: z.string().min(1, 'Initial balance is required').regex(/^-?\d+(\.\d{1,4})?$/, 'Invalid amount'),
  currency: z.string().regex(/^[A-Z]{3}$/, 'Must be a 3-letter ISO code'),
  institutionName: z.string().max(100).optional().or(z.literal('')),
  accountNumberLast4: z
    .string()
    .optional()
    .or(z.literal(''))
    .refine((v) => !v || /^\d{4}$/.test(v), 'Must be exactly 4 digits'),
})

type CreateAccountForm = z.infer<typeof createAccountSchema>

export function CreateAccountPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CreateAccountForm>({
    resolver: zodResolver(createAccountSchema),
    defaultValues: {
      currency: 'USD',
      initialBalance: '0',
    },
  })

  const onSubmit = async (data: CreateAccountForm) => {
    setError(null)
    try {
      const request = {
        name: data.name,
        accountTypeCode: data.accountTypeCode,
        initialBalance: parseFloat(data.initialBalance),
        currency: data.currency,
        institutionName: data.institutionName || undefined,
        accountNumberLast4: data.accountNumberLast4 || undefined,
      }
      await createAccount(request)
      toast.success('Account created successfully')
      navigate('/accounts')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to create account')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Create Account</h1>

      <Card>
        <CardHeader>
          <CardTitle>Account Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="space-y-2">
              <Label htmlFor="name">Account Name</Label>
              <Input id="name" placeholder="e.g., Main Checking" {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>

            <div className="space-y-2">
              <Label>Account Type</Label>
              <Select onValueChange={(v) => setValue('accountTypeCode', v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select account type" />
                </SelectTrigger>
                <SelectContent>
                  {ACCOUNT_TYPES.map((type) => (
                    <SelectItem key={type.code} value={type.code}>
                      {type.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.accountTypeCode && (
                <p className="text-sm text-destructive">{errors.accountTypeCode.message}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="initialBalance">Initial Balance</Label>
                <Input id="initialBalance" placeholder="0.00" {...register('initialBalance')} />
                {errors.initialBalance && (
                  <p className="text-sm text-destructive">{errors.initialBalance.message}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="currency">Currency</Label>
                <Input id="currency" placeholder="USD" {...register('currency')} />
                {errors.currency && <p className="text-sm text-destructive">{errors.currency.message}</p>}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="institutionName">Institution Name (optional)</Label>
              <Input id="institutionName" placeholder="e.g., Chase Bank" {...register('institutionName')} />
              {errors.institutionName && (
                <p className="text-sm text-destructive">{errors.institutionName.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="accountNumberLast4">Last 4 Digits (optional)</Label>
              <Input id="accountNumberLast4" placeholder="1234" maxLength={4} {...register('accountNumberLast4')} />
              {errors.accountNumberLast4 && (
                <p className="text-sm text-destructive">{errors.accountNumberLast4.message}</p>
              )}
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creating...' : 'Create Account'}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/accounts')}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  )
}
