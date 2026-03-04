import { Link } from 'react-router-dom'
import { Plus, TrendingUp, TrendingDown, DollarSign } from 'lucide-react'
import { useAccounts, useNetWorth } from '@/hooks/useAccounts'
import { MoneyDisplay } from '@/components/shared/MoneyDisplay'
import { LoadingSpinner } from '@/components/shared/LoadingSpinner'
import { ErrorAlert } from '@/components/shared/ErrorAlert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

export function DashboardPage() {
  const { netWorth, isLoading: nwLoading, error: nwError } = useNetWorth()
  const { accounts, isLoading: accLoading, error: accError } = useAccounts()

  if (nwLoading || accLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <LoadingSpinner size="lg" />
      </div>
    )
  }

  const error = nwError || accError
  if (error) {
    return <ErrorAlert message={error} />
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <Button asChild>
          <Link to="/accounts/new">
            <Plus className="mr-2 h-4 w-4" />
            Add Account
          </Link>
        </Button>
      </div>

      {netWorth && (
        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Net Worth</CardTitle>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                <MoneyDisplay amount={netWorth.netWorth} currency={netWorth.currency} colored />
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Assets</CardTitle>
              <TrendingUp className="h-4 w-4 text-green-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                <MoneyDisplay amount={netWorth.totalAssets} currency={netWorth.currency} />
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Liabilities</CardTitle>
              <TrendingDown className="h-4 w-4 text-red-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                <MoneyDisplay amount={netWorth.totalLiabilities} currency={netWorth.currency} />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Accounts</CardTitle>
        </CardHeader>
        <CardContent>
          {accounts.length === 0 ? (
            <p className="py-4 text-center text-muted-foreground">
              No accounts yet.{' '}
              <Link to="/accounts/new" className="text-primary hover:underline">
                Create your first account
              </Link>
            </p>
          ) : (
            <div className="space-y-3">
              {accounts.map((account) => (
                <Link
                  key={account.id}
                  to={`/accounts/${account.id}`}
                  className="flex items-center justify-between rounded-md border p-3 transition-colors hover:bg-accent"
                >
                  <div className="flex items-center gap-3">
                    <div>
                      <p className="font-medium">{account.name}</p>
                      <Badge variant="secondary" className="mt-1">
                        {account.accountTypeName}
                      </Badge>
                    </div>
                  </div>
                  <MoneyDisplay
                    amount={account.currentBalance}
                    currency={account.currency}
                    colored
                    className="text-lg font-semibold"
                  />
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
