import { api } from './client'
import type { DashboardData, SpendingReport, MonthlyTrend } from '@/types/report.types'

const BASE = '/api/v1/reports'

export function getDashboard(): Promise<DashboardData> {
  return api.get<DashboardData>(`${BASE}/dashboard`)
}

export function getSpendingReport(month: string): Promise<SpendingReport> {
  return api.get<SpendingReport>(`${BASE}/spending?month=${month}`)
}

export function getTrend(months: number = 6): Promise<MonthlyTrend> {
  return api.get<MonthlyTrend>(`${BASE}/trend?months=${months}`)
}
