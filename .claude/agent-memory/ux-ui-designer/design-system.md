# Design System — Personal Finance Tracker
**Author**: UX/UI Designer
**Date**: 2026-02-28
**Status**: Authoritative — all frontend developers build from this document

---

## 0. Guiding Principles

1. **Clarity over cleverness** — every pixel earns its place; finance data must be scannable at a glance
2. **Numbers are the hero** — typography hierarchy serves the monetary figures, not the chrome
3. **Color carries meaning, never alone** — green/red/amber always paired with text or icon per WCAG 1.4.1
4. **Mobile-first, platform-agnostic tokens** — the token system maps to CSS custom properties on web and React Native StyleSheet on mobile with no structural changes
5. **Progressive disclosure** — show summaries first; details on demand

---

## 1. Color Palette

### 1.1 Brand Colors

```
--color-brand-50:   #EEF2FF   /* lightest tint — backgrounds */
--color-brand-100:  #E0E7FF
--color-brand-200:  #C7D2FE
--color-brand-300:  #A5B4FC
--color-brand-400:  #818CF8
--color-brand-500:  #6366F1   /* primary brand — Indigo */
--color-brand-600:  #4F46E5   /* primary interactive */
--color-brand-700:  #4338CA   /* pressed states */
--color-brand-800:  #3730A3
--color-brand-900:  #312E81   /* dark text on light backgrounds */
```

Rationale: Indigo reads as trustworthy and modern — distinct from the red/green used for financial semantics, so there is no ambiguity between brand and data meaning. Indigo also passes WCAG AA contrast at 600+ on white.

### 1.2 Semantic — Income (Green)

```
--color-income-50:  #F0FDF4
--color-income-100: #DCFCE7
--color-income-200: #BBF7D0
--color-income-500: #22C55E   /* label text on white */
--color-income-600: #16A34A   /* primary income text — 4.5:1 on white */
--color-income-700: #15803D   /* pressed / dark mode */
--color-income-bg:  #F0FDF4   /* row tint backgrounds */
```

### 1.3 Semantic — Expense (Red)

```
--color-expense-50:  #FFF1F2
--color-expense-100: #FFE4E6
--color-expense-200: #FECDD3
--color-expense-500: #EF4444
--color-expense-600: #DC2626   /* primary expense text — 4.5:1 on white */
--color-expense-700: #B91C1C   /* pressed / dark mode */
--color-expense-bg:  #FFF1F2   /* row tint backgrounds */
```

### 1.4 Semantic — Liability / Warning (Amber)

```
--color-liability-50:  #FFFBEB
--color-liability-100: #FEF3C7
--color-liability-400: #FBBF24
--color-liability-500: #F59E0B
--color-liability-600: #D97706   /* 4.5:1 on white */
--color-liability-700: #B45309
--color-liability-bg:  #FFFBEB
```

Used for: credit card balances, budget WARNING state, liability account type badges.

### 1.5 Semantic — Transfer (Blue-Slate)

```
--color-transfer-500: #64748B
--color-transfer-600: #475569   /* 4.5:1 on white */
--color-transfer-bg:  #F8FAFC
```

### 1.6 Budget Status Colors

```
ON_TRACK:    --color-income-600    (#16A34A)
WARNING:     --color-liability-600 (#D97706)
OVER_BUDGET: --color-expense-600   (#DC2626)
```

### 1.7 Neutral Scale (Grays)

```
--color-neutral-0:   #FFFFFF
--color-neutral-50:  #F9FAFB
--color-neutral-100: #F3F4F6
--color-neutral-200: #E5E7EB
--color-neutral-300: #D1D5DB
--color-neutral-400: #9CA3AF   /* placeholder text */
--color-neutral-500: #6B7280   /* secondary text */
--color-neutral-600: #4B5563   /* body text */
--color-neutral-700: #374151   /* strong body */
--color-neutral-800: #1F2937   /* headings */
--color-neutral-900: #111827   /* darkest text */
```

### 1.8 Dark Mode Token Mapping

| Light Token | Dark Equivalent |
|---|---|
| --color-neutral-0 (white bg) | #0F172A |
| --color-neutral-50 (page bg) | #1E293B |
| --color-neutral-100 (card bg) | #334155 |
| --color-neutral-200 (border) | #475569 |
| --color-neutral-800 (heading) | #F1F5F9 |
| --color-neutral-600 (body) | #CBD5E1 |
| --color-brand-500 | #818CF8 (lighter for dark bg contrast) |
| --color-income-600 | #4ADE80 |
| --color-expense-600 | #F87171 |
| --color-liability-600 | #FCD34D |

Implementation note: Expose all colors as CSS custom properties on `:root`. Toggle dark mode via `[data-theme="dark"]` on `<html>`. On React Native, create a `useColorScheme()` hook that returns the appropriate token set.

---

## 2. Typography Scale

### 2.1 Font Families

```
--font-family-sans: 'Inter', 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-family-mono: 'JetBrains Mono', 'SF Mono', 'Fira Code', monospace;
```

Inter is used for all UI text. The mono family is reserved for account numbers, reference numbers, and raw amount inputs only.

### 2.2 Type Scale (4pt baseline, 4:5 modular scale rounded to whole px)

| Token | Size | Line Height | Weight | Use |
|---|---|---|---|---|
| --text-xs | 12px | 16px | 400 | Captions, helper text, timestamps |
| --text-sm | 14px | 20px | 400 | Secondary body, labels, table data |
| --text-base | 16px | 24px | 400 | Primary body, form inputs |
| --text-lg | 18px | 28px | 500 | Card section headings |
| --text-xl | 20px | 28px | 600 | Page sub-headings |
| --text-2xl | 24px | 32px | 600 | Widget primary figures |
| --text-3xl | 30px | 36px | 700 | Dashboard net worth (primary hero) |
| --text-4xl | 36px | 40px | 700 | Account detail current balance |

### 2.3 Font Weight Tokens

```
--font-weight-regular:  400
--font-weight-medium:   500
--font-weight-semibold: 600
--font-weight-bold:     700
```

### 2.4 Letter Spacing

```
--tracking-tight:  -0.025em   /* large hero numbers */
--tracking-normal: 0em        /* body text */
--tracking-wide:   0.05em     /* badge labels, ALL-CAPS status text */
```

### 2.5 Money Display Rules

- Always use tabular-nums (`font-variant-numeric: tabular-nums`) on amount columns so decimal points align
- Positive amounts: prefix "+", color --color-income-600
- Negative / expense amounts: prefix "-", color --color-expense-600
- Liability amounts: amber, label "Amount Owed" appended
- Never display "NaN", "null", "undefined" — show "--" as fallback
- Decimal places: always 2 for display regardless of stored 4dp precision
- Thousands separator: locale-aware (en-US: comma; en-IN: South Asian grouping)

---

## 3. Spacing System

### 3.1 Base Grid: 4px

All spacing values are multiples of 4px. The 8px unit is the canonical rhythm unit — most spacing is 8, 16, 24, or 32.

```
--space-0:   0px
--space-1:   4px
--space-2:   8px
--space-3:   12px
--space-4:   16px
--space-5:   20px
--space-6:   24px
--space-8:   32px
--space-10:  40px
--space-12:  48px
--space-16:  64px
--space-20:  80px
--space-24:  96px
```

### 3.2 Component Spacing Application

| Context | Value |
|---|---|
| Inline element gap (icon + label) | --space-2 (8px) |
| Form field vertical gap | --space-4 (16px) |
| Card internal padding | --space-6 (24px) |
| Section vertical gap | --space-8 (32px) |
| Page horizontal padding (mobile) | --space-4 (16px) |
| Page horizontal padding (tablet+) | --space-6 (24px) |
| List row height (transaction row) | 56px minimum (meets 44px touch target with padding) |
| Sidebar width (desktop) | 240px |
| Bottom tab bar height (mobile) | 64px |

---

## 4. Component Tokens

### 4.1 Border Radius

```
--radius-sm:   4px    /* badges, chips, small buttons */
--radius-md:   8px    /* inputs, cards */
--radius-lg:   12px   /* modals, dropdown panels */
--radius-xl:   16px   /* bottom sheets on mobile */
--radius-full: 9999px /* pills, avatar circles, progress bars */
```

### 4.2 Shadows / Elevation

```
--shadow-xs:  0 1px 2px 0 rgba(0,0,0,0.05)
--shadow-sm:  0 1px 3px 0 rgba(0,0,0,0.10), 0 1px 2px -1px rgba(0,0,0,0.10)
--shadow-md:  0 4px 6px -1px rgba(0,0,0,0.10), 0 2px 4px -2px rgba(0,0,0,0.10)
--shadow-lg:  0 10px 15px -3px rgba(0,0,0,0.10), 0 4px 6px -4px rgba(0,0,0,0.10)
--shadow-xl:  0 20px 25px -5px rgba(0,0,0,0.10), 0 8px 10px -6px rgba(0,0,0,0.10)
```

Elevation usage:
- Page surface: no shadow (flat)
- Card / panel: --shadow-sm
- Sticky header: --shadow-md (applied on scroll)
- Dropdown / popover: --shadow-lg
- Modal / dialog: --shadow-xl
- FAB (Floating Action Button): --shadow-md

### 4.3 Border

```
--border-width: 1px
--border-color: var(--color-neutral-200)
--border-color-focus: var(--color-brand-500)
--border-color-error: var(--color-expense-600)
--border-color-success: var(--color-income-600)
```

### 4.4 Focus Ring

```
--focus-ring: 0 0 0 3px rgba(99, 102, 241, 0.35)
```

Applied to all interactive elements on :focus-visible. Never suppress outline entirely — use this styled ring instead.

### 4.5 Transition

```
--transition-fast:   100ms ease
--transition-base:   150ms ease
--transition-slow:   200ms ease-in-out
--transition-spring: 300ms cubic-bezier(0.34, 1.56, 0.64, 1)  /* for FAB, modals */
```

---

## 5. Responsive Breakpoints

### 5.1 Breakpoint Definitions (mobile-first)

```
--bp-xs:   320px    /* smallest phones */
--bp-sm:   480px    /* large phones (landscape) */
--bp-md:   768px    /* tablets / large phone landscape */
--bp-lg:   1024px   /* desktop minimum */
--bp-xl:   1280px   /* standard desktop */
--bp-2xl:  1440px   /* wide desktop */
```

CSS media query convention (min-width, mobile-first):
```css
/* Base styles: 320px+ (mobile) */
/* @media (min-width: 480px)  — sm: large phone */
/* @media (min-width: 768px)  — md: tablet */
/* @media (min-width: 1024px) — lg: desktop */
/* @media (min-width: 1440px) — 2xl: wide desktop */
```

### 5.2 Layout Behavior at Each Breakpoint

| Breakpoint | Navigation | Content Layout | Sidebar |
|---|---|---|---|
| <768px | Bottom tab bar | Single column, full width | Hidden |
| 768–1023px | Bottom tab bar | Single column, max-width 640px centered | Hidden |
| 1024–1279px | Sidebar (collapsed, 64px icon-only) | Main content, 2-col grids | 64px |
| 1280px+ | Sidebar (expanded, 240px with labels) | Main content, 2–3 col grids | 240px |

### 5.3 Fluid Type (optional enhancement)

For hero numbers (net worth, account balance), consider fluid sizing:
```css
font-size: clamp(24px, 4vw, 36px);
```

---

## 6. Iconography

Use Lucide Icons (open source, MIT, available for both React web and React Native via `lucide-react-native`). This ensures a single icon set across platforms.

Key icon assignments:

| Concept | Lucide Icon Name |
|---|---|
| Dashboard | LayoutDashboard |
| Accounts | Wallet |
| Transactions | ArrowLeftRight |
| Budgets | PieChart |
| Categories | Tag |
| Income | TrendingUp |
| Expense | TrendingDown |
| Transfer | Repeat |
| Add / New | Plus |
| Edit | Pencil |
| Delete | Trash2 |
| Settings / Profile | Settings |
| Lock (system category) | Lock |
| Warning | AlertTriangle |
| Success / On Track | CheckCircle2 |
| Error / Over Budget | XCircle |
| Search | Search |
| Filter | Filter |
| Chevron / Expand | ChevronRight |
| Checking Account | Building2 |
| Savings Account | Piggy Bank |
| Credit Card | CreditCard |
| Cash | Banknote |
| Digital Wallet | Smartphone |
| Investment | TrendingUp |
| Loan | Landmark |

Icon sizes:
```
--icon-sm:   16px   /* inline with small text */
--icon-md:   20px   /* standard UI icon */
--icon-lg:   24px   /* nav items, card headers */
--icon-xl:   32px   /* empty state illustrations */
--icon-2xl:  48px   /* empty state hero */
```

---

## 7. Z-Index Scale

```
--z-base:       0
--z-raised:     10
--z-dropdown:   100
--z-sticky:     200
--z-fixed:      300
--z-overlay:    400   /* modal backdrop */
--z-modal:      500
--z-toast:      600
--z-tooltip:    700
```

---

## 8. Animation & Motion

- Skeleton loaders: shimmer animation, 1.5s loop, left-to-right gradient sweep
- Page transitions: fade in 150ms (no sliding — avoid motion sickness in financial data)
- Toast notifications: slide in from top-right (desktop) or top (mobile), 300ms, auto-dismiss 4s
- Modal: fade backdrop + scale 0.95 to 1.0, 200ms
- Progress bar fill: transition width 400ms ease-out (on mount)
- Amount change (balance update): brief highlight flash (500ms yellow-to-transparent)

Respect `prefers-reduced-motion`:
```css
@media (prefers-reduced-motion: reduce) {
  * { animation-duration: 0.01ms !important; transition-duration: 0.01ms !important; }
}
```

---

## 9. Design Token Export Format

For implementation, tokens are exported as:

**Web (CSS Custom Properties) — `tokens.css`:**
```css
:root {
  --color-brand-600: #4F46E5;
  --space-4: 16px;
  --text-base: 16px;
  /* ... all tokens */
}
[data-theme="dark"] {
  --color-brand-600: #818CF8;
  /* ... dark overrides */
}
```

**React Native (`tokens.ts`):**
```typescript
export const tokens = {
  colors: {
    brand600: '#4F46E5',
    income600: '#16A34A',
    expense600: '#DC2626',
  },
  space: { 4: 16, 6: 24, 8: 32 },
  text: { base: 16, lg: 18 },
} as const;
```

Both derive from the same source-of-truth JSON (Style Dictionary or manual sync). When implementing, keep a `design-tokens.json` that both builds consume.
