import {defineStore} from 'pinia'
import {ref} from 'vue'
import PQueue from 'p-queue'
import {uploadApi} from '../api/upload'

export const useQueueStore = defineStore('queue', () => {
  // 固定并发数
  const concurrency = 3

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
  queue.on('active', updateStats)

  // 添加文件到上传队列
  const addFileToQueue = async (file: File) => {
    await queue.add(async () => {
      updateStats()
      try {
        await uploadApi.uploadFile(file)
      } catch (error) {
        console.error(`Upload failed for ${file.name}:`, error)
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
