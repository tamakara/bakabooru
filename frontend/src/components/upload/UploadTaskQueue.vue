<script setup lang="ts">
import {computed, h, ref} from 'vue'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {uploadApi, type UploadTask} from '../../api/upload'
import {systemApi} from '../../api/system'
import {useQueueStore} from '../../stores/queue'
import {
  NButton,
  NCard,
  NDataTable,
  NIcon,
  NStatistic,
  NCheckbox,
  NTag,
  type DataTableColumns,
  useMessage
} from 'naive-ui'
import {
  Dismiss24Regular as DismissIcon
} from '@vicons/fluent'

const message = useMessage()
const queryClient = useQueryClient()
const queueStore = useQueueStore()

// 获取系统设置以确定轮询间隔
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings
})

const pollInterval = parseInt(settings.value?.['upload.poll-interval'] ?? '1000')

// 获取任务列表的 Query, 轮询更新
const {data: taskList, refetch} = useQuery({
  queryKey: ['uploadTasks'],
  queryFn: uploadApi.listTasks,
  refetchInterval: pollInterval
})

// 仅显示失败任务的开关
const showFailedOnly = ref(false)

// 任务统计信息
const taskStats = computed(() => {
  const list = taskList.value || []
  return {
    waiting: queueStore.waitingCount, // 待入队 (本地等待上传)
    uploading: queueStore.processingCount, // 上传中 (本地正在上传)
    total: list.length, // 后端任务总数
    pending: list.filter(t => t.status === 'PENDING').length, // 后端待处理
    inQueue: list.filter(t => !['COMPLETED', 'FAILED'].includes(t.status)).length, // 后端处理中
    success: list.filter(t => t.status === 'COMPLETED').length, // 成功
    failed: list.filter(t => t.status === 'FAILED').length // 失败
  }
})

/**
 * 根据筛选条件展示的任务列表
 */
const filteredTaskList = computed(() => {
  if (!taskList.value) return []

  if (showFailedOnly.value) {
    return taskList.value.filter(t => t.status === 'FAILED')
  }
  return taskList.value
})

// 删除单个任务
const {mutate: deleteTask} = useMutation({
  mutationFn: uploadApi.deleteTask,
  onSuccess: () => queryClient.invalidateQueries({queryKey: ['uploadTasks']}),
  onError: (err: Error) => message.error(`删除任务失败: ${err.message}`)
})

// 清空所有任务
const {mutate: clearTasks} = useMutation({
  mutationFn: uploadApi.clearTasks,
  onSuccess: () => {
    refetch()
    message.success('任务列表已清空')

  },
  onError: (err: Error) => message.error(`清空任务失败: ${err.message}`)
})

// 定义表格列
const columns: DataTableColumns<UploadTask> = [
  {title: '文件名', key: 'filename'},
  {
    title: '创建时间',
    key: 'createdAt',
    width: 180,
    render: (row) => row.createdAt ? new Date(row.createdAt).toLocaleString() : '-'
  },
  {
    title: '大小',
    key: 'size',
    width: 100,
    render: (row) => `${(row.size / 1024 / 1024).toFixed(2)} MB`
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row) => {
      const typeMap: Record<string, 'default' | 'success' | 'info' | 'error' | 'warning'> = {
        'COMPLETED': 'success',
        'FAILED': 'error',
        'PROCESSING': 'info',
        'TAGGING': 'info',
        'UPLOADING': 'info',
        'SAVING': 'success',
        'PENDING': 'default'
      }
      return h(NTag, {type: typeMap[row.status] || 'default'}, {default: () => row.status})
    }
  },
  {title: '消息', key: 'errorMessage', ellipsis: {tooltip: true}},
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) => h(NButton, {
      size: 'small',
      quaternary: true,
      circle: true,
      onClick: () => deleteTask(row.id)
    }, {icon: () => h(NIcon, null, {default: () => h(DismissIcon)})})
  }
]
</script>

<template>
  <n-card title="上传任务" class="flex-1 overflow-hidden flex flex-col">
    <template #header-extra>
      <div class="flex items-center gap-4">
        <n-checkbox v-model:checked="showFailedOnly">
          失败任务
        </n-checkbox>
        <n-button  @click="clearTasks()">
          清空全部
        </n-button>
      </div>
    </template>

    <div class="flex flex-col h-full gap-4">
      <!-- 统计信息区域 -->
      <div class="grid grid-cols-7 gap-2 p-3 border border-gray-100 dark:border-gray-800 rounded-lg">
        <n-statistic label="待上传" :value="taskStats.waiting"/>
        <n-statistic label="上传中" :value="taskStats.uploading"/>
        <n-statistic label="总任务" :value="taskStats.total"/>
        <n-statistic label="待处理" :value="taskStats.pending"/>
        <n-statistic label="处理中" :value="taskStats.inQueue"/>
        <n-statistic label="成功" :value="taskStats.success"/>
        <n-statistic label="失败" :value="taskStats.failed"/>
      </div>

      <n-data-table
          :columns="columns"
          :data="filteredTaskList"
          class="flex-1"
          flex-height
          virtual-scroll
      />
    </div>
  </n-card>
</template>

