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

export interface AiModelDto {
  id: string
  name: string
  capability: 'TEXT_TO_IMAGE' | 'IMAGE_TO_IMAGE' | 'TAGGING'
  version: string
  dimension?: number
  status: string
  downloaded: boolean
}

export const aiModelApi = {
  list: async () => (await apiClient.get<AiModelDto[]>('/ai/models')).data,
  download: async (id: string) => (await apiClient.post<AiModelDto>(`/ai/models/${id}/download`)).data,
  enable: async (id: string) => (await apiClient.post<AiModelDto>(`/ai/models/${id}/enable`)).data,
  disable: async (id: string) => (await apiClient.post<AiModelDto>(`/ai/models/${id}/disable`)).data,
  uninstall: async (id: string) => (await apiClient.delete<AiModelDto>(`/ai/models/${id}/artifact`)).data
}
