import { useState, useRef, useEffect } from 'react'
import { Pencil, Plus, Check, X } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

interface Props {
  value: string | null
  currency: string
  onSave: (amount: number) => Promise<void>
}

export function EditableAmountCell({ value, currency, onSave }: Props) {
  const [editing, setEditing] = useState(false)
  const [inputValue, setInputValue] = useState('')
  const [saving, setSaving] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus()
    }
  }, [editing])

  const startEdit = () => {
    setInputValue(value ? parseFloat(value).toString() : '')
    setEditing(true)
  }

  const handleSave = async () => {
    const amount = parseFloat(inputValue)
    if (isNaN(amount) || amount < 0) return
    setSaving(true)
    try {
      await onSave(amount)
      setEditing(false)
    } finally {
      setSaving(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSave()
    if (e.key === 'Escape') setEditing(false)
  }

  if (editing) {
    return (
      <div className="flex items-center gap-1">
        <Input
          ref={inputRef}
          type="number"
          min="0"
          step="0.01"
          value={inputValue}
          onChange={e => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          className="h-7 w-28 text-sm"
          disabled={saving}
        />
        <Button size="icon" variant="ghost" className="h-7 w-7" onClick={handleSave} disabled={saving}>
          <Check className="h-3.5 w-3.5 text-green-600" />
        </Button>
        <Button size="icon" variant="ghost" className="h-7 w-7" onClick={() => setEditing(false)} disabled={saving}>
          <X className="h-3.5 w-3.5 text-muted-foreground" />
        </Button>
      </div>
    )
  }

  if (!value || value === '0' || value === '0.0000') {
    return (
      <Button variant="ghost" size="sm" className="h-7 text-muted-foreground gap-1 px-2" onClick={startEdit}>
        <Plus className="h-3.5 w-3.5" />
        Set budget
      </Button>
    )
  }

  return (
    <div className="group flex items-center gap-1 cursor-pointer" onClick={startEdit}>
      <span className="text-sm font-medium">
        {parseFloat(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        {currency && <span className="ml-1 text-xs text-muted-foreground">{currency}</span>}
      </span>
      <Pencil className="h-3 w-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
    </div>
  )
}
