export interface FieldError {
  field: string
  code: string
  message: string
}

export interface ApiError {
  status: number
  error: string
  message: string
  errors: FieldError[]
  timestamp: string
  path: string
}
