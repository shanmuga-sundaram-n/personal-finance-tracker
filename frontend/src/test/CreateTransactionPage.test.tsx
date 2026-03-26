import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { axe } from 'vitest-axe'
import { MemoryRouter } from 'react-router-dom'
import { CreateTransactionPage } from '@/pages/transactions/CreateTransactionPage'

// Mock navigation
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => vi.fn() }
})

// Mock hooks — return empty lists so the form renders immediately
vi.mock('@/hooks/useAccounts', () => ({
  useAccounts: () => ({ accounts: [], isLoading: false, error: null }),
}))

vi.mock('@/hooks/useCategories', () => ({
  useCategories: () => ({ categories: [], isLoading: false, error: null }),
}))

// Mock the API so submitting does not hit the network
vi.mock('@/api/transactions.api', () => ({
  createTransaction: vi.fn().mockResolvedValue({}),
}))

// Suppress sonner toasts in tests
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

function renderForm() {
  return render(
    <MemoryRouter>
      <CreateTransactionPage />
    </MemoryRouter>
  )
}

describe('CreateTransactionPage — aria-describedby on inputs', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders amount input with no aria-describedby when there is no error', () => {
    renderForm()
    const amountInput = screen.getByLabelText(/amount/i)
    expect(amountInput).not.toHaveAttribute('aria-describedby')
  })

  it('shows error messages with role="alert" when submitted blank', async () => {
    const user = userEvent.setup()
    renderForm()

    // Submit the form with no data filled in
    const submitButton = screen.getByRole('button', { name: /create transaction/i })
    await user.click(submitButton)

    await waitFor(() => {
      const alerts = screen.getAllByRole('alert')
      expect(alerts.length).toBeGreaterThan(0)
    })
  })

  it('amount input gets aria-describedby="amount-error" after failed submit', async () => {
    const user = userEvent.setup()
    renderForm()

    const amountInput = screen.getByLabelText(/amount/i)
    // Clear the field then trigger validation
    await user.clear(amountInput)
    await user.click(screen.getByRole('button', { name: /create transaction/i }))

    await waitFor(() => {
      expect(amountInput).toHaveAttribute('aria-describedby', 'amount-error')
    })
  })

  it('amount input has aria-invalid="true" after failed submit', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.clear(screen.getByLabelText(/amount/i))
    await user.click(screen.getByRole('button', { name: /create transaction/i }))

    await waitFor(() => {
      expect(screen.getByLabelText(/amount/i)).toHaveAttribute('aria-invalid', 'true')
    })
  })

  it('date input has aria-describedby="transactionDate-error" after failed submit with cleared date', async () => {
    const user = userEvent.setup()
    renderForm()

    const dateInput = screen.getByLabelText(/date/i)
    await user.clear(dateInput)
    await user.click(screen.getByRole('button', { name: /create transaction/i }))

    await waitFor(() => {
      expect(dateInput).toHaveAttribute('aria-describedby', 'transactionDate-error')
    })
  })

  it('passes axe accessibility scan on initial render', async () => {
    const { container } = renderForm()
    const results = await axe(container)
    expect(results).toHaveNoViolations()
  })
})
