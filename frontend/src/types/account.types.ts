export interface Account {
  id: number
  name: string
  accountTypeCode: string
  accountTypeName: string
  currentBalance: string
  initialBalance: string
  currency: string
  institutionName: string | null
  accountNumberLast4: string | null
  isActive: boolean
  includeInNetWorth: boolean
  isLiability: boolean
  createdAt: string
}

export interface CreateAccountRequest {
  name: string
  accountTypeCode: string
  initialBalance: number
  currency: string
  institutionName?: string
  accountNumberLast4?: string
}

export interface UpdateAccountRequest {
  name: string
  institutionName?: string
}

export interface NetWorth {
  totalAssets: string
  totalLiabilities: string
  netWorth: string
  currency: string
}
