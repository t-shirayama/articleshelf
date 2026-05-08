export interface User {
  id: string
  username: string
  displayName: string
  roles: string[]
}

export interface AuthResponse {
  user: User
  accessToken: string
}

export interface AuthCredentials {
  username: string
  password: string
}

export interface RegisterInput extends AuthCredentials {
  displayName: string
}

export interface ChangePasswordInput {
  currentPassword: string
  newPassword: string
}

export interface DeleteAccountInput {
  currentPassword: string
}

export interface AdminResetPasswordInput {
  newPassword: string
}
