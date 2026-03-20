import { useState, useMemo, useEffect, useCallback, useRef } from 'react'
import {
  Lock,
  Pencil,
  Plus,
  Trash2,
  Search,
  ChevronLeft,
  ChevronRight,
  Tag,
  Info,
  X,
  FolderOpen,
  FolderPlus,
  ChevronDown,
  ChevronRight as ChevronRightSmall,
} from 'lucide-react'
import { toast } from 'sonner'
import { useCategories } from '@/hooks/useCategories'
import { createCategory, deleteCategory, updateCategory } from '@/api/categories.api'
import { listBudgets } from '@/api/budgets.api'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog'
import type { Category, UpdateCategoryRequest } from '@/types/category.types'
import type { Budget } from '@/types/budget.types'

const PAGE_SIZE = 20

// ─── helpers ────────────────────────────────────────────────────────────────


// ─── skeleton ────────────────────────────────────────────────────────────────

function SkeletonLoader() {
  return (
    <div className="space-y-6">
      {/* stat cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[0, 1, 2].map((i) => (
          <div key={i} className="overflow-hidden rounded-none border bg-card animate-pulse">
            <div className="h-1 bg-muted rounded-t" />
            <div className="p-4 space-y-2">
              <div className="h-3 w-24 bg-muted rounded" />
              <div className="h-7 w-12 bg-muted rounded" />
            </div>
          </div>
        ))}
      </div>
      {/* tab bar */}
      <div className="flex gap-2 animate-pulse">
        <div className="h-9 w-28 bg-muted rounded" />
        <div className="h-9 w-28 bg-muted rounded" />
      </div>
      {/* rows */}
      <div className="space-y-2">
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <div
            key={i}
            className="flex items-center gap-3 rounded-md border-l-4 border-muted bg-muted/30 px-4 py-3 animate-pulse"
          >
            <div className="h-8 w-8 flex-shrink-0 rounded-none bg-muted" />
            <div
              className="h-4 flex-1 rounded bg-muted"
              style={{ maxWidth: `${120 + (i % 3) * 60}px` }}
            />
            <div className="h-11 w-11 rounded bg-muted" />
            <div className="h-11 w-11 rounded bg-muted" />
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── edit dialog ──────────────────────────────────────────────────────────────

interface EditDialogProps {
  category: Category | null
  onClose: () => void
  onSaved: () => void
}

function EditDialog({ category, onClose, onSaved }: EditDialogProps) {
  const [name, setName] = useState('')
  const [icon, setIcon] = useState('')
  const [color, setColor] = useState('#6b7280')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (category) {
      setName(category.name)
      setIcon(category.icon ?? '')
      setColor(category.color ?? '#6b7280')
    }
  }, [category])

  const handleSave = async () => {
    if (!category) return
    const trimmedName = name.trim()
    if (!trimmedName) return
    setSaving(true)
    try {
      const payload: UpdateCategoryRequest = {
        name: trimmedName,
        icon: icon.trim() || undefined,
        color: color || undefined,
      }
      await updateCategory(category.id, payload)
      toast.success('Category updated')
      onSaved()
      onClose()
    } catch {
      toast.error('Failed to update category')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={category !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Edit "{category?.name}"</DialogTitle>
          <DialogDescription>
            Update the name or colour for this category.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label htmlFor="edit-name">Name</Label>
            <Input
              id="edit-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Category name"
              className="h-11"
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="edit-color">Colour</Label>
            <div className="flex items-center gap-3">
              <div
                className="h-11 w-11 flex-shrink-0 rounded-md border border-input shadow-sm"
                style={{ backgroundColor: color }}
                aria-hidden="true"
              />
              <input
                id="edit-color"
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                className="h-11 w-14 cursor-pointer rounded border border-input bg-background p-1"
                aria-label="Choose category colour"
              />
              <span className="text-sm text-muted-foreground font-mono">{color}</span>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={saving} className="h-11">
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={saving || !name.trim()} className="h-11">
            {saving ? 'Saving…' : 'Save changes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── create category dialog ───────────────────────────────────────────────────

interface CreateDialogState {
  open: boolean
  /** When set, this dialog is adding a child to this parent */
  parentCategory: Category | null
  /** Pre-selected type (matches the active tab) */
  defaultType: 'EXPENSE' | 'INCOME'
}

interface CreateCategoryDialogProps {
  state: CreateDialogState
  allCategories: Category[]
  onClose: () => void
  onCreated: (type: string) => void
}

function CreateCategoryDialog({
  state,
  allCategories,
  onClose,
  onCreated,
}: CreateCategoryDialogProps) {
  const { open, parentCategory, defaultType } = state

  const [name, setName] = useState('')
  const [selectedParentId, setSelectedParentId] = useState<string>('')
  const [icon, setIcon] = useState('')
  const [color, setColor] = useState('#6b7280')
  const [saving, setSaving] = useState(false)
  const nameRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (open) {
      setName('')
      setSelectedParentId(parentCategory ? String(parentCategory.id) : '')
      setIcon('')
      setColor(parentCategory?.color ?? '#6b7280')
      // Focus name input after the dialog animation settles
      setTimeout(() => nameRef.current?.focus(), 80)
    }
  }, [open, defaultType, parentCategory])

  const parentOptions = useMemo(
    () =>
      allCategories.filter(
        (c) => c.categoryTypeCode === defaultType && c.parentCategoryId == null
      ),
    [allCategories, defaultType]
  )

  const handleSave = async () => {
    const trimmedName = name.trim()
    if (!trimmedName) return
    setSaving(true)
    try {
      const typeCode = parentCategory ? parentCategory.categoryTypeCode : defaultType
      await createCategory({
        name: trimmedName,
        categoryTypeCode: typeCode,
        parentCategoryId: parentCategory
          ? parentCategory.id
          : selectedParentId
            ? parseInt(selectedParentId, 10)
            : undefined,
        icon: icon.trim() || undefined,
        color: color || undefined,
      })
      toast.success('Category created')
      onCreated(typeCode)
      onClose()
    } catch {
      toast.error('Failed to create category')
    } finally {
      setSaving(false)
    }
  }

  const typeLabel = parentCategory
    ? parentCategory.categoryTypeName
    : defaultType === 'EXPENSE'
      ? 'Expense'
      : 'Income'

  const dialogTitle = parentCategory
    ? `Add sub-category under "${parentCategory.name}"`
    : `New ${typeLabel} Category`

  const dialogDescription = parentCategory
    ? `This will be nested under "${parentCategory.name}".`
    : `Create a new top-level ${typeLabel.toLowerCase()} category, or choose a parent to nest it.`

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{dialogTitle}</DialogTitle>
          <DialogDescription>{dialogDescription}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-1">
          <div className="space-y-1.5">
            <Label htmlFor="create-name">Name</Label>
            <Input
              ref={nameRef}
              id="create-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && !saving && name.trim() && handleSave()}
              placeholder="e.g. Groceries"
              className="h-11"
            />
          </div>

          {!parentCategory && (
            <div className="space-y-1.5">
              <Label>
                Parent{' '}
                <span className="font-normal text-muted-foreground">(optional)</span>
              </Label>
              <Select
                value={selectedParentId || 'none'}
                onValueChange={(v) => setSelectedParentId(v === 'none' ? '' : v)}
              >
                <SelectTrigger className="h-11">
                  <SelectValue placeholder="None — top-level category" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None — top-level category</SelectItem>
                  {parentOptions.map((cat) => (
                    <SelectItem key={cat.id} value={String(cat.id)}>
                      {cat.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Leave empty to create a top-level category, or choose a parent to nest this as a
                sub-category.
              </p>
            </div>
          )}

          <div className="grid grid-cols-1 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="create-color">Colour</Label>
              <div className="flex items-center gap-2">
                <div
                  className="h-11 w-11 flex-shrink-0 rounded-md border border-input shadow-sm"
                  style={{ backgroundColor: color }}
                  aria-hidden="true"
                />
                <input
                  id="create-color"
                  type="color"
                  value={color}
                  onChange={(e) => setColor(e.target.value)}
                  className="h-11 w-full cursor-pointer rounded border border-input bg-background p-1"
                  aria-label="Choose category colour"
                />
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={saving} className="h-11">
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={saving || !name.trim()}
            className="h-11"
          >
            {saving ? 'Creating…' : 'Create category'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── single category row ──────────────────────────────────────────────────────

interface CategoryRowProps {
  category: Category
  isParent: boolean
  typeCode: string
  budgetedIds: Set<number>
  isExpanded?: boolean
  childCount?: number
  onToggleExpand?: () => void
  onEdit: (cat: Category) => void
  onDelete: (id: number, name: string) => void
  onAddChild: (cat: Category) => void
}

function CategoryRow({
  category,
  isParent,
  typeCode,
  budgetedIds,
  isExpanded,
  childCount = 0,
  onToggleExpand,
  onEdit,
  onDelete,
  onAddChild,
}: CategoryRowProps) {
  const isBudgeted = typeCode !== 'TRANSFER' && budgetedIds.has(category.id)
  const isSystem = category.isSystem
  const hasChildren = childCount > 0

  const borderColor = category.color ?? (isSystem ? '#6b7280' : undefined)
  const rowClasses = isParent
    ? 'group flex items-center gap-2 border-l-4 px-3 py-2.5 transition-colors hover:bg-muted/40'
    : 'group flex items-center gap-2 border-l border-muted/40 py-2 pl-10 pr-3 transition-colors hover:bg-muted/20'

  return (
    <div
      className={rowClasses}
      style={isParent && borderColor ? { borderLeftColor: borderColor } : undefined}
    >
      {/* Chevron for parents with children */}
      {isParent && hasChildren && onToggleExpand && (
        <button
          type="button"
          onClick={onToggleExpand}
          className="flex h-6 w-6 flex-shrink-0 items-center justify-center text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none"
          aria-label={isExpanded ? `Collapse ${category.name}` : `Expand ${category.name}`}
          aria-expanded={isExpanded}
        >
          {isExpanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRightSmall className="h-3.5 w-3.5" />}
        </button>
      )}
      {/* Spacer for parents without children */}
      {isParent && !hasChildren && <span className="w-6 flex-shrink-0" />}

      {/* Name + child count */}
      <div className="min-w-0 flex-1">
        <span
          className={[
            'block truncate',
            isParent ? 'text-sm font-semibold' : 'text-sm text-foreground/80',
            isSystem && isParent ? 'text-foreground/60' : '',
          ]
            .filter(Boolean)
            .join(' ')}
        >
          {category.name}
          {isParent && hasChildren && (
            <span className="ml-1.5 text-xs font-normal text-muted-foreground">
              {childCount}
            </span>
          )}
        </span>
      </div>

      {/* Badges */}
      <div className="flex shrink-0 items-center gap-1.5">
        {isBudgeted && (
          <>
            <Badge
              variant="outline"
              className="hidden shrink-0 border-green-500/50 bg-green-50 text-green-700 text-[11px] dark:bg-green-950/30 dark:text-green-400 sm:flex"
            >
              Budgeted
            </Badge>
            <span
              className="h-2 w-2 shrink-0 rounded-none bg-green-600 sm:hidden"
              role="img"
              aria-label="Has active budget"
            />
          </>
        )}
        {isSystem && (
          <Badge
            variant="outline"
            className="hidden shrink-0 text-[11px] text-muted-foreground/70 sm:flex"
          >
            System
          </Badge>
        )}
      </div>

      {/* Actions */}
      <div className="flex shrink-0 items-center">
        {isParent && !isSystem && (
          <Button
            variant="ghost"
            size="icon"
            className="h-11 w-11 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100"
            title="Add sub-category"
            aria-label={`Add sub-category under ${category.name}`}
            onClick={() => onAddChild(category)}
          >
            <FolderPlus className="h-4 w-4" />
          </Button>
        )}

        {isSystem ? (
          <span
            className="flex h-11 w-11 items-center justify-center"
            role="img"
            aria-label="System category — cannot be edited or deleted"
          >
            <Lock className="h-3.5 w-3.5 text-muted-foreground/40" />
          </span>
        ) : (
          <>
            <Button
              variant="ghost"
              size="icon"
              className="h-11 w-11 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100 focus-visible:opacity-100"
              title="Edit"
              aria-label={`Edit ${category.name}`}
              onClick={() => onEdit(category)}
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="h-11 w-11 text-muted-foreground opacity-0 transition-opacity hover:text-destructive hover:bg-destructive/10 group-hover:opacity-100 focus-visible:opacity-100"
              title="Delete"
              aria-label={`Delete ${category.name}`}
              onClick={() => onDelete(category.id, category.name)}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          </>
        )}
      </div>
    </div>
  )
}

// ─── section header ───────────────────────────────────────────────────────────

interface SectionHeaderProps {
  label: string
  count: number
  typeCode: string
  isSystem?: boolean
}

function SectionHeader({ label, count, typeCode, isSystem = false }: SectionHeaderProps) {
  const accentColor = isSystem
    ? 'border-muted-foreground/30'
    : typeCode === 'EXPENSE'
      ? 'border-red-500'
      : 'border-green-500'

  return (
    <div
      className={`flex items-center gap-2 border-l-4 pl-3 ${accentColor} mb-1 mt-4 first:mt-0`}
    >
      {isSystem ? (
        <Lock className="h-3 w-3 text-muted-foreground/50" aria-hidden="true" />
      ) : (
        <FolderOpen className="h-3 w-3 text-muted-foreground/70" aria-hidden="true" />
      )}
      <span className="text-xs font-semibold uppercase tracking-wide text-foreground/60">
        {label}
      </span>
      <span className="text-xs text-muted-foreground">({count})</span>
    </div>
  )
}

// ─── tab content ──────────────────────────────────────────────────────────────

interface CategoryTabContentProps {
  categories: Category[]
  typeCode: string
  typeName: string
  search: string
  budgetedIds: Set<number>
  onEdit: (cat: Category) => void
  onDelete: (id: number, name: string) => void
  onAddChild: (cat: Category) => void
  onAddTopLevel: () => void
}

function CategoryTabContent({
  categories,
  typeCode,
  typeName,
  search,
  budgetedIds,
  onEdit,
  onDelete,
  onAddChild,
  onAddTopLevel,
}: CategoryTabContentProps) {
  const [currentPage, setCurrentPage] = useState(0)
  // Track expanded state per parent id; default expanded
  const [collapsedIds, setCollapsedIds] = useState<Set<number>>(new Set())

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCurrentPage(0)
  }, [search, categories])

  const toggleExpand = useCallback((id: number) => {
    setCollapsedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }, [])

  const allChildren = useMemo(
    () => categories.filter((c) => c.parentCategoryId != null),
    [categories]
  )

  const buildHierarchy = useCallback(
    (parents: Category[]) =>
      parents.map((p) => ({
        ...p,
        children: allChildren.filter((c) => c.parentCategoryId === p.id),
      })),
    [allChildren]
  )

  const hasUserChild = useCallback(
    (parentId: number) =>
      allChildren.some((ch) => ch.parentCategoryId === parentId && !ch.isSystem),
    [allChildren]
  )

  const filterHierarchy = useCallback(
    (hier: ReturnType<typeof buildHierarchy>) => {
      if (!search.trim()) return hier
      const q = search.toLowerCase()
      return hier.filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          p.children.some((ch) => ch.name.toLowerCase().includes(q))
      )
    },
    [search]
  )

  const customHierarchy = useMemo(() => {
    const parents = categories.filter(
      (c) => c.parentCategoryId == null && (!c.isSystem || hasUserChild(c.id))
    )
    return filterHierarchy(buildHierarchy(parents))
  }, [categories, search, hasUserChild, filterHierarchy, buildHierarchy])

  const systemHierarchy = useMemo(() => {
    const parents = categories.filter(
      (c) => c.isSystem && c.parentCategoryId == null && !hasUserChild(c.id)
    )
    return filterHierarchy(buildHierarchy(parents))
  }, [categories, search, hasUserChild, filterHierarchy, buildHierarchy])

  // Build flat rows for pagination — honouring collapsed state when search is empty
  const allRows = useMemo(() => {
    const rows: Array<{ category: Category; isParent: boolean }> = []
    const allHier = [...customHierarchy, ...systemHierarchy]
    allHier.forEach((p) => {
      rows.push({ category: p, isParent: true })
      const expanded = !collapsedIds.has(p.id) || search.trim() !== ''
      if (expanded) {
        p.children.forEach((ch) => rows.push({ category: ch, isParent: false }))
      }
    })
    return rows
  }, [customHierarchy, systemHierarchy, collapsedIds, search])

  const totalPages = Math.ceil(allRows.length / PAGE_SIZE)
  const pagedRows = allRows.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)
  const pagedIds = useMemo(() => new Set(pagedRows.map((r) => r.category.id)), [pagedRows])

  // Reconstruct grouped hierarchy from paged rows for rendering
  const allHierarchy = useMemo(
    () => [...customHierarchy, ...systemHierarchy],
    [customHierarchy, systemHierarchy]
  )
  const allOnPage = allHierarchy.filter(
    (p) => pagedIds.has(p.id) || p.children.some((ch) => pagedIds.has(ch.id))
  )

  // ── empty states ────────────────────────────────────────────────────────────

  if (allRows.length === 0 && search.trim()) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-4 py-12">
          <div className="flex h-12 w-12 items-center justify-center rounded-none bg-muted">
            <Search className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
          </div>
          <div className="space-y-1 text-center">
            <p className="text-sm font-medium">No results for "{search}"</p>
            <p className="text-sm text-muted-foreground">
              Try a different search term, or check the other tab.
            </p>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (allRows.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center gap-4 py-12">
          <div
            className={`flex h-12 w-12 items-center justify-center rounded-none ${
              typeCode === 'EXPENSE' ? 'bg-red-50 dark:bg-red-950/30' : 'bg-green-50 dark:bg-green-950/30'
            }`}
          >
            <Tag
              className={`h-6 w-6 ${typeCode === 'EXPENSE' ? 'text-red-500' : 'text-green-600'}`}
              aria-hidden="true"
            />
          </div>
          <div className="space-y-1 text-center">
            <p className="text-sm font-semibold">No {typeName.toLowerCase()} categories yet</p>
            <p className="text-sm text-muted-foreground">
              Create your first {typeName.toLowerCase()} category to start organising transactions.
            </p>
          </div>
          <Button onClick={onAddTopLevel} className="h-11">
            <Plus className="mr-2 h-4 w-4" />
            Add {typeName} Category
          </Button>
        </CardContent>
      </Card>
    )
  }

  // ── rendered list ───────────────────────────────────────────────────────────

  const customOnPage = allOnPage.filter(
    (p) => !p.isSystem || customHierarchy.some((cp) => cp.id === p.id)
  )
  const systemOnPage = allOnPage.filter(
    (p) => p.isSystem && systemHierarchy.some((sp) => sp.id === p.id)
  )

  function renderGroup(group: typeof allOnPage) {
    return group.map((parent) => {
      const isExpanded = !collapsedIds.has(parent.id) || search.trim() !== ''
      const visibleChildren = parent.children.filter(
        (ch) => pagedIds.has(ch.id) || pagedIds.has(parent.id)
      )
      return (
        <div key={parent.id}>
          <CategoryRow
            category={parent}
            isParent
            typeCode={typeCode}
            budgetedIds={budgetedIds}
            isExpanded={isExpanded}
            childCount={parent.children.length}
            onToggleExpand={() => toggleExpand(parent.id)}
            onEdit={onEdit}
            onDelete={onDelete}
            onAddChild={onAddChild}
          />
          {isExpanded &&
            visibleChildren.map((child) => (
              <CategoryRow
                key={child.id}
                category={child}
                isParent={false}
                typeCode={typeCode}
                budgetedIds={budgetedIds}
                onEdit={onEdit}
                onDelete={onDelete}
                onAddChild={onAddChild}
              />
            ))}
        </div>
      )
    })
  }

  return (
    <div className="space-y-4">
      <div className="space-y-0.5">
        {/* Custom / user-owned categories */}
        {customOnPage.length > 0 && !search.trim() && (
          <SectionHeader
            label="Your categories"
            count={customHierarchy.length}
            typeCode={typeCode}
          />
        )}
        {renderGroup(customOnPage)}

        {/* System-only categories */}
        {systemOnPage.length > 0 && !search.trim() && (
          <SectionHeader
            label="System categories"
            count={systemHierarchy.length}
            typeCode={typeCode}
            isSystem
          />
        )}
        {renderGroup(systemOnPage)}
      </div>

      {/* Add category shortcut at bottom of list */}
      <button
        type="button"
        onClick={onAddTopLevel}
        className="flex w-full items-center gap-2 rounded-md border border-dashed border-muted-foreground/30 px-4 py-2.5 text-sm text-muted-foreground transition-colors hover:border-muted-foreground/50 hover:bg-muted/30 hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <Plus className="h-4 w-4" aria-hidden="true" />
        Add {typeName.toLowerCase()} category
      </button>

      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-1">
          <p className="text-sm text-muted-foreground">
            Page {currentPage + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              className="h-9"
              disabled={currentPage === 0}
              onClick={() => setCurrentPage((p) => p - 1)}
            >
              <ChevronLeft className="mr-1 h-4 w-4" />
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="h-9"
              disabled={currentPage >= totalPages - 1}
              onClick={() => setCurrentPage((p) => p + 1)}
            >
              Next
              <ChevronRight className="ml-1 h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── main page ────────────────────────────────────────────────────────────────

export function CategoriesListPage() {
  const { categories, isLoading, error, refresh } = useCategories()

  const [budgets, setBudgets] = useState<Budget[]>([])
  const [deleteTarget, setDeleteTarget] = useState<{ id: number; name: string } | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [editTarget, setEditTarget] = useState<Category | null>(null)
  const [search, setSearch] = useState('')
  const [activeTab, setActiveTab] = useState<'EXPENSE' | 'INCOME'>('EXPENSE')
  const [createDialog, setCreateDialog] = useState<CreateDialogState>({
    open: false,
    parentCategory: null,
    defaultType: 'EXPENSE',
  })

  useEffect(() => {
    listBudgets()
      .then(setBudgets)
      .catch(() => {
        // budgets are supplementary — don't surface an error for this
      })
  }, [])

  const budgetedIds = useMemo(
    () => new Set(budgets.filter((b) => b.isActive).map((b) => b.categoryId)),
    [budgets]
  )

  const handleDelete = async () => {
    if (deleteTarget === null) return
    setDeleting(true)
    try {
      await deleteCategory(deleteTarget.id)
      toast.success('Category deleted')
      setDeleteTarget(null)
      await refresh()
    } catch {
      toast.error('Failed to delete category')
    } finally {
      setDeleting(false)
    }
  }

  const handleAddChild = useCallback((cat: Category) => {
    setCreateDialog({
      open: true,
      parentCategory: cat,
      defaultType: cat.categoryTypeCode as 'EXPENSE' | 'INCOME',
    })
  }, [])

  const openCreateDialog = useCallback(() => {
    setCreateDialog({ open: true, parentCategory: null, defaultType: activeTab })
  }, [activeTab])

  const closeCreateDialog = useCallback(() => {
    setCreateDialog((prev) => ({ ...prev, open: false }))
  }, [])

  const expenseCategories = useMemo(
    () => categories.filter((c) => c.categoryTypeCode === 'EXPENSE'),
    [categories]
  )
  const incomeCategories = useMemo(
    () => categories.filter((c) => c.categoryTypeCode === 'INCOME'),
    [categories]
  )
  const transferCategories = useMemo(
    () => categories.filter((c) => c.categoryTypeCode === 'TRANSFER'),
    [categories]
  )

  // Tab counts: count visible parent groups (mirrors CategoryTabContent grouping)
  const filteredExpenseCount = useMemo(() => {
    const parents = expenseCategories.filter((c) => c.parentCategoryId == null)
    if (!search.trim()) return parents.length
    const q = search.toLowerCase()
    return parents.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        expenseCategories.some(
          (ch) => ch.parentCategoryId === p.id && ch.name.toLowerCase().includes(q)
        )
    ).length
  }, [expenseCategories, search])

  const filteredIncomeCount = useMemo(() => {
    const parents = incomeCategories.filter((c) => c.parentCategoryId == null)
    if (!search.trim()) return parents.length
    const q = search.toLowerCase()
    return parents.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        incomeCategories.some(
          (ch) => ch.parentCategoryId === p.id && ch.name.toLowerCase().includes(q)
        )
    ).length
  }, [incomeCategories, search])

  // Stat card values
  const totalCount = categories.length
  const customCount = categories.filter((c) => !c.isSystem).length
  const budgetedCategoryCount = useMemo(() => {
    const activeBudgetCategoryIds = new Set(
      budgets.filter((b) => b.isActive).map((b) => b.categoryId)
    )
    return categories.filter((c) => activeBudgetCategoryIds.has(c.id)).length
  }, [categories, budgets])

  if (isLoading) return <SkeletonLoader />
  if (error) {
    return (
      <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
        {error}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* ── Page header ──────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Categories</h1>
          <p className="mt-0.5 text-sm text-muted-foreground">
            Organise your transactions into expense and income groups.
          </p>
        </div>
        <Button onClick={openCreateDialog} className="h-11 shrink-0">
          <Plus className="mr-2 h-4 w-4" />
          <span className="hidden sm:inline">Add Category</span>
          <span className="sm:hidden">Add</span>
        </Button>
      </div>

      {/* ── Stat cards ───────────────────────────────────────────────────────── */}
      <div className="grid grid-cols-3 gap-3">
        <Card className="overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-indigo-500 via-indigo-400/70 to-indigo-300/30" />
          <CardContent className="p-4">
            <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
              Total
            </p>
            <p className="mt-1 text-2xl font-bold tabular-nums">{totalCount}</p>
          </CardContent>
        </Card>

        <Card className="overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-green-600 via-green-500/70 to-green-400/30" />
          <CardContent className="p-4">
            <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
              Yours
            </p>
            <p className="mt-1 text-2xl font-bold tabular-nums">{customCount}</p>
          </CardContent>
        </Card>

        <Card className="overflow-hidden">
          <div className="h-1 bg-gradient-to-r from-amber-500 via-amber-400/70 to-amber-300/30" />
          <CardContent className="p-4">
            <p className="text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
              Budgeted
            </p>
            <p className="mt-1 text-2xl font-bold tabular-nums">{budgetedCategoryCount}</p>
          </CardContent>
        </Card>
      </div>

      {/* ── Search ───────────────────────────────────────────────────────────── */}
      <div className="relative">
        <Search
          className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          placeholder="Search categories…"
          aria-label="Search categories"
          className="h-11 pl-9 pr-9"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {search && (
          <button
            type="button"
            onClick={() => setSearch('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 rounded-sm text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            aria-label="Clear search"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* ── Expense / Income tabs ─────────────────────────────────────────────── */}
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'EXPENSE' | 'INCOME')}>
        <TabsList className="h-10">
          <TabsTrigger value="EXPENSE" className="h-9 gap-1.5">
            <span
              className="inline-block h-2 w-2 rounded-none bg-red-500"
              aria-hidden="true"
            />
            Expense
            <span className="ml-0.5 tabular-nums text-muted-foreground">
              ({filteredExpenseCount})
            </span>
          </TabsTrigger>
          <TabsTrigger value="INCOME" className="h-9 gap-1.5">
            <span
              className="inline-block h-2 w-2 rounded-none bg-green-500"
              aria-hidden="true"
            />
            Income
            <span className="ml-0.5 tabular-nums text-muted-foreground">
              ({filteredIncomeCount})
            </span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="EXPENSE" className="mt-4">
          <CategoryTabContent
            categories={expenseCategories}
            typeCode="EXPENSE"
            typeName="Expense"
            search={search}
            budgetedIds={budgetedIds}
            onEdit={setEditTarget}
            onDelete={(id, name) => setDeleteTarget({ id, name })}
            onAddChild={handleAddChild}
            onAddTopLevel={openCreateDialog}
          />
        </TabsContent>

        <TabsContent value="INCOME" className="mt-4">
          <CategoryTabContent
            categories={incomeCategories}
            typeCode="INCOME"
            typeName="Income"
            search={search}
            budgetedIds={budgetedIds}
            onEdit={setEditTarget}
            onDelete={(id, name) => setDeleteTarget({ id, name })}
            onAddChild={handleAddChild}
            onAddTopLevel={openCreateDialog}
          />
        </TabsContent>
      </Tabs>

      {/* ── Transfer section ─────────────────────────────────────────────────── */}
      <div className="space-y-3 border-t pt-5">
        <div className="flex items-center gap-2 border-l-4 border-slate-400 pl-3">
          <span className="text-xs font-semibold uppercase tracking-wide text-foreground/60">
            Transfers
          </span>
        </div>
        <div className="flex items-start gap-3 rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-800/40 dark:text-slate-300">
          <Info
            className="mt-0.5 h-4 w-4 shrink-0 text-slate-500"
            aria-hidden="true"
          />
          <p>
            Transfers use a system-managed category. Custom transfer categories are not
            supported.
          </p>
        </div>
        {transferCategories.length > 0 && (
          <div className="space-y-0.5">
            {transferCategories.map((cat) => (
              <div
                key={cat.id}
                className="flex items-center gap-3 rounded-md border-l-4 border-slate-400 bg-muted/20 px-3 py-2.5"
              >
                <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-none bg-slate-100 dark:bg-slate-800">
                  <div
                    className="h-3 w-3 rounded-none"
                    style={{ backgroundColor: cat.color ?? '#e5e7eb' }}
                    aria-hidden="true"
                  />
                </div>
                <span className="flex-1 text-sm font-semibold text-foreground/60">
                  {cat.name}
                </span>
                <Badge
                  variant="outline"
                  className="hidden text-[11px] text-muted-foreground/70 sm:flex"
                >
                  System
                </Badge>
                <span
                  className="flex h-11 w-11 items-center justify-center"
                  role="img"
                  aria-label="System category — cannot be edited"
                >
                  <Lock className="h-3.5 w-3.5 text-muted-foreground/40" />
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Dialogs ──────────────────────────────────────────────────────────── */}
      <CreateCategoryDialog
        state={createDialog}
        allCategories={categories}
        onClose={closeCreateDialog}
        onCreated={(type) => {
          if (type === 'EXPENSE' || type === 'INCOME') setActiveTab(type)
          refresh()
        }}
      />

      <EditDialog
        category={editTarget}
        onClose={() => setEditTarget(null)}
        onSaved={refresh}
      />

      <AlertDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete "{deleteTarget?.name}"?</AlertDialogTitle>
            <AlertDialogDescription>
              This category will be deactivated and hidden from new transactions. Your existing
              transaction history is preserved.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleting}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {deleting ? 'Deleting…' : 'Delete'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
