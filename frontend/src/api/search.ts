import apiClient from './client'
import type { SearchResult, ImageThumbnailDto } from './gallery'

export interface SearchRequestDto {
  tags?: string
  keyword?: string
  semanticQuery?: string  // 语义描述搜索
  status?: 'AVAILABLE' | 'PROCESSING' | 'MISSING'
  randomSeed?: string
  widthMin?: number
  widthMax?: number
  heightMin?: number
  heightMax?: number
  sizeMin?: number
  sizeMax?: number
  page?: number
  size?: number
  sort?: string
  vectorModelIds?: string[]
  tagModelId?: string
}

export const searchApi = {
  search: async (request: SearchRequestDto) => {
    const response = await apiClient.post<SearchResult<ImageThumbnailDto>>('/search', request)
    return response.data
  },

  queryParse: async (query: string): Promise<string> => {
    const response = await apiClient.post<string>('/search/parse', query, {
      headers: {
        'Content-Type': 'text/plain'
      }
    })
    return response.data
  },

  searchByImage: async (file: File, threshold: number, page: number, size: number, vectorModelIds?: string[], tagModelId?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<SearchResult<ImageThumbnailDto>>('/search/image', formData, {
      params: { threshold, page, size, vectorModelIds, tagModelId },
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  }
}
