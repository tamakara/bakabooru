import apiClient from './client'

export const systemApi = {
  getSettings: async () => {
    const response = await apiClient.get<Record<string, string>>('/system/settings')
    return response.data
  },

  updateSettings: async (settings: Record<string, string>) => {
    return (await apiClient.post('/system/settings', settings)).data
  },

  getSettingsMetadata: async () => {
    const response = await apiClient.get('/system/settings/metadata')
    return response.data
  }
}

export interface ModelDto {
  id: string
  name: string
  capability: 'TEXT_TO_IMAGE' | 'IMAGE_TO_IMAGE' | 'TAGGING'
  type: 'TAGGER' | 'CLIP'
  version: string
  dimension?: number
  status?: string
  artifactState: 'NOT_INSTALLED' | 'DOWNLOADING' | 'RESTART_REQUIRED' | 'READY' | 'FAILED'
  enabled: boolean
  restartRequired: boolean
  capabilities: string[]
  errorMessage?: string
}

export const modelManagerApi = {
  list: async () => (await apiClient.get<ModelDto[]>('/ai/models')).data,
  download: async (id: string) => (await apiClient.post<ModelDto>(`/ai/models/${id}/download`)).data,
  enable: async (id: string) => (await apiClient.post<ModelDto>(`/ai/models/${id}/enable`)).data
}
