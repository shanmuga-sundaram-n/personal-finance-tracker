import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, ArrowLeftRight, Trash2, Pencil, ChevronLeft, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { useTransactions } from '@/hooks/useTransactions'
import { useAccounts } from '@/hooks/useAccounts'
import { useCategories } from '@/hooks/useCategories'
import { deleteTransaction } from '@/api/transactions.api'
import { TRANSACTION_TYPE_OPTIONS } from '@/constants/transaction-types'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { TransactionAmount } from '@/components/shared/TransactionAmount'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
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
import type { Transaction } from '@/types/transaction.types'
import { typeBadgeVariant, typeLabel, rowBorderClass } from '@/lib/transactionUtils'

function getPageNumbers(current: number, total: number): (number | '...')[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i)
  }
  const pages: (number | '...')[] = [0]
  if (current > 2) pages.push('...')
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) {
    pages.push(i)
  }
  if (current < total - 3) pages.push('...')
  pages.push(total - 1)
  return pages
}

export function TransactionsListPage() {
  const { transactions, page, totalPages, totalElements, isLoading, error, filters, setFilters, refresh } = useTransactions()
  const { accounts } = useAccounts()
  const { categories } = useCategories()
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null)
  const [deleting, setDeleting] = useState(false)

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteTransaction(deleteTarget.id)
      toast.success('Transaction deleted')
      setDeleteTarget(null)
      refresh()
    } catch {
      toast.error('Failed to delete transaction')
    } finally {
      setDeleting(false)
    }
  }

  const goToPage = (p: number) => {
    setFilters((prev) => ({ ...prev, page: p }))
  }

  if (isLoading && transactions.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) return <ErrorAlert message={error} />

  const pageNumbers = getPageNumbers(page, totalPages)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Transactions</h1>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link to="/transactions/transfer">
              <ArrowLeftRight className="mr-2 h-4 w-4" />
              Transfer
            </Link>
          </Button>
          <Button asChild>
            <Link to="/transactions/new">
              <Plus className="mr-2 h-4 w-4" />
              Add Transaction
            </Link>
          </Button>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="grid grid-cols-2 items-end gap-3 pt-6 md:grid-cols-3 lg:grid-cols-5">
          <div className="space-y-1">
            <label className="text-sm font-medium">Account</label>
            <Select
              value={filters.accountId ? String(filters.accountId) : 'all'}
              onValueChange={(v) => setFilters((prev) => ({ ...prev, accountId: v === 'all' ? undefined : Number(v), page: 0 }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Accounts</SelectItem>
                {accounts.map((a) => (
                  <SelectItem key={a.id} value={String(a.id)}>
                    {a.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Category</label>
            <Select
              value={filters.categoryId ? String(filters.categoryId) : 'all'}
              onValueChange={(v) => setFilters((prev) => ({ ...prev, categoryId: v === 'all' ? undefined : Number(v), page: 0 }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Categories</SelectItem>
                {categories.filter((c) => c.isActive).map((c) => (
                  <SelectItem key={c.id} value={String(c.id)}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Type</label>
            <Select
              value={filters.type || 'ALL'}
              onValueChange={(v) => setFilters((prev) => ({ ...prev, type: v === 'ALL' ? undefined : v, page: 0 }))}
            >
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TRANSACTION_TYPE_OPTIONS.map((t) => (
                  <SelectItem key={t.code} value={t.code}>
                    {t.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">From</label>
            <Input
              type="date"
              className="w-full"
              value={filters.from || ''}
              onChange={(e) => setFilters((prev) => ({ ...prev, from: e.target.value || undefined, page: 0 }))}
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">To</label>
            <Input
              type="date"
              className="w-full"
              value={filters.to || ''}
              onChange={(e) => setFilters((prev) => ({ ...prev, to: e.target.value || undefined, page: 0 }))}
            />
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <div className="relative">
      {isLoading && transactions.length > 0 && (
        <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-background/60 backdrop-blur-sm">
          <LoadingSpinner size="lg" />
        </div>
      )}
      <Card>
        <CardContent className="p-0">
          {transactions.length === 0 ? (
            <div className="py-12 text-center">
              <p className="text-muted-foreground">No transactions found.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="sticky top-0 z-10 bg-background">
                  <tr className="border-b text-left text-sm text-muted-foreground">
                    <th className="px-4 py-3 font-medium">Date</th>
                    <th className="px-4 py-3 font-medium">Description</th>
                    <th className="px-4 py-3 font-medium">Category</th>
                    <th className="px-4 py-3 font-medium">Account</th>
                    <th className="px-4 py-3 font-medium">Type</th>
                    <th className="px-4 py-3 text-right font-medium">Amount</th>
                    <th className="px-4 py-3 w-10"></th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((tx) => (
                    <tr key={tx.id} className={`border-b last:border-0 hover:bg-accent/50 ${rowBorderClass(tx.type)}`}>
                      <td className="px-4 py-3 text-sm">{tx.transactionDate}</td>
                      <td className="px-4 py-3">
                        <div className="text-sm font-medium">{tx.description || tx.merchantName || '—'}</div>
                        {tx.merchantName && tx.description && (
                          <div className="text-xs text-muted-foreground">{tx.merchantName}</div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm">{tx.categoryName}</td>
                      <td className="px-4 py-3 text-sm">{tx.accountName}</td>
                      <td className="px-4 py-3">
                        <Badge variant={typeBadgeVariant(tx.type)}>{typeLabel(tx.type)}</Badge>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <TransactionAmount
                          amount={tx.amount}
                          currency={tx.currency}
                          type={tx.type}
                          className="text-sm font-medium"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-1">
                          {!tx.type.startsWith('TRANSFER') && (
                            <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                              <Link to={`/transactions/${tx.id}/edit`}>
                                <Pencil className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                              </Link>
                            </Button>
                          )}
                          <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setDeleteTarget(tx)}>
                            <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages} ({totalElements} total)
          </p>
          <div className="flex items-center gap-1">
            <Button variant="outline" size="sm" disabled={page === 0} onClick={() => goToPage(page - 1)}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            {pageNumbers.map((p, idx) =>
              p === '...' ? (
                <span key={`ellipsis-${idx}`} className="px-2 text-sm text-muted-foreground">
                  …
                </span>
              ) : (
                <Button
                  key={p}
                  variant={p === page ? 'default' : 'outline'}
                  size="sm"
                  className="h-8 w-8 p-0"
                  onClick={() => goToPage(p)}
                >
                  {p + 1}
                </Button>
              )
            )}
            <Button variant="outline" size="sm" disabled={page >= totalPages - 1} onClick={() => goToPage(page + 1)}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}

      {/* Delete Dialog */}
      <AlertDialog open={deleteTarget !== null} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Transaction</AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTarget?.transferPairId
                ? 'This is a transfer transaction. Deleting it will also delete the linked transfer leg and reverse both account balances. This action cannot be undone.'
                : 'Are you sure you want to delete this transaction? The account balance will be reversed. This action cannot be undone.'}
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
