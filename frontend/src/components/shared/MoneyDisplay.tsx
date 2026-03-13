import { useContext } from 'react'
import { cn } from '@/lib/utils'
import { AuthContext } from '@/contexts/AuthContext'

interface MoneyDisplayProps {
  amount: string
  currency?: string
  colored?: boolean
  className?: string
}

export function MoneyDisplay({ amount, currency, colored = false, className }: MoneyDisplayProps) {
  const auth = useContext(AuthContext)
  const resolvedCurrency = currency ?? auth?.user?.preferredCurrency ?? 'USD'

  const numericAmount = parseFloat(amount)
  const formatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: resolvedCurrency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numericAmount)

  return (
    <span
      className={cn(
        'font-mono',
        colored && numericAmount > 0 && 'text-green-600 dark:text-green-400',
        colored && numericAmount < 0 && 'text-red-600 dark:text-red-400',
        className
      )}
    >
      {formatted}
    </span>
  )
}
