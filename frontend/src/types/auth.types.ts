export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
}

export interface UserProfile {
  id: number
  username: string
  email: string
  firstName: string
  lastName: string
  preferredCurrency: string
  createdAt: string
}
