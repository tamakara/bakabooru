import apiClient from './client'

export const systemApi = {
  getSettings: async () => {
    const response = await apiClient.get<Record<string, string>>('/system/settings')
    return response.data
  },

  updateSettings: async (settings: Record<string, string>) => {
    await apiClient.post('/system/settings', settings)
  }
}
