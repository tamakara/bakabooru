import apiClient from './client'

export interface UploadTask {
  id: string
  filename: string
  size: number
  status: 'PENDING' | 'UPLOADING' | 'PROCESSING' | 'TAGGING' | 'SAVING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export const uploadApi = {
  uploadFile: async (file: File, enableTagging: boolean = true) => {
    const formData = new FormData()
    // 处理文件名，确保不包含路径
    // 有些浏览器在扫描文件夹时可能会把路径带入文件名，或者用户希望强制去除路径
    const filename = file.name.split(/[/\\]/).pop() || file.name
    formData.append('file', file, filename)
    formData.append('enableTagging', enableTagging.toString())

    const response = await apiClient.post<UploadTask>('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  listTasks: async () => {
    const response = await apiClient.get<UploadTask[]>('/upload/tasks')
    return response.data
  },

  deleteTask: async (id: string) => {
    await apiClient.delete(`/upload/tasks/${id}`)
  },

  clearTasks: async () => {
    await apiClient.delete('/upload/tasks')
  }
}
