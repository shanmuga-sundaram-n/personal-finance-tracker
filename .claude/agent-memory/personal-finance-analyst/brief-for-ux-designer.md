# Coordination Brief for UX/UI Designer
**From**: personal-finance-analyst (Product Domain Owner)
**Date**: 2026-02-28
**Full domain reference**: `DOMAIN-OWNERSHIP.md` in this directory

This document describes what users need to accomplish, what data appears on each screen, and what error states are possible. The visual design, component choices, and layout decisions are yours to make. Do NOT invent features or data fields not listed here — surface additional ideas as proposals to the domain owner.

---

## 1. User Mental Model

Users think of their finances in this hierarchy:
1. **Accounts** — where their money lives (bank accounts, credit cards, cash)
2. **Transactions** — individual money movements in/out of accounts
3. **Categories** — labels that classify transactions (Groceries, Salary, etc.)
4. **Budgets** — spending targets per category per month
5. **Dashboard** — the at-a-glance summary of everything

Design should reflect this hierarchy in navigation. Accounts and Transactions are the core; budgets are a power feature.

---

## 2. Screen-by-Screen Specification

### 2.1 Registration Screen
**User goal**: Create an account quickly.

**Fields to collect**:
- First Name (required)
- Last Name (required)
- Username (required, 3-50 chars, letters/numbers/underscores)
- Email (required)
- Password (required)
- Confirm Password (required)

**Behaviour and states**:
- Real-time validation: show inline error as soon as user leaves a field
- Password strength indicator (visual feedback, not a blocker)
- Show/hide password toggle
- On submit: show loading state on button
- Success: redirect to Dashboard (no separate "check email" step in MVP)
- Errors to display:
  - Username already taken: "This username is not available. Try another."
  - Email already registered: "An account with this email already exists."
  - Password too short (min 8 chars): "Password must be at least 8 characters."
  - Passwords don't match: "Passwords do not match."

---

### 2.2 Login Screen
**User goal**: Access their account.

**Fields to collect**:
- Username or Email (single field accepts either)
- Password

**Behaviour and states**:
- On failed login: show generic message "Invalid username or password." (never reveal which field is wrong)
- After 5 failed attempts: show "Too many attempts. Please try again in 5 minutes."
- Remember me: checkbox (extends session; Phase 2)
- Show/hide password toggle
- Forgot password link (Phase 2 feature — show as disabled/greyed in MVP)
- Success: redirect to Dashboard

---

### 2.3 Dashboard Screen
**User goal**: Get an instant picture of their financial health.

**Data to display** (all for the authenticated user, current month context):
- **Net Worth panel**: Total Assets, Total Liabilities, Net Worth (assets - liabilities). Net Worth shown prominently.
- **Cash Flow panel** (this month): Income, Expenses, Net Cash Flow (income - expenses). Color-code: green if positive, red if negative.
- **Account list**: All active accounts grouped by type. Each row: account name, institution name, current_balance. Total at bottom of each group.
- **Top Expenses panel**: Top 5 expense categories this month. Each row: category icon + name, amount spent, percentage of total spending.
- **Recent Transactions**: Last 5 transactions. Each row: date, merchant/description, category name, account name, signed amount (green for income, red for expense).

**States to handle**:
- Empty state (new user with no accounts): Friendly prompt to add their first account. No empty tables.
- Loading state: skeleton placeholders for all panels.
- Accounts with zero balance: still show, clearly labelled "0.00".

**Navigation**: Dashboard is the home/landing page after login. All primary navigation accessible from here.

---

### 2.4 Accounts List Screen
**User goal**: See all accounts and total balance.

**Data to display**:
- Grouped by account type (ASSET accounts first, LIABILITY accounts second)
- Each account row: account name, institution name, account type badge, current_balance
- Color convention: positive asset balance = neutral/green; credit card/loan balance = amber (it is money owed)
- Total net worth at the top or bottom of the list
- "Add Account" button prominently placed

**Empty state**: "You haven't added any accounts yet. Start by adding your first account."

**States**: Loading skeleton, list with data, empty state.

---

### 2.5 Add / Edit Account Form
**User goal**: Connect a new financial account.

**Fields**:
- Account Type (required) — dropdown: Checking, Savings, Credit Card, Loan, Cash, Digital Wallet, Investment
- Account Name (required) — free text, e.g. "Chase Checking"
- Institution Name (optional) — e.g. "Chase Bank"
- Initial Balance (required) — numeric input
  - Help text for context: "Enter your current balance as shown on your statement."
  - For Credit Card and Loan: "Enter the amount you currently owe."
- Account Number Last 4 Digits (optional) — 4-digit input, display only
- Currency (required, defaults to user's preferred currency — Phase 2 to allow change)

**Behaviour**:
- Account type selection should visually communicate what the account is for
- Initial balance label changes based on account type: "Current Balance" for assets, "Current Amount Owed" for liabilities
- On save: navigate back to Account detail or Account list
- Errors: duplicate name ("You already have an account called [name]. Please use a different name."), invalid amount

---

### 2.6 Account Detail Screen
**User goal**: See balance and transaction history for one account.

**Data to display**:
- Account name, type, institution name, account_number_last4 (masked: ****1234)
- Current balance (large, prominent)
- Balance history chart (Phase 2)
- Transaction list filtered to this account:
  - Each row: date, merchant_name (or description), category name + icon, signed amount
  - Paginated: 30 per page, newest first
  - Filter options: date range picker, category dropdown, transaction type
- "Add Transaction" button (pre-fills account)
- Edit account and Deactivate account options (in header or overflow menu)

---

### 2.7 Transactions List Screen
**User goal**: Review all transactions across all accounts.

**Data to display**:
- Default view: all transactions, current month, newest first
- Each row: date, account name, merchant_name or description, category icon + name, signed amount (+ or -)
- Color: positive amounts green (income/transfer_in), negative amounts red (expense/transfer_out)
- Filters (always visible or in a filter panel): date range, account(s), category(s), transaction type, amount range
- Search bar: searches description and merchant_name
- Pagination: 30 per page
- Running total or summary line at top: "X transactions | Total income: $Y | Total expenses: $Z"

**Empty state**: "No transactions found. [Add your first transaction] or [clear filters]"

---

### 2.8 Add / Edit Transaction Form
**User goal**: Record a financial event.

**Fields**:
- Transaction Type (required) — radio or tab: Income / Expense / Transfer
  - When Transfer is selected, the form changes to the Transfer form (see 2.9)
- Account (required) — dropdown of user's active accounts
- Category (required) — searchable dropdown; filtered by transaction type (INCOME shows only INCOME categories, EXPENSE shows only EXPENSE categories)
- Amount (required) — numeric input, positive number only
- Date (required) — date picker, defaults to today
- Merchant / Payee Name (optional) — free text
- Description / Notes (optional) — text area

**Behaviour**:
- Category dropdown: show parent categories as group headers, child categories as options
- Amount: keyboard should open numeric keypad on mobile
- Submit creates transaction and updates account balance immediately (optimistic UI or reload)
- Errors to display:
  - Amount = 0: "Amount must be greater than zero."
  - Date too far in past (>10 years): "Transaction date cannot be more than 10 years ago."
  - Date too far in future (>30 days): "Transaction date cannot be more than 30 days in the future."
  - SAVINGS/CASH overdraft: "This transaction would leave your [Account Name] balance below zero, which is not allowed for this account type. Current balance: $X, transaction amount: $Y."

---

### 2.9 Transfer Form
**User goal**: Move money between own accounts.

**Fields**:
- From Account (required) — dropdown (exclude destination if already chosen)
- To Account (required) — dropdown (exclude source; cannot equal source)
- Amount (required)
- Date (required, defaults to today)
- Description (optional)

**Behaviour**:
- Show live preview: "[From Account] balance: $X -> $X - $amount" and "[To Account] balance: $Y -> $Y + $amount"
- Prevent selection of same account for both from/to
- Error: SAVINGS/CASH overdraft prevention same as regular expense

---

### 2.10 Categories Screen
**User goal**: View and manage transaction categories.

**Data to display**:
- Two tabs or sections: INCOME categories, EXPENSE categories
- System categories: listed with lock icon, not editable
- User custom categories: listed with edit/deactivate option
- Hierarchy: parent categories show child categories indented beneath them
- Each row: category icon (if set), category name, type badge
- "Add Custom Category" button

**Empty custom section state**: "You haven't created any custom categories yet."

---

### 2.11 Add / Edit Category Form
**User goal**: Create a custom category.

**Fields**:
- Category Type (required) — dropdown: Income or Expense (TRANSFER not user-selectable)
- Parent Category (optional) — dropdown of top-level categories of matching type
- Name (required)
- Icon (optional) — icon picker (emoji or icon library selection)
- Color (optional) — color picker

**Behaviour**:
- Parent category dropdown only shows top-level categories (those with no parent themselves)
- Parent category type must match selected category type (dynamically filter dropdown)
- Error: duplicate name at same level: "You already have a category called [name] under [parent]. Please use a different name."
- System categories cannot be selected for deletion

---

### 2.12 Budgets Screen
**User goal**: See spending limits and whether they are on track.

**Data to display**:
- Monthly budget by default (period toggle: weekly, monthly, quarterly, annual)
- Each budget row:
  - Category icon + name
  - Progress bar: spent / budget amount (color-coded: green < 75%, amber 75-99%, red >= 100%)
  - Text: "$spent of $budget" and "($remaining remaining)" or "$over over budget"
  - Status badge: ON TRACK / WARNING / OVER BUDGET
- Total row at bottom: sum of all budget amounts, sum of spent
- Unbudgeted spending section: categories with spending this period but no budget
- "Add Budget" button

**Empty state**: "No budgets set for this period. [Create your first budget]"

---

### 2.13 Add / Edit Budget Form
**User goal**: Set a spending limit for a category.

**Fields**:
- Category (required) — searchable dropdown, EXPENSE or INCOME categories only, no TRANSFER
- Period Type (required) — dropdown: Weekly, Monthly (default), Quarterly, Annually, Custom
- If Custom: Start Date and End Date pickers
- Budget Amount (required) — numeric input
- Alert Threshold % (optional) — slider or input, 1-100
  - Help text: "Get notified when you've used X% of your budget."

**Behaviour**:
- Error: duplicate budget for same category + period: "You already have an active budget for [Category] for [period type]. Edit the existing budget instead."
- On save: navigate back to Budgets list

---

## 3. Global UI Patterns

### 3.1 Navigation
- Primary navigation: Dashboard, Accounts, Transactions, Budgets, Categories
- Secondary/profile navigation: Profile Settings, Logout
- Persistent "Add Transaction" floating action button or shortcut (most frequent action)

### 3.2 Transaction Amount Display
- Income / Transfer In: show in green, prefixed with "+" (e.g., "+$1,500.00")
- Expense / Transfer Out: show in red, prefixed with "-" (e.g., "-$52.40")
- Credit card balances: show in amber with context label "Amount Owed"

### 3.3 Currency Display
- Always show currency symbol before amount (e.g., "$" for USD)
- Two decimal places for display
- Use locale-appropriate number formatting (comma thousands separator for USD)
- Never show "NaN", "null", or raw numeric without currency context

### 3.4 Empty States
Every list screen must have a designed empty state with:
- A descriptive illustration or icon
- A short explanation of what this section does
- A clear call to action button to add the first item

### 3.5 Loading States
- All data fetches show skeleton loaders (not spinners) for better perceived performance
- Forms show loading state on submit button while request is in flight
- Button must be disabled while request is in flight (prevent double-submit)

### 3.6 Error Handling
- Field-level errors: shown inline below the relevant input
- Form-level / API errors: shown as a banner at the top of the form
- Network errors: toast notification "Something went wrong. Please try again."
- Session expired: redirect to login with message "Your session has expired. Please log in again."

### 3.7 Confirmation Dialogs
Required before:
- Deactivating an account ("This will hide the account and all its transactions from new views. Your data is preserved. Are you sure?")
- Deleting a transaction ("This will permanently remove this transaction and adjust your account balance. Are you sure?")
- Deactivating a budget

---

## 4. Key User Workflows (Step by Step)

### Workflow A: New User First-Time Setup
1. Register -> land on Dashboard (empty state)
2. Prompted: "Let's add your first account to get started"
3. Add Account form -> submit -> land on Account Detail (empty transactions)
4. Prompted: "Add your first transaction"
5. Add Transaction form -> submit -> see transaction in list, see balance update
6. Navigate to Budgets -> set a monthly budget for Groceries

### Workflow B: Daily Transaction Entry
1. Open app -> Dashboard shows today's summary
2. Tap "Add Transaction" (floating button)
3. Select: Expense, Checking account, Groceries category, $45.00, today
4. Submit -> toast "Transaction added" -> return to previous screen
5. Dashboard/Account balance updates immediately

### Workflow C: Review Monthly Spending
1. Navigate to Budgets -> see current month budget progress
2. Notice "Dining Out" is at 95% (WARNING state)
3. Navigate to Transactions -> filter by "Dining Out" category, current month
4. Review the list -> identify specific transactions

### Workflow D: Account Balance Check
1. Navigate to Accounts
2. See all accounts with balances at a glance
3. Notice credit card shows $480 owed (liability)
4. Tap account -> see transaction history
5. Add payment transaction (Income/Payment category to reduce credit card balance)

---

## 5. Accessibility Requirements (Non-Negotiable)

- WCAG 2.1 AA minimum compliance
- All form inputs must have visible labels (not just placeholders)
- Color alone must not convey information (e.g., over-budget status needs both color AND text/icon)
- All interactive elements must be keyboard navigable
- Amount inputs must be clearly labelled (screen readers must read "Amount in USD dollars")
- Progress bars must have text equivalents (e.g., aria-valuenow, aria-valuemin, aria-valuemax, aria-label)
- Error messages must be programmatically associated with their input fields (aria-describedby)

---

## 6. Data That Must NOT Appear in the UI

- password_hash (never)
- Full account numbers (only last 4 digits allowed)
- Session tokens
- Internal IDs in URLs where possible (use slugs or opaque IDs)
- Other users' data — the UI should be built assuming every response is already user-scoped
