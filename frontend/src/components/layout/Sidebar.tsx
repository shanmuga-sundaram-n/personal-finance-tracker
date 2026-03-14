import { NavLink, Link } from 'react-router-dom'
import { LayoutDashboard, Wallet, Tag, ArrowLeftRight, PieChart, BarChart3 } from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/accounts', label: 'Accounts', icon: Wallet },
  { to: '/categories', label: 'Categories', icon: Tag },
  { to: '/transactions', label: 'Transactions', icon: ArrowLeftRight },
  { to: '/budgets', label: 'Budgets', icon: PieChart },
  { to: '/reports', label: 'Reports', icon: BarChart3 },
]

export function Sidebar({ className }: { className?: string }) {
  return (
    <aside className={cn('flex flex-col p-4', className)}>
      {/* Brand */}
      <Link to="/dashboard" className="group mb-6 flex items-center gap-2.5 px-2">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 shadow-sm shadow-blue-600/20 transition-shadow group-hover:shadow-md group-hover:shadow-blue-600/30">
          <Wallet className="h-4 w-4 text-white" />
        </div>
        <span className="text-base font-bold tracking-tight">Finance Tracker</span>
      </Link>

      {/* Navigation */}
      <nav className="flex flex-col gap-0.5">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200',
                isActive
                  ? 'bg-blue-500/10 text-blue-600 dark:text-blue-400 dark:bg-blue-500/15'
                  : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
              )
            }
          >
            <Icon className="h-4 w-4 shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
