import apiClient from './client'

export interface AuthStatus {
  initialized: boolean
  passwordSet: boolean
}

export interface LoginResponse {
  token: string
}

export const authApi = {
  getStatus: async () => {
    const response = await apiClient.get<AuthStatus>('/auth/status')
    return response.data
  },

  login: async (password: string) => {
    const response = await apiClient.post<LoginResponse>('/auth/login', { password })
    return response.data
  },

  setup: async (password: string) => {
    await apiClient.post('/auth/setup', { password })
  },

  updatePassword: async (password: string) => {
    await apiClient.post('/auth/password', { password })
  }
}

