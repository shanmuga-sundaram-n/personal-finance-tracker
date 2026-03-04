import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Wallet } from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/accounts', label: 'Accounts', icon: Wallet },
  // { to: '/categories', label: 'Categories', icon: Tag },
  // { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  // { to: '/budgets', label: 'Budgets', icon: PieChart },
  // { to: '/reports', label: 'Reports', icon: BarChart3 },
]

export function Sidebar({ className }: { className?: string }) {
  return (
    <aside className={cn('flex flex-col gap-2 p-4', className)}>
      <div className="mb-4 flex items-center gap-2 px-2">
        <Wallet className="h-6 w-6 text-primary" />
        <span className="text-lg font-semibold">Finance Tracker</span>
      </div>
      <nav className="flex flex-col gap-1">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground',
                isActive && 'bg-accent text-accent-foreground'
              )
            }
          >
            <Icon className="h-4 w-4" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
