export interface User {
  id: string
  email: string
  displayName: string
  roles: string[]
}

export interface AuthResponse {
  user: User
  accessToken: string
}

export interface AuthCredentials {
  email: string
  password: string
}

export interface RegisterInput extends AuthCredentials {
  displayName: string
}
