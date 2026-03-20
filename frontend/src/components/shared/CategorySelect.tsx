import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
  SelectSeparator,
} from '@/components/ui/select'
import type { Category } from '@/types/category.types'

interface CategorySelectProps {
  categories: Category[]
  value?: string
  onValueChange: (value: string) => void
  placeholder?: string
  /** Render an "All Categories" option at the top */
  includeAll?: boolean
  className?: string
}

/**
 * Renders a category dropdown with parent → children hierarchy.
 * Parent categories are shown as group labels (non-selectable).
 * Leaf children are shown as indented selectable items.
 * Categories with no parent are grouped under their own name as a label.
 */
export function CategorySelect({
  categories,
  value,
  onValueChange,
  placeholder = 'Select category',
  includeAll = false,
  className,
}: CategorySelectProps) {
  // Separate parents and children
  const parentIds = new Set(
    categories.map((c) => c.parentCategoryId).filter((id): id is number => id != null)
  )

  // Root categories that have children
  const parents = categories.filter((c) => parentIds.has(c.id))
  // Leaf categories under a parent
  const children = categories.filter((c) => c.parentCategoryId != null)
  // Leaf categories with no parent at all (standalone roots)
  const standalone = categories.filter((c) => c.parentCategoryId == null && !parentIds.has(c.id))

  // Group children by parentCategoryId
  const byParent = new Map<number, Category[]>()
  for (const child of children) {
    const pid = child.parentCategoryId!
    if (!byParent.has(pid)) byParent.set(pid, [])
    byParent.get(pid)!.push(child)
  }

  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger className={className}>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {includeAll && (
          <>
            <SelectItem value="all">All Categories</SelectItem>
            {(parents.length > 0 || standalone.length > 0) && <SelectSeparator />}
          </>
        )}

        {/* Grouped: parent label + indented children */}
        {parents.map((parent) => {
          const kids = byParent.get(parent.id) ?? []
          if (kids.length === 0) return null
          return (
            <SelectGroup key={parent.id}>
              <SelectLabel>{parent.name}</SelectLabel>
              {kids.map((child) => (
                <SelectItem key={child.id} value={String(child.id)} className="pl-6">
                  {child.name}
                </SelectItem>
              ))}
            </SelectGroup>
          )
        })}

        {/* Standalone leaf categories (no parent) */}
        {standalone.length > 0 && (
          <SelectGroup>
            {parents.length > 0 && <SelectSeparator />}
            {standalone.map((c) => (
              <SelectItem key={c.id} value={String(c.id)}>
                {c.name}
              </SelectItem>
            ))}
          </SelectGroup>
        )}
      </SelectContent>
    </Select>
  )
}
