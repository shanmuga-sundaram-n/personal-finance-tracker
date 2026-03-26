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
import { CategorySelect } from '@/components/shared/CategorySelect'
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
import { typeBadgeVariant, typeLabel } from '@/lib/transactionUtils'

// ── Page size options ─────────────────────────────────────────────────────────

const PAGE_SIZE_OPTIONS = [5, 10, 20, 50] as const
type PageSize = (typeof PAGE_SIZE_OPTIONS)[number]
const DEFAULT_PAGE_SIZE: PageSize = 10

// ── Column header strip ───────────────────────────────────────────────────────

function ColumnHeaderStrip() {
  return (
    <div
      className="hidden sm:grid sm:grid-cols-[100px_1fr_130px_100px_110px_72px] sm:gap-x-3 py-2 px-3 mb-1 border-b border-border/40"
      aria-hidden="true"
    >
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">Date</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">Category</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">Account</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">Type</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">Amount</p>
      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground text-right">Actions</p>
    </div>
  )
}

// ── Transaction row ───────────────────────────────────────────────────────────

interface TransactionRowProps {
  tx: Transaction
  onDelete: (tx: Transaction) => void
}

function TransactionRow({ tx, onDelete }: TransactionRowProps) {
  const description = tx.description || tx.merchantName || '—'
  const subtext = tx.merchantName && tx.description ? tx.merchantName : null
  return (
    <div className="py-3 border-b border-border/40 last:border-0 pl-3">
      {/* ── Desktop layout ── */}
      <div className="hidden sm:grid sm:grid-cols-[100px_1fr_130px_100px_110px_72px] sm:gap-x-3 sm:items-center">
        {/* Date */}
        <p className="text-sm text-muted-foreground tabular-nums">{tx.transactionDate}</p>

        {/* Category */}
        <p className="text-sm truncate">{tx.categoryName}</p>

        {/* Account */}
        <p className="text-sm truncate text-muted-foreground">{tx.accountName}</p>

        {/* Type badge */}
        <div>
          <Badge variant={typeBadgeVariant(tx.type)} className="text-[11px]">{typeLabel(tx.type)}</Badge>
        </div>

        {/* Amount */}
        <div className="text-right">
          <TransactionAmount
            amount={tx.amount}
            currency={tx.currency}
            type={tx.type}
            className="text-sm font-semibold tabular-nums"
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-0.5">
          {!tx.type.startsWith('TRANSFER') && (
            <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
              <Link to={`/transactions/${tx.id}/edit`} aria-label={`Edit ${description}`}>
                <Pencil className="h-4 w-4 text-muted-foreground" />
              </Link>
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => onDelete(tx)}
            aria-label={`Delete ${description}`}
          >
            <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
          </Button>
        </div>
      </div>

      {/* ── Mobile layout ── */}
      <div className="sm:hidden space-y-2">
        {/* Row 1: description + amount */}
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-sm font-medium truncate">{description}</p>
            {subtext && <p className="text-xs text-muted-foreground truncate">{subtext}</p>}
          </div>
          <TransactionAmount
            amount={tx.amount}
            currency={tx.currency}
            type={tx.type}
            className="text-sm font-semibold tabular-nums shrink-0"
          />
        </div>

        {/* Row 2: date · category · type */}
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-muted-foreground tabular-nums">{tx.transactionDate}</span>
          <span className="text-xs text-muted-foreground">·</span>
          <span className="text-xs text-muted-foreground truncate">{tx.categoryName}</span>
          <span className="text-xs text-muted-foreground">·</span>
          <Badge variant={typeBadgeVariant(tx.type)} className="text-[10px] px-1.5 py-0">{typeLabel(tx.type)}</Badge>
        </div>

        {/* Row 3: account + actions */}
        <div className="flex items-center justify-between">
          <span className="text-xs text-muted-foreground truncate">{tx.accountName}</span>
          <div className="flex gap-0.5 shrink-0">
            {!tx.type.startsWith('TRANSFER') && (
              <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                <Link to={`/transactions/${tx.id}/edit`} aria-label={`Edit ${description}`}>
                  <Pencil className="h-4 w-4 text-muted-foreground" />
                </Link>
              </Button>
            )}
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={() => onDelete(tx)}
              aria-label={`Delete ${description}`}
            >
              <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Pagination controls ───────────────────────────────────────────────────────

interface PaginationControlsProps {
  page: number           // 0-based from server
  pageSize: PageSize
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
  onPageSizeChange: (size: PageSize) => void
}

function PaginationControls({
  page,
  pageSize,
  totalElements,
  totalPages,
  onPageChange,
  onPageSizeChange,
}: PaginationControlsProps) {
  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, totalElements)

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-4 mt-2 border-t border-border/40">
      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground whitespace-nowrap">Rows per page:</span>
        <Select
          value={String(pageSize)}
          onValueChange={(val) => onPageSizeChange(Number(val) as PageSize)}
        >
          <SelectTrigger className="h-8 w-16 text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PAGE_SIZE_OPTIONS.map((size) => (
              <SelectItem key={size} value={String(size)} className="text-xs">
                {size}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground tabular-nums whitespace-nowrap">
          {from}–{to} of {totalElements}
        </span>
        <Button
          variant="outline"
          size="icon"
          className="h-11 w-11"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          aria-label="Previous page"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <Button
          variant="outline"
          size="icon"
          className="h-11 w-11"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          aria-label="Next page"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

function currentMonthRange() {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const pad = (n: number) => String(n).padStart(2, '0')
  const lastDay = new Date(y, m, 0).getDate()
  return {
    from: `${y}-${pad(m)}-01`,
    to: `${y}-${pad(m)}-${pad(lastDay)}`,
  }
}

export function TransactionsListPage() {
  const { transactions, page, totalPages, totalElements, isLoading, error, filters, setFilters, refresh } =
    useTransactions({ size: DEFAULT_PAGE_SIZE, ...currentMonthRange() })

  const { accounts } = useAccounts()
  const { categories } = useCategories()
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null)
  const [deleting, setDeleting] = useState(false)

  const currentPageSize = (filters.size ?? DEFAULT_PAGE_SIZE) as PageSize

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

  const goToPage = (p: number) => setFilters((prev) => ({ ...prev, page: p }))

  const changePageSize = (size: PageSize) =>
    setFilters((prev) => ({ ...prev, size, page: 0 }))

  if (isLoading && transactions.length === 0) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) return <ErrorAlert message={error} />

  return (
    <div className="space-y-6">
      {/* ── Page header ── */}
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

      {/* ── Filters ── */}
      <Card>
        <CardContent className="grid grid-cols-2 items-end gap-3 pt-6 md:grid-cols-3 lg:grid-cols-5">
          <div className="space-y-1">
            <label className="text-sm font-medium">Account</label>
            <Select
              value={filters.accountId ? String(filters.accountId) : 'all'}
              onValueChange={(v) =>
                setFilters((prev) => ({ ...prev, accountId: v === 'all' ? undefined : Number(v), page: 0 }))
              }
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
            <label htmlFor="filter-category" className="text-sm font-medium">Category</label>
            <CategorySelect
              id="filter-category"
              categories={categories.filter((c) => c.isActive)}
              value={filters.categoryId ? String(filters.categoryId) : 'all'}
              onValueChange={(v) =>
                setFilters((prev) => ({ ...prev, categoryId: v === 'all' ? undefined : Number(v), page: 0 }))
              }
              includeAll
              className="w-full"
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Type</label>
            <Select
              value={filters.type || 'ALL'}
              onValueChange={(v) =>
                setFilters((prev) => ({ ...prev, type: v === 'ALL' ? undefined : v, page: 0 }))
              }
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
              onChange={(e) =>
                setFilters((prev) => ({ ...prev, from: e.target.value || undefined, page: 0 }))
              }
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">To</label>
            <Input
              type="date"
              className="w-full"
              value={filters.to || ''}
              onChange={(e) =>
                setFilters((prev) => ({ ...prev, to: e.target.value || undefined, page: 0 }))
              }
            />
          </div>
        </CardContent>
      </Card>

      {/* ── Transaction list ── */}
      <div className="relative">
        {isLoading && transactions.length > 0 && (
          <div className="absolute inset-0 z-10 flex items-center justify-center rounded-xl bg-background/60 backdrop-blur-sm">
            <LoadingSpinner size="lg" />
          </div>
        )}

        <Card className="overflow-hidden">
          <CardContent className="px-5 pt-3 pb-5">
            {transactions.length === 0 ? (
              <div className="py-12 text-center">
                <p className="text-muted-foreground">No transactions found.</p>
              </div>
            ) : (
              <>
                <ColumnHeaderStrip />

                {transactions.map((tx) => (
                  <TransactionRow key={tx.id} tx={tx} onDelete={setDeleteTarget} />
                ))}

                <PaginationControls
                  page={page}
                  pageSize={currentPageSize}
                  totalElements={totalElements}
                  totalPages={totalPages}
                  onPageChange={goToPage}
                  onPageSizeChange={changePageSize}
                />
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* ── Delete dialog ── */}
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
