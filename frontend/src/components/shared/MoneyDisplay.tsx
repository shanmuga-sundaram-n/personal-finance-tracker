import { useContext } from 'react'
import { cn } from '@/lib/utils'
import { AuthContext } from '@/contexts/AuthContext'

interface MoneyDisplayProps {
  amount: string
  currency?: string
  colored?: boolean
  className?: string
  /** Explicit sign prefix to prepend, e.g. '+' or '-' */
  sign?: string
  /** Strip trailing .00 when the amount is a whole number */
  trimDecimals?: boolean
}

export function MoneyDisplay({
  amount,
  currency,
  colored = false,
  className,
  sign,
  trimDecimals = false,
}: MoneyDisplayProps) {
  const auth = useContext(AuthContext)
  const resolvedCurrency = currency ?? auth?.user?.preferredCurrency ?? 'USD'

  const numericAmount = parseFloat(amount)
  const isWhole = Number.isInteger(numericAmount)

  const formatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: resolvedCurrency,
    minimumFractionDigits: trimDecimals && isWhole ? 0 : 2,
    maximumFractionDigits: 2,
  }).format(numericAmount)

  // Build a screen-reader-friendly label that makes sign and currency explicit
  const signLabel = sign
    ? sign === '+' ? 'positive' : sign === '-' ? 'negative' : ''
    : numericAmount > 0
    ? 'positive'
    : numericAmount < 0
    ? 'negative'
    : 'zero'
  const ariaLabel = `${signLabel} ${formatted}`

  return (
    <span
      aria-label={ariaLabel}
      className={cn(
        'font-mono',
        colored && numericAmount > 0 && 'text-green-600 dark:text-green-400',
        colored && numericAmount < 0 && 'text-red-600 dark:text-red-400',
        className
      )}
    >
      {sign}{formatted}
    </span>
  )
}
