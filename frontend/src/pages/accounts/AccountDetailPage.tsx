import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { getAccount, updateAccount, deleteAccount } from '@/api/accounts.api'
import { ApiClientError } from '@/api/client'
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
    } catch {
      toast.error('Failed to delete account')
      setDeleting(false)
    }
  }

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

      <Card>
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
