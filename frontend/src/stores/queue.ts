import {defineStore} from 'pinia'
import {ref} from 'vue'
import PQueue from 'p-queue'
import {useQuery} from '@tanstack/vue-query'
import {uploadApi} from '../api/upload'
import {systemApi} from '../api/system'

export const useQueueStore = defineStore('queue', () => {
  // 获取系统设置
  const {data: settings} = useQuery({
    queryKey: ['settings'],
    queryFn: systemApi.getSettings
  })

  // 最大同时上传数量
  const concurrency = parseInt(settings.value?.['upload.concurrency'] ?? '3')

  // 上传队列
  const queue = new PQueue({concurrency})

  // 队列统计
  const waitingCount = ref(0)
  const processingCount = ref(0)

  // 更新统计数据
  const updateStats = () => {
    waitingCount.value = queue.size
    processingCount.value = queue.pending
  }

  // 监听队列事件
  queue.on('add', updateStats)
  queue.on('next', updateStats)
  queue.on('idle', updateStats)
  queue.on('active', updateStats) // 当任务开始处理时触发

  // 添加文件到上传队列
  const addFileToQueue = async (file: File, enableTagging: boolean) => {
    await queue.add(async () => {
      updateStats() // 确保状态更新
      try {
        await uploadApi.uploadFile(file, enableTagging)
      } catch (error) {
        console.error(`Upload failed for ${file.name}:`, error)
        // 这里可以扩展添加客户端错误通知
      } finally {
        updateStats()
      }
    })
    updateStats()
  }

  return {
    waitingCount,
    processingCount,
    addFileToQueue
  }
})
