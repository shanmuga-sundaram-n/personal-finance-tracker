import { useState, useMemo, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Trash2, Search, ChevronLeft, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { useCategories } from '@/hooks/useCategories'
import { deleteCategory } from '@/api/categories.api'
import { CATEGORY_TYPES } from '@/constants/category-types'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
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
import type { Category } from '@/types/category.types'

const PAGE_SIZE = 12

function buildHierarchy(cats: Category[]) {
  const topLevel = cats.filter((c) => c.parentCategoryId == null)
  const children = cats.filter((c) => c.parentCategoryId != null)
  return topLevel.map((parent) => ({
    ...parent,
    children: children.filter((c) => c.parentCategoryId === parent.id),
  }))
}

interface CategoryTabContentProps {
  categories: Category[]
  typeName: string
  search: string
  onDelete: (id: number) => void
}

function CategoryTabContent({ categories, typeName, search, onDelete }: CategoryTabContentProps) {
  const [currentPage, setCurrentPage] = useState(0)

  useEffect(() => {
    setCurrentPage(0)
  }, [search])

  const hierarchy = useMemo(() => buildHierarchy(categories), [categories])

  const filtered = useMemo(() => {
    if (!search.trim()) return hierarchy
    const q = search.toLowerCase()
    return hierarchy.filter(
      (parent) =>
        parent.name.toLowerCase().includes(q) ||
        parent.children.some((child) => child.name.toLowerCase().includes(q))
    )
  }, [hierarchy, search])

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)

  if (filtered.length === 0) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <p className="text-muted-foreground">
            {search ? 'No categories match your search.' : `No ${typeName.toLowerCase()} categories yet.`}
          </p>
        </CardContent>
      </Card>
    )
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {paged.map((parent) => (
          <Card key={parent.id} className="transition-all duration-200 hover:shadow-sm">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-start gap-3">
                  <div
                    className="h-10 w-10 flex-shrink-0 rounded-full"
                    style={{ backgroundColor: parent.color ?? '#e5e7eb' }}
                  />
                  <div>
                    <div className="flex items-center gap-1.5">
                      <CardTitle className="text-base leading-tight">{parent.name}</CardTitle>
                      {parent.icon && <span className="text-base">{parent.icon}</span>}
                    </div>
                    <div className="mt-1 flex flex-wrap gap-1">
                      {parent.isSystem && (
                        <Badge variant="outline" className="text-xs">System</Badge>
                      )}
                      {parent.children.length > 0 && (
                        <Badge variant="secondary" className="text-xs">
                          {parent.children.length} sub-{parent.children.length === 1 ? 'category' : 'categories'}
                        </Badge>
                      )}
                    </div>
                  </div>
                </div>
                {!parent.isSystem && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 flex-shrink-0"
                    onClick={() => onDelete(parent.id)}
                  >
                    <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                  </Button>
                )}
              </div>
            </CardHeader>
            {parent.children.length > 0 && (
              <CardContent className="pt-0">
                <div className="flex flex-wrap gap-1.5">
                  {parent.children.map((child) => (
                    <div
                      key={child.id}
                      className="flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs"
                    >
                      {child.color && (
                        <div
                          className="h-2.5 w-2.5 rounded-full"
                          style={{ backgroundColor: child.color }}
                        />
                      )}
                      <span>{child.name}</span>
                      {child.icon && <span>{child.icon}</span>}
                      {!child.isSystem && (
                        <button
                          className="ml-0.5 text-muted-foreground hover:text-destructive"
                          onClick={() => onDelete(child.id)}
                        >
                          ×
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            )}
          </Card>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {currentPage + 1} of {totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === 0}
              onClick={() => setCurrentPage((p) => p - 1)}
            >
              <ChevronLeft className="mr-1 h-4 w-4" />
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
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

export function CategoriesListPage() {
  const { categories, isLoading, error, refresh } = useCategories()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)
  const [search, setSearch] = useState('')

  const handleDelete = async () => {
    if (deleteId === null) return
    setDeleting(true)
    try {
      await deleteCategory(deleteId)
      toast.success('Category deleted')
      setDeleteId(null)
      refresh()
    } catch {
      toast.error('Failed to delete category')
    } finally {
      setDeleting(false)
    }
  }

  const groupedByType = useMemo(
    () =>
      CATEGORY_TYPES.reduce(
        (acc, type) => {
          acc[type.code] = categories.filter((c) => c.categoryTypeCode === type.code)
          return acc
        },
        {} as Record<string, Category[]>
      ),
    [categories]
  )

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
        <h1 className="text-3xl font-bold">Categories</h1>
        <Button asChild>
          <Link to="/categories/new">
            <Plus className="mr-2 h-4 w-4" />
            Add Category
          </Link>
        </Button>
      </div>

      {/* Search bar */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search categories..."
          className="pl-9"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <Tabs defaultValue="EXPENSE">
        <TabsList>
          {CATEGORY_TYPES.map((type) => (
            <TabsTrigger key={type.code} value={type.code}>
              {type.name} ({groupedByType[type.code]?.length ?? 0})
            </TabsTrigger>
          ))}
        </TabsList>

        {CATEGORY_TYPES.map((type) => (
          <TabsContent key={type.code} value={type.code} className="mt-4">
            <CategoryTabContent
              categories={groupedByType[type.code] ?? []}
              typeName={type.name}
              search={search}
              onDelete={setDeleteId}
            />
          </TabsContent>
        ))}
      </Tabs>

      <AlertDialog open={deleteId !== null} onOpenChange={(open) => !open && setDeleteId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Category</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this category? This action cannot be undone.
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
