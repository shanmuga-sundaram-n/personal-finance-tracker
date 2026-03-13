export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER_IN' | 'TRANSFER_OUT'

export interface Transaction {
  id: number
  accountId: number
  accountName: string
  categoryId: number
  categoryName: string
  amount: string
  currency: string
  type: TransactionType
  transactionDate: string
  description: string | null
  merchantName: string | null
  referenceNumber: string | null
  transferPairId: number | null
  isRecurring: boolean
  isReconciled: boolean
  createdAt: string
}

export interface TransactionPage {
  content: Transaction[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateTransactionRequest {
  accountId: number
  categoryId: number
  amount: number
  currency: string
  type: 'INCOME' | 'EXPENSE'
  transactionDate: string
  description?: string
  merchantName?: string
  referenceNumber?: string
}

export interface CreateTransferRequest {
  fromAccountId: number
  toAccountId: number
  categoryId: number
  amount: number
  currency: string
  transactionDate: string
  description?: string
}

export interface UpdateTransactionRequest {
  categoryId: number
  amount: number
  currency: string
  transactionDate: string
  description?: string
  merchantName?: string
  referenceNumber?: string
}

export interface TransferResult {
  outboundId: number
  inboundId: number
}

export interface TransactionFilters {
  accountId?: number
  categoryId?: number
  type?: string
  from?: string
  to?: string
  page?: number
  size?: number
}
