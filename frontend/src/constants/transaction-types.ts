export const TRANSACTION_TYPES = [
  { code: 'INCOME', name: 'Income', color: 'text-green-600' },
  { code: 'EXPENSE', name: 'Expense', color: 'text-red-600' },
  { code: 'TRANSFER_IN', name: 'Transfer In', color: 'text-blue-600' },
  { code: 'TRANSFER_OUT', name: 'Transfer Out', color: 'text-orange-600' },
] as const

export const TRANSACTION_TYPE_OPTIONS = [
  { code: 'ALL', name: 'All Types' },
  { code: 'INCOME', name: 'Income' },
  { code: 'EXPENSE', name: 'Expense' },
  { code: 'TRANSFER_IN', name: 'Transfer In' },
  { code: 'TRANSFER_OUT', name: 'Transfer Out' },
] as const
