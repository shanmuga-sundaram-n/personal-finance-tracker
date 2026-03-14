import { cn } from '@/lib/utils'
import { MoneyDisplay } from './MoneyDisplay'

const TYPE_COLOR: Record<string, string> = {
  INCOME:       'text-green-600 dark:text-green-400',
  TRANSFER_IN:  'text-green-600 dark:text-green-400',
  EXPENSE:      'text-red-600 dark:text-red-400',
  TRANSFER_OUT: 'text-red-600 dark:text-red-400',
}

interface TransactionAmountProps {
  amount: string
  currency: string
  type: string
  className?: string
}

export function TransactionAmount({ amount, currency, type, className }: TransactionAmountProps) {
  return (
    <MoneyDisplay
      amount={amount}
      currency={currency}
      className={cn(TYPE_COLOR[type] ?? '', className)}
    />
  )
}
