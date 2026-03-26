# Frontend Design Spec: Budget Copy from Previous Month
**Author**: UX/UI Designer
**Date**: 2026-03-26
**Status**: Ready for implementation
**Feature Brief**: `.claude/agent-memory/engineering-manager/feature-briefs/budget-copy-from-previous-month.md`
**Pipeline Phase**: 2B — Frontend design & component spec
**Implements Acceptance Criteria**: AC-1 through AC-7 (frontend scope)

---

## 1. Overview

A single "Copy from Previous Month" button is added to the `BudgetPlanPage` header row, visible **only when the user is viewing the current calendar month**. The feature executes a two-step API flow: a dry-run first call (`overwriteExisting: false`) followed, when necessary, by an overwrite call (`overwriteExisting: true`) gated behind a confirmation dialog.

The interaction is designed to be **low-risk**: the button uses a non-primary (outline/secondary) variant to avoid accidental activation, the destructive path requires an explicit confirmation, and the safe path (no conflicts) proceeds silently with a success toast.

---

## 2. Page Header Changes

### 2.1 Current Header Structure (BudgetPlanPage, line 824–830)

```tsx
<div>
  <h1 className="text-3xl font-bold">Budget Plan</h1>
  <p className="text-muted-foreground text-sm mt-1">
    Set and review your planned income and spending per category
  </p>
</div>
```

### 2.2 New Header Structure

The title block and the action area sit in a single flex row. On mobile the title stacks above the button (column direction); on sm+ they sit inline (row, space-between).

```tsx
<div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
  {/* Left: title + subtitle */}
  <div>
    <h1 className="text-3xl font-bold">Budget Plan</h1>
    <p className="text-muted-foreground text-sm mt-1">
      Set and review your planned income and spending per category
    </p>
  </div>

  {/* Right: Copy button — visible only on current month */}
  {isCurrentMonth && (
    <CopyFromPreviousMonthButton
      isCopying={isCopying}
      onClick={handleCopyClick}
    />
  )}
</div>
```

### 2.3 Current-Month Detection

Computed from the existing `year` and `month` state variables in `BudgetPlanPage`. No new state needed for this check.

```ts
// Inside BudgetPlanPage, near existing useMemo calls
const isCurrentMonth = useMemo(() => {
  const now = new Date()
  return year === now.getFullYear() && month === now.getMonth()
}, [year, month])
```

`month` is 0-indexed (JavaScript `Date`), matching the existing `useState(now.getMonth())` initialisation.

---

## 3. Copy Button Component

### 3.1 Specification

**Component name**: `CopyFromPreviousMonthButton`
**File**: inline in `BudgetPlanPage.tsx` (no separate file needed — it is a single-use presentational wrapper around shadcn `Button`)

**Icon**: `Copy` from `lucide-react` — sized `h-4 w-4`, `aria-hidden="true"`, spaced `mr-2`

**Variant**: `outline` (secondary-risk affordance — avoids visual competition with the primary action pattern elsewhere on the page)

**Size**: `sm` (matches existing "Manage categories" button in `EmptySection` on line 491)

**Touch target**: `h-11` minimum (44px) — consistent with every interactive element on this page (month nav buttons, Set/Edit Budget buttons, delete buttons)

### 3.2 All Interaction States

| State | Visual |
|---|---|
| **Default** | Outline button, Copy icon + "Copy from Previous Month" label |
| **Hover** | `hover:bg-accent hover:text-accent-foreground` (shadcn outline default) |
| **Focus** | `focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2` (shadcn default) |
| **Loading** | Spinner (Loader2, animate-spin) replaces Copy icon; label becomes "Copying…"; button `disabled` |
| **Disabled (non-current month)** | Button is not rendered at all (conditional render, not just `disabled`) |

### 3.3 Markup

```tsx
function CopyFromPreviousMonthButton({
  isCopying,
  onClick,
}: {
  isCopying: boolean
  onClick: () => void
}) {
  return (
    <Button
      variant="outline"
      size="sm"
      className="h-11 px-4 shrink-0"
      onClick={onClick}
      disabled={isCopying}
      aria-label={isCopying ? 'Copying budgets from previous month' : 'Copy budgets from previous month'}
    >
      {isCopying ? (
        <>
          <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
          Copying…
        </>
      ) : (
        <>
          <Copy className="h-4 w-4 mr-2" aria-hidden="true" />
          Copy from Previous Month
        </>
      )}
    </Button>
  )
}
```

### 3.4 Mobile Behaviour

At 375px the button label "Copy from Previous Month" is 22 characters and fits on one line at `text-sm` (14px / Inter) within an `h-11` button with `px-4` — verified against the 375px viewport minimum. The flex-column header layout (section 2.2) ensures the button sits below the title, full-width context maintained.

On narrow viewports the button naturally stretches to the available width because it is the sole element on its row inside the `flex-col` parent. No additional `w-full sm:w-auto` modifier is required — shadcn `Button` is `inline-flex` by default and will not stretch beyond content width. If the product team wants a full-width mobile button, add `w-full sm:w-auto` to the className.

---

## 4. Flow Logic

### 4.1 Decision Tree

```
User clicks "Copy from Previous Month"
    │
    ▼
isCopying = true  (spinner shown, button disabled)
    │
    ▼
POST /api/v1/budgets/copy-from-previous-month
  { targetYear, targetMonth, overwriteExisting: false }
    │
    ├── API error (network / 4xx / 5xx)
    │     │
    │     ▼
    │   isCopying = false
    │   toast.error("Failed to copy budgets. Please try again.")
    │   [stop]
    │
    └── 200 OK → CopyBudgetsResultDto received
          │
          ├── copiedCount === 0 AND conflictCount === 0
          │     │
          │     ▼
          │   isCopying = false
          │   toast.info("No budgets found in {previousMonthName}")
          │   [stop — AC-7]
          │
          ├── conflictCount === 0 AND copiedCount > 0
          │     │
          │     ▼
          │   isCopying = false
          │   toast.success("Copied {copiedCount} budget(s) from {previousMonthName}")
          │   refresh()  ← reload BudgetPlan data
          │   [stop — AC-2]
          │
          └── conflictCount > 0
                │
                ▼
              isCopying = false
              dryRunResult = response  ← store for dialog use
              showCopyDialog = true    ← open confirmation dialog
              [wait for user action]
                │
                ├── User clicks "Cancel"
                │     showCopyDialog = false
                │     dryRunResult = null
                │     [stop — AC-5]
                │
                └── User clicks "Yes, overwrite"
                      │
                      ▼
                    isOverwriting = true  (confirm button spinner)
                      │
                      ▼
                    POST /api/v1/budgets/copy-from-previous-month
                      { targetYear, targetMonth, overwriteExisting: true }
                      │
                      ├── API error
                      │     isOverwriting = false
                      │     toast.error("Failed to copy budgets. Please try again.")
                      │     [dialog remains open so user can retry or cancel]
                      │
                      └── 200 OK
                            isOverwriting = false
                            showCopyDialog = false
                            dryRunResult = null
                            toast.success("Copied {copiedCount} and updated {conflictCount} budget(s)")
                            refresh()
                            [stop — AC-4]
```

### 4.2 Previous Month Name Helper

The toast messages reference `{previousMonthName}`. Compute it from the same `year`/`month` state:

```ts
function previousMonthLabel(year: number, month: number): string {
  // month is 0-indexed; subtract 1, handle January wrap
  const prevDate = new Date(year, month - 1, 1)
  return prevDate.toLocaleString(undefined, { month: 'long', year: 'numeric' })
}
```

This correctly handles January (month=0): `new Date(2026, -1, 1)` resolves to December 2025.

---

## 5. Confirmation Dialog

### 5.1 When Shown

Only when `conflictCount > 0` from the dry-run response. This is set by `showCopyDialog = true` in the flow above.

### 5.2 Component

Use the existing shadcn/ui `AlertDialog` component already imported in `BudgetPlanPage.tsx` (lines 25–27). This is consistent with the existing "Remove Budget" dialog (lines 929–948) and avoids introducing a new dialog component.

### 5.3 Content

| Element | Value |
|---|---|
| Title | "Overwrite existing budgets?" |
| Body | "{conflictCount} budget(s) in this month will be replaced with last month's amounts." |
| Cancel button | Label: "Cancel" / variant: ghost (AlertDialogCancel default) |
| Confirm button | Label: "Yes, overwrite" / variant: destructive (red) |

The body copy is intentionally plain prose — no bullet lists, no technical language. The count makes the consequence concrete and scannable.

### 5.4 Markup

```tsx
<AlertDialog
  open={showCopyDialog}
  onOpenChange={(open) => {
    if (!open && !isOverwriting) {
      setShowCopyDialog(false)
      setDryRunResult(null)
    }
  }}
>
  <AlertDialogContent>
    <AlertDialogHeader>
      <AlertDialogTitle>Overwrite existing budgets?</AlertDialogTitle>
      <AlertDialogDescription>
        {dryRunResult?.conflictCount ?? 0} budget(s) in this month will be replaced
        with last month's amounts.
      </AlertDialogDescription>
    </AlertDialogHeader>
    <AlertDialogFooter>
      <AlertDialogCancel disabled={isOverwriting}>Cancel</AlertDialogCancel>
      <AlertDialogAction
        onClick={handleOverwriteConfirm}
        disabled={isOverwriting}
        className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
      >
        {isOverwriting ? (
          <>
            <Loader2 className="h-4 w-4 mr-2 animate-spin" aria-hidden="true" />
            Copying…
          </>
        ) : (
          'Yes, overwrite'
        )}
      </AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>
```

### 5.5 Focus Management

shadcn/ui `AlertDialog` handles focus trap automatically via Radix UI `Dialog.Root`. On open, focus lands on the first focusable element — which is the `AlertDialogCancel` button ("Cancel"). This is the **safer default**: the user must consciously move to "Yes, overwrite" to confirm the destructive path. Matches the `ConfirmationDialog` pattern in `component-library.md` section 7 ("On open: focus moves to the Cancel button").

On close (either action), Radix returns focus to the element that triggered the dialog — which is the "Copy from Previous Month" button.

### 5.6 Dismiss Rules

- Backdrop click: dismisses dialog (calls onCancel path). **Only when not overwriting** — block dismiss during the API call (`isOverwriting`) to prevent partial-state confusion.
- Escape key: same as backdrop click.
- "Cancel" button: dismisses, no API call.
- "Yes, overwrite": initiates second API call; spinner shown; dialog stays open until call resolves.

---

## 6. Toast Messages

All toasts use the existing `sonner` library (`import { toast } from 'sonner'`) already used throughout `BudgetPlanPage.tsx`.

| Scenario | Toast type | Message |
|---|---|---|
| No previous month budgets | `toast.info` | `"No budgets found in {previousMonthName}"` |
| Copy success, no conflicts | `toast.success` | `"Copied {copiedCount} budget(s) from {previousMonthName}"` |
| Copy success with overwrites | `toast.success` | `"Copied {copiedCount} and updated {conflictCount} budget(s)"` |
| API error (any call) | `toast.error` | `"Failed to copy budgets. Please try again."` |

Notes:
- "No budgets found" uses `toast.info` (not `toast.error`) — it is an informational state, not a failure.
- The success-with-overwrites message uses the `conflictCount` from the dry-run result (not the second call's response) since the second call returns `conflictCount: 0` and `overwrittenCount` instead. Store `dryRunResult` to reference the original conflict count for the message.
- Plural `budget(s)` is acceptable for MVP. If the team prefers "1 budget" / "2 budgets", apply a `pluralize(n, 'budget')` helper.

---

## 7. State Variables

All new state lives in `BudgetPlanPage` (the existing single export). No new hook or context file is needed.

```ts
// Copy flow state — add to BudgetPlanPage alongside existing deleteTarget / deleting state

/** True while the dry-run POST is in-flight */
const [isCopying, setIsCopying] = useState(false)

/** Result of the dry-run call; non-null when the conflict dialog is open */
const [dryRunResult, setDryRunResult] = useState<CopyBudgetsResultDto | null>(null)

/** True while the overwrite POST (second call) is in-flight */
const [isOverwriting, setIsOverwriting] = useState(false)

/** Controls AlertDialog visibility */
const [showCopyDialog, setShowCopyDialog] = useState(false)
```

### 7.1 Derived Value

```ts
/** True when the page is showing the current calendar month */
const isCurrentMonth = useMemo(() => {
  const now = new Date()
  return year === now.getFullYear() && month === now.getMonth()
}, [year, month])
```

---

## 8. API Layer

### 8.1 New Type (budget.types.ts)

```ts
export interface CopyBudgetsResultDto {
  copiedCount: number
  skippedCount: number
  conflictCount: number
  overwrittenCount: number
}
```

### 8.2 New API Function (budgets.api.ts)

```ts
export interface CopyBudgetsFromPreviousMonthRequest {
  targetYear: number
  targetMonth: number   // 1-12 (convert from JS 0-indexed month before calling)
  overwriteExisting: boolean
}

export function copyBudgetsFromPreviousMonth(
  data: CopyBudgetsFromPreviousMonthRequest
): Promise<CopyBudgetsResultDto> {
  return api.post<CopyBudgetsResultDto>(
    `${BASE}/copy-from-previous-month`,
    data
  )
}
```

**Important**: the API expects `targetMonth` as 1–12 (Java `Month` convention). The page's `month` state is 0-indexed. The caller must pass `month + 1`.

---

## 9. Handler Functions

Add to `BudgetPlanPage` alongside existing `handleDeleteBudget` and `handleSetBudgetSuccess`.

```ts
// ── Copy from previous month ───────────────────────────────────────────────────

const prevMonthName = previousMonthLabel(year, month)

const handleCopyClick = async () => {
  setIsCopying(true)
  try {
    const result = await copyBudgetsFromPreviousMonth({
      targetYear: year,
      targetMonth: month + 1,   // convert 0-indexed → 1-indexed
      overwriteExisting: false,
    })

    if (result.copiedCount === 0 && result.conflictCount === 0) {
      // AC-7: previous month has no valid budgets
      toast.info(`No budgets found in ${prevMonthName}`)
      return
    }

    if (result.conflictCount === 0) {
      // AC-2: clean copy, no conflicts
      toast.success(`Copied ${result.copiedCount} budget(s) from ${prevMonthName}`)
      refresh().catch(() => toast.error('Failed to refresh plan'))
      return
    }

    // AC-3: conflicts exist — store result, show dialog
    setDryRunResult(result)
    setShowCopyDialog(true)
  } catch {
    toast.error('Failed to copy budgets. Please try again.')
  } finally {
    setIsCopying(false)
  }
}

const handleOverwriteConfirm = async () => {
  if (!dryRunResult) return
  const originalConflictCount = dryRunResult.conflictCount
  setIsOverwriting(true)
  try {
    const result = await copyBudgetsFromPreviousMonth({
      targetYear: year,
      targetMonth: month + 1,
      overwriteExisting: true,
    })
    setShowCopyDialog(false)
    setDryRunResult(null)
    toast.success(
      `Copied ${result.copiedCount} and updated ${originalConflictCount} budget(s)`
    )
    refresh().catch(() => toast.error('Failed to refresh plan'))
  } catch {
    toast.error('Failed to copy budgets. Please try again.')
    // Dialog stays open — user can cancel or retry
  } finally {
    setIsOverwriting(false)
  }
}
```

---

## 10. New Imports Required

Add to `BudgetPlanPage.tsx` import block:

```ts
// lucide-react — add to existing import
import { Copy, Loader2, /* existing: ChevronLeft, ChevronRight, ... */ } from 'lucide-react'

// API layer
import { copyBudgetsFromPreviousMonth } from '@/api/budgets.api'
import type { CopyBudgetsResultDto } from '@/types/budget.types'
```

---

## 11. Accessibility Checklist

| Criterion | Implementation |
|---|---|
| WCAG 2.1 SC 4.1.2 Name, Role, Value | Button has explicit `aria-label` for both idle and loading states |
| WCAG 2.1 SC 2.5.5 Target Size (44×44px minimum) | Button uses `h-11` (44px height); width naturally exceeds 44px |
| WCAG 2.1 SC 2.1.1 Keyboard | Button is a native `<button>` via shadcn `Button`; fully keyboard reachable |
| WCAG 2.1 SC 1.4.3 Contrast | Outline variant uses `text-foreground` on white — exceeds 4.5:1 |
| WCAG 2.1 SC 4.1.3 Status Messages | Toasts from sonner are rendered in a `role="status"` region (sonner default) |
| WCAG 2.1 SC 2.4.3 Focus Order | AlertDialog focus trap provided by Radix UI (already in use) |
| WCAG 2.1 SC 1.3.3 Sensory Characteristics | Conflict count in dialog body text — not communicated by color alone |
| WCAG 2.1 SC 2.4.6 Headings and Labels | Dialog uses `AlertDialogTitle` (maps to `<h2>`) and `AlertDialogDescription` (maps to `<p>`) |
| Button hidden when not applicable | `{isCurrentMonth && <CopyFromPreviousMonthButton … />}` — AC-6: not rendered for non-current months. No `disabled` attribute on a hidden element |
| Loading state announced | `aria-label` on button changes to "Copying budgets from previous month" during load; `aria-busy` not needed since the button is `disabled` and screen readers announce disabled state |

---

## 12. Responsive Wireframes

### Mobile (375px)

```
+---------------------------------------------+
| Budget Plan                                  |
| Set and review your planned income…          |
|                                              |
| [ Copy  Copy from Previous Month          ]  |
|                                              |
| [<]    March 2026          [>]               |
|                                              |
| ┌─ Planned Income ──────────────────────┐    |
| │  $3,200.00 /mo   $38,400.00 /yr       │    |
| └───────────────────────────────────────┘    |
| ...                                          |
+---------------------------------------------+
```

Button is full-row on mobile (column flex, button is inline-flex content-width but sits on its own line with surrounding whitespace). Minimum 44px height met via `h-11`.

### Desktop (1280px)

```
+-------------------------------------------------------------------+
| Budget Plan                       [ Copy  Copy from Previous Month ]
| Set and review your planned…                                      |
|                                                                   |
|  [<]        March 2026        [>]                                 |
|                                                                   |
|  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           |
|  │ Planned      │  │ Planned      │  │ Planned Net  │           |
|  │ Income       │  │ Spending     │  │              │           |
|  │ $3,200.00    │  │ $2,150.00    │  │ +$1,050.00   │           |
|  └──────────────┘  └──────────────┘  └──────────────┘           |
+-------------------------------------------------------------------+
```

On desktop the title block and button sit inline on the same row. The button is right-aligned (`justify-between` on the flex row) and does not wrap.

### Dialog (all breakpoints)

```
+--------------------------------------------+
|  [overlay]                                 |
|   +------------------------------------+   |
|   | Overwrite existing budgets?        |   |
|   |                                    |   |
|   | 3 budget(s) in this month will be  |   |
|   | replaced with last month's amounts.|   |
|   |                                    |   |
|   | [ Cancel ]      [ Yes, overwrite ] |   |
|   +------------------------------------+   |
+--------------------------------------------+
```

Dialog max-width: 400px (shadcn `AlertDialogContent` default, which is `sm:max-w-lg` — acceptable). On mobile it spans full width with edge margin. "Cancel" button receives initial focus.

---

## 13. Loading / Error / Empty States

| Scenario | State handled | Where |
|---|---|---|
| Dry-run in-flight | `isCopying=true` → spinner on button, button disabled | Copy button |
| Dry-run network error | `toast.error(…)` | Page-level toast |
| Overwrite in-flight | `isOverwriting=true` → spinner on confirm button, confirm + cancel disabled | Dialog |
| Overwrite network error | `toast.error(…)` | Dialog stays open; user retries or cancels |
| Empty previous month | `toast.info(…)` | Page-level toast |
| Button not applicable | Component not rendered at all | Page header |

---

## 14. What full-stack-dev Needs to Implement

### Files to modify

1. **`frontend/src/types/budget.types.ts`** — add `CopyBudgetsResultDto` interface
2. **`frontend/src/api/budgets.api.ts`** — add `CopyBudgetsFromPreviousMonthRequest` interface and `copyBudgetsFromPreviousMonth()` function
3. **`frontend/src/pages/budgets/BudgetPlanPage.tsx`** — all changes listed below

### Changes to BudgetPlanPage.tsx

- Add imports: `Copy`, `Loader2` from `lucide-react`; `copyBudgetsFromPreviousMonth` from `@/api/budgets.api`; `CopyBudgetsResultDto` type
- Add `previousMonthLabel()` utility function (alongside existing `monthLabel()`)
- Add `CopyFromPreviousMonthButton` sub-component (alongside existing `SkeletonRow`, `BudgetPlanSkeleton`, etc.)
- Add 4 state variables to `BudgetPlanPage`: `isCopying`, `dryRunResult`, `isOverwriting`, `showCopyDialog`
- Add `isCurrentMonth` derived value via `useMemo`
- Add `prevMonthName` derived string
- Add `handleCopyClick` handler function
- Add `handleOverwriteConfirm` handler function
- Replace single-`<div>` page header with flex row header (section 2.2)
- Add `<AlertDialog>` block for the overwrite confirmation (after the existing delete `<AlertDialog>`)

### Do NOT change

- The month navigation row — it remains unchanged
- `useBudgetPlan` hook — no changes needed; `refresh()` is already exposed
- `SetBudgetDialog` — no changes needed
- `deleteBudget` AlertDialog — no changes needed
- `SummaryCards`, `PlanSection`, `ExpenseSection`, all sub-components — no changes needed

---

## 15. Open Decisions for Implementation

These are minor decisions the full-stack-dev can resolve at implementation time without UX review:

1. **Plural "budget(s)"**: The spec uses `budget(s)` as a simple placeholder. The team may prefer a `pluralize(n, 'budget', 'budgets')` helper for cleaner copy. Either is acceptable.
2. **Dialog position of action buttons**: shadcn `AlertDialogFooter` renders Cancel first on mobile (stacked column, reversed on sm+). This is the shadcn default and requires no override.
3. **`previousMonthLabel` locale**: Uses `undefined` (system locale) matching existing `monthLabel()` usage in the page. No change needed.
