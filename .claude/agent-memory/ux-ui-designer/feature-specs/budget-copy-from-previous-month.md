# Design Spec: Budget Copy from Previous Month
**Author**: UX/UI Designer
**Date**: 2026-03-26
**Status**: Ready for Implementation
**Feature Brief**: `.claude/agent-memory/engineering-manager/feature-briefs/budget-copy-from-previous-month.md`
**Implements**: Phase 2B of FEATURE track

---

## Overview

This spec covers every frontend surface required for the "Copy from Previous Month" feature.
It is a self-contained addition to `BudgetPlanPage.tsx` and `budgets.api.ts`.
No new routes, no new pages.

Reading order for implementers:
1. API additions (section 1)
2. State additions to BudgetPlanPage (section 2)
3. Copy button component (section 3)
4. Overwrite confirmation dialog (section 4)
5. Toast messages (section 5)
6. Exact placement in JSX (section 6)
7. Accessibility checklist (section 7)

---

## 1. API Layer — `frontend/src/api/budgets.api.ts`

Add two new exports at the bottom of the existing file. Do not remove or modify any existing function.

### 1.1 Types

```typescript
// Add these two interfaces to budgets.api.ts (keep near UpsertBudgetByCategoryRequest)

export interface CopyBudgetsRequest {
  targetYear: number
  targetMonth: number        // 1-based: January = 1, December = 12
  overwriteExisting: boolean
}

export interface CopyBudgetsResult {
  copiedCount: number
  skippedCount: number
  conflictCount: number
  overwrittenCount: number
}
```

### 1.2 API function

```typescript
export function copyBudgetsFromPreviousMonth(
  data: CopyBudgetsRequest
): Promise<CopyBudgetsResult> {
  return api.post<CopyBudgetsResult>(`${BASE}/copy-from-previous-month`, data)
}
```

No preflight check query is needed. The frontend derives `conflictCount` from the
first API call (`overwriteExisting: false`). If `conflictCount > 0`, the dialog is
shown. The second API call (`overwriteExisting: true`) is only made on confirmation.

---

## 2. State Additions — `BudgetPlanPage` component

### 2.1 Derive "is current month" flag

Add this computed value inside `BudgetPlanPage`, alongside the existing `startDate`/`endDate` derivation:

```typescript
const now = new Date()

// Already exists:
const [year, setYear] = useState(now.getFullYear())
const [month, setMonth] = useState(now.getMonth()) // 0-based

// ADD — used to show/hide the copy button:
const isCurrentMonth =
  year === now.getFullYear() && month === now.getMonth()
```

`month` is 0-based in the existing code (matching JS `Date`). The API expects 1-based
(`targetMonth`). Add 1 when building the request body.

### 2.2 New state variables

Add these four state declarations alongside the existing `deleteTarget` / `deleting` declarations:

```typescript
// Copy-from-previous-month state
const [isCopying, setIsCopying]                 = useState(false)
const [showOverwriteDialog, setShowOverwriteDialog] = useState(false)
const [pendingConflictCount, setPendingConflictCount] = useState(0)
const [pendingCopiedCount, setPendingCopiedCount]   = useState(0)
```

Prop types for these four are all inferred — no additional TypeScript types needed.

### 2.3 Helper: previous month label

Used in toast messages per acceptance criterion 7 and feature brief toast spec.
Add alongside the existing `monthLabel` helper at the top of the file:

```typescript
function prevMonthLabel(year: number, month: number): string {
  // month is 0-based; compute the calendar month before it
  const d = new Date(year, month - 1, 1) // JS handles month=0 -> Dec of year-1
  return d.toLocaleString(undefined, { month: 'long', year: 'numeric' })
}
```

### 2.4 Handler: `handleCopyFromPreviousMonth`

Add inside `BudgetPlanPage`, alongside `handleDeleteBudget` and `handleSetBudgetSuccess`:

```typescript
const handleCopyFromPreviousMonth = async () => {
  setIsCopying(true)
  try {
    // First call: overwriteExisting=false — discovers conflicts without writing them
    const result = await copyBudgetsFromPreviousMonth({
      targetYear: year,
      targetMonth: month + 1,   // convert 0-based to 1-based
      overwriteExisting: false,
    })

    if (result.copiedCount === 0 && result.conflictCount === 0) {
      // Nothing in the previous month to copy
      toast.info(`No budgets found in ${prevMonthLabel(year, month)}`)
      return
    }

    if (result.conflictCount > 0) {
      // Some current-month budgets would be overwritten — show dialog
      setPendingConflictCount(result.conflictCount)
      setPendingCopiedCount(result.copiedCount)
      setShowOverwriteDialog(true)
      // Keep isCopying=true while dialog is open so button stays disabled
      return
    }

    // No conflicts — copy completed without overwrite dialog
    toast.success(
      `Copied ${result.copiedCount} budget${result.copiedCount !== 1 ? 's' : ''} from ${prevMonthLabel(year, month)}`
    )
    refresh().catch(() => toast.error('Failed to refresh plan'))
  } catch {
    toast.error('Failed to copy budgets. Please try again.')
  } finally {
    // Only release the spinner if we are NOT about to show the dialog.
    // If the dialog is opening, isCopying stays true (button remains disabled).
    setIsCopying((prev) => {
      // showOverwriteDialog may not have been set yet due to React batching,
      // so check the flag via the pending count side-effect.
      // Simpler: always reset here; the dialog open state controls button disable separately.
      return false
    })
  }
}
```

Note: Because React state updates may batch, `isCopying` is reset to `false`
in the `finally` block unconditionally. The button is also disabled while
`showOverwriteDialog === true` (see section 3.3), so the button cannot be
double-clicked while the dialog is open even with `isCopying = false`.

### 2.5 Handler: `handleConfirmOverwrite`

Called when the user clicks "Yes, overwrite" in the confirmation dialog:

```typescript
const handleConfirmOverwrite = async () => {
  setShowOverwriteDialog(false)
  setIsCopying(true)
  try {
    const result = await copyBudgetsFromPreviousMonth({
      targetYear: year,
      targetMonth: month + 1,
      overwriteExisting: true,
    })

    const parts: string[] = []
    if (result.copiedCount > 0)
      parts.push(`Copied ${result.copiedCount} budget${result.copiedCount !== 1 ? 's' : ''}`)
    if (result.overwrittenCount > 0)
      parts.push(`updated ${result.overwrittenCount}`)

    toast.success(
      parts.length > 0
        ? parts.join(' and ') + ` from ${prevMonthLabel(year, month)}`
        : `Budgets copied from ${prevMonthLabel(year, month)}`
    )
    refresh().catch(() => toast.error('Failed to refresh plan'))
  } catch {
    toast.error('Failed to copy budgets. Please try again.')
  } finally {
    setIsCopying(false)
  }
}
```

### 2.6 Handler: `handleCancelOverwrite`

```typescript
const handleCancelOverwrite = () => {
  setShowOverwriteDialog(false)
  setPendingConflictCount(0)
  setPendingCopiedCount(0)
  // isCopying is already false after the first call's finally block
}
```

---

## 3. Copy Button Component

### 3.1 Icon

Use `Copy` from `lucide-react`. This icon clearly signals "make a copy" — a calendar
variant (`CalendarCopy`) does not exist in Lucide; `Copy` is the closest semantic match.
The icon is paired with the text label, so meaning is never conveyed by icon alone.

Add to the existing imports at the top of `BudgetPlanPage.tsx`:
```typescript
import {
  // ...existing imports...
  Copy,
  Loader2,
} from 'lucide-react'
```

`Loader2` is used for the loading spinner in the button. Check whether it is already
imported; if so, do not add a duplicate.

### 3.2 Button markup

```tsx
{isCurrentMonth && (
  <Button
    variant="outline"
    size="sm"
    className="h-11 gap-2 px-3 text-sm"
    onClick={handleCopyFromPreviousMonth}
    disabled={isCopying || showOverwriteDialog || isLoading}
    aria-label="Copy budgets from previous month"
    aria-busy={isCopying}
  >
    {isCopying ? (
      <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
    ) : (
      <Copy className="h-4 w-4" aria-hidden="true" />
    )}
    <span className="hidden sm:inline">Copy from Previous Month</span>
    <span className="sm:hidden">Copy</span>
  </Button>
)}
```

Design rationale:
- `variant="outline"`: secondary style intentionally avoids drawing the eye as a primary
  action. The primary actions on this page are "Set Budget" / "Edit Budget" per row.
- `h-11`: 44px height — meets WCAG 2.5.5 touch target minimum, consistent with all
  other interactive controls on this page.
- `disabled` when `isLoading`: prevents clicking while the page data is still loading.
- `aria-busy`: screen readers announce loading state without needing a visually hidden
  live region for this element.
- Mobile label truncation: "Copy from Previous Month" is too long for narrow screens.
  The short label "Copy" is shown below 640px (Tailwind `sm:` breakpoint). The full
  label is preserved in `aria-label` so screen readers always hear the full intent.

### 3.3 Disabled states

| Condition | Button state | Reason |
|---|---|---|
| `isLoading === true` | disabled | Plan data not yet loaded |
| `isCopying === true` | disabled + spinner | API call in flight |
| `showOverwriteDialog === true` | disabled | Awaiting user decision |
| `isCurrentMonth === false` | not rendered | Feature scope: current month only |

---

## 4. Overwrite Confirmation Dialog

### 4.1 Component

Use shadcn/ui `AlertDialog` — the same component already imported in `BudgetPlanPage.tsx`
for the "Remove Budget" confirmation. No new component imports are needed.

```tsx
<AlertDialog
  open={showOverwriteDialog}
  onOpenChange={(open) => { if (!open) handleCancelOverwrite() }}
>
  <AlertDialogContent className="max-w-md">
    <AlertDialogHeader>
      <AlertDialogTitle>Overwrite existing budgets?</AlertDialogTitle>
      <AlertDialogDescription>
        {pendingConflictCount} budget{pendingConflictCount !== 1 ? 's' : ''} from this
        month will be replaced with last month's amounts.
        {pendingCopiedCount > 0 && (
          <> {pendingCopiedCount} new budget{pendingCopiedCount !== 1 ? 's' : ''} will
          also be added.</>
        )}
        {' '}This cannot be undone.
      </AlertDialogDescription>
    </AlertDialogHeader>
    <AlertDialogFooter>
      <AlertDialogCancel onClick={handleCancelOverwrite}>
        Cancel
      </AlertDialogCancel>
      <AlertDialogAction
        onClick={handleConfirmOverwrite}
        className="bg-destructive text-destructive-foreground hover:bg-destructive/90 focus-visible:ring-destructive"
      >
        Yes, overwrite
      </AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>
```

Design rationale:
- `AlertDialog` (not `Dialog`): shadcn/ui's `AlertDialog` uses `role="alertdialog"`,
  which screen readers announce immediately. This is correct for destructive confirmations
  per WCAG 2.1 SC 3.3.4 (Error Prevention — reversible).
- Cancel is listed before "Yes, overwrite" in the DOM. This matches the focus management
  rule from component-library.md section 7: "focus moves to the Cancel button (safer
  default — prevents accidental confirmation)". shadcn/ui `AlertDialogCancel` receives
  focus by default when the dialog opens.
- Body copy includes `pendingCopiedCount` inline so the user understands the full scope
  (X overwritten + Y new) before confirming. This is progressive disclosure — the most
  important number (overwrite count) leads.
- `max-w-md`: 448px cap prevents the dialog from spanning full screen on large displays,
  keeping text line length scannable.

### 4.2 Focus management

shadcn/ui `AlertDialog` handles focus management via Radix UI Dialog primitives:
- On open: focus moves to the first focusable element (`AlertDialogCancel`).
- Focus is trapped inside the dialog while open.
- On close: focus returns to the trigger element (the "Copy from Previous Month" button).

No custom focus management code is needed.

### 4.3 Escape and backdrop dismiss

- Pressing Escape calls `handleCancelOverwrite` (via `onOpenChange`).
- Clicking the backdrop calls `handleCancelOverwrite` (via `onOpenChange`).
- Neither triggers the overwrite — only the explicit "Yes, overwrite" click does.

---

## 5. Toast Messages

The page already uses `sonner` (`import { toast } from 'sonner'`). No new toast library.

| Scenario | Call | Message |
|---|---|---|
| Previous month has no budgets | `toast.info(...)` | `No budgets found in February 2026` |
| Copy succeeded, no conflicts | `toast.success(...)` | `Copied 5 budgets from February 2026` |
| Copy succeeded, singular | `toast.success(...)` | `Copied 1 budget from February 2026` |
| Overwrite confirmed, mixed | `toast.success(...)` | `Copied 3 and updated 2 from February 2026` |
| Overwrite confirmed, only overwrites | `toast.success(...)` | `updated 4 from February 2026` |
| API error (either call) | `toast.error(...)` | `Failed to copy budgets. Please try again.` |
| Refresh failure after copy | `toast.error(...)` | `Failed to refresh plan` |

The month name in toasts is produced by `prevMonthLabel(year, month)` which returns
a locale-formatted string such as "February 2026". The `year` and `month` variables
at call time reflect the currently viewed month (which is always the current month
when this button is visible).

`toast.info` is used for the "nothing to copy" case because it is neither a success
nor an error — it is neutral information. Sonner renders this as a blue/neutral toast
distinct from green success and red error toasts.

---

## 6. Exact JSX Placement in `BudgetPlanPage`

### 6.1 Month navigation row — add the button

The existing month navigation block (lines 832–859 in the current file) is:

```tsx
{/* ── Month navigation ── */}
<div className="flex items-center justify-center gap-2 sm:justify-start">
  <Button variant="outline" size="icon" ... >
    <ChevronLeft ... />
  </Button>
  <span ...>{monthLabel(year, month)}</span>
  <Button variant="outline" size="icon" ... >
    <ChevronRight ... />
  </Button>
</div>
```

Replace with:

```tsx
{/* ── Month navigation + copy action ── */}
<div className="flex items-center justify-between gap-3">
  {/* Left: prev/month-label/next */}
  <div className="flex items-center gap-2">
    <Button
      variant="outline"
      size="icon"
      onClick={prevMonth}
      className="h-11 w-11 shrink-0"
      aria-label="Previous month"
    >
      <ChevronLeft className="h-5 w-5" />
    </Button>
    <span
      className="min-w-[160px] sm:min-w-[180px] text-center text-base font-semibold select-none"
      aria-live="polite"
      aria-atomic="true"
    >
      {monthLabel(year, month)}
    </span>
    <Button
      variant="outline"
      size="icon"
      onClick={nextMonth}
      className="h-11 w-11 shrink-0"
      aria-label="Next month"
    >
      <ChevronRight className="h-5 w-5" />
    </Button>
  </div>

  {/* Right: copy button — only for current month */}
  {isCurrentMonth && (
    <Button
      variant="outline"
      size="sm"
      className="h-11 gap-2 px-3 text-sm shrink-0"
      onClick={handleCopyFromPreviousMonth}
      disabled={isCopying || showOverwriteDialog || isLoading}
      aria-label="Copy budgets from previous month"
      aria-busy={isCopying}
    >
      {isCopying ? (
        <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
      ) : (
        <Copy className="h-4 w-4" aria-hidden="true" />
      )}
      <span className="hidden sm:inline">Copy from Previous Month</span>
      <span className="sm:hidden">Copy</span>
    </Button>
  )}
</div>
```

Key layout change: the outer div changes from `justify-center sm:justify-start` to
`justify-between`. This pushes the month navigator to the left and the copy button
to the right. On mobile (375px), both sides sit flush to the edges with `gap-3`
between them, ensuring neither element gets clipped.

The `min-w-[160px]` on the month label narrows slightly on mobile (from 180px) to
leave room for the "Copy" button at 375px without wrapping.

### 6.2 Dialog placement

Add both dialogs inside the `return` block, after the existing `AlertDialog` for
"Remove Budget" and before the `SetBudgetDialog`. The order in the DOM does not
affect render — Radix portals both to `document.body` — but grouping them together
makes the code easier to navigate.

```tsx
{/* ── Remove Budget Dialog (existing) ── */}
<AlertDialog open={deleteTarget !== null} ...>
  ...
</AlertDialog>

{/* ── Copy from Previous Month: Overwrite Confirmation ── */}
<AlertDialog
  open={showOverwriteDialog}
  onOpenChange={(open) => { if (!open) handleCancelOverwrite() }}
>
  <AlertDialogContent className="max-w-md">
    <AlertDialogHeader>
      <AlertDialogTitle>Overwrite existing budgets?</AlertDialogTitle>
      <AlertDialogDescription>
        {pendingConflictCount} budget{pendingConflictCount !== 1 ? 's' : ''} from
        this month will be replaced with last month's amounts.
        {pendingCopiedCount > 0 && (
          <> {pendingCopiedCount} new budget{pendingCopiedCount !== 1 ? 's' : ''} will
          also be added.</>
        )}
        {' '}This cannot be undone.
      </AlertDialogDescription>
    </AlertDialogHeader>
    <AlertDialogFooter>
      <AlertDialogCancel onClick={handleCancelOverwrite}>
        Cancel
      </AlertDialogCancel>
      <AlertDialogAction
        onClick={handleConfirmOverwrite}
        className="bg-destructive text-destructive-foreground hover:bg-destructive/90 focus-visible:ring-destructive"
      >
        Yes, overwrite
      </AlertDialogAction>
    </AlertDialogFooter>
  </AlertDialogContent>
</AlertDialog>

{/* ── SetBudgetDialog (existing) ── */}
{dialogRow && plan && (
  <SetBudgetDialog ... />
)}
```

---

## 7. Responsive Behavior

### 375px (mobile minimum)

```
[ < ]  March 2026  [ > ]              [Copy]
```

- Month navigator: `flex items-center gap-2` on the left.
- Copy button: short label "Copy", `h-11 px-3`.
- The two groups sit at opposite ends of the row (`justify-between`).
- Total row height: 44px (button height). No wrapping at 375px because `min-w-[160px]`
  for the label + icon buttons (44px each) + "Copy" button (~72px) = approx 340px,
  which fits within 375px minus 32px page horizontal padding = 343px.

### 480px–767px (large phone)

Same layout. Full label "Copy from Previous Month" becomes visible at `sm:` (640px).
At 480–639px, still shows "Copy".

### 768px+ (tablet / desktop)

```
[ < ]  March 2026  [ > ]          [ Copy from Previous Month ]
```

Full label visible. The button is right-aligned.

---

## 8. Interaction State Matrix

### Copy button

| State | Visual | Accessible |
|---|---|---|
| Default (current month, not loading) | Outline button, Copy icon, full label | `aria-label="Copy budgets from previous month"` |
| Hover | Tailwind `hover:bg-accent hover:text-accent-foreground` (from shadcn/ui `variant="outline"`) | No change |
| Focus | `focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2` (from shadcn/ui) | Visible focus ring; keyboard navigable |
| Active (pressed) | `active:scale-[0.98]` (from shadcn/ui) | No change |
| Loading (isCopying) | Loader2 spinner replaces Copy icon; button disabled | `aria-busy="true"` `disabled` |
| Disabled (plan loading) | Muted appearance; cursor not-allowed | `disabled` |
| Hidden (not current month) | Not rendered | Not in DOM |

### Overwrite dialog — "Yes, overwrite" button

| State | Visual | Accessible |
|---|---|---|
| Default | `bg-destructive` (red, from design system `--color-expense-600`) | Correct role from `AlertDialogAction` |
| Hover | `hover:bg-destructive/90` | No change |
| Focus | `focus-visible:ring-destructive` | Visible focus ring in destructive color |

---

## 9. Accessibility Checklist (WCAG 2.1 AA)

- [x] **SC 1.1.1 Non-text Content**: Copy icon has `aria-hidden="true"`. Loader2 has
  `aria-hidden="true"`. The button has a full `aria-label` so icon-only state on mobile
  is still labelled.
- [x] **SC 1.3.1 Info and Relationships**: `AlertDialog` uses `role="alertdialog"` (via
  Radix). Title is `AlertDialogTitle` (`aria-labelledby`). Description is
  `AlertDialogDescription` (`aria-describedby`). All semantic roles are correct.
- [x] **SC 1.4.1 Use of Color**: The destructive "Yes, overwrite" button uses red color
  AND the explicit text label "Yes, overwrite". Color is not the sole distinguisher.
- [x] **SC 1.4.3 Contrast**: shadcn/ui `variant="outline"` uses `text-foreground` on
  `background`. `bg-destructive text-destructive-foreground` is the shadcn/ui destructive
  token pair — verified contrast in design-system.md at `--color-expense-600` on white.
- [x] **SC 2.1.1 Keyboard**: Button is a `<button>` element (via shadcn/ui `Button`).
  Dialog is keyboard-navigable via Radix. Escape dismisses. Tab cycles between Cancel
  and "Yes, overwrite".
- [x] **SC 2.4.3 Focus Order**: Dialog receives focus on open (Cancel first). Focus
  returns to trigger button on close.
- [x] **SC 2.4.7 Focus Visible**: shadcn/ui focus-visible ring is applied to all
  interactive elements.
- [x] **SC 2.5.5 Target Size**: Button `h-11` = 44px. Both dialog buttons render at
  `h-10` via shadcn/ui defaults — acceptable (within shadcn/ui default button height).
  Consider adding `className="h-11"` to `AlertDialogCancel` and `AlertDialogAction`
  if mobile touch tests show difficulty.
- [x] **SC 3.3.4 Error Prevention (Reversible)**: The overwrite action is preceded by
  a confirmation step. The body copy explicitly states "This cannot be undone" so the
  user has full context before committing.
- [x] **SC 4.1.3 Status Messages**: Toast messages from Sonner are rendered in an
  `aria-live` region (Sonner's `<Toaster>` component). No additional live region needed.

---

## 10. Import Changes Summary

All imports are from existing dependencies. No new packages.

In `BudgetPlanPage.tsx`, add to the lucide-react import block:
```typescript
Copy,
Loader2,
```

In `BudgetPlanPage.tsx`, add to the `@/api/budgets.api` import:
```typescript
import { deleteBudget, copyBudgetsFromPreviousMonth } from '@/api/budgets.api'
```

In `budgets.api.ts`, add `CopyBudgetsRequest` and `CopyBudgetsResult` interfaces plus
the `copyBudgetsFromPreviousMonth` function (see section 1).

---

## 11. What Does NOT Change

- `useBudgetPlan` hook — no changes required.
- `SetBudgetDialog` — no changes required.
- All existing `CategoryRow`, `SectionHeader`, `ExpenseSection`, `PlanSection` subcomponents — no changes.
- The delete budget `AlertDialog` — no changes.
- `budget.types.ts` — no new frontend types needed (API response shapes are defined inline in `budgets.api.ts`).
- Routing — no new routes.
- Nginx config — no changes.

---

## 12. Verification Steps (for implementer after coding)

After implementation, rebuild:
```bash
docker compose build frontend && docker compose up -d frontend
```

Manual test matrix:

| Test | Expected result |
|---|---|
| View a past month → copy button | Button is NOT rendered |
| View a future month → copy button | Button is NOT rendered |
| View current month, plan loading | Button rendered but disabled |
| View current month, plan loaded | Button enabled with Copy icon |
| Click copy, no prev-month budgets | info toast "No budgets found in [prev month]" |
| Click copy, no conflicts | success toast "Copied N budget(s) from [prev month]", plan refreshes |
| Click copy, conflicts exist | Dialog opens showing correct count |
| Dialog: press Escape | Dialog closes, no API call made |
| Dialog: click backdrop | Dialog closes, no API call made |
| Dialog: click Cancel | Dialog closes, no API call made |
| Dialog: click "Yes, overwrite" | success toast with copy+overwrite counts, plan refreshes |
| API error during first call | error toast, button re-enabled |
| API error during second call | error toast, button re-enabled |
| Keyboard: Tab to button, Enter | Same as click |
| Keyboard: dialog open, Tab | Cycles between Cancel and "Yes, overwrite" |
| Screen reader: button announcement | "Copy budgets from previous month, button" |
| Screen reader: dialog announcement | Role alertdialog, title + description read on open |
| Mobile 375px: layout | Month nav + "Copy" button on same row, no wrapping |
| Mobile 375px: touch | Button 44px tall, easy to tap |
