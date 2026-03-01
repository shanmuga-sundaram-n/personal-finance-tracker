# Navigation Architecture — Personal Finance Tracker
**Author**: UX/UI Designer
**Date**: 2026-02-28
**Reference**: brief-for-ux-designer.md Section 3.1; component-library.md (NavigationSidebar, BottomTabBar)

---

## 1. Information Architecture (Sitemap)

```
/                             -> redirects to /dashboard (if auth) or /login (if not)
│
├── PUBLIC ROUTES (no auth required)
│   ├── /login
│   └── /register
│
└── PROTECTED ROUTES (require auth token)
    │
    ├── /dashboard                        <- PRIMARY HOME
    │
    ├── /accounts
    │   ├── /accounts/new                 <- Add Account form
    │   ├── /accounts/{id}                <- Account Detail
    │   ├── /accounts/{id}/edit           <- Edit Account form
    │   └── /accounts/{id}/transactions   <- Transactions filtered to one account
    │                                        (alias for /transactions?accountId={id})
    │
    ├── /transactions
    │   ├── /transactions/new             <- Add Transaction form
    │   └── /transactions/{id}/edit       <- Edit Transaction form
    │
    ├── /transfers
    │   └── /transfers/new                <- Transfer form
    │                                        (accessible via /transactions/new?type=transfer)
    │
    ├── /budgets
    │   ├── /budgets/new                  <- Add Budget form
    │   └── /budgets/{id}/edit            <- Edit Budget form
    │
    ├── /categories
    │   ├── /categories/new               <- Add Category form
    │   └── /categories/{id}/edit         <- Edit Category form
    │
    └── /settings                         <- Phase 2: profile, currency, preferences
```

---

## 2. Navigation Hierarchy

### Primary Navigation (5 items — always visible)

| Order | Label | Route | Icon |
|---|---|---|---|
| 1 | Dashboard | /dashboard | LayoutDashboard |
| 2 | Accounts | /accounts | Wallet |
| 3 | Transactions | /transactions | ArrowLeftRight |
| 4 | Budgets | /budgets | PieChart |
| 5 | Categories | /categories | Tag |

### Secondary Navigation (in sidebar footer / "More" menu on mobile)

| Label | Route | Icon |
|---|---|---|
| Settings | /settings | Settings |
| Sign Out | (action) | LogOut |

### Persistent Action

- **Add Transaction** button/FAB: accessible from every screen in the app (not just the nav). On desktop: "+ Add Transaction" button in top-right toolbar of every page. On mobile: FAB center button in BottomTabBar.

---

## 3. Desktop Navigation — Sidebar

### Breakpoint: >= 1024px

The sidebar is the primary navigation mechanism on desktop. It remains persistent on the left side of all protected routes.

### Expanded State (>= 1280px wide viewport)

**Width**: 240px fixed

```
+---------------------------+
| [logo]  FinanceTracker    |  height: 64px, border-bottom
|---------------------------|
|                           |
| [icon]  Dashboard         |  height: 48px, padding: 0 16px
| [icon]  Accounts          |
| [icon]  Transactions      |
| [icon]  Budgets           |
| [icon]  Categories        |
|                           |
|   (flex-grow spacer)      |
|                           |
| [avatar] First Last       |  height: 48px
| [icon]  Settings          |  height: 48px
| [icon]  Sign Out          |  height: 48px
+---------------------------+
```

### Collapsed State (1024px–1279px)

**Width**: 64px

```
+--------+
| [logo] |  logo icon only (no text), centered
|--------|
| [icon] |  Dashboard (tooltip on hover/focus)
| [icon] |  Accounts
| [icon] |  Transactions
| [icon] |  Budgets
| [icon] |  Categories
|        |
|   ...  |
| [ava]  |  avatar (40x40px)
| [icon] |  Settings
| [icon] |  Sign Out
+--------+
```

Icon-only sidebar shows label tooltip on hover (positioned right of icon) and on focus.

### Active Item Style

```css
.nav-item[aria-current="page"] {
  background-color: var(--color-brand-50);
  color: var(--color-brand-700);
  /* left accent bar */
  border-left: 3px solid var(--color-brand-600);
  padding-left: calc(var(--space-4) - 3px); /* compensate for border */
}
```

### React Router Integration (Web)

```tsx
// Using React Router v6
import { NavLink } from 'react-router-dom';

<NavLink
  to="/dashboard"
  className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}
  aria-current={({ isActive }) => isActive ? 'page' : undefined}
>
  <LayoutDashboardIcon aria-hidden="true" />
  <span className="nav-label">Dashboard</span>
</NavLink>
```

`NavLink` sets `aria-current="page"` automatically through the className callback pattern.

---

## 4. Mobile Navigation — Bottom Tab Bar

### Breakpoint: < 768px

### Layout

```
+-----------------------------------------------+
|  [icon] [icon]  [FAB +]  [icon]  [icon]       |
|  Dash   Accts   (Add)    Budgets  More         |
+-----------------------------------------------+
height: 64px + safe area (iOS home indicator ~34px)
```

### Tab Items

| Position | Label | Route | Icon | Notes |
|---|---|---|---|---|
| 1 (left) | Dashboard | /dashboard | LayoutDashboard | |
| 2 | Accounts | /accounts | Wallet | |
| 3 (center) | Add | — | Plus | FAB; opens AddTransaction modal or route |
| 4 | Budgets | /budgets | PieChart | |
| 5 (right) | More | — | MoreHorizontal | Opens bottom sheet |

### FAB Center Tab

```css
.fab-tab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background-color: var(--color-brand-600);
  color: white;
  box-shadow: var(--shadow-md);
  /* Lifted above bar */
  margin-top: -16px;
}
```

Touch target: 56x56px (exceeds 44px minimum).

### "More" Bottom Sheet

Tapping the "More" tab opens a bottom sheet (slide up from bottom) containing:

```
+----------------------------------+
|  [handle bar]                    |
|                                  |
|  [Tag]      Categories     [>]   |
|  [Settings] Settings       [>]   |
|  [LogOut]   Sign Out             |
|                                  |
|  (close on backdrop tap)         |
+----------------------------------+
```

### Tablet Middle-Ground (768px–1023px)

Between mobile and desktop breakpoints, show a collapsed icon-only sidebar (64px) rather than the bottom tab bar. This avoids the "accordion" effect on tablets in landscape.

### React Navigation Integration (Future Mobile)

```
// React Navigation v7 structure (for when React Native is built)

Root Stack:
  - Auth Stack: Login, Register
  - Main Tabs (BottomTabNavigator):
      - Tab 1 - Dashboard Stack: Dashboard
      - Tab 2 - Accounts Stack: AccountsList, AccountDetail, AddEditAccount
      - Tab 3 - Add (modal trigger — no stack)
      - Tab 4 - Budgets Stack: BudgetsList, AddEditBudget
      - Tab 5 - More Stack: Categories, CategoryDetail, Settings

Modal Stack (presented over tabs):
  - AddTransaction (modal)
  - Transfer (modal)
  - ConfirmationDialog (modal)
```

---

## 5. Deep Linking Patterns

### Web (React Router)

All routes are linkable. URL structure is meaningful to users but does not expose internal IDs in human-facing paths where possible. In MVP, IDs in paths are acceptable.

| URL Pattern | Screen | Notes |
|---|---|---|
| /dashboard | Dashboard | Home after login |
| /accounts | Accounts List | |
| /accounts/new | Add Account | |
| /accounts/42 | Account Detail (id=42) | |
| /accounts/42/edit | Edit Account (id=42) | |
| /transactions | Transactions List | |
| /transactions/new | Add Transaction | |
| /transactions/new?type=transfer | Transfer Form | Query param morphs form |
| /transactions/new?accountId=42 | Add Tx (account pre-filled) | From account detail |
| /transactions/99/edit | Edit Transaction (id=99) | |
| /budgets | Budgets List | |
| /budgets/new?categoryId=5 | Add Budget (category pre-filled) | From unbudgeted section |
| /categories | Categories | |

### Pre-fill Pattern

Forms support pre-filling via URL query parameters. This enables deep-links from one screen to a related form:

- `/transactions/new?accountId=42` — pre-fills the account dropdown
- `/transactions/new?type=transfer&from=42` — opens transfer form with from-account set
- `/budgets/new?categoryId=8` — opens budget form with category pre-selected

### React Router Route Configuration

```tsx
// routes/index.tsx
import { createBrowserRouter } from 'react-router-dom';

export const router = createBrowserRouter([
  { path: '/login',    element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: <AuthGuard><AppShell /></AuthGuard>,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <Dashboard /> },

      // Accounts
      { path: 'accounts', element: <AccountsList /> },
      { path: 'accounts/new', element: <AddEditAccount /> },
      { path: 'accounts/:id', element: <AccountDetail /> },
      { path: 'accounts/:id/edit', element: <AddEditAccount /> },

      // Transactions
      { path: 'transactions', element: <TransactionsList /> },
      { path: 'transactions/new', element: <AddEditTransaction /> },
      { path: 'transactions/:id/edit', element: <AddEditTransaction /> },

      // Transfers (separate controller on backend, same form morphs)
      { path: 'transfers/new', element: <AddEditTransaction defaultType="TRANSFER" /> },

      // Budgets
      { path: 'budgets', element: <BudgetsList /> },
      { path: 'budgets/new', element: <AddEditBudget /> },
      { path: 'budgets/:id/edit', element: <AddEditBudget /> },

      // Categories
      { path: 'categories', element: <CategoriesList /> },
      { path: 'categories/new', element: <AddEditCategory /> },
      { path: 'categories/:id/edit', element: <AddEditCategory /> },

      // Settings (Phase 2)
      { path: 'settings', element: <Settings /> },
    ],
  },
  { path: '*', element: <NotFound /> },
]);
```

### AuthGuard Component

```tsx
// Wraps all protected routes
function AuthGuard({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const location = useLocation();

  if (!token) {
    // Store intended URL so login can redirect back
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
```

### Post-login Redirect

```tsx
// LoginPage.tsx
const navigate = useNavigate();
const location = useLocation();

const from = location.state?.from?.pathname || '/dashboard';

// On successful login:
navigate(from, { replace: true });
```

---

## 6. AppShell Layout

The AppShell is the root layout component for all protected routes. It renders the navigation and main content area.

### Desktop Layout Structure

```
+-----------------------------------------------+
| [NavigationSidebar - 240px or 64px]            |
|  +--------------------------------------------+|
|  | [TopBar: page title + actions]             ||
|  |--------------------------------------------|
|  | [main content area — scrollable]           ||
|  |                                            ||
|  | [rendered route child component]           ||
|  |                                            ||
|  +--------------------------------------------+|
+-----------------------------------------------+
```

### Mobile Layout Structure

```
+---------------------------+
| [TopBar: title + actions] |  height: 56px, sticky
|---------------------------|
| [main content area]       |  scrollable, padding-bottom: 80px (above tab bar)
|                           |
|                           |
|---------------------------|
| [BottomTabBar]            |  height: 64px + safe area, fixed bottom
+---------------------------+
```

### CSS Grid Layout (Desktop)

```css
.app-shell {
  display: grid;
  grid-template-columns: var(--sidebar-width, 240px) 1fr;
  grid-template-rows: 1fr;
  min-height: 100vh;
}

@media (max-width: 1279px) {
  .app-shell { --sidebar-width: 64px; }
}

@media (max-width: 767px) {
  .app-shell {
    grid-template-columns: 1fr;
    grid-template-rows: 56px 1fr 64px;
  }
}
```

### TopBar Component

**Desktop**: Shows page title (`<h1>`) and contextual actions (e.g., "+ Add Transaction" button).
**Mobile**: Shows page title and a menu button (if needed for overflow actions).

---

## 7. Focus Management on Navigation

### Page Transitions

When navigating to a new route, focus should move to the page `<h1>` element. This ensures screen reader users hear the new page name without needing to navigate the full page.

Implementation:
```tsx
// In a router-aware layout component
const location = useLocation();
const h1Ref = useRef<HTMLHeadingElement>(null);

useEffect(() => {
  h1Ref.current?.focus();
}, [location.pathname]);

// Each page renders: <h1 tabIndex={-1} ref={h1Ref}>{pageTitle}</h1>
```

`tabIndex={-1}` allows programmatic focus without adding to the tab order.

### Skip Navigation Link

A skip-to-content link must be present at the very top of the DOM, visible only on focus:

```html
<a href="#main-content" class="skip-link">Skip to main content</a>

<main id="main-content" tabindex="-1">
  <!-- route content -->
</main>
```

```css
.skip-link {
  position: absolute;
  top: -100px;
  left: 16px;
  padding: 8px 16px;
  background: var(--color-brand-600);
  color: white;
  border-radius: 4px;
  z-index: var(--z-fixed);
  transition: top 0.1s;
}
.skip-link:focus {
  top: 16px;
}
```

---

## 8. Scroll Behavior

### Scroll Restoration

When returning from a detail view to a list (e.g., Account Detail -> Accounts List), restore the previous scroll position.

React Router v6 does not restore scroll by default. Use `<ScrollRestoration />` component or implement custom:

```tsx
// In AppShell
import { ScrollRestoration } from 'react-router-dom';
<ScrollRestoration />
```

### List Scroll on Filter Change

When a filter changes on the Transactions list, scroll back to the top of the list:
```tsx
useEffect(() => {
  listRef.current?.scrollTo({ top: 0, behavior: 'smooth' });
}, [filterState]);
```

### Infinite Scroll vs Pagination

MVP uses **pagination** (not infinite scroll) for the following reasons:
1. Users need to find specific transactions by page (reconciliation workflow)
2. Predictable performance with large datasets
3. Simpler to implement with Spring Data Pageable
4. Deep linking to page N is meaningful ("page 3 of my January transactions")

Pagination component:
- Shows: `< Previous` | `Page 2 of 5` | `Next >`
- Accessible: `<nav aria-label="Transaction pagination">` with `aria-current="page"` on current page button
- Mobile: simplified `< 2 of 5 >` with larger touch targets
