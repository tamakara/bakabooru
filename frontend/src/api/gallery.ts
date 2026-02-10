import apiClient from './client'

/**
 * 图片数据传输对象
 */
export interface ImageDto {
  /** 图片ID */
  id: number
  /** 图片标题 (通常为文件名) */
  title: string
  /** 原始文件名 */
  fileName: string
  /** 文件扩展名 */
  extension: string
  /** 文件大小 (字节) */
  size: number
  width: number
  height: number
  /** 图片哈希值 (SHA256) */
  hash: string
  /** 查看次数 */
  viewCount: number
  createdAt: string
  updatedAt: string
  /** 原始图片访问URL */
  url: string
  /** 缩略图访问URL */
  thumbnailUrl: string
  /** 关联标签列表 */
  tags: TagDto[]
}

/**
 * 标签数据传输对象
 */
export interface TagDto {
  id: number
  /** 标签名称 */
  name: string
  /** 标签类型 (copyright, character, artist, etc.) */
  type: string
}

/**
 * 分页响应对象
 */
export interface Page<T> {
  content: T[]
  totalPages: number
  totalElements: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export const galleryApi = {
  /**
   * 获取单张图片详情
   * @param id 图片ID
   */
  getImage: async (id: number) => {
    const response = await apiClient.get<ImageDto>(`/images/${id}`)
    return response.data
  },

  /**
   * 删除图片
   * @param id 图片ID
   */
  deleteImage: async (id: number) => {
    await apiClient.delete(`/images/${id}`)
  },

  /**
   * 更新图片信息 (如标题)
   * @param id 图片ID
   * @param dto 更新内容
   */
  updateImage: async (id: number, dto: Partial<ImageDto>) => {
    const response = await apiClient.put<ImageDto>(`/images/${id}`, dto)
    return response.data
  },


  /**
   * 重新生成图片标签
   * @param id 图片ID
   */
  regenerateTags: async (id: number) => {
    const response = await apiClient.post<ImageDto>(`/images/${id}/tags/regenerate`)
    return response.data
  },

  /**
   * 添加标签到图片
   * @param id 图片ID
   * @param tag 标签信息
   */
  addTag: async (id: number, tag: Partial<TagDto>) => {
    const response = await apiClient.post<ImageDto>(`/images/${id}/tags`, tag)
    return response.data
  },

  /**
   * 从图片移除标签
   * @param id 图片ID
   * @param tagId 标签ID
   */
  removeTag: async (id: number, tagId: number) => {
    const response = await apiClient.delete<ImageDto>(`/images/${id}/tags/${tagId}`)
    return response.data
  },

  /**
   * 批量删除图片
   * @param ids 图片ID数组
   */
  deleteImages: async (ids: number[]) => {
    await apiClient.post('/images/batch/delete', ids)
  },

  /**
   * 批量下载图片
   * @param ids 图片ID数组
   */
  downloadImages: async (ids: number[]) => {
    const response = await apiClient.post('/images/batch/download', ids, {
      responseType: 'blob'
    })
    return response.data
  }
}


