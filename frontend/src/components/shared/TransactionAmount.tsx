import { cn } from '@/lib/utils'
import { MoneyDisplay } from './MoneyDisplay'

const TYPE_COLOR: Record<string, string> = {
  INCOME:       'text-green-600 dark:text-green-400',
  TRANSFER_IN:  'text-green-600 dark:text-green-400',
  EXPENSE:      'text-red-600 dark:text-red-400',
  TRANSFER_OUT: 'text-red-600 dark:text-red-400',
}

const TYPE_SIGN: Record<string, string> = {
  INCOME:       '+',
  TRANSFER_IN:  '+',
  EXPENSE:      '-',
  TRANSFER_OUT: '-',
}

interface TransactionAmountProps {
  amount: string
  currency: string
  type: string
  className?: string
}

export function TransactionAmount({ amount, currency, type, className }: TransactionAmountProps) {
  const sign = TYPE_SIGN[type] ?? ''
  return (
    <MoneyDisplay
      amount={amount}
      currency={currency}
      sign={sign}
      className={cn(TYPE_COLOR[type] ?? '', className)}
    />
  )
}
