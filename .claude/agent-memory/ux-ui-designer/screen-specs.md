# Screen Specifications — Personal Finance Tracker
**Author**: UX/UI Designer
**Date**: 2026-02-28
**Reference**: design-system.md for all tokens; brief-for-ux-designer.md for data requirements

---

## Reading This Document

Each screen spec contains:
- ASCII wireframe showing layout (desktop first, then mobile differences)
- Exact data fields from the API
- All interaction states
- Accessibility requirements
- Empty, loading, and error states

Wireframe legend:
```
[ ]  = button
[__] = text input / field
( )  = radio button / option
[v]  = dropdown / select
|||  = divider / separator
###  = skeleton loader placeholder
---  = horizontal rule / separator
```

---

## Screen 1: Registration

**Route**: `/register`
**Auth**: Public

### Desktop Wireframe (>= 1024px)

```
+----------------------------------------------------------+
|  [logo] Personal Finance Tracker                         |
+----------------------------------------------------------+
|                                                          |
|         +----------------------------------+             |
|         |  Create your account            |             |
|         |  Already have one? [Sign in]    |             |
|         |----------------------------------|             |
|         |  First Name *                   |             |
|         |  [________________________]     |             |
|         |                                 |             |
|         |  Last Name *                    |             |
|         |  [________________________]     |             |
|         |                                 |             |
|         |  Username *                     |             |
|         |  [________________________]     |             |
|         |  Letters, numbers, underscores  |             |
|         |                                 |             |
|         |  Email address *                |             |
|         |  [________________________]     |             |
|         |                                 |             |
|         |  Password *                     |             |
|         |  [______________________] [eye] |             |
|         |  [====----] Strength: Fair      |             |
|         |                                 |             |
|         |  Confirm Password *             |             |
|         |  [______________________] [eye] |             |
|         |                                 |             |
|         |  [ Create Account (loading...) ]|             |
|         +----------------------------------+             |
|                                                          |
+----------------------------------------------------------+
```

### Mobile Wireframe (< 768px)

Full-width card, same field order, single column. Logo above card. Keyboard-aware scroll — form scrolls up when keyboard opens.

### Data Fields

| Field | Validation | API Field |
|---|---|---|
| First Name | Required, max 100 chars | first_name |
| Last Name | Required, max 100 chars | last_name |
| Username | Required, 3-50 chars, `^[a-z0-9_]+$` | username |
| Email | Required, valid email format, max 254 chars | email |
| Password | Required, min 8 chars, max 72 chars | password |
| Confirm Password | Must match Password | (client-only) |

### Interaction States

**Idle**: All fields empty. Button enabled (do not disable before attempt — allow user to discover validation).

**Field focus**: Field border turns --color-brand-500. Helper text appears below field.

**Inline validation (on blur)**:
- Username: `^[a-z0-9_]{3,50}$` — if fails: "Username must be 3–50 characters. Only lowercase letters, numbers, and underscores."
- Email: basic format check — "Please enter a valid email address."
- Password strength indicator: 4-segment bar
  - 1 segment (red): < 8 chars or only one character class
  - 2 segments (amber): 8+ chars, 2 character classes
  - 3 segments (green): 8+ chars, 3 character classes
  - 4 segments (green): 12+ chars, 3+ character classes, includes special char
  - Labels: "Weak", "Fair", "Good", "Strong"
- Confirm Password: check on blur and on password field change

**Submit (loading)**: Button label changes to "Creating account..." with spinner icon. Button disabled. All fields remain editable (allow correction during flight).

**API error — duplicate username**:
- Banner at top of form: (do NOT use — show inline)
- Inline below Username field: "This username is not available. Try another."
- Field border: --color-expense-600

**API error — duplicate email**:
- Inline below Email field: "An account with this email already exists."

**API error — network/500**:
- Banner at top of form: "Something went wrong. Please try again." (red banner, dismissible)

**Success**: Redirect to `/dashboard`. No intermediate confirmation screen.

### Accessibility

- `<form>` element with `aria-label="Create account"`
- All inputs have `<label for="...">` visible above field (not as placeholder)
- Error messages use `aria-describedby` linking input to error `<p id="...error">`
- `aria-invalid="true"` on fields with errors
- Password strength bar: `role="meter" aria-valuenow={strength} aria-valuemin="0" aria-valuemax="4" aria-label="Password strength"`
- Eye toggle button: `aria-label="Show password"` / `"Hide password"` toggled on state change
- Tab order: First Name > Last Name > Username > Email > Password > Confirm Password > Submit
- Submit button: `type="submit"` inside form

### Focus Order

1. First Name
2. Last Name
3. Username
4. Email
5. Password
6. Password show/hide toggle
7. Confirm Password
8. Confirm Password show/hide toggle
9. Create Account button

---

## Screen 2: Login

**Route**: `/login`
**Auth**: Public

### Desktop Wireframe

```
+----------------------------------------------------------+
|  [logo] Personal Finance Tracker                         |
+----------------------------------------------------------+
|                                                          |
|         +----------------------------------+             |
|         |  Welcome back                   |             |
|         |  New here? [Create an account]  |             |
|         |----------------------------------|             |
|         |  Username or Email *            |             |
|         |  [________________________]     |             |
|         |                                 |             |
|         |  Password *                     |             |
|         |  [______________________] [eye] |             |
|         |                                 |             |
|         |  [Forgot password? — disabled]  |             |
|         |                                 |             |
|         |  [ Sign In ]                    |             |
|         +----------------------------------+             |
|                                                          |
+----------------------------------------------------------+
```

### Data Fields

| Field | Notes |
|---|---|
| Username or Email | Single field; backend accepts either |
| Password | Show/hide toggle |

### Interaction States

**Failed login (< 5 attempts)**:
- No field-level highlighting (never reveal which field is wrong per domain security rule)
- Banner inside form card: "Invalid username or password."
- Clear banner on next keystroke in either field

**Failed login (>= 5 attempts in 5 minutes)**:
- Banner: "Too many sign-in attempts. Please try again in 5 minutes."
- Both fields disabled. Submit button disabled.
- Display countdown timer if feasible (nice-to-have Phase 2).

**Loading**: Button "Signing in..." with spinner. Fields disabled.

**Forgot password link**: Rendered as a `<button>` styled as a link with `aria-disabled="true"` and `title="Password reset is not available in this version"`. Tooltip explains it is coming soon. Do NOT render as a dead `<a href="#">`.

**Success**: Redirect to `/dashboard`.

### Accessibility

- `<form aria-label="Sign in">`
- `aria-describedby` for error banner linked from form element
- Error banner has `role="alert"` for screen reader announcement
- Autocomplete: `username` on Username/Email field, `current-password` on Password field

---

## Screen 3: Dashboard

**Route**: `/dashboard`
**Auth**: Required

### Desktop Wireframe (>= 1024px)

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Dashboard          [+ Add Transaction]          |
|          |--------------------------------------------------|
| Dashboard|  February 2026                                   |
| Accounts |  +-------------------+  +--------------------+  |
| Transact |  | NET WORTH         |  | CASH FLOW          |  |
| Budgets  |  | $12,450.00        |  | Income  +$3,200.00 |  |
| Categor. |  | Assets  $15,450   |  | Expense -$1,850.00 |  |
|          |  | Liabil. $3,000    |  | Net     +$1,350.00 |  |
|----------|  +-------------------+  +--------------------+  |
| [Avatar] |                                                  |
| Settings |  ACCOUNTS                                        |
| Logout   |  +----------------------------------------------+|
+----------+  | ASSET ACCOUNTS                               ||
              |  Chase Checking    Checking  $2,450.00       ||
              |  Wells Fargo Save  Savings   $8,000.00       ||
              |  PayPal            Wallet    $1,000.00       ||
              |                    Total    $11,450.00       ||
              |  LIABILITY ACCOUNTS                          ||
              |  Visa Credit Card  Credit    $3,000.00 owed  ||
              |                    Total     $3,000.00       ||
              +----------------------------------------------+|
                                                              |
              TOP EXPENSES THIS MONTH                         |
              +----------------------------------------------+|
              | Food / Groceries         $420.00   (22.7%)   ||
              | Housing / Rent         $1,200.00   (64.9%)   ||
              | Transportation           $180.00    (9.7%)   ||
              | Entertainment             $50.00    (2.7%)   ||
              +----------------------------------------------+|
                                                              |
              RECENT TRANSACTIONS (last 5)                    |
              +----------------------------------------------+|
              | Feb 28  Amazon          Shopping   -$89.00   ||
              | Feb 27  Salary (Chase)  Salary   +$3200.00   ||
              | Feb 26  Uber Eats       Food        -$28.50  ||
              | Feb 25  Electric Bill   Utilities  -$120.00  ||
              | Feb 24  Coffee Shop     Food         -$6.50  ||
              |                            [View all]        ||
              +----------------------------------------------+|
```

### Mobile Wireframe (< 768px)

```
+----------------------------------+
| [hamburger] Dashboard  [+ Add]   |
|----------------------------------|
| February 2026                    |
| NET WORTH                        |
| $12,450.00                       |
| Assets $15,450 | Liab. $3,000    |
|----------------------------------|
| CASH FLOW (this month)           |
| Income   +$3,200.00              |
| Expenses -$1,850.00              |
| Net Flow +$1,350.00              |
|----------------------------------|
| ACCOUNTS          [View all >]   |
| Chase Checking         $2,450.00 |
| Visa Credit Card  $3,000.00 owed |
|----------------------------------|
| TOP EXPENSES      [View all >]   |
| Groceries $420 |||||||||||  22%  |
| Rent   $1,200 ||||||||||||||||||  |
|----------------------------------|
| RECENT            [View all >]   |
| Feb 28  Amazon        -$89.00    |
| Feb 27  Salary      +$3,200.00   |
| Feb 26  Uber Eats     -$28.50    |
|                                  |
| [Home] [Accounts] [+] [Budget] [More] |
+----------------------------------+
```

### Data Displayed

**Net Worth Panel**:
- `totalAssets` (from `NetWorthResponse`) — labeled "Assets"
- `totalLiabilities` — labeled "Liabilities"
- `netWorth` — hero figure, 36px bold. Color: green if > 0, red if < 0

**Cash Flow Panel** (current month):
- `monthlyIncome` — green, prefixed "+"
- `monthlyExpenses` — red, prefixed "-"
- `netCashFlow` — color-coded. Label: "Net Flow"

**Accounts section**:
- Grouped: ASSET accounts first, LIABILITY accounts second
- Per row: name, accountTypeName badge, currentBalance
- Credit/Loan balances: amber color, "(Amount Owed)" suffix
- Group subtotal row

**Top Expenses**:
- Up to 5 rows: category icon, category name, amount (red), percentage of total expense
- Mini progress bar per row showing percentage visually

**Recent Transactions** (last 5):
- date (formatted "Feb 28"), merchantName or description, categoryName, signedAmount (+ green / - red)
- "View all" link to `/transactions`

### Empty State (new user, no accounts)

```
+----------------------------------------------+
|                                              |
|         [Wallet icon — 48px]                 |
|                                              |
|    Welcome to your Finance Dashboard         |
|                                              |
|    Track all your accounts, spending,        |
|    and budgets in one place.                 |
|                                              |
|         [ Add Your First Account ]           |
|                                              |
+----------------------------------------------+
```

No data panels shown until at least one account exists. The "Add Your First Account" button navigates to `/accounts/new`.

### Loading State

All panels replaced with skeleton cards:
- Net Worth panel: 2 skeleton lines (40px height, 200px width; 24px height, 120px width)
- Cash Flow panel: 3 skeleton lines
- Accounts: 3 skeleton rows (full width, 20px height)
- Top Expenses: 5 skeleton rows
- Recent Transactions: 5 skeleton rows

Skeleton animation: shimmer (background: linear-gradient(90deg, #E5E7EB 25%, #F3F4F6 50%, #E5E7EB 75%)) cycling left to right.

### Accessibility

- Page `<h1>` = "Dashboard"
- Net worth figure: `aria-label="Net worth: $12,450.00"`
- Cash flow amounts: `aria-label="Monthly income: $3,200.00"` etc.
- Account list: `<table>` or `role="list"` with proper headers
- Color amounts: always paired with "+" / "-" prefix text, not color alone
- "View all" links: descriptive, not generic "click here"

---

## Screen 4: Accounts List

**Route**: `/accounts`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Accounts                  [ + Add Account ]    |
|          |--------------------------------------------------|
|          |  NET WORTH: $12,450.00                           |
|          |                                                  |
|          |  ASSET ACCOUNTS                                  |
|          |  +--------------------------------------------+ |
|          |  | Name           Type       Balance          | |
|          |  |--------------------------------------------|  |
|          |  | Chase Checking  Checking  $2,450.00         | |
|          |  | Wells Savings   Savings   $8,000.00         | |
|          |  | PayPal Wallet   Wallet    $1,000.00         | |
|          |  |--------------------------------------------|  |
|          |  | Total Assets              $11,450.00        | |
|          |  +--------------------------------------------+ |
|          |                                                  |
|          |  LIABILITY ACCOUNTS                              |
|          |  +--------------------------------------------+ |
|          |  | Visa Credit    Credit Card  $3,000.00 owed | |
|          |  | Car Loan       Loan         $8,200.00 owed | |
|          |  |--------------------------------------------|  |
|          |  | Total Liabilities          $11,200.00       | |
|          |  +--------------------------------------------+ |
+----------+--------------------------------------------------+
```

### Mobile Wireframe

Single column card list. Each account = full-width card. Group headers as section labels.

### Data Per Account Row

- `name`
- `accountTypeName` — displayed as a badge (rounded pill)
- `institutionName` — secondary text (small, gray) below name
- `currentBalance` — right-aligned, large
  - Asset accounts: neutral/dark color
  - Liability accounts: amber, "(Amount Owed)" subtitle
- Tap/click row → navigates to `/accounts/{id}`

### Empty State

```
[Wallet icon]
You haven't added any accounts yet.
Track all your bank accounts, credit cards, and cash.
[ Add Your First Account ]
```

### Account Type Badge Colors

| Type | Badge Color |
|---|---|
| CHECKING | --color-brand-100 / --color-brand-700 |
| SAVINGS | --color-income-100 / --color-income-700 |
| CREDIT_CARD | --color-liability-100 / --color-liability-700 |
| INVESTMENT | --color-transfer-bg / --color-transfer-600 |
| LOAN | --color-expense-100 / --color-expense-700 |
| CASH | --color-neutral-100 / --color-neutral-700 |
| DIGITAL_WALLET | --color-brand-100 / --color-brand-700 |

### Accessibility

- `<h1>` = "Accounts"
- Net worth: `aria-label="Net worth: twelve thousand four hundred fifty dollars"` (screen reader reads full number)
- Asset/Liability sections: `<h2>` headings
- Table with `<thead>` and `scope="col"` on headers
- Each row: `aria-label="[Account name], [type], balance [amount]"`

---

## Screen 5: Add / Edit Account Form

**Route**: `/accounts/new` (add) | `/accounts/{id}/edit` (edit)
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Add Account  (or Edit Account)                  |
|          |--------------------------------------------------|
|          |                                                  |
|          |  Account Type *                                  |
|          |  [v Checking                             ]       |
|          |                                                  |
|          |  Account Name *                                  |
|          |  [________________________________________]      |
|          |  e.g. "Chase Checking"                           |
|          |                                                  |
|          |  Institution Name                                |
|          |  [________________________________________]      |
|          |  e.g. "Chase Bank" (optional)                    |
|          |                                                  |
|          |  Current Balance *   [? help]                    |
|          |  [$ ______________________________________]      |
|          |  "Enter your balance as shown on your statement" |
|          |                                                  |
|          |  Account Number (last 4 digits)                  |
|          |  [____]                                          |
|          |  Stored for display only — we never store your   |
|          |  full account number.                            |
|          |                                                  |
|          |  Currency                                        |
|          |  [USD — US Dollar                         ] (*)  |
|          |  (*) Currency is set by your profile preference  |
|          |                                                  |
|          |  [ Cancel ]  [ Save Account ]                   |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Field Specifications

| Field | Type | Required | Notes |
|---|---|---|---|
| Account Type | Select dropdown | Yes | Options: Checking, Savings, Credit Card, Loan, Cash, Digital Wallet, Investment |
| Account Name | Text input | Yes | Max 100 chars |
| Institution Name | Text input | No | Max 100 chars |
| Initial / Current Balance | Number input | Yes | Min 0; label changes based on account type |
| Account Number Last 4 | Text input, pattern `[0-9]{4}` | No | 4 digits only |
| Currency | Display only (read from user profile) | Auto | Disabled in MVP; future: editable |

### Dynamic Behavior

**Balance label changes by account type**:
- CHECKING, SAVINGS, CASH, INVESTMENT, DIGITAL_WALLET: label = "Current Balance"
  - Help text: "Enter your current balance as shown on your bank statement."
- CREDIT_CARD, LOAN: label = "Current Amount Owed"
  - Help text: "Enter the outstanding balance you owe. A positive number means you owe money."

**Edit mode differences**:
- Account Type field is read-only (cannot change type after creation)
- Initial Balance field is hidden in edit mode (balance is driven by transactions)
- Only Name and Institution Name are editable

### Error Messages

| Condition | Message | Location |
|---|---|---|
| Duplicate name | "You already have an account called '[name]'. Please use a different name." | Below Name field |
| Invalid amount | "Please enter a valid positive amount." | Below Balance field |
| Max accounts reached | Form-level banner: "You have reached the maximum of 20 accounts. Deactivate an existing account to add a new one." | Top of form |

### Accessibility

- `<fieldset>` wrapping the form with `<legend>` = "Add Account" or "Edit Account"
- All inputs: visible `<label>` elements, not placeholder-only
- Balance input: `aria-label="Current balance in US dollars"` (dynamic based on account type)
- Account number: `aria-label="Last 4 digits of account number"`, `inputmode="numeric"`, `maxlength="4"`
- Amount input: `inputmode="decimal"` for numeric keyboard on mobile

---

## Screen 6: Account Detail

**Route**: `/accounts/{id}`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Chase Checking       [Edit] [...more]           |
|          |  Checking Account  ****1234   Chase Bank         |
|          |--------------------------------------------------|
|          |  CURRENT BALANCE                                 |
|          |  $2,450.00                                       |
|          |  (last updated Feb 28, 2026)                     |
|          |--------------------------------------------------|
|          |  TRANSACTIONS           [ + Add Transaction ]    |
|          |  +------------------------------------------+   |
|          |  Filters: [Date range v] [Category v] [Type v]   |
|          |  |------------------------------------------|    |
|          |  | Feb 28  Amazon        Shopping  -$89.00  |    |
|          |  | Feb 27  Salary        Income  +$3,200.00 |    |
|          |  | Feb 25  Electric Bill Utilities -$120.00 |    |
|          |  | Feb 24  Coffee Shop   Food        -$6.50 |    |
|          |  |  ... (30 per page)                       |    |
|          |  | [< Prev]        Page 1 of 3    [Next >]  |    |
|          |  +------------------------------------------+   |
+----------+--------------------------------------------------+
```

### "More" Overflow Menu Options

- Edit account
- Deactivate account (triggers confirmation dialog)

### Data Displayed

- `name` — page heading `<h1>`
- `accountTypeName` — sub-heading badge
- `accountNumberLast4` — displayed as `****{last4}` or "(no account number)" if null
- `institutionName` — secondary subtitle
- `currentBalance` — hero figure
  - ASSET: dark neutral color
  - LIABILITY: amber, with "(Amount Owed)" label beneath
- Transaction list: filtered to this account
  - Fields per row: `transactionDate`, `merchantName` or `description`, `categoryName` + icon, signed amount
  - Date format: "Feb 28" (short) or "Feb 28, 2026" (if not current year)

### Filter Controls

- Date Range picker (defaulting to current month)
- Category dropdown (shows all active categories)
- Transaction Type: All / Income / Expense / Transfer (radio-style tabs)

### Empty Transactions State (new account)

```
[ArrowLeftRight icon]
No transactions yet.
Add your first transaction to see your activity here.
[ Add Transaction ]
```

### Accessibility

- `<h1>` = account name
- Balance: `aria-label="Current balance: two thousand four hundred fifty dollars"`
- For liability accounts: balance element has `aria-describedby` pointing to "(Amount Owed)" descriptor
- Transactions table: proper `<th scope="col">` headers
- Pagination: `<nav aria-label="Transaction pages">` with `aria-current="page"` on active page

---

## Screen 7: Transactions List

**Route**: `/transactions`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Transactions           [ + Add Transaction ]    |
|          |--------------------------------------------------|
|          |  [Search: description or merchant...  [search]]  |
|          |  Filters: [Date range v] [Account v] [Cat v] [Type v] [Amount v] |
|          |                                                  |
|          |  32 transactions | Income: +$3,200 | Expenses: -$1,847 |
|          |  +--------------------------------------------+ |
|          |  | Date   | Account     | Merchant/Desc | Category | Amount | |
|          |  |--------|-------------|---------------|----------|---------||
|          |  | Feb 28 | Chase Chk   | Amazon        | Shopping |-$89.00 ||
|          |  | Feb 27 | Chase Chk   | Salary Dep.   | Salary  |+$3,200 ||
|          |  | Feb 26 | Visa CC     | Uber Eats     | Food     |-$28.50 ||
|          |  | Feb 25 | Chase Chk   | Electric Bill | Utilities|-$120.00||
|          |  |  ...                                                      ||
|          |  | [< Prev]          Page 1 of 2          [Next >]          ||
|          |  +--------------------------------------------+ |
+----------+--------------------------------------------------+
```

### Mobile Wireframe

```
+----------------------------------+
| Transactions        [ + Add ]    |
| [Search...]             [filter] |
|----------------------------------|
| 32 transactions this month       |
|----------------------------------|
| Feb 28                           |
|  Amazon         Shopping -$89.00 |
|  (Chase Checking)                |
|----------------------------------|
| Feb 27                           |
|  Salary Deposit  Salary +$3,200  |
|  (Chase Checking)                |
|----------------------------------|
(scrollable, date-grouped on mobile)
```

### Data Per Transaction Row

| Field | Display Rule |
|---|---|
| `transactionDate` | "Feb 28" (short format; year if not current year) |
| `accountName` | Secondary text, gray, below merchant on mobile |
| `merchantName` or `description` | Primary text. If both exist, merchantName wins; description as subtitle |
| `categoryName` + icon | Category icon (16px) + name |
| Amount | Right-aligned. "+" prefix in --color-income-600 for INCOME/TRANSFER_IN. "-" prefix in --color-expense-600 for EXPENSE/TRANSFER_OUT |

### Summary Bar

Above the list, always visible:
- "N transactions | Income: +$X | Expenses: -$Y"
- These totals reflect the current filter state, not all-time

### Filter Panel

Filters shown inline on desktop (as a horizontal row of dropdowns). On mobile, a "Filter" button opens a bottom sheet modal with all filter controls.

| Filter | Control | Values |
|---|---|---|
| Date Range | Date picker (From / To) | Default: current month |
| Account | Multi-select dropdown | All active accounts |
| Category | Multi-select dropdown with search | All categories, grouped |
| Transaction Type | Segmented control / pills | All / Income / Expense / Transfer |
| Amount Range | Two number inputs (Min / Max) | Optional |

Active filters: displayed as dismissible chips above the list. Each chip shows the filter name and value. "Clear all filters" link.

### Search

- Full-text search on `description` + `merchantName`
- Debounce: 300ms after last keystroke
- `aria-label="Search transactions by description or merchant name"`
- `role="search"` on the search container

### Empty State (no results with filters)

```
[Search icon]
No transactions found.
Try adjusting your filters or search terms.
[ Clear all filters ]
```

Empty state (no transactions at all):
```
[ArrowLeftRight icon]
No transactions yet.
Start by recording your first income or expense.
[ Add Transaction ]
```

### Accessibility

- `<h1>` = "Transactions"
- Summary bar: `aria-live="polite"` to announce result count changes on filter change
- Table: `<table role="grid">` with sortable column headers having `aria-sort`
- Each row: can be focused (row has `tabindex="0"`) and navigated with arrow keys
- Amount column: `aria-label` on each amount cell includes "income" or "expense" not just the number

---

## Screen 8: Add / Edit Transaction Form

**Route**: `/transactions/new` | `/transactions/{id}/edit`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Add Transaction                                 |
|          |--------------------------------------------------|
|          |                                                  |
|          |  Transaction Type *                              |
|          |  ( Income )  ( Expense )  ( Transfer -> )        |
|          |                                                  |
|          |  Account *                                       |
|          |  [v Chase Checking — $2,450.00 current balance ] |
|          |                                                  |
|          |  Category *                                      |
|          |  [v Search categories...                    ]    |
|          |  (dropdown opens with grouped parent/child list) |
|          |                                                  |
|          |  Amount *            Date *                      |
|          |  [$ ______________]  [v Feb 28, 2026     ]       |
|          |                                                  |
|          |  Merchant / Payee (optional)                     |
|          |  [________________________________________]      |
|          |                                                  |
|          |  Notes (optional)                                |
|          |  [________________________________________]      |
|          |  [________________________________________]      |
|          |                                                  |
|          |  [ Cancel ]              [ Add Transaction ]     |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Transaction Type Selector

Three tabs/pills at the top. Selecting "Transfer" morphs the form into the Transfer Form (Screen 9). "Income" and "Expense" use this form.

When type = Income: category dropdown filters to show INCOME categories only.
When type = Expense: category dropdown filters to show EXPENSE categories only.

### Category Dropdown

- Searchable (`combobox` pattern)
- Grouped: parent categories as non-selectable group headers (bold, gray)
- Child categories indented below parent
- Category icon and color chip shown if available
- `aria-label="Select category"`, `role="combobox"`, `aria-autocomplete="list"`, `aria-expanded`

### Amount Input

- `inputmode="decimal"` (opens numeric keypad on mobile)
- `type="text"` with validation (not `type="number"` to avoid browser rounding issues with BigDecimal)
- Currency symbol prefix: "$" shown as an inset prefix inside the input
- Placeholder: "0.00"
- Only accepts positive values; sign implied by transaction type

### Date Picker

- Defaults to today
- Native `<input type="date">` on mobile for best UX
- Custom styled date picker on desktop (using a library like react-day-picker)
- Constraints enforced client-side: min = today - 10 years, max = today + 30 days
- Display format: "Feb 28, 2026"

### Error Messages

| Condition | Message | Location |
|---|---|---|
| Amount = 0 | "Amount must be greater than zero." | Below Amount field |
| Amount missing | "Please enter a transaction amount." | Below Amount field |
| Date too far past | "Transaction date cannot be more than 10 years ago." | Below Date field |
| Date too far future | "Transaction date cannot be more than 30 days in the future." | Below Date field |
| SAVINGS/CASH overdraft | "This transaction would leave your [Account Name] balance below zero. Current balance: $[X], transaction amount: $[Y]. Savings and cash accounts cannot have a negative balance." | Below Account field (API error banner) |
| No category selected | "Please select a category." | Below Category field |
| Network error | Toast: "Something went wrong. Please try again." | Top-right toast |

### Success

- On success: toast "Transaction added successfully."
- Navigate back to previous screen (account detail or transaction list)
- Account balance updates immediately in background data cache

### Accessibility

- `<h1>` = "Add Transaction" or "Edit Transaction"
- Transaction type: `<fieldset>` with `<legend>="Transaction type"`, each option a `<input type="radio">`
- Amount: `aria-label="Amount in US dollars"`, `aria-required="true"`
- Date: `aria-label="Transaction date"`, `aria-describedby` pointing to constraint hint
- Error: `role="alert"` on API error banners; `aria-describedby` on field-level errors
- Category search: full `combobox` ARIA pattern with `aria-activedescendant` on keyboard navigation

---

## Screen 9: Transfer Form

**Route**: `/transfers/new` | accessible via Add Transaction > Transfer tab
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  New Transfer                                    |
|          |--------------------------------------------------|
|          |                                                  |
|          |  Transaction Type *                              |
|          |  ( Income )  ( Expense )  ( Transfer [active] )  |
|          |                                                  |
|          |  From Account *                                  |
|          |  [v Chase Checking — $2,450.00           ]       |
|          |                                                  |
|          |  [arrows-updown icon — swap accounts button]     |
|          |                                                  |
|          |  To Account *                                    |
|          |  [v Wells Fargo Savings — $8,000.00       ]      |
|          |                                                  |
|          |  Amount *                                        |
|          |  [$ ______]                                      |
|          |                                                  |
|          |  Preview (updates live):                         |
|          |  +--------------------------------------------+ |
|          |  | Chase Checking:     $2,450.00 -> $1,950.00 | |
|          |  | Wells Fargo Savings: $8,000.00 -> $8,500.00 | |
|          |  +--------------------------------------------+ |
|          |                                                  |
|          |  Date *                    Description           |
|          |  [v Feb 28, 2026  ]        [________________]   |
|          |                                                  |
|          |  [ Cancel ]         [ Transfer Funds ]           |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Dynamic Rules

- From Account dropdown excludes whatever is selected in To Account, and vice versa
- "Swap" button (arrows icon) swaps the two selected accounts with animation
- Live preview updates whenever amount OR account selection changes
- Preview shows current balance -> projected balance after transfer
- If projected From balance would go negative on a SAVINGS/CASH account: preview balance shown in red + warning text inline

### Error Messages

| Condition | Message |
|---|---|
| Same account selected | "The source and destination accounts must be different." |
| SAVINGS/CASH would go negative | "This transfer would leave your [Account Name] balance at $[X], which is not allowed. Current balance: $[Y]." |
| Amount missing | "Please enter a transfer amount." |

---

## Screen 10: Categories

**Route**: `/categories`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Categories         [ + Add Custom Category ]    |
|          |--------------------------------------------------|
|          |                                                  |
|          |  [ Expense ] [ Income ]  (tabs)                  |
|          |                                                  |
|          |  SYSTEM CATEGORIES (not editable)                |
|          |  +--------------------------------------------+ |
|          |  | [lock] Housing                           > | |
|          |  |     Rent                                    | |
|          |  |     Mortgage                                 | |
|          |  |     HOA                                      | |
|          |  | [lock] Transportation                     > | |
|          |  |     Car Payment                              | |
|          |  |     Gas                                      | |
|          |  | [lock] Food                               > | |
|          |  |   ...                                        | |
|          |  +--------------------------------------------+ |
|          |                                                  |
|          |  YOUR CUSTOM CATEGORIES                          |
|          |  +--------------------------------------------+ |
|          |  | [custom-icon] My Business                   | |
|          |  |   [edit]  [deactivate]                      | |
|          |  +--------------------------------------------+ |
|          |  (empty state if no custom categories)           |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Data Per Row

**System category (parent)**:
- Lock icon (not editable indicator)
- Category icon (from predefined set) or default icon
- Category name
- Expand/collapse toggle to show children
- No edit/delete controls

**System category (child)**:
- Indented (24px left offset)
- Name only
- No controls

**User custom category**:
- Category icon (if set) with category color
- Category name
- "Edit" button (pencil icon)
- "Deactivate" button (visible on hover/focus; uses trash/X icon)

### Empty Custom Category State

```
[Tag icon]
You haven't created any custom categories yet.
Custom categories let you track spending your way.
[ Add Custom Category ]
```

### Accessibility

- Tab panel: `role="tablist"` with `<button role="tab" aria-selected>` for Expense/Income tabs
- Category list: `role="tree"` with `role="treeitem"` per category and `aria-expanded` on parents
- Lock icon: `aria-hidden="true"` with screen reader text "(system category, not editable)" on the row

---

## Screen 11: Add / Edit Category Form

**Route**: `/categories/new` | `/categories/{id}/edit`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Add Custom Category                             |
|          |--------------------------------------------------|
|          |                                                  |
|          |  Category Type *                                 |
|          |  [v Expense                            ]         |
|          |                                                  |
|          |  Parent Category (optional)                      |
|          |  [v None (top-level)                   ]         |
|          |  Only top-level categories shown here.           |
|          |                                                  |
|          |  Name *                                          |
|          |  [________________________________________]      |
|          |                                                  |
|          |  Icon (optional)                                 |
|          |  [current-icon] [ Pick an icon... ]              |
|          |  (icon picker grid opens in popover)             |
|          |                                                  |
|          |  Color (optional)                                |
|          |  [color-swatch] [#4F46E5] [ Pick... ]            |
|          |  (color picker opens in popover)                 |
|          |                                                  |
|          |  [ Cancel ]      [ Save Category ]               |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Dynamic Behavior

- When Category Type changes, the Parent Category dropdown re-populates (only shows top-level categories of the matching type)
- Parent Category cannot itself have a parent (only depth-1 parents shown)

### Error Messages

| Condition | Message |
|---|---|
| Duplicate name | "You already have a category called '[name]' [under '[parent]']. Please use a different name." |
| Missing name | "Category name is required." |

### Icon Picker

- Grid of 40–60 common Lucide icons in a popover/modal
- Search filter within the picker
- Selected icon highlighted
- "None" option to clear icon

### Color Picker

- 16 preset swatches covering common category colors
- Optional hex input field for custom color
- Preview swatch shown in form

---

## Screen 12: Budgets

**Route**: `/budgets`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Budgets              [ + Add Budget ]           |
|          |--------------------------------------------------|
|          |  Period: ( Weekly ) ( Monthly [active] ) ( Quarterly ) ( Annual ) |
|          |  February 2026                                   |
|          |  +--------------------------------------------+ |
|          |  | Category       Spent / Budget   Status     | |
|          |  |--------------------------------------------|  |
|          |  | Groceries      $420 / $500                  | |
|          |  |  [||||||||||||||||||||||||-----------]  84% | |
|          |  |  $80.00 remaining          [! WARNING]      | |
|          |  |                                             | |
|          |  | Rent         $1,200 / $1,200                | |
|          |  |  [||||||||||||||||||||||||||||||||||||] 100% | |
|          |  |  $0.00 remaining        [!! OVER BUDGET]    | |
|          |  |                                             | |
|          |  | Dining Out    $38 / $100                    | |
|          |  |  [||||||||--------------------------]  38%  | |
|          |  |  $62.00 remaining           [ON TRACK]      | |
|          |  |--------------------------------------------|  |
|          |  | Total        $1,658 / $1,800                | |
|          |  +--------------------------------------------+ |
|          |                                                  |
|          |  UNBUDGETED SPENDING THIS MONTH                  |
|          |  +--------------------------------------------+ |
|          |  | Entertainment  $50.00 (no budget set)      | |
|          |  | Coffee Shops   $18.50 (no budget set)      | |
|          |  +--------------------------------------------+ |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Mobile Wireframe

Each budget = a card. Period selector at top as a horizontally scrollable pill row.

### Data Per Budget Row

| Field | Source | Display |
|---|---|---|
| Category icon + name | category | Left-aligned |
| Progress bar | (spent / amount) * 100% | Color-coded fill |
| "$X of $Y" | spent, amount | Below bar, left |
| Status badge | budget_status | Right-aligned pill |
| Remaining / Over | amount - spent | Below bar |

**Progress Bar Colors**:
- 0–74%: --color-income-600 fill
- 75–99%: --color-liability-600 fill
- 100%+: --color-expense-600 fill (can overflow visually with distinct pattern or cap at 100% with color change)

**Status Badges**:
| Status | Text | Color |
|---|---|---|
| ON_TRACK | "On Track" | --color-income-600 bg |
| WARNING | "Warning" | --color-liability-600 bg |
| OVER_BUDGET | "Over Budget" | --color-expense-600 bg |

### Period Toggle

Segmented control (pill group): Weekly / Monthly / Quarterly / Annual. Selecting a period reloads budget data for that period. Current period label shown below: "February 2026" for monthly.

### Unbudgeted Spending Section

Shows categories with transactions this period but no active budget. Goal: encourage budget creation.

Per row: category name, amount spent. "Set Budget" quick-action button.

### Empty State

```
[PieChart icon]
No budgets set for February 2026.
Create budgets to track where your money goes.
[ Create Your First Budget ]
```

### Accessibility

- Period toggle: `role="radiogroup"` with each period as `role="radio"`
- Progress bars: `role="progressbar" aria-valuenow={pct} aria-valuemin="0" aria-valuemax="100" aria-label="Groceries budget: 84% used"`
- Status badge: NOT communicated by color alone; text "On Track", "Warning", "Over Budget" always present
- Budget row: complete information available as text; row can be focused with `tabindex="0"`

---

## Screen 13: Add / Edit Budget Form

**Route**: `/budgets/new` | `/budgets/{id}/edit`
**Auth**: Required

### Desktop Wireframe

```
+----------+--------------------------------------------------+
| SIDEBAR  |  Add Budget                                      |
|          |--------------------------------------------------|
|          |                                                  |
|          |  Category *                                      |
|          |  [v Search Expense or Income categories... ]     |
|          |  (TRANSFER categories excluded)                  |
|          |                                                  |
|          |  Period Type *                                   |
|          |  [v Monthly                            ]         |
|          |                                                  |
|          |  (if Custom selected):                           |
|          |  Start Date *         End Date *                 |
|          |  [v Feb 1, 2026 ]     [v Feb 28, 2026 ]         |
|          |                                                  |
|          |  Budget Amount *                                 |
|          |  [$ ______________________________________]      |
|          |                                                  |
|          |  Alert Threshold (optional)                      |
|          |  Notify me when I reach: [ 80 ]% of budget       |
|          |  [--------|------o----------] (slider: 1-100)    |
|          |  "You'll be alerted at $400.00 of $500 budget"   |
|          |  (live preview updates as user moves slider)     |
|          |                                                  |
|          |  [ Cancel ]          [ Save Budget ]             |
|          |                                                  |
+----------+--------------------------------------------------+
```

### Dynamic Behavior

- Alert Threshold slider: live preview text shows `"You'll be alerted at $[amount * threshold%] of $[amount] budget"` — updates on every slider move and on budget amount change
- Period Type = Custom: reveals Start Date and End Date pickers
- Period Type = Monthly: Start Date pre-filled to 1st of current month (hidden; informational note: "Budget applies to calendar months")

### Error Messages

| Condition | Message |
|---|---|
| Duplicate budget | "You already have an active budget for [Category] for [period type]. Edit the existing budget instead." |
| Amount = 0 | "Budget amount must be greater than zero." |
| Custom: end before start | "End date must be after start date." |

### Alert Threshold Slider Accessibility

```html
<input type="range"
  min="1" max="100"
  aria-label="Alert threshold percentage"
  aria-valuetext="80 percent — you will be alerted at $400"
/>
```

---

## Global: Confirmation Dialog

Used before: deactivating an account, deleting a transaction, deactivating a budget.

### Wireframe

```
+----------------------------------+
| +------------------------------+ |
| | Deactivate Account?          | |
| |                              | |
| | This will hide Chase Checking| |
| | and all its transactions from| |
| | new views. Your data is      | |
| | preserved.                   | |
| |                              | |
| | [ Cancel ]  [ Deactivate ]   | |
| +------------------------------+ |
+----------------------------------+
```

Destructive button (Deactivate / Delete) = --color-expense-600 background.
Cancel = outline button.

Modal: `role="dialog" aria-modal="true" aria-labelledby="dialog-title"`.
Focus trapped inside modal while open.
On close: focus returns to the trigger element.

---

## Global: Toast Notifications

```
Top-right on desktop:
+--------------------------------+
| [CheckCircle] Transaction added |  [x]
+--------------------------------+

Top-center on mobile:
+----------------------------+
| [!] Something went wrong.   |  [x]
+----------------------------+
```

Types:
- Success: --color-income-600 left border / icon
- Error: --color-expense-600 left border / icon
- Warning: --color-liability-600 left border / icon
- Info: --color-brand-500 left border / icon

`role="status"` (or `role="alert"` for errors) for screen reader announcement.
Auto-dismiss: 4 seconds (6 seconds for error toasts).
Manual dismiss: [x] button always available.
