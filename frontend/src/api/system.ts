import apiClient from './client'

export const systemApi = {
  getSettings: async () => {
    const response = await apiClient.get<Record<string, string>>('/system/settings')
    return response.data
  },

  updateSettings: async (settings: Record<string, string>) => {
    await apiClient.post('/system/settings', settings)
  },

  getSettingsMetadata: async () => {
    const response = await apiClient.get('/system/settings/metadata')
    return response.data
  }
}

export interface AiModelDto {
  id: string
  name: string
  capability: 'TEXT_TO_IMAGE' | 'IMAGE_TO_IMAGE' | 'TAGGING'
  type: 'TAGGER' | 'CLIP'
  version: string
  dimension?: number
  status?: string
  artifactState: 'NOT_INSTALLED' | 'DOWNLOADING' | 'RESTART_REQUIRED' | 'READY' | 'FAILED'
  capabilities: string[]
  errorMessage?: string
}

export const aiModelApi = {
  list: async () => (await apiClient.get<AiModelDto[]>('/ai/models')).data,
  download: async (id: string) => (await apiClient.post<AiModelDto>(`/ai/models/${id}/download`)).data,
  uninstall: async (id: string) => (await apiClient.delete<AiModelDto>(`/ai/models/${id}/artifact`)).data
}
