import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { AreaChart, Area, XAxis, Tooltip, ResponsiveContainer } from 'recharts'
import { getAccount, updateAccount, deleteAccount } from '@/api/accounts.api'
import { ApiClientError } from '@/api/client'
import { useTransactions } from '@/hooks/useTransactions'
import type { Account } from '@/types/account.types'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'

export function AccountDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [account, setAccount] = useState<Account | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editInstitution, setEditInstitution] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)

  const txFilters = useMemo(
    () => ({ accountId: id ? parseInt(id, 10) : 0, size: 30 }),
    [id]
  )
  const { transactions } = useTransactions(txFilters)

  const load = useCallback(async () => {
    if (!id) return
    setIsLoading(true)
    try {
      const data = await getAccount(parseInt(id))
      setAccount(data)
      setEditName(data.name)
      setEditInstitution(data.institutionName ?? '')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load account')
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  const handleSave = async () => {
    if (!account) return
    setSaving(true)
    setError(null)
    try {
      const updated = await updateAccount(account.id, {
        name: editName,
        institutionName: editInstitution || undefined,
      })
      setAccount(updated)
      setEditing(false)
      toast.success('Account updated')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to update')
      }
      toast.error('Failed to update account')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!account) return
    setDeleting(true)
    try {
      await deleteAccount(account.id)
      toast.success('Account deleted')
      navigate('/accounts')
    } catch (e) {
      const message = e instanceof ApiClientError
        ? (e.apiError?.message ?? 'Failed to delete account')
        : 'Failed to delete account'
      toast.error(message)
      setDeleting(false)
    }
  }

  // Compute sparkline data: work backwards from currentBalance through sorted transactions
  const sparkData = useMemo(() => {
    if (!account || transactions.length === 0) return []
    const sorted = [...transactions].sort((a, b) =>
      b.transactionDate.localeCompare(a.transactionDate)
    )
    let balance = parseFloat(account.currentBalance)
    const points: { date: string; balance: number }[] = [
      { date: 'Now', balance: Math.round(balance * 100) / 100 },
    ]
    sorted.forEach((tx) => {
      const amt = parseFloat(tx.amount)
      if (tx.type === 'INCOME' || tx.type === 'TRANSFER_IN') {
        balance -= amt
      } else {
        balance += amt
      }
      points.push({
        date: tx.transactionDate,
        balance: Math.round(balance * 100) / 100,
      })
    })
    return points.reverse()
  }, [account, transactions])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) return <ErrorAlert message={error} />
  if (!account) return <ErrorAlert message="Account not found" />

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/accounts')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <h1 className="text-3xl font-bold">{account.name}</h1>
        <Badge variant="secondary">{account.accountTypeName}</Badge>
      </div>

      <Card className="overflow-hidden">
        <div className="h-1 bg-gradient-to-r from-blue-600 via-blue-500/70 to-blue-400/30" />
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Account Info</CardTitle>
          <div className="flex gap-2">
            {editing ? (
              <>
                <Button size="sm" onClick={handleSave} disabled={saving}>
                  {saving ? 'Saving...' : 'Save'}
                </Button>
                <Button size="sm" variant="outline" onClick={() => setEditing(false)}>
                  Cancel
                </Button>
              </>
            ) : (
              <Button size="sm" variant="outline" onClick={() => setEditing(true)}>
                Edit
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {editing ? (
            <>
              <div className="space-y-2">
                <Label>Account Name</Label>
                <Input value={editName} onChange={(e) => setEditName(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label>Institution Name</Label>
                <Input
                  value={editInstitution}
                  onChange={(e) => setEditInstitution(e.target.value)}
                  placeholder="Optional"
                />
              </div>
            </>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Current Balance</p>
                <MoneyDisplay
                  amount={account.currentBalance}
                  currency={account.currency}
                  colored
                  className="text-2xl font-bold"
                />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Initial Balance</p>
                <MoneyDisplay
                  amount={account.initialBalance}
                  currency={account.currency}
                  className="text-lg"
                />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Type</p>
                <p className="font-medium">{account.accountTypeName}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Currency</p>
                <p className="font-medium">{account.currency}</p>
              </div>
              {account.institutionName && (
                <div>
                  <p className="text-sm text-muted-foreground">Institution</p>
                  <p className="font-medium">{account.institutionName}</p>
                </div>
              )}
              {account.accountNumberLast4 && (
                <div>
                  <p className="text-sm text-muted-foreground">Account Number</p>
                  <p className="font-medium">****{account.accountNumberLast4}</p>
                </div>
              )}
              <div>
                <p className="text-sm text-muted-foreground">Include in Net Worth</p>
                <p className="font-medium">{account.includeInNetWorth ? 'Yes' : 'No'}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Created</p>
                <p className="font-medium">{new Date(account.createdAt).toLocaleDateString()}</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Balance Sparkline */}
      <Card>
        <CardHeader>
          <CardTitle>Balance History</CardTitle>
        </CardHeader>
        <CardContent>
          {sparkData.length === 0 ? (
            <p className="text-sm text-muted-foreground">No transaction history yet</p>
          ) : (
            <ResponsiveContainer width="100%" height={160}>
              <AreaChart data={sparkData} margin={{ top: 4, right: 8, bottom: 0, left: 0 }}>
                <defs>
                  <linearGradient id="balanceGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="hsl(var(--chart-1))" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="hsl(var(--chart-1))" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis
                  dataKey="date"
                  tick={{ fontSize: 10 }}
                  interval="preserveStartEnd"
                />
                <Tooltip
                  formatter={(value: unknown) => [
                    typeof value === 'number'
                      ? `${account.currency} ${value.toFixed(2)}`
                      : '',
                    'Balance',
                  ]}
                  contentStyle={{ borderRadius: '8px', fontSize: '12px' }}
                />
                <Area
                  type="monotone"
                  dataKey="balance"
                  stroke="hsl(var(--chart-1))"
                  strokeWidth={2}
                  fill="url(#balanceGradient)"
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Transaction history will appear here in a future update.
          </p>
        </CardContent>
      </Card>

      <Separator />

      <AlertDialog>
        <AlertDialogTrigger asChild>
          <Button variant="destructive" className="w-full">
            <Trash2 className="mr-2 h-4 w-4" />
            Delete Account
          </Button>
        </AlertDialogTrigger>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Account</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete "{account.name}"? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleting ? 'Deleting...' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
