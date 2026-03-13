import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useCategories } from '@/hooks/useCategories'
import { deleteCategory } from '@/api/categories.api'
import { CATEGORY_TYPES } from '@/constants/category-types'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
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

export function CategoriesListPage() {
  const { categories, isLoading, error, refresh } = useCategories()
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)

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

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  if (error) return <ErrorAlert message={error} />

  const groupedByType = CATEGORY_TYPES.reduce(
    (acc, type) => {
      acc[type.code] = categories.filter((c) => c.categoryTypeCode === type.code)
      return acc
    },
    {} as Record<string, Category[]>
  )

  const buildHierarchy = (cats: Category[]) => {
    const topLevel = cats.filter((c) => c.parentCategoryId == null)
    const children = cats.filter((c) => c.parentCategoryId != null)
    return topLevel.map((parent) => ({
      ...parent,
      children: children.filter((c) => c.parentCategoryId === parent.id),
    }))
  }

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

      <Tabs defaultValue="EXPENSE">
        <TabsList>
          {CATEGORY_TYPES.map((type) => (
            <TabsTrigger key={type.code} value={type.code}>
              {type.name} ({groupedByType[type.code]?.length ?? 0})
            </TabsTrigger>
          ))}
        </TabsList>

        {CATEGORY_TYPES.map((type) => (
          <TabsContent key={type.code} value={type.code} className="space-y-3">
            {(groupedByType[type.code]?.length ?? 0) === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <p className="text-muted-foreground">No {type.name.toLowerCase()} categories yet.</p>
                </CardContent>
              </Card>
            ) : (
              buildHierarchy(groupedByType[type.code]).map((parent) => (
                <Card key={parent.id}>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <div className="flex items-center gap-3">
                      {parent.color && (
                        <div
                          className="h-4 w-4 rounded-full"
                          style={{ backgroundColor: parent.color }}
                        />
                      )}
                      <CardTitle className="text-base">{parent.name}</CardTitle>
                      {parent.icon && <span className="text-sm">{parent.icon}</span>}
                      {parent.isSystem && <Badge variant="outline">System</Badge>}
                    </div>
                    {!parent.isSystem && (
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => setDeleteId(parent.id)}
                      >
                        <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                      </Button>
                    )}
                  </CardHeader>
                  {parent.children.length > 0 && (
                    <CardContent className="pt-0">
                      <div className="ml-6 space-y-1">
                        {parent.children.map((child) => (
                          <div
                            key={child.id}
                            className="flex items-center justify-between rounded-md px-3 py-1.5 text-sm hover:bg-accent/50"
                          >
                            <div className="flex items-center gap-2">
                              {child.color && (
                                <div
                                  className="h-3 w-3 rounded-full"
                                  style={{ backgroundColor: child.color }}
                                />
                              )}
                              <span>{child.name}</span>
                              {child.icon && <span className="text-xs">{child.icon}</span>}
                              {child.isSystem && (
                                <Badge variant="outline" className="text-xs">
                                  System
                                </Badge>
                              )}
                            </div>
                            {!child.isSystem && (
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-7 w-7"
                                onClick={() => setDeleteId(child.id)}
                              >
                                <Trash2 className="h-3 w-3 text-muted-foreground hover:text-destructive" />
                              </Button>
                            )}
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  )}
                </Card>
              ))
            )}
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
