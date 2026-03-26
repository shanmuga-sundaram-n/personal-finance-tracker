import '@testing-library/jest-dom'
import { expect } from 'vitest'
import { toHaveNoViolations } from 'vitest-axe/matchers'

expect.extend({ toHaveNoViolations })
