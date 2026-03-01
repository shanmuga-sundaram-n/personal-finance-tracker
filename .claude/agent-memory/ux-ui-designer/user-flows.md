# User Flows — Personal Finance Tracker
**Author**: UX/UI Designer
**Date**: 2026-02-28
**Reference**: brief-for-ux-designer.md Workflows A–D; DOMAIN-OWNERSHIP.md features F-001 through F-007

---

## Reading This Document

Each flow is documented as:
1. Step-by-step path (happy path)
2. Decision points (branching logic)
3. Error branches
4. System feedback at each step

Notation:
- [SCREEN] = a named screen the user sees
- {ACTION} = a user action (tap, type, click)
- <SYSTEM> = a system response or validation
- [DECISION] = a branching point with outcomes

---

## Flow 1: New User Registration and First-Time Setup

**Trigger**: User arrives at the app for the first time.
**End goal**: User has registered, added their first account, and recorded their first transaction.
**Corresponds to**: Workflow A from the brief.

### Happy Path

```
Step 1: Arrive at app
  [LOGIN SCREEN] shown by default (not authenticated)
  {Click "Create an account" link}

Step 2: Registration form
  [REGISTRATION SCREEN]
  {Fill: First Name, Last Name, Username, Email, Password, Confirm Password}

  [DECISION: Inline validation on blur]
    - Username format invalid -> error shown, user corrects
    - Email format invalid -> error shown, user corrects
    - Password < 8 chars -> strength bar shows "Weak", error on blur
    - Passwords don't match -> error shown instantly

  {Click "Create Account"}
  <SYSTEM: loading state — button shows "Creating account...">

  [DECISION: API response]
    - 409 Username taken -> inline error: "This username is not available."
    - 409 Email taken -> inline error: "An account with this email already exists."
    - 422 Validation -> inline field errors shown
    - 500 / Network -> error banner: "Something went wrong. Please try again."
    - 201 Created -> proceed

Step 3: First Dashboard (empty state)
  [DASHBOARD - EMPTY STATE]
  "Welcome to your Finance Dashboard"
  Prompt: "Let's add your first account to get started"
  {Click "Add Your First Account"}

Step 4: Add first account
  [ADD ACCOUNT FORM]
  {Select account type from dropdown, e.g. "Checking"}
  {Enter Account Name, e.g. "Chase Checking"}
  {Enter Institution Name (optional), e.g. "Chase Bank"}
  {Enter Initial Balance based on bank statement}

  [DECISION: label context]
    - CHECKING/SAVINGS/CASH/WALLET/INVESTMENT -> label: "Current Balance"
    - CREDIT_CARD/LOAN -> label: "Current Amount Owed"

  {Click "Save Account"}
  <SYSTEM: account created, redirect to account detail>

  [DECISION: API response]
    - 409 Duplicate name -> inline error: "You already have an account called..."
    - 422 Validation -> inline errors
    - 201 Created -> proceed

Step 5: Account detail (empty transactions)
  [ACCOUNT DETAIL - EMPTY STATE]
  "No transactions yet. Add your first transaction to see your activity here."
  {Click "Add Transaction"}

Step 6: First transaction
  [ADD TRANSACTION FORM]
  Transaction type defaults to "Expense"
  Account pre-filled from the account detail context
  {Select Category from dropdown}
  {Enter Amount}
  Date defaults to today
  {Click "Add Transaction"}
  <SYSTEM: success toast "Transaction added successfully.">
  <SYSTEM: redirect to account detail>
  <SYSTEM: account balance updates to reflect new transaction>

Step 7: View updated balance
  [ACCOUNT DETAIL]
  Balance reflects the recorded transaction
  Transaction appears in the list

  Optional continuation: navigate to Budgets
  {Click "Budgets" in sidebar/nav}

Step 8: Set first budget (optional but guided)
  [BUDGETS - EMPTY STATE]
  {Click "Create Your First Budget"}
  [ADD BUDGET FORM]
  {Select Category: e.g. Groceries}
  {Period Type: Monthly (default)}
  {Budget Amount: e.g. $500}
  {Click "Save Budget"}
  <SYSTEM: redirect to Budgets list showing new budget>
```

### Flow Notes

- Session is created on registration; user is immediately logged in — no email confirmation in MVP.
- The empty state on Dashboard is the key onboarding entry point. If the user closes or navigates away, they can always return to add an account via the Accounts page.
- Do not show tooltips or coach marks during this flow. The empty states with clear CTAs are the onboarding mechanism.

---

## Flow 2: Daily Transaction Entry

**Trigger**: User wants to log a purchase they just made.
**End goal**: Transaction recorded, balance updated, user returned to their context.
**Corresponds to**: Workflow B from the brief.

### Happy Path

```
Step 1: Open app
  [DASHBOARD]
  Today's cash flow shows in the Cash Flow panel.

Step 2: Initiate transaction entry
  [DECISION: entry point]
    a) {Click FAB "+" button} (floating, always visible)
    b) {Click "+ Add Transaction" button in toolbar}
    c) {Click "Add Transaction" in Account Detail}

  All lead to -> [ADD TRANSACTION FORM]

Step 3: Fill in transaction details
  Transaction type: Expense (most common; default to Expense)
  {Select Account from dropdown}
  {Select Category using search or browse}
  {Enter Amount — numeric keypad opens on mobile}
  Date: auto-filled as today; user can change
  Merchant name: optional
  {Click "Add Transaction"}

Step 4: Validation
  [DECISION: client-side validation first]
    - Amount = 0 -> "Amount must be greater than zero."
    - Date out of range -> specific date error message
    - Missing required fields -> field-level errors

  [DECISION: API response]
    - 422 Insufficient balance (SAVINGS/CASH) ->
      Error shown below account field with current balance and transaction amount context
      User corrects: choose different account OR adjust amount
    - 422 Other validation -> field errors
    - 201 Created -> proceed

Step 5: Success feedback
  <SYSTEM: success toast slides in — "Transaction added">
  <SYSTEM: navigate back to previous screen (dashboard or account detail)>
  <SYSTEM: dashboard balance / cash flow updates>
```

### Micro-interaction Notes

- Amount field: receives focus automatically when the form opens on mobile
- Numeric keypad: `inputmode="decimal"` ensures numeric keypad on iOS and Android
- Category field: type-ahead search from the first character. Most common categories surface quickly.
- FAB button: 56x56px on mobile (exceeds 44px minimum touch target). Fixed position above bottom nav.

### Error Branch: SAVINGS/CASH Overdraft

```
User enters $500 transfer from Savings with $300 balance
<SYSTEM: API returns 422 InsufficientBalanceException>
[Error shown]:
  "This transaction would leave your Wells Fargo Savings balance below zero,
   which is not allowed for savings accounts.
   Current balance: $300.00, transaction amount: $500.00."

[OPTIONS for user]:
  - Change Account to a checking/credit account
  - Reduce amount to $300.00 or less
  - Cancel
```

---

## Flow 3: Monthly Budget Review

**Trigger**: User wants to see how their spending compares to their budget this month.
**End goal**: User understands their budget status and can drill into overspent categories.
**Corresponds to**: Workflow C from the brief.

### Happy Path

```
Step 1: Navigate to Budgets
  {Click "Budgets" in sidebar/bottom nav}
  [BUDGETS LIST]
  Period defaults to "Monthly" showing current month.

Step 2: Scan budget status
  User sees progress bars and status badges at a glance.

  [DECISION: what they see]
    - ON TRACK (green, < 75%): no action needed
    - WARNING (amber, 75-99%): budget row highlighted
    - OVER BUDGET (red, >= 100%): row highlighted, amount over shown in red

Step 3: Drill into a specific category
  {Click on "Dining Out" row (WARNING state)}
  [DECISION: navigation behavior]
    - Clicking budget row navigates to:
      [TRANSACTIONS LIST filtered by that category + current month]

Step 4: Review transactions for the category
  [TRANSACTIONS LIST]
  Filtered to: Category = "Dining Out", Date = February 2026
  Summary bar: "8 transactions | Total: $95.00"
  User reviews individual transactions.

Step 5: Return to budgets
  {Click back or "Budgets" in nav}
  [BUDGETS LIST] — state preserved

Optional: Create a budget for unbudgeted spending
  User notices "Entertainment $50.00 (no budget set)" in unbudgeted section
  {Click "Set Budget" on that row}
  -> [ADD BUDGET FORM] with Entertainment pre-filled as category
```

### Period Toggle Flow

```
User wants to see quarterly spending
{Click "Quarterly" in period toggle}
<SYSTEM: reload budgets for current quarter Q1 2026>
Period label updates to "Q1 2026 (Jan 1 – Mar 31)"
Budget rows update with new spent amounts
```

---

## Flow 4: Transfer Between Accounts

**Trigger**: User wants to move money from checking to savings (or pay a credit card).
**End goal**: Both accounts show updated balances. Transfer appears in both accounts' transaction history.
**Corresponds to**: Workflow D (partial) from the brief.

### Happy Path

```
Step 1: Initiate transfer
  [DECISION: entry point]
    a) {Click FAB "+"} -> [ADD TRANSACTION FORM] -> {Click "Transfer" tab}
    b) {Navigate to Accounts} -> {Click "Add Transaction" on source account} -> {Click Transfer tab}

  [TRANSFER FORM]

Step 2: Fill in transfer details
  {Select From Account, e.g. "Chase Checking - $2,450.00"}
  {Select To Account, e.g. "Wells Fargo Savings - $8,000.00"}
    <SYSTEM: To Account dropdown excludes the selected From Account>
  {Enter Amount: $500.00}

  <SYSTEM: Live preview updates>
    "Chase Checking: $2,450.00 -> $1,950.00"
    "Wells Fargo Savings: $8,000.00 -> $8,500.00"

  Date defaults to today
  Description: optional

  {Click "Transfer Funds"}

Step 3: Validation
  [DECISION: client-side]
    - Same account for both -> "Source and destination must be different."
    - Amount = 0 -> "Please enter a transfer amount."
    - SAVINGS/CASH from-account would go negative -> preview shows red, warning text

  [DECISION: API response]
    - 422 Insufficient balance -> error message with balances
    - 201 Created -> proceed

Step 4: Success
  <SYSTEM: toast "Transfer completed.">
  <SYSTEM: navigate to Accounts list or Dashboard>
  <SYSTEM: both account balances update immediately>
```

### Account-Level Transfer Entry

```
User is in [ACCOUNT DETAIL - Chase Checking]
{Click "Add Transaction"}
[ADD TRANSACTION FORM - pre-filled with Chase Checking as Account]
{Click "Transfer" tab}
  <SYSTEM: "From Account" pre-filled with Chase Checking>
  <SYSTEM: "To Account" dropdown shows all accounts EXCEPT Chase Checking>
[Complete transfer as per happy path above]
```

---

## Flow 5: Account Balance Check and Credit Card Payment

**Trigger**: User notices credit card balance and wants to record a payment.
**End goal**: Credit card balance reduced, payment recorded as transaction.

```
Step 1: Check account balances
  [ACCOUNTS LIST]
  "Visa Credit Card  Credit Card  $3,000.00 owed"
  Amber color, "(Amount Owed)" label visible.

Step 2: Drill into credit card account
  {Click on Visa Credit Card row}
  [ACCOUNT DETAIL - Visa Credit Card]
  Balance: $3,000.00 (Amount Owed) — displayed in amber

Step 3: Add payment transaction
  {Click "+ Add Transaction"}
  [ADD TRANSACTION FORM]
  Account: Visa Credit Card (pre-filled)
  Transaction Type: Income (a payment to a credit card is income that reduces debt)
  Category: select "Account Transfer" or relevant INCOME/PAYMENT category
  Amount: $500.00
  {Click "Add Transaction"}

Step 4: Success
  <SYSTEM: balance on Visa Credit Card updates to $2,500.00>
  <SYSTEM: both Dashboard and Account Detail reflect new balance>

ALTERNATIVE: Use Transfer instead
  A credit card payment can also be modeled as a Transfer (from Checking to Credit Card)
  This is the semantically correct approach — it records both the outflow from Checking
  and the reduction in credit card debt in one atomic operation.

  {From Account: Chase Checking}
  {To Account: Visa Credit Card}
  {Amount: $500.00}
  Result: Checking -$500, Credit Card "owed" -$500 (debt reduced)
```

---

## Flow 6: Category Management (Add Custom Category)

**Trigger**: User wants to track "Side Business" expenses separately.
**End goal**: Custom category created and available for use in transactions.

### Happy Path

```
Step 1: Navigate to Categories
  {Click "Categories" in sidebar/nav}
  [CATEGORIES - Expense tab shown by default]

Step 2: Initiate new category
  {Click "+ Add Custom Category"}
  [ADD CATEGORY FORM]

Step 3: Configure category
  {Category Type: Expense (default)}
  {Parent Category: None (top-level) OR select "Shopping" as parent}
  {Name: "Side Business"}
  {Icon: optional — open picker, select briefcase icon}
  {Color: optional — pick purple swatch}

Step 4: Save
  {Click "Save Category"}

  [DECISION: API response]
    - 409 Duplicate name -> "You already have a category called 'Side Business'..."
    - 201 Created -> redirect to Categories screen

Step 5: New category available
  [CATEGORIES]
  "YOUR CUSTOM CATEGORIES" section shows:
  [briefcase] Side Business  [Edit]  [Deactivate]

  Category is now available in all transaction and budget category dropdowns.
```

### Edit Category Flow

```
{Click [Edit] on existing custom category}
[EDIT CATEGORY FORM]
  Name field pre-filled
  Icon and color pre-filled
  Category Type and Parent: read-only (cannot change type after creation)
{Update Name, Icon, or Color}
{Click "Save Category"}
<SYSTEM: category updated across all past and future references>
```

### Deactivate Category Flow

```
{Click [Deactivate] on custom category}
[CONFIRMATION DIALOG]
  "Deactivate 'Side Business'?
   This will hide the category from future transaction entry.
   Transactions already categorized as 'Side Business' will retain their category."
  [ Cancel ]  [ Deactivate ]

{Click "Deactivate"}
<SYSTEM: category.isActive = false>
<SYSTEM: category removed from transaction and budget dropdowns>
<SYSTEM: existing transactions unaffected — still show the category name>
```

---

## Flow 7: Account Reconciliation (Phase 2 Feature, Designed for Awareness)

**Note**: The reconciliation feature (F-011) is Phase 2. This flow documents the intended UX for when it is built, so the design system and navigation can accommodate it without restructuring.

### Intended Flow

```
Step 1: Open Account Detail for a checking account
  {Click [...more] -> "Reconcile Account"}
  [RECONCILIATION VIEW]

Step 2: Reconciliation mode
  Reconciliation view shows two columns:
  - Left: unreconciled transactions (is_reconciled = false)
  - Right: reconciled transactions (is_reconciled = true)

  Running "reconciled balance" shown at top.
  User enters their bank statement ending balance.
  System highlights discrepancy if any.

Step 3: Mark transactions reconciled
  {Click transaction row to mark as reconciled}
  Transaction moves from left to right column.
  Reconciled balance updates.

  {Click "Reconcile All Up To [Date]"} for bulk reconciliation.

Step 4: Match found
  When reconciled balance = bank statement balance:
  <SYSTEM: confirmation "Account reconciled! Balances match.">
  <SYSTEM: all selected transactions marked is_reconciled = true>
```

---

## Error Flow Library

### Session Expiry (Universal)

```
<SYSTEM: any API call returns 401>
<SYSTEM: clear local session state>
[REDIRECT to LOGIN SCREEN]
Toast / banner: "Your session has expired. Please sign in again."
[LOGIN SCREEN]
After successful login -> redirect to the page the user was on (store intended URL)
```

### Network Error (Universal)

```
<SYSTEM: fetch fails with network error (no response)>
Toast: "Unable to connect. Please check your internet connection and try again."
[Do NOT navigate away. Keep user on current screen with data intact.]
```

### 403 Forbidden (Unexpected)

```
<SYSTEM: API returns 403>
Toast: "You don't have permission to do that."
[Log the error internally. User sees only the toast.]
```

### 500 Server Error

```
<SYSTEM: API returns 500>
Toast: "Something went wrong on our end. Please try again in a moment."
[Keep user on current screen. Do not navigate away.]
```

---

## Navigation State Rules

1. **Back navigation always returns to the logical previous screen** — not necessarily the browser history back. For example, after saving a transaction from Account Detail, return to Account Detail (not the browser's previous entry which might be the form itself).

2. **Form state on navigation away**: if user has unsaved changes in a form and navigates away (e.g., taps a nav item), show a confirmation: "Leave this page? Your unsaved changes will be lost." with "Stay" and "Leave" options.

3. **Filters are preserved within a session** but reset on full app restart. If the user filters the Transactions list by "Dining Out" and navigates to a budget, coming back to Transactions should restore the filter.

4. **Scroll position preservation**: returning to a list after viewing a detail item should restore scroll position. Use React Query's cache or equivalent with scroll restoration in React Router.
