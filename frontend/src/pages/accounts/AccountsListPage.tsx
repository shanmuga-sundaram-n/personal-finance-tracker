import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useAccounts } from '@/hooks/useAccounts'
import { deleteAccount } from '@/api/accounts.api'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
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

export function AccountsListPage() {
  const { accounts, isLoading, error, refresh } = useAccounts()
  const navigate = useNavigate()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

  const handleDelete = async () => {
    if (deleteId === null) return
    setDeleting(true)
    try {
      await deleteAccount(deleteId)
      toast.success('Account deleted')
      setDeleteId(null)
      refresh()
    } catch {
      toast.error('Failed to delete account')
    } finally {
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

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Accounts</h1>
        <Button asChild>
          <Link to="/accounts/new">
            <Plus className="mr-2 h-4 w-4" />
            Add Account
          </Link>
        </Button>
      </div>

      {accounts.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">No accounts yet.</p>
            <Button asChild className="mt-4">
              <Link to="/accounts/new">Create your first account</Link>
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {accounts.map((account) => (
            <Card
              key={account.id}
              className="cursor-pointer transition-all duration-200 hover:-translate-y-0.5 hover:bg-accent/50 hover:shadow-md"
              onClick={() => navigate(`/accounts/${account.id}`)}
            >
              <CardContent className="pb-5 pt-4">
                {/* Top row: type badge + delete */}
                <div className="flex items-center justify-between">
                  <Badge variant="secondary">{account.accountTypeName}</Badge>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    onClick={(e) => {
                      e.stopPropagation()
                      setDeleteId(account.id)
                    }}
                  >
                    <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                  </Button>
                </div>

                {/* Middle: account name + institution */}
                <div className="mt-3">
                  <p className="text-xl font-bold leading-tight">{account.name}</p>
                  {(account.institutionName || account.accountNumberLast4) && (
                    <p className="mt-0.5 text-sm text-muted-foreground">
                      {account.institutionName}
                      {account.accountNumberLast4 && ` ****${account.accountNumberLast4}`}
                    </p>
                  )}
                </div>

                {/* Bottom: balance */}
                <div className="mt-4">
                  <MoneyDisplay
                    amount={account.currentBalance}
                    currency={account.currency}
                    colored
                    className="text-2xl font-bold"
                  />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <AlertDialog open={deleteId !== null} onOpenChange={(open) => !open && setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Account</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this account? This action cannot be undone.
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
