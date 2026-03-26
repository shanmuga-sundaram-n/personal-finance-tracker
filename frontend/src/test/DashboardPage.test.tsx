import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { axe } from 'vitest-axe'
import { MemoryRouter } from 'react-router-dom'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import type { DashboardData } from '@/types/report.types'

// Mock the useDashboard hook so no real API calls are made
vi.mock('@/hooks/useDashboard', () => ({
  useDashboard: vi.fn(),
}))

// Mock AuthContext so MoneyDisplay can resolve currency without a provider
vi.mock('@/contexts/AuthContext', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/contexts/AuthContext')>()
  const { createContext } = await import('react')
  return {
    ...actual,
    AuthContext: createContext({ user: { preferredCurrency: 'USD' }, isLoading: false }),
  }
})

import { useDashboard } from '@/hooks/useDashboard'

const mockDashboard: DashboardData = {
  netWorth: '5000.00',
  totalAssets: '10000.00',
  totalLiabilities: '5000.00',
  currency: 'USD',
  currentMonthIncome: '3000.00',
  currentMonthExpense: '1500.00',
  netCashFlow: '1500.00',
  accountBalances: [
    { id: 1, name: 'Checking', balance: '5000.00', currency: 'USD', isLiability: false },
  ],
  topExpenseCategories: [
    { categoryId: 1, categoryName: 'Groceries', amount: '400.00' },
    { categoryId: 2, categoryName: 'Utilities', amount: '200.00' },
  ],
  recentTransactions: [
    {
      id: 1,
      description: 'Supermarket',
      amount: '50.00',
      currency: 'USD',
      type: 'EXPENSE',
      categoryName: 'Groceries',
      date: '2026-03-01',
    },
  ],
  budgetAlerts: [],
}

function renderDashboard() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(useDashboard).mockReturnValue({
      dashboard: mockDashboard,
      isLoading: false,
      error: null,
      refresh: vi.fn(),
    })
  })

  it('renders the page heading', () => {
    renderDashboard()
    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument()
  })

  it('renders the donut chart with role="img" and aria-label', () => {
    renderDashboard()
    const chart = screen.getByRole('img', { name: /donut chart showing top expense categories/i })
    expect(chart).toBeInTheDocument()
  })

  it('renders budget progress bars with aria attributes', () => {
    vi.mocked(useDashboard).mockReturnValue({
      dashboard: {
        ...mockDashboard,
        budgetAlerts: [
          {
            budgetId: 1,
            categoryName: 'Food',
            percentUsed: 90,
            amount: '500.00',
            spent: '450.00',
          },
        ],
      },
      isLoading: false,
      error: null,
      refresh: vi.fn(),
    })
    renderDashboard()
    const progressbar = screen.getByRole('progressbar', { name: /food budget/i })
    expect(progressbar).toHaveAttribute('aria-valuenow', '90')
    expect(progressbar).toHaveAttribute('aria-valuemin', '0')
    expect(progressbar).toHaveAttribute('aria-valuemax', '100')
  })

  it('passes axe accessibility scan', async () => {
    const { container } = renderDashboard()
    const results = await axe(container)
    expect(results).toHaveNoViolations()
  })
})
