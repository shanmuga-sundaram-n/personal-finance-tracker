# UX/UI Designer — Persistent Memory
**Project**: Personal Finance Tracker
**Last updated**: 2026-03-26

---

## Project Summary

Spring Boot 3.2.2 / Java 17 backend. Frontend is React (web) first; React Native mobile is planned but not built yet. All design decisions must support both platforms from the same token system.

---

## Design System Key Decisions

- **Brand color**: Indigo (#6366F1 / --color-brand-500). Chosen to avoid confusion with semantic red/green.
- **Income = Green** (#16A34A / --color-income-600), **Expense = Red** (#DC2626 / --color-expense-600), **Liability = Amber** (#D97706 / --color-liability-600)
- **Base grid**: 4px. Most spacing uses 8px multiples. Standard card padding = 24px (--space-6).
- **Icon library**: Lucide Icons (MIT; works on both React web and React Native via lucide-react-native).
- **Font**: Inter (web) / SF Pro Display (iOS native fallback). Tabular-nums on all amount columns.
- **Dark mode**: Token mapping defined in design-system.md. MVP is light-only. Implement via [data-theme="dark"] on html.
- **Money display**: Always string format from API (BigDecimal as string per ADR-009). Never use JS float math on amounts.

## Breakpoints

- < 768px: mobile (BottomTabBar nav)
- 768–1023px: tablet (64px collapsed sidebar)
- 1024–1279px: desktop small (64px collapsed sidebar)
- 1280px+: desktop full (240px expanded sidebar)

## Component Library Status

10 core components specified in component-library.md:
AmountInput, AccountCard, TransactionRow, CategoryPicker, BudgetProgressBar, DateRangePicker, ConfirmationDialog, EmptyState, NavigationSidebar/BottomTabBar, DashboardWidget.

## Screen Count

13 screens fully specified in screen-specs.md:
Registration, Login, Dashboard, Accounts List, Add/Edit Account, Account Detail, Transactions List, Add/Edit Transaction, Transfer Form, Categories, Add/Edit Category, Budgets, Add/Edit Budget.

## Key UX Rules

- NEVER communicate information by color alone — always pair with text/icon (WCAG 1.4.1)
- All lists must have designed empty states with a CTA
- Use skeleton loaders (not spinners) for data fetching
- Disable submit buttons during API calls to prevent double-submit
- Form errors: field-level inline (via aria-describedby), API errors as top-of-form banner
- Session expiry 401: redirect to /login with "Your session has expired" message
- No hard-coded prices/amounts — always format from BigDecimal string using locale formatter

## Navigation Architecture

- React Router v6 with nested routes inside AuthGuard
- AppShell layout: sidebar + main content on desktop; top bar + main + bottom tabs on mobile
- "Add Transaction" FAB always visible (center tab on mobile, button in toolbar on desktop)
- Deep link query params for pre-filling forms: ?accountId=, ?type=transfer, ?categoryId=

## Accessibility Commitments (WCAG 2.1 AA)

- All inputs: visible label (not placeholder-only)
- Focus management: on route change, focus moves to page h1 (tabIndex=-1)
- Skip-to-content link at top of DOM
- Progress bars: role="progressbar" aria-valuenow/min/max/label
- Dialogs: role="dialog" aria-modal focus trap, return focus on close
- Category picker: full combobox ARIA pattern
- Amounts: aria-label includes currency context (not just the number)

## Implemented Patterns (confirmed in production code)

- **Dashboard card style**: `border-{color}-500/20 bg-gradient-to-br from-{color}-500/10 to-{alt}-500/5` + icon in `h-9 w-9 rounded-lg bg-{color}-500/15`
- **Section header accent**: colored left-border (`border-l-4 border-{color}-500 pl-3`) + icon + colored title text — used in BudgetPlanPage income/expense sections
- **Stat cell pattern**: `text-[11px] font-medium uppercase tracking-wide text-muted-foreground` label above `text-sm font-semibold tabular-nums` value
- **Progress bar**: full-width `h-2 rounded-full bg-muted`, fill with `transition-all duration-500`, always `role="progressbar" aria-valuenow/min/max/label`
- **Percentage badge**: `inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-semibold` with semantic bg/text color pair
- **Skeleton loader**: `animate-pulse rounded bg-muted` divs matching real layout shape — NOT a spinner
- **Empty state**: icon in colored circle + heading + body + CTA Button with `asChild` Link — see BudgetPlanPage `EmptySection`
- **Touch target minimum**: 44px — month nav buttons use `h-11 w-11`; avoid `h-9 w-9` (36px) on primary interactive controls
- **Budget row "Set vs Edit" pattern**: `variant="outline"` + Plus icon for `hasBudget=false`; `variant="secondary"` + Pencil icon for `hasBudget=true`. Both h-11 (44px touch target). Applied to both income and expense rows.
- **Expense parent header row**: `border-l-4 border-red-500 bg-muted/50 rounded-sm` with bold name + group monthly/yr totals on right. Child rows indented `pl-8`.
- **SetBudgetDialog title**: Always shows `categoryName` as `DialogTitle`, not a generic "Set Budget" string. Subtitle (DialogDescription) is contextual: "Update the budget…" vs "Set a budget…" based on `existingBudgetId !== null`. SelectTrigger and Input use `h-11` for 44px touch targets.
- **BudgetPlanPage is planning-only** (as of 2026-03-19): No Actual/Variance/% Used columns. Columns: Category | Frequency | Monthly | Yearly | Action. No progress bars. No TotalsRow component — section totals are inline in the section header (`SectionHeader` component) as `{fmt}/mo  {fmt}/yr` on the right side. Summary cards show Planned Income, Planned Spending, Planned Net (each with monthly primary + yearly secondary text). Safe accessor functions `getTotalsMonthly` / `getTotalsYearly` handle future `totalMonthly`/`totalYearly` fields from API with `totalBudgeted` fallback (cast via `as unknown as Record<string, unknown>`).
- **EditableAmountCell**: file still exists at `frontend/src/components/budgets/EditableAmountCell.tsx` but is NOT used in BudgetPlanPage (inline editing was replaced by SetBudgetDialog). Do not re-introduce it into BudgetPlanPage.
- **Info callout banner**: `flex items-start gap-3 rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700` + `Info` icon (`h-4 w-4 shrink-0 mt-0.5 text-slate-500`) — used for Transfer section notice in CategoriesListPage. Dark mode: `dark:border-slate-700 dark:bg-slate-800/40 dark:text-slate-300`.
- **Section header (list page variant)**: `flex items-center gap-2 border-l-4 border-{color} pl-3 mb-2` + small icon + `text-xs font-semibold uppercase tracking-wide text-foreground/70` — used in CategoriesListPage system/custom sections.
- **Color input in dialogs**: always `h-11` (44px); pair with a separate preview swatch `h-11 w-11 rounded-md border border-input shadow-sm` reflecting current value live. Add `aria-label="Choose category color"` to the `<input type="color">`.
- **Lock icon for system rows**: wrap in `<span role="img" aria-label="System category — cannot be edited" className="flex h-11 w-11 items-center justify-center">` — do NOT put aria-label directly on the SVG icon.
- **Budgeted badge mobile fallback**: badge is `hidden sm:flex`; add a `<span className="h-2 w-2 rounded-full bg-green-600 sm:hidden" role="img" aria-label="Has active budget" />` alongside it so the info is conveyed on mobile without relying on color alone (WCAG 1.4.1).
- **Category row expand/collapse**: parent rows use a chevron toggle button (`h-8 w-8 rounded` ghost-style) that sets `aria-expanded`. When parent has no children, show the colour dot inside the same button slot. Default state is expanded; collapsed IDs tracked in a `Set<number>` state. During search, force-expand all rows to show matches.
- **Row action visibility**: use `opacity-0 group-hover:opacity-100 focus-visible:opacity-100 transition-opacity` on Pencil/Trash/FolderPlus buttons. This declutters the list at rest while keeping actions fully keyboard-accessible. Always set `group` on the row `div`.
- **Delete dialog contextual title**: always include the category name: `Delete "{name}"?` — never a generic "Delete Category". Store `{ id, name }` in `deleteTarget` state instead of just `id`.
- **Add-row affordance at bottom of list**: a dashed-border button `border-dashed border-muted-foreground/30` at the end of the list reinforces discoverability for adding new items without requiring users to scroll back to the header.
- **Stat card label style**: `text-[11px] font-medium uppercase tracking-wide text-muted-foreground` above `text-2xl font-bold tabular-nums` — NOT text-3xl (reserved for monetary hero figures per design system).
- **Tab trigger colour dot**: `inline-block h-2 w-2 rounded-full bg-red-500` (or green) inside the TabsTrigger alongside the label text, `aria-hidden="true"`. Pairs colour with text per WCAG 1.4.1 and makes tabs scan faster.
- **Search clear button**: when search has content, render an `X` icon button `absolute right-3` inside the search input wrapper with `aria-label="Clear search"`. Use `pr-9` on the Input to prevent text overlapping the clear button.
- **SectionHeader component (CategoriesListPage)**: separates "Your categories" (custom) from "System categories" in the same tab. Uses `border-l-4` accent + small icon (FolderOpen for custom, Lock for system) + `text-xs font-semibold uppercase tracking-wide text-foreground/60` label + muted count. Hidden during search (show flat results instead).
- **DialogDescription required**: every Dialog must include a `DialogDescription` (even one sentence) — this satisfies the `aria-describedby` association that some screen readers require and avoids shadcn/ui console warnings.
- **CardTitle renders as `<div>`, not `<h3>`**: changed `frontend/src/components/ui/card.tsx` to emit `<div>` instead of `<h3>`. All page sections already use `<h1>` directly; card section labels are visual affordances, not document-outline headings. Using `<h3>` skipped heading levels (h1 → h3) — WCAG 2.1 SC 1.3.1 violation.
- **CategorySelect requires `id` prop for label association**: `SelectTrigger` must receive `id` so that a `<Label htmlFor="…">` can point to it. All three usages updated: CreateTransactionPage (`id="categoryId"`), EditTransactionPage (`id="categoryId"`), TransactionsListPage filter (`id="filter-category"`).
- **CategorySelect `aria-describedby` prop**: added to `CategorySelectProps` interface and forwarded to `SelectTrigger`. Pass `errors.categoryId ? 'categoryId-error' : undefined` from parent forms.
- **Test infrastructure (F-4 accessibility)**: vitest configured in `vite.config.ts` (via `vitest/config`), `test` script added to `package.json`. Setup file at `frontend/src/test/setup.ts` registers `toHaveNoViolations` via `expect.extend({ toHaveNoViolations })` from `vitest-axe/matchers`. Test files excluded from production `tsconfig.app.json` via `exclude` array; `tsconfig.test.json` created for test-only type checking. Two test files: `DashboardPage.test.tsx` (4 tests — heading, chart role/aria-label, progressbar ARIA, axe scan) and `CreateTransactionPage.test.tsx` (6 tests — aria-describedby on inputs, aria-invalid, error message IDs, axe scan). All 10 pass.

## Pipeline Entry & Health Gate
All tracks enter via `engineering-manager`. ux-ui-designer is spawned at Phase 2B (design spec)
and Phase 4B (WCAG 2.1 AA accessibility + mobile review).

After frontend changes, `engineering-manager` runs:
```bash
.claude/hooks/verify-app-health.sh --quick   # layers 4+5: containers + HTTP responding
```
Also verify: `npm run build` (Layer 3) and `npm run test` (axe-core tests: DashboardPage + CreateTransactionPage — 10 tests must pass).

## Files Reference

- design-system.md — tokens, colors, spacing, typography, dark mode mapping
- screen-specs.md — 13 screen wireframes, data fields, all states
- user-flows.md — 7 flows with decision points and error branches
- component-library.md — 12 components with props, states, ARIA, mobile notes
- navigation.md — sitemap, React Router config, sidebar/bottomtabs, focus management
