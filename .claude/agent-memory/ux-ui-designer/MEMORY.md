# UX/UI Designer — Persistent Memory
**Project**: Personal Finance Tracker
**Last updated**: 2026-02-28

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

## Files Reference

- design-system.md — tokens, colors, spacing, typography, dark mode mapping
- screen-specs.md — 13 screen wireframes, data fields, all states
- user-flows.md — 7 flows with decision points and error branches
- component-library.md — 12 components with props, states, ARIA, mobile notes
- navigation.md — sitemap, React Router config, sidebar/bottomtabs, focus management
