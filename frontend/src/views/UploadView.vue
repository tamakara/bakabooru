<script setup lang="ts">
import {computed, h, ref} from 'vue'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {uploadApi, type UploadTask} from '../api/upload'
import {systemApi} from '../api/system'
import {useQueueStore} from '../stores/queue'
import {
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NEmpty,
  NIcon,
  NScrollbar,
  NSpace,
  NTag,
  NText,
  NTooltip,
  NUpload,
  NUploadDragger,
  type DataTableColumns,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
  useMessage
} from 'naive-ui'
import {
  ArrowUpload24Regular as UploadIcon,
  ArrowSync24Regular as RetryIcon,
  Delete24Regular as DeleteIcon
} from '@vicons/fluent'

const message = useMessage()
const queryClient = useQueryClient()
const queueStore = useQueueStore()

// ===== 上传区域 =====
const isRecursiveScan = ref(true)
const uploadFileList = ref<UploadFileInfo[]>([])
const folderInputRef = ref<HTMLInputElement | null>(null)

function addToQueue(file: File) {
  queueStore.addFileToQueue(file)
}

const scanFolderBatchUpload = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (!target.files) return

  let fileArray = Array.from(target.files)
      .filter(f => f.type.startsWith('image/') && !f.name.startsWith('.'))

  if (!isRecursiveScan.value) {
    fileArray = fileArray.filter(f => {
      const depth = f.webkitRelativePath?.split('/').length ?? 0
      return depth <= 2
    })
  }

  if (fileArray.length > 0) {
    message.info(`已添加 ${fileArray.length} 张图片到队列`)
    fileArray.forEach(addToQueue)
  }
  target.value = ''
}

const handleDragOrSelectUpload = ({file}: UploadCustomRequestOptions) => {
  if (file.file && file.file.type.startsWith('image/')) {
    addToQueue(file.file)
  }
  const index = uploadFileList.value.findIndex(f => f.id === file.id)
  if (index > -1) uploadFileList.value.splice(index, 1)
}

const openFolderDialog = () => folderInputRef.value?.click()

// ===== 任务队列 =====
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings
})

const pollInterval = computed(() => parseInt(settings.value?.['upload.poll-interval'] ?? '1000'))

const {data: tasksInfo} = useQuery({
  queryKey: ['uploadTasksInfo'],
  queryFn: uploadApi.getTasksInfo,
  refetchInterval: pollInterval
})

// 统计
const localWaiting = computed(() => queueStore.waitingCount)
const localUploading = computed(() => queueStore.processingCount)
const serverPending = computed(() => tasksInfo.value?.pendingCount ?? 0)
const processingTask = computed(() => tasksInfo.value?.processingTask)
const failedTasks = computed(() => tasksInfo.value?.failedTasks ?? [])

// 总计待处理数量（本地等待 + 本地上传中 + 服务端待处理）
const totalPending = computed(() => localWaiting.value + localUploading.value + serverPending.value)

// 格式化文件大小
const formatSize = (size: number) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(1) + ' MB'
}

// 重试任务
const {mutate: retryTask} = useMutation({
  mutationFn: uploadApi.retryTask,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['uploadTasksInfo']})
    message.success('已重新加入队列')
  },
  onError: (err: Error) => message.error(`重试失败: ${err.message}`)
})

// 清空失败任务
const {mutate: clearFailedTasks} = useMutation({
  mutationFn: uploadApi.clearFailedTasks,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['uploadTasksInfo']})
    message.success('已清空')
  },
  onError: (err: Error) => message.error(`清空失败: ${err.message}`)
})

// 失败任务表格列
const columns: DataTableColumns<UploadTask> = [
  {
    title: '文件名',
    key: 'filename',
    ellipsis: {tooltip: true},
    render: (row) => h('span', {class: 'font-mono text-xs'}, row.filename)
  },
  {
    title: '大小',
    key: 'size',
    width: 80,
    render: (row) => h('span', {class: 'text-gray-500 text-xs'}, formatSize(row.size))
  },
  {
    title: '错误信息',
    key: 'errorMessage',
    ellipsis: {tooltip: true},
    render: (row) => h('span', {class: 'text-red-500 text-xs'}, row.errorMessage || '-')
  },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render: (row) => h(NSpace, {size: 'small', justify: 'center'}, {
      default: () => [
        h(NTooltip, null, {
          trigger: () => h(NButton, {
            size: 'tiny',
            quaternary: true,
            circle: true,
            onClick: () => retryTask(row.id)
          }, {icon: () => h(NIcon, {size: 14}, {default: () => h(RetryIcon)})}),
          default: () => '重试'
        })
      ]
    })
  }
]
</script>

<template>
  <n-scrollbar class="h-full">
    <div class="p-4 max-w-2xl mx-auto space-y-4">

      <!-- 上传区域 -->
      <n-card size="small">
        <template #header>
          <span class="text-sm font-medium">上传图片</span>
        </template>
        <template #header-extra>
          <div class="flex items-center gap-3">
            <n-checkbox v-model:checked="isRecursiveScan" size="small">递归扫描</n-checkbox>
            <n-button size="small" @click="openFolderDialog">选择文件夹</n-button>
            <input
                type="file"
                ref="folderInputRef"
                webkitdirectory
                directory
                multiple
                class="hidden"
                @change="scanFolderBatchUpload"
            />
          </div>
        </template>
        <n-upload
            v-model:file-list="uploadFileList"
            multiple
            accept="image/*"
            :show-file-list="false"
            :custom-request="handleDragOrSelectUpload"
        >
          <n-upload-dragger>
            <div class="flex flex-col items-center justify-center py-4">
              <n-icon size="32" :depth="3">
                <upload-icon/>
              </n-icon>
              <n-text class="mt-2 text-sm" depth="3">点击或拖拽图片到此处上传</n-text>
            </div>
          </n-upload-dragger>
        </n-upload>
      </n-card>

      <!-- 任务队列 -->
      <n-card size="small">
        <template #header>
          <span class="text-sm font-medium">任务队列</span>
        </template>
        <template #header-extra>
          <n-button
              size="small"
              type="error"
              secondary
              :disabled="failedTasks.length === 0"
              @click="clearFailedTasks()"
          >
            清空失败任务
          </n-button>
        </template>

        <div class="space-y-3">
          <!-- 当前处理状态 -->
          <div class="flex items-center gap-4 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
            <!-- 正在处理 -->
            <div class="flex-1 min-w-0">
              <div class="text-xs text-gray-500 mb-1">正在处理</div>
              <div v-if="processingTask" class="flex items-center gap-2">
                <n-tag type="info" size="small" round>
                  <template #icon>
                    <div class="w-2 h-2 bg-blue-500 rounded-full animate-pulse"/>
                  </template>
                  处理中
                </n-tag>
                <span class="text-sm font-mono truncate">{{ processingTask.filename }}</span>
                <span class="text-xs text-gray-400">({{ formatSize(processingTask.size) }})</span>
              </div>
              <div v-else class="text-sm text-gray-400">无</div>
            </div>

            <!-- 待处理数量 -->
            <div class="text-center px-4 border-l border-gray-200 dark:border-gray-700">
              <div class="text-xs text-gray-500 mb-1">待处理</div>
              <div class="text-xl font-semibold" :class="totalPending > 0 ? 'text-blue-500' : 'text-gray-400'">
                {{ totalPending }}
              </div>
            </div>

            <!-- 失败数量 -->
            <div class="text-center px-4 border-l border-gray-200 dark:border-gray-700">
              <div class="text-xs text-gray-500 mb-1">失败</div>
              <div class="text-xl font-semibold" :class="failedTasks.length > 0 ? 'text-red-500' : 'text-gray-400'">
                {{ failedTasks.length }}
              </div>
            </div>
          </div>

          <!-- 失败任务列表 -->
          <div v-if="failedTasks.length > 0">
            <div class="text-xs text-gray-500 mb-2">失败任务列表</div>
            <n-data-table
                :columns="columns"
                :data="failedTasks"
                size="small"
                :max-height="250"
                :bordered="false"
            />
          </div>
        </div>
      </n-card>

    </div>
  </n-scrollbar>
</template>

