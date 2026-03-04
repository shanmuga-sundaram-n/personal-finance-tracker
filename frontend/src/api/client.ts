import type { ApiError } from '@/types/api.types'

const TOKEN_KEY = 'auth_token'
const EXPIRY_KEY = 'auth_expiry'

type OnUnauthorizedCallback = () => void
let onUnauthorized: OnUnauthorizedCallback | null = null

export function setOnUnauthorized(callback: OnUnauthorizedCallback) {
  onUnauthorized = callback
}

export class ApiClientError extends Error {
  public readonly status: number
  public readonly apiError: ApiError | null

  constructor(status: number, apiError: ApiError | null, message?: string) {
    super(message ?? apiError?.message ?? `Request failed with status ${status}`)
    this.status = status
    this.apiError = apiError
  }
}

function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  const expiry = localStorage.getItem(EXPIRY_KEY)
  if (!token || !expiry) return null
  if (new Date(expiry) <= new Date()) {
    clearToken()
    return null
  }
  return token
}

export function storeToken(token: string, expiresAt: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(EXPIRY_KEY, expiresAt)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EXPIRY_KEY)
}

export function hasToken(): boolean {
  return getToken() !== null
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }
  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(url, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    // Handle 401 globally — clear token and notify app
    if (res.status === 401) {
      clearToken()
      onUnauthorized?.()
    }

    let apiError: ApiError | null = null
    try {
      const json = await res.json()
      // Backend filter returns partial shape {status, error, message}
      // GlobalExceptionHandler returns full shape with timestamp, errors, path
      // Normalize to full ApiError shape
      apiError = {
        status: json.status ?? res.status,
        error: json.error ?? res.statusText,
        message: json.message ?? 'An error occurred',
        errors: json.errors ?? [],
        timestamp: json.timestamp ?? new Date().toISOString(),
        path: json.path ?? url,
      }
    } catch {
      // response body wasn't JSON
    }
    throw new ApiClientError(res.status, apiError)
  }

  if (res.status === 204) return undefined as T
  return res.json()
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body),
  put: <T>(url: string, body?: unknown) => request<T>('PUT', url, body),
  delete: (url: string) => request<void>('DELETE', url),
}
