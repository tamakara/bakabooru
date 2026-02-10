import apiClient from './client'
import type {TagDto} from './gallery'


export const tagsApi = {
  listTags: async (query?: string) => {
    const response = await apiClient.get<TagDto[]>('/tags', {
      params: {query}
    })
    return response.data
  }
}
