import { api, storeToken, clearToken } from './client'
import type { LoginRequest, LoginResponse, RegisterRequest, UserProfile } from '@/types/auth.types'

export async function login(data: LoginRequest): Promise<UserProfile> {
  const res = await api.post<LoginResponse>('/api/v1/auth/login', data)
  storeToken(res.token, res.expiresAt)
  return getProfile()
}

export async function register(data: RegisterRequest): Promise<UserProfile> {
  return api.post<UserProfile>('/api/v1/auth/register', data)
}

export async function logout(): Promise<void> {
  try {
    await api.post<void>('/api/v1/auth/logout')
  } finally {
    clearToken()
  }
}

export async function getProfile(): Promise<UserProfile> {
  return api.get<UserProfile>('/api/v1/auth/me')
}
