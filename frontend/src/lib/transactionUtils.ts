export function typeBadgeVariant(type: string): 'default' | 'destructive' | 'outline' | 'secondary' {
  switch (type) {
    case 'INCOME':      return 'default'
    case 'EXPENSE':     return 'destructive'
    case 'TRANSFER_IN': return 'outline'
    case 'TRANSFER_OUT':return 'secondary'
    default:            return 'outline'
  }
}

export function typeLabel(type: string): string {
  switch (type) {
    case 'INCOME':      return 'Income'
    case 'EXPENSE':     return 'Expense'
    case 'TRANSFER_IN': return 'Transfer In'
    case 'TRANSFER_OUT':return 'Transfer Out'
    default:            return type
  }
}

export function rowBorderClass(type: string): string {
  switch (type) {
    case 'INCOME':
    case 'TRANSFER_IN':  return 'border-l-4 border-green-500'
    case 'EXPENSE':
    case 'TRANSFER_OUT': return 'border-l-4 border-red-500'
    default:             return ''
  }
}
