# Component Library Specification — Personal Finance Tracker
**Author**: UX/UI Designer
**Date**: 2026-02-28
**Reference**: design-system.md for all tokens; screen-specs.md for usage context

---

## Component Conventions

**Naming**: PascalCase for component names. Props use camelCase.
**States documented**: default, hover, focus, active (pressed), disabled, loading, error.
**Touch targets**: all interactive elements minimum 44x44px per WCAG 2.5.5 (target size).
**Platform notes**: each component notes adaptations for React Native (future mobile).
**CSS**: Use CSS custom properties from design-system.md. No hardcoded colors or sizes.

---

## Component 1: AmountInput

**Purpose**: Monetary value entry. Handles currency formatting, positive-only enforcement, numeric keyboard on mobile.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| value | string | Yes | "" | Controlled value (string to avoid float precision issues; matches BigDecimal) |
| onChange | (value: string) => void | Yes | — | Callback with raw numeric string |
| currency | string | Yes | "USD" | ISO 4217 code; drives symbol display |
| label | string | Yes | — | Visible label text |
| placeholder | string | No | "0.00" | Placeholder within input |
| disabled | boolean | No | false | |
| error | string | No | undefined | Error message text |
| id | string | Yes | — | For label association |
| required | boolean | No | false | |
| allowNegative | boolean | No | false | Always false in MVP (sign implicit from tx type) |
| showCurrencySymbol | boolean | No | true | Prepends "$" or locale symbol |
| onBlur | () => void | No | — | |

### Visual States

**Default**:
```
[label text *]
[$] [0.00___________________]
```
Border: 1px solid --color-neutral-300
Background: --color-neutral-0
Text: --color-neutral-800

**Focus**:
Border: 2px solid --color-brand-500
Box-shadow: --focus-ring
Background: --color-neutral-0

**Filled (valid value)**:
Border: 1px solid --color-neutral-300
Text: --color-neutral-900, font-variant-numeric: tabular-nums

**Error**:
Border: 1px solid --color-expense-600
Below input: `<p id="{id}-error" role="alert" style="color: --color-expense-600">{error}</p>`

**Disabled**:
Background: --color-neutral-100
Text: --color-neutral-400
Cursor: not-allowed
Border: 1px solid --color-neutral-200

### HTML Structure

```html
<div class="amount-input-wrapper">
  <label for="{id}">{label} {required && <span aria-hidden="true">*</span>}</label>
  <div class="amount-input-field" aria-label="{label} in {currencyName}">
    <span class="currency-symbol" aria-hidden="true">$</span>
    <input
      id="{id}"
      type="text"
      inputmode="decimal"
      pattern="[0-9]*\.?[0-9]*"
      value={value}
      placeholder="0.00"
      aria-required={required}
      aria-invalid={!!error}
      aria-describedby={error ? `${id}-error` : undefined}
    />
  </div>
  {error && <p id="{id}-error" class="error-text" role="alert">{error}</p>}
</div>
```

### Formatting Logic

On blur: format value to 2 decimal places (e.g., "5" -> "5.00"; "5.1" -> "5.10").
On focus: show raw value (allow editing).
Prevent: letters, multiple decimal points, negative sign.
Accept: paste of formatted numbers (strip commas before parsing).
Display thousands separator in formatted state (e.g., "1,500.00").

### Mobile Adaptation (React Native)

```jsx
<TextInput
  keyboardType="decimal-pad"
  accessibilityLabel={`${label} in ${currencyName}`}
  value={value}
  onChangeText={handleChange}
/>
```

Use `react-native-currency-input` or implement custom formatter.

---

## Component 2: AccountCard

**Purpose**: Displays one account with its balance in a list or grid context.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| account | AccountResponse | Yes | — | Full account data object |
| onPress | () => void | No | — | Navigate to account detail |
| compact | boolean | No | false | Compact row mode vs card mode |
| showBadge | boolean | No | true | Show account type badge |

### AccountResponse shape (relevant fields)

```typescript
{
  id: number;
  name: string;
  accountTypeCode: string; // "CHECKING", "SAVINGS", etc.
  accountTypeName: string; // "Checking Account"
  currentBalance: string;  // BigDecimal as string
  institutionName?: string;
  accountNumberLast4?: string;
  isLiability: boolean;
  currency: string;
}
```

### Visual States

**Card mode (default)**:
```
+----------------------------------------+
| [AccountTypeIcon]  Chase Checking      |
|                    Chase Bank  ****1234 |
|                    [Checking]           |
|                                         |
|                    $2,450.00            |
+----------------------------------------+
```
- Account name: --text-lg, --font-weight-semibold
- Institution + last4: --text-sm, --color-neutral-500
- Balance: --text-2xl, --font-weight-bold, tabular-nums
- Liability account balance: --color-liability-600, "(Amount Owed)" subtitle

**Compact row mode**:
```
[AccountTypeIcon] Chase Checking   [Checking]   $2,450.00
                  Chase Bank
```

**Hover**: background transitions to --color-neutral-50. Subtle shadow lift (--shadow-sm -> --shadow-md).
**Focus**: --focus-ring applied to card container.
**Active / pressed**: scale(0.99) transform, 100ms.

### Account Type Icon Mapping (Lucide)

```
CHECKING:       Building2
SAVINGS:        PiggyBank
CREDIT_CARD:    CreditCard
INVESTMENT:     TrendingUp
LOAN:           Landmark
CASH:           Banknote
DIGITAL_WALLET: Smartphone
```

### Badge Color by Account Type

See design-system.md Section 8 / screen-specs.md Screen 4.

### Accessibility

```html
<article
  role="article"
  aria-label="{name}, {accountTypeName}, balance {formattedBalance}"
  tabindex="0"
  onClick={onPress}
  onKeyDown={(e) => e.key === 'Enter' && onPress()}
>
```

Screen reader reads: "Chase Checking, Checking Account, balance two thousand four hundred fifty dollars."

### Mobile Adaptation

Full-width card. Minimum height 80px. Tap ripple effect. No hover state.

---

## Component 3: TransactionRow

**Purpose**: Single transaction in a list. Must convey type (income/expense/transfer), amount, and key metadata at a glance.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| transaction | TransactionRowData | Yes | — | Transaction display data |
| onPress | () => void | No | — | Navigate to transaction detail / edit |
| showAccount | boolean | No | true | Show account name (false on Account Detail) |

### TransactionRowData shape

```typescript
{
  id: number;
  transactionDate: string; // "2026-02-28"
  merchantName?: string;
  description?: string;
  categoryName: string;
  categoryIcon?: string;  // Lucide icon name
  accountName: string;
  transactionType: "INCOME" | "EXPENSE" | "TRANSFER_IN" | "TRANSFER_OUT";
  amount: string; // BigDecimal string, always positive
  isReconciled: boolean;
}
```

### Visual Layout

```
[Date]  [category-icon]  [Primary text]        [+/-$Amount]
        [Account name]   [secondary text]
```

**Desktop row** (table row):
```
| Feb 28 | [icon] Shopping | Amazon         | Chase Checking  | -$89.00  |
```

**Mobile card row**:
```
[icon]  Amazon                          -$89.00
        Shopping  •  Chase Checking
        Feb 28
```

### Amount Color Rules

```
INCOME:      color: --color-income-600;  prefix: "+"
EXPENSE:     color: --color-expense-600; prefix: "-"
TRANSFER_IN: color: --color-income-600;  prefix: "+"; label: "Transfer In"
TRANSFER_OUT:color: --color-transfer-600; prefix: "-"; label: "Transfer Out"
```

### Primary Text Logic

```
if (merchantName) -> show merchantName as primary, description as subtitle
else if (description) -> show description as primary
else -> show categoryName as primary
```

### Reconciled Indicator

Small green checkmark icon (CheckCircle2, 12px) when `isReconciled = true`. `aria-label="Reconciled"` on the icon. Hidden from visual display by default — only shown when a reconciliation view is active (Phase 2).

### Hover / Interaction

Hover: row background --color-neutral-50
Focus: --focus-ring on row
Active: row background --color-neutral-100

### Accessibility

```html
<tr
  tabindex="0"
  role="row"
  aria-label="{merchantName or description}, {categoryName}, {account}, {type} {formattedAmount}, {date}"
>
```

Screen reader: "Amazon, Shopping, Chase Checking, expense eighty nine dollars, February twenty-eighth"

---

## Component 4: CategoryPicker

**Purpose**: Searchable dropdown for selecting a category, supporting a two-level hierarchy.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| value | number or null | Yes | null | Selected category ID |
| onChange | (id: number) => void | Yes | — | |
| categories | Category[] | Yes | — | Full list to display |
| filterType | "INCOME" or "EXPENSE" or "ALL" | No | "ALL" | Filters which categories to show |
| label | string | Yes | — | |
| placeholder | string | No | "Search categories..." | |
| disabled | boolean | No | false | |
| error | string | No | undefined | |
| id | string | Yes | — | |
| required | boolean | No | false | |

### Visual Structure

```
[Category *]
[Search categories...                     v]

When open:
+--------------------------------------+
| [search input]                       |
|--------------------------------------|
| EXPENSE CATEGORIES                   |
|  Housing                             |  <- parent (non-selectable group header)
|    Rent                              |  <- child (selectable)
|    Mortgage                          |  <- child (selectable)
|    HOA                               |
|  Transportation                      |
|    Car Payment                       |
|    Gas                               |
|  [icon] My Custom Category           |  <- user category with custom icon
|--------------------------------------|
| INCOME CATEGORIES                    |  (shown when filterType = "ALL")
|  Salary                              |
|  ...                                 |
+--------------------------------------+
```

### Behavior

- Typing in search filters categories in real-time (both parent names and child names)
- Parent categories shown as bold, non-selectable group headers
- If search matches a parent name, show all its children
- If search matches a child name, show the matched child under its parent header
- Keyboard: arrow keys navigate options; Enter selects; Escape closes
- Selected category displays with its icon and color chip

### ARIA Implementation

```html
<div role="combobox" aria-expanded={isOpen} aria-haspopup="listbox" aria-owns="category-listbox">
  <input
    id="{id}"
    aria-autocomplete="list"
    aria-controls="category-listbox"
    aria-activedescendant={activeOptionId}
    value={searchText}
  />
</div>
<ul id="category-listbox" role="listbox" aria-label="Categories">
  <li role="group" aria-label="Housing">
    <span role="presentation">Housing</span>  <!-- group header, not selectable -->
    <ul>
      <li role="option" id="cat-1" aria-selected={value === 1}>Rent</li>
      <li role="option" id="cat-2" aria-selected={value === 2}>Mortgage</li>
    </ul>
  </li>
</ul>
```

### Mobile Adaptation

On mobile, CategoryPicker opens a full-screen bottom sheet instead of an inline dropdown. The bottom sheet has a sticky search input at the top, and a scrollable list below. This gives more screen space for the hierarchy.

---

## Component 5: BudgetProgressBar

**Purpose**: Visual representation of budget consumption with color-coded status.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| spent | number | Yes | — | Amount spent (numeric, for calculation) |
| budget | number | Yes | — | Budget limit (numeric) |
| status | "ON_TRACK" or "WARNING" or "OVER_BUDGET" | Yes | — | From API |
| currency | string | No | "USD" | For amount display |
| showText | boolean | No | true | Show "$X of $Y" and remaining text |
| size | "sm" or "md" or "lg" | No | "md" | Bar height: 4px / 8px / 12px |

### Visual Structure

**Full budget row** (size="md"):
```
Groceries                           84%   [ WARNING ]
[$|||||||||||||||||||||||||||||___]
$420.00 of $500.00 • $80.00 remaining
```

**Compact** (size="sm", showText=false):
```
[$||||||||||||||||||||||||||||____]  84%
```

### Color States

```
ON_TRACK (< 75%):
  Fill: --color-income-600 (#16A34A)
  Background track: --color-income-100

WARNING (75–99%):
  Fill: --color-liability-600 (#D97706)
  Background track: --color-liability-100

OVER_BUDGET (>= 100%):
  Fill: --color-expense-600 (#DC2626)
  Bar width capped at 100%; consider striped pattern on overfill
  Background track: --color-expense-100
```

### Over-budget display

When spent > budget, the text shows "$[over] over budget" in --color-expense-600 instead of remaining.

### HTML

```html
<div class="budget-progress">
  <div
    role="progressbar"
    aria-valuenow={Math.min(percentage, 100)}
    aria-valuemin="0"
    aria-valuemax="100"
    aria-label="Groceries budget: {percentage}% used. {status}. Spent ${spent} of ${budget}."
    class="progress-bar {statusClass}"
    style="--fill-width: {Math.min(percentage, 100)}%"
  >
    <div class="progress-fill"></div>
  </div>
  {showText && (
    <div class="progress-labels">
      <span>${formatAmount(spent)} of ${formatAmount(budget)}</span>
      <span class={remainingClass}>{remainingText}</span>
    </div>
  )}
</div>
```

### Mobile Adaptation

Identical on mobile. Ensure minimum bar height 4px. Row height allows comfortable scanning.

---

## Component 6: DateRangePicker

**Purpose**: Select a start and end date for filtering transactions or defining custom budget periods.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| startDate | Date or null | Yes | — | |
| endDate | Date or null | Yes | — | |
| onStartChange | (date: Date) => void | Yes | — | |
| onEndChange | (date: Date) => void | Yes | — | |
| minDate | Date | No | today - 10 years | |
| maxDate | Date | No | today + 30 days | |
| label | string | No | "Date Range" | |
| presets | DatePreset[] | No | [] | Quick-select presets |

### Presets (for transaction filter)

```
[ This Month ] [ Last Month ] [ Last 3 Months ] [ This Year ] [ Custom ]
```

### Visual Structure (desktop)

```
From date           To date
[v Feb 1, 2026 ]    [v Feb 28, 2026 ]

[February 2026                   >]
Mo Tu We Th Fr Sa Su
             1   2
 3  4  5  6  7  8  9
10 11 12 13 14 15 16
17 18 19 20 21 22 23
24 25 26 27 28
```

Start date highlighted in brand color; range fill in --color-brand-100; end date highlighted.

### Mobile Adaptation

On mobile, use native `<input type="date">` for start and end separately. No calendar UI — native date picker is superior for touch on mobile.

### Accessibility

- Both date inputs: `type="date"` with `aria-label` and valid min/max attributes
- Calendar grid (desktop): `role="grid"` with `role="gridcell"` per day, `aria-selected`, `aria-disabled`
- Keyboard: arrow keys navigate days, Enter selects, Escape closes

---

## Component 7: ConfirmationDialog

**Purpose**: Interrupts a destructive or irreversible action to confirm user intent.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| isOpen | boolean | Yes | — | Controls visibility |
| title | string | Yes | — | Dialog heading |
| body | string or ReactNode | Yes | — | Explanation text |
| confirmLabel | string | Yes | — | Destructive action label |
| cancelLabel | string | No | "Cancel" | |
| onConfirm | () => void | Yes | — | |
| onCancel | () => void | Yes | — | |
| isLoading | boolean | No | false | Confirm button loading state |
| variant | "danger" or "warning" | No | "danger" | Confirm button color |

### Visual Structure

```
+--------------------------------------+
| [backdrop — semi-transparent overlay]|
|   +--------------------------------+ |
|   |  Deactivate Account?           | |
|   |  ──────────────────────────── | |
|   |  This will hide Chase Checking | |
|   |  and all its transactions from | |
|   |  new views. Your data is       | |
|   |  preserved.                    | |
|   |                                | |
|   |  [ Cancel ]  [ Deactivate ]    | |
|   +--------------------------------+ |
+--------------------------------------+
```

- Confirm button: --color-expense-600 background (danger variant) or --color-liability-600 (warning)
- Cancel button: outline style, --color-neutral-600 border and text
- Max width: 400px; centers on screen; rounded corners --radius-lg

### Interaction States

**Idle (loading=false)**: Both buttons active.
**Loading (loading=true)**: Confirm button shows spinner + disabled. Cancel remains active.
**Success**: Dialog closes programmatically; success toast shown.

### Accessibility

```html
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="dialog-title"
  aria-describedby="dialog-body"
>
  <h2 id="dialog-title">{title}</h2>
  <p id="dialog-body">{body}</p>
  <button onClick={onCancel}>{cancelLabel}</button>
  <button onClick={onConfirm} aria-busy={isLoading}>{confirmLabel}</button>
</div>
```

Focus management:
- On open: focus moves to the Cancel button (safer default — prevents accidental confirmation)
- Focus trapped inside dialog (Tab cycles: Cancel <-> Confirm)
- On close: focus returns to the element that triggered the dialog
- Backdrop click: calls onCancel
- Escape key: calls onCancel

---

## Component 8: EmptyState

**Purpose**: Full-section placeholder when a list or view has no data.

### Props

| Prop | Type | Required | Default | Description |
|---|---|---|---|---|
| icon | LucideIconComponent | Yes | — | Lucide icon component |
| title | string | Yes | — | Primary message |
| description | string | No | — | Secondary explanation |
| actionLabel | string | No | — | CTA button text |
| onAction | () => void | No | — | CTA handler |
| size | "sm" or "md" or "lg" | No | "md" | Icon size and spacing |

### Visual Structure (md size)

```
        [Icon — 48px, --color-neutral-300]

        Title text
        (--text-xl, --color-neutral-700)

        Description text here explaining
        what this section is for.
        (--text-base, --color-neutral-500)

        [ Action Button ]
        (if actionLabel provided)
```

All centered horizontally. Minimum vertical padding: --space-16 (64px) top and bottom.

### Usage by Screen

| Screen | Icon | Title | CTA |
|---|---|---|---|
| Accounts | Wallet | "You haven't added any accounts yet." | "Add Your First Account" |
| Transactions | ArrowLeftRight | "No transactions yet." | "Add Transaction" |
| Budgets | PieChart | "No budgets set for this period." | "Create Your First Budget" |
| Categories (custom section) | Tag | "No custom categories yet." | "Add Custom Category" |
| Transactions (filtered) | Search | "No transactions found." | "Clear all filters" |

### Accessibility

- `role="status"` on the container (polite notification of empty state)
- Icon: `aria-hidden="true"` (decorative)
- CTA button: descriptive label (not generic "Click here")

---

## Component 9: NavigationSidebar (Desktop) / BottomTabBar (Mobile)

### NavigationSidebar (Desktop, >= 1024px)

**Width states**:
- Expanded: 240px (icon + label)
- Collapsed: 64px (icon only, with tooltip on hover showing label)

### Sidebar Structure

```
+---------------------------+
| [logo] FinanceTracker     |  <- 56px header
|---------------------------|
| [LayoutDashboard] Dashboard  |  <- nav item
| [Wallet] Accounts           |
| [ArrowLeftRight] Transactions|
| [PieChart] Budgets          |
| [Tag] Categories            |
|                             |
|         (spacer flex-grow)  |
|                             |
| [Avatar] First Last         |  <- user section
| [Settings] Settings         |
| [LogOut] Sign Out           |
+---------------------------+
```

**Nav item states**:
- Default: icon --color-neutral-500, label --color-neutral-600
- Hover: background --color-neutral-100, icon --color-neutral-700
- Active/current: background --color-brand-50, icon --color-brand-600, label --color-brand-700, left accent bar (3px --color-brand-600)
- Focus: --focus-ring

**Collapsed sidebar (64px)**:
- Labels hidden; icons centered
- Tooltip on hover/focus: label in popover

**HTML**:
```html
<nav aria-label="Primary navigation">
  <ul role="list">
    <li>
      <a href="/dashboard" aria-current={isActive('/dashboard') ? 'page' : undefined}>
        <DashboardIcon aria-hidden="true" />
        <span class="nav-label">Dashboard</span>
      </a>
    </li>
    <!-- ... -->
  </ul>
</nav>
```

### BottomTabBar (Mobile, < 768px)

**Height**: 64px (includes safe area inset on iOS for home indicator)
**Structure**: Fixed to bottom, 5 items maximum

```
+------------------------------------------+
| [Home]   [Wallet]   [+FAB]  [Pie]  [More] |
| Dashboard Accounts  Add   Budgets  More   |
+------------------------------------------+
```

**Center item**: Floating Action Button for "Add Transaction" — 56x56px, elevated, --color-brand-600 background.

**The "More" tab**: Opens a bottom sheet or slide-in menu for:
- Categories
- Settings
- Sign Out

**Tab item states**:
- Default: icon --color-neutral-400, label --text-xs --color-neutral-400
- Active: icon --color-brand-600, label --text-xs --color-brand-600
- Focus: --focus-ring on tab button

**HTML (Mobile)**:
```html
<nav aria-label="Primary navigation" class="bottom-tab-bar">
  <a href="/dashboard" aria-label="Dashboard" aria-current={isActive ? 'page' : undefined}>
    <DashboardIcon aria-hidden="true" />
    <span>Dashboard</span>
  </a>
  <!-- ... -->
  <button aria-label="Add transaction" class="fab-tab">
    <PlusIcon aria-hidden="true" />
  </button>
  <!-- ... -->
</nav>
```

### React Native Adaptation

Use `@react-navigation/bottom-tabs` for the BottomTabBar. The sidebar pattern becomes a Drawer navigator on tablet breakpoints. FAB implemented as a custom `tabBarButton` on the center tab.

---

## Component 10: DashboardWidget

**Purpose**: A contained informational card used on the Dashboard to display summaries.

### Variants

- `BalanceWidget` — Net Worth or single account balance
- `CashFlowWidget` — Income / Expense / Net for a period
- `TopExpensesWidget` — Ranked expense category list
- `RecentTransactionsWidget` — Last N transactions

### Common Props

| Prop | Type | Required | Description |
|---|---|---|---|
| title | string | Yes | Widget heading |
| isLoading | boolean | No | Shows skeleton loader |
| error | string | No | Shows error state |
| onViewAll | () => void | No | "View all" link handler |

### BalanceWidget Props

| Prop | Type | Description |
|---|---|---|
| primaryLabel | string | e.g., "Net Worth" |
| primaryAmount | string | BigDecimal string |
| primaryIsPositive | boolean | True for net worth > 0 |
| rows | {label, amount}[] | Sub-rows (Assets, Liabilities) |

### Visual Structure — BalanceWidget

```
+------------------------------+
| Net Worth                    |
|                              |
|  $12,450.00       [color]    |
|  ──────────────              |
|  Assets      $15,450.00      |
|  Liabilities  $3,000.00      |
+------------------------------+
```

### CashFlowWidget Structure

```
+------------------------------+
| Cash Flow — February 2026   |
|------------------------------|
| Income      +$3,200.00 [grn] |
| Expenses    -$1,850.00 [red] |
|------------------------------|
| Net Flow    +$1,350.00 [grn] |
+------------------------------+
```

### Loading Skeleton

```
+------------------------------+
| [## skeleton title ##]       |
|  [########### 200px #######] |
|  [####### 120px ######]      |
|  [####### 120px ######]      |
+------------------------------+
```

Skeleton lines: background --color-neutral-200, shimmer animation.

### Error State

```
+------------------------------+
| Net Worth                    |
|  [AlertTriangle icon]        |
|  Unable to load data.        |
|  [ Retry ]                   |
+------------------------------+
```

### Accessibility

- Widget container: `<section aria-labelledby="{widget-id}-title">`
- `<h2 id="{widget-id}-title">{title}</h2>`
- Primary amount: `aria-label="Net worth: twelve thousand four hundred fifty dollars"`
- Loading: `aria-busy="true"` on section; `aria-label="Loading net worth data"`

---

## Component 11: SkeletonLoader

**Purpose**: Content placeholder during data fetching.

### Props

| Prop | Type | Default | Description |
|---|---|---|---|
| width | string or number | "100%" | Width of skeleton |
| height | string or number | 16px | Height of skeleton |
| borderRadius | string | "4px" | |
| count | number | 1 | Number of skeleton lines |
| gap | number | 8 | Gap between lines in px |

### Animation

```css
@keyframes shimmer {
  0%   { background-position: -200% 0; }
  100% { background-position:  200% 0; }
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-neutral-200) 25%,
    var(--color-neutral-100) 50%,
    var(--color-neutral-200) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
```

### Accessibility

```html
<div aria-hidden="true" class="skeleton-loader">
  <!-- skeleton placeholders are decorative; real content announced when loaded -->
</div>
```

The loading container: `aria-busy="true" aria-label="Loading [content description]"`.

---

## Component 12: StatusBadge

**Purpose**: Inline label for budget status, account type, or category type.

### Props

| Prop | Type | Required | Description |
|---|---|---|---|
| label | string | Yes | Display text |
| variant | "success" or "warning" or "error" or "info" or "neutral" | Yes | |
| size | "sm" or "md" | No | |
| icon | LucideIcon | No | Optional leading icon |

### Variant Map

```
success: bg --color-income-100,  text --color-income-700
warning: bg --color-liability-100, text --color-liability-700
error:   bg --color-expense-100,  text --color-expense-700
info:    bg --color-brand-100,   text --color-brand-700
neutral: bg --color-neutral-100, text --color-neutral-700
```

Border radius: --radius-full (pill shape)
Padding: 2px 8px (sm), 4px 12px (md)
Font: --text-xs, --font-weight-medium, letter-spacing: --tracking-wide

### Usage

```
Budget Status: <StatusBadge label="On Track" variant="success" icon={CheckCircle2} />
Budget Status: <StatusBadge label="Warning" variant="warning" icon={AlertTriangle} />
Budget Status: <StatusBadge label="Over Budget" variant="error" icon={XCircle} />
Account Type:  <StatusBadge label="Checking" variant="neutral" />
```

### Accessibility

Badge is purely informational. The containing element (row/card) carries the accessible meaning. Badge itself: `aria-hidden="true"` if the text is already conveyed by the row's `aria-label`. Alternatively, include the status text in the row's accessible label and keep badge visible only.

---

## Component Dependency Graph

```
AmountInput
  └── used in: AddTransactionForm, AddAccountForm, TransferForm, AddBudgetForm

AccountCard
  └── used in: AccountsList, Dashboard (compact)

TransactionRow
  └── used in: TransactionsList, AccountDetail, Dashboard (recent)

CategoryPicker
  └── used in: AddTransactionForm, AddBudgetForm, AddCategoryForm (parent selection)

BudgetProgressBar
  └── used in: BudgetsList, Dashboard (mini in TopExpenses)

DateRangePicker
  └── used in: TransactionFilter, AddBudgetForm (custom period)

ConfirmationDialog
  └── used in: AccountsList (deactivate), TransactionRow (delete), BudgetsList (deactivate)

EmptyState
  └── used in: AccountsList, TransactionsList, BudgetsList, Categories

NavigationSidebar / BottomTabBar
  └── used in: AppShell (layout wrapper)

DashboardWidget
  └── used in: Dashboard

SkeletonLoader
  └── used in: all widgets and list pages during data fetch

StatusBadge
  └── used in: BudgetProgressBar, AccountCard, TransactionRow (transfer indicator)
```
