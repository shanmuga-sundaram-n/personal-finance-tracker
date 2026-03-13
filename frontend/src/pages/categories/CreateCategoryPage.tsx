import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { createCategory } from '@/api/categories.api'
import { ApiClientError } from '@/api/client'
import { CATEGORY_TYPES } from '@/constants/category-types'
import { useCategories } from '@/hooks/useCategories'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ErrorAlert } from '@/components/shared/ErrorAlert'

const createCategorySchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  categoryTypeCode: z.string().min(1, 'Category type is required'),
  parentCategoryId: z.string().optional().or(z.literal('')),
  icon: z.string().max(50).optional().or(z.literal('')),
  color: z
    .string()
    .optional()
    .or(z.literal(''))
    .refine((v) => !v || /^#[0-9A-Fa-f]{6}$/.test(v), 'Must be a valid hex color (e.g. #FF5733)'),
})

type CreateCategoryForm = z.infer<typeof createCategorySchema>

export function CreateCategoryPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const { categories } = useCategories()

  const topLevelCategories = categories.filter((c) => c.parentCategoryId == null)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<CreateCategoryForm>({
    resolver: zodResolver(createCategorySchema),
    defaultValues: {
      color: '',
    },
  })

  const selectedType = watch('categoryTypeCode')

  const filteredParents = topLevelCategories.filter(
    (c) => c.categoryTypeCode === selectedType
  )

  const onSubmit = async (data: CreateCategoryForm) => {
    setError(null)
    try {
      const request = {
        name: data.name,
        categoryTypeCode: data.categoryTypeCode,
        parentCategoryId: data.parentCategoryId ? parseInt(data.parentCategoryId, 10) : undefined,
        icon: data.icon || undefined,
        color: data.color || undefined,
      }
      await createCategory(request)
      toast.success('Category created successfully')
      navigate('/categories')
    } catch (e) {
      if (e instanceof ApiClientError) {
        setError(e.apiError?.message ?? 'Failed to create category')
      } else {
        setError('An unexpected error occurred')
      }
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-3xl font-bold">Create Category</h1>

      <Card>
        <CardHeader>
          <CardTitle>Category Details</CardTitle>
        </CardHeader>
        <form onSubmit={handleSubmit(onSubmit)}>
          <CardContent className="space-y-4">
            {error && <ErrorAlert message={error} />}

            <div className="space-y-2">
              <Label htmlFor="name">Category Name</Label>
              <Input id="name" placeholder="e.g., Groceries" {...register('name')} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>

            <div className="space-y-2">
              <Label>Category Type</Label>
              <Select onValueChange={(v) => setValue('categoryTypeCode', v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select category type" />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORY_TYPES.map((type) => (
                    <SelectItem key={type.code} value={type.code}>
                      {type.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {errors.categoryTypeCode && (
                <p className="text-sm text-destructive">{errors.categoryTypeCode.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label>Parent Category (optional)</Label>
              <Select onValueChange={(v) => setValue('parentCategoryId', v === 'none' ? '' : v)}>
                <SelectTrigger>
                  <SelectValue placeholder="None (top-level)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None (top-level)</SelectItem>
                  {filteredParents.map((cat) => (
                    <SelectItem key={cat.id} value={String(cat.id)}>
                      {cat.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="icon">Icon (optional)</Label>
                <Input id="icon" placeholder="e.g., shopping-cart" {...register('icon')} />
                {errors.icon && <p className="text-sm text-destructive">{errors.icon.message}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="color">Color (optional)</Label>
                <Input id="color" type="color" {...register('color')} />
                {errors.color && <p className="text-sm text-destructive">{errors.color.message}</p>}
              </div>
            </div>

            <div className="flex gap-4 pt-4">
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Creating...' : 'Create Category'}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate('/categories')}>
                Cancel
              </Button>
            </div>
          </CardContent>
        </form>
      </Card>
    </div>
  )
}
