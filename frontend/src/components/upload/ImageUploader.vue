<script setup lang="ts">
import { ref } from 'vue'
import {
  NButton,
  NCard,
  NCheckbox,
  NIcon,
  NText,
  NUpload,
  NUploadDragger,
  type UploadCustomRequestOptions,
  type UploadFileInfo,
  useMessage
} from 'naive-ui'
import { Archive24Regular as ArchiveIcon } from '@vicons/fluent'
import { useQueueStore } from '../../stores/queue'

const message = useMessage()
const queueStore = useQueueStore()

// 自动标签生成
const isTaggingEnabled = ref(true)
// 递归扫描
const isRecursiveScan = ref(true)
// 文件列表
const uploadFileList = ref<UploadFileInfo[]>([])
// 用于触发文件夹选择对话框
const folderInputRef = ref<HTMLInputElement | null>(null)

// 将文件加入上传队列
function addToQueue(file: File) {
  queueStore.addFileToQueue(file, isTaggingEnabled.value)
}

// 扫描文件夹并批量上传
const scanFolderBatchUpload = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (!target.files) return

  // 转换为数组并筛选图片文件
  let fileArray = Array.from(target.files)
    .filter(f => f.type.startsWith('image/') && !f.name.startsWith('.'))

  // 根据配置决定是否进行递归过滤
  if (!isRecursiveScan.value) {
    fileArray = fileArray.filter(f => {
      const depth = f.webkitRelativePath?.split('/').length ?? 0
      return depth <= 2
    })
  }

  // 如果有有效文件，加入队列并提示
  if (fileArray.length > 0) {
    message.info(`发现 ${fileArray.length} 张图片，已加入上传队列`)
    fileArray.forEach(addToQueue)
  }

  // 重置 input 值
  target.value = ''
}

// 处理拖拽上传或文件选择上传
const handleDragOrSelectUpload = ({ file }: UploadCustomRequestOptions) => {
  // 仅处理图片类型
  if (file.file && file.file.type.startsWith('image/')) {
    addToQueue(file.file)
  }

  // 立即从 UI 列表中移除，因为上传进度和状态由任务队列组件统一管理
  const index = uploadFileList.value.findIndex(f => f.id === file.id)
  if (index > -1) uploadFileList.value.splice(index, 1)
}

// 触发文件夹选择对话框
const openFolderDialog = () => {
  folderInputRef.value?.click()
}
</script>

<template>
  <n-card title="上传图片">
    <div class="flex flex-col gap-4">
      <!-- 顶部控制栏：选项与操作按钮 -->
      <div class="flex flex-wrap items-center gap-4">
        <n-checkbox v-model:checked="isTaggingEnabled">生成标签</n-checkbox>
        <n-checkbox v-model:checked="isRecursiveScan">递归扫描</n-checkbox>
        <n-button @click="openFolderDialog">
          上传文件夹
        </n-button>
        <!-- 隐藏的文件夹 input -->
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

      <!-- 拖拽/点击上传区域 -->
      <n-upload
          v-model:file-list="uploadFileList"
          multiple
          accept="image/*"
          :show-file-list="false"
          :custom-request="handleDragOrSelectUpload"
      >
        <n-upload-dragger>
          <div style="margin-bottom: 12px">
            <n-icon size="48" :depth="3">
              <archive-icon/>
            </n-icon>
          </div>
          <n-text style="font-size: 16px">
            点击或拖拽文件/文件夹到此处上传
          </n-text>
        </n-upload-dragger>
      </n-upload>
    </div>
  </n-card>
</template>

