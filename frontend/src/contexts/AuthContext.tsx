import { createContext, useCallback, useEffect, useState, type ReactNode } from 'react'
import type { UserProfile } from '@/types/auth.types'
import type { LoginRequest, RegisterRequest } from '@/types/auth.types'
import * as authApi from '@/api/auth.api'
import { hasToken, setOnUnauthorized } from '@/api/client'

interface AuthContextValue {
  user: UserProfile | null
  isLoading: boolean
  login: (data: LoginRequest) => Promise<void>
  register: (data: RegisterRequest) => Promise<UserProfile>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Register global 401 handler to auto-clear user state
  useEffect(() => {
    setOnUnauthorized(() => {
      setUser(null)
    })
  }, [])

  useEffect(() => {
    if (!hasToken()) {
      setIsLoading(false)
      return
    }
    authApi
      .getProfile()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false))
  }, [])

  const login = useCallback(async (data: LoginRequest) => {
    const profile = await authApi.login(data)
    setUser(profile)
  }, [])

  const register = useCallback(async (data: RegisterRequest) => {
    return authApi.register(data)
  }, [])

  const logout = useCallback(async () => {
    await authApi.logout()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
