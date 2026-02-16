import apiClient from './client'

/**
 * 上传任务 (简化版，用于后端队列)
 */
export interface UploadTask {
  id: string
  filename: string
  size: number
  tempFilePath?: string
  errorMessage?: string
}

/**
 * 任务列表信息
 */
export interface TasksInfoDto {
  /** 等待处理的任务数量 */
  pendingCount: number
  /** 当前正在处理的任务 */
  processingTask: UploadTask | null
  /** 失败的任务列表 */
  failedTasks: UploadTask[]
}

export const uploadApi = {
  /**
   * 上传文件
   */
  uploadFile: async (file: File) => {
    const formData = new FormData()
    // 处理文件名，确保不包含路径
    const filename = file.name.split(/[/\\]/).pop() || file.name
    formData.append('file', file, filename)

    await apiClient.post('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  /**
   * 获取任务列表信息
   */
  getTasksInfo: async () => {
    const response = await apiClient.get<TasksInfoDto>('/upload/tasks')
    return response.data
  },

  /**
   * 重试任务
   */
  retryTask: async (id: string) => {
    await apiClient.post(`/upload/tasks`, null, { params: { id } })
  },

  /**
   * 清空失败任务
   */
  clearFailedTasks: async () => {
    await apiClient.delete('/upload/tasks')
  }
}
