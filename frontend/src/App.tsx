import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/contexts/AuthContext'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AppLayout } from '@/components/layout/AppLayout'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { AccountsListPage } from '@/pages/accounts/AccountsListPage'
import { CreateAccountPage } from '@/pages/accounts/CreateAccountPage'
import { AccountDetailPage } from '@/pages/accounts/AccountDetailPage'
import { ProfilePage } from '@/pages/settings/ProfilePage'
import { CategoriesListPage } from '@/pages/categories/CategoriesListPage'
import { CreateCategoryPage } from '@/pages/categories/CreateCategoryPage'
import { TransactionsListPage } from '@/pages/transactions/TransactionsListPage'
import { CreateTransactionPage } from '@/pages/transactions/CreateTransactionPage'
import { CreateTransferPage } from '@/pages/transactions/CreateTransferPage'
import { EditTransactionPage } from '@/pages/transactions/EditTransactionPage'
import { BudgetsListPage } from '@/pages/budgets/BudgetsListPage'
import { CreateBudgetPage } from '@/pages/budgets/CreateBudgetPage'
import { EditBudgetPage } from '@/pages/budgets/EditBudgetPage'
import { BudgetPlanPage } from '@/pages/budgets/BudgetPlanPage'
import { ReportsPage } from '@/pages/reports/ReportsPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/accounts" element={<AccountsListPage />} />
              <Route path="/accounts/new" element={<CreateAccountPage />} />
              <Route path="/accounts/:id" element={<AccountDetailPage />} />
              <Route path="/categories" element={<CategoriesListPage />} />
              <Route path="/categories/new" element={<CreateCategoryPage />} />
              <Route path="/transactions" element={<TransactionsListPage />} />
              <Route path="/transactions/new" element={<CreateTransactionPage />} />
              <Route path="/transactions/transfer" element={<CreateTransferPage />} />
              <Route path="/transactions/:id/edit" element={<EditTransactionPage />} />
              <Route path="/budgets" element={<BudgetsListPage />} />
              <Route path="/budgets/plan" element={<BudgetPlanPage />} />
              <Route path="/budgets/new" element={<CreateBudgetPage />} />
              <Route path="/budgets/:id/edit" element={<EditBudgetPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/settings/profile" element={<ProfilePage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
