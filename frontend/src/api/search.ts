import apiClient from './client'
import type { Page, ImageThumbnailDto } from './gallery'

export interface SearchRequestDto {
  tags?: string
  keyword?: string
  semanticQuery?: string  // 语义描述搜索
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
}

export const searchApi = {
  search: async (request: SearchRequestDto) => {
    const response = await apiClient.post<Page<ImageThumbnailDto>>('/search', request)
    return response.data
  },

  queryParse: async (query: string): Promise<string> => {
    const response = await apiClient.post<string>('/search/parse', query, {
      headers: {
        'Content-Type': 'text/plain'
      }
    })
    return response.data
  }
}
