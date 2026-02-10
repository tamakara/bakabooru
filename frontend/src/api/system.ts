import apiClient from './client'

export const systemApi = {
  getSettings: async () => {
    const response = await apiClient.get<Record<string, string>>('/system/settings')
    return response.data
  },

  updateSettings: async (settings: Record<string, string>) => {
    await apiClient.post('/system/settings', settings)
  },

  downloadBackup: () => {
    window.location.href = '/api/system/backup'
  },

  restoreBackup: async (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    await apiClient.post('/system/backup/restore', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  clearCache: async () => {
    await apiClient.post('/system/settings/clear-cache')
  },

  resetSystem: async () => {
    await apiClient.delete('/system/backup/reset')
  }
}
