<script setup lang="ts">
import {ref, watch} from 'vue'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {systemApi} from '../api/system'
import {authApi} from '../api/auth'
import {
  NButton,
  NCard,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NPopconfirm,
  NSelect,
  NSpace,
  NUpload,
  useMessage
} from 'naive-ui'

const message = useMessage()
const queryClient = useQueryClient()

// --- 系统设置 ---
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings,
})

const settingsForm = ref<Record<string, string>>({
  'upload.max-file-size': '',
  'upload.allowed-extensions': '',
  'upload.concurrency': '3',
  'upload.poll-interval': '1000',
  'file.thumbnail.quality': '80',
  'file.thumbnail.max-size': '800',
  'tag.threshold': '0.6',
  'llm.url': '',
  'llm.api-key': '',
  'llm.model': ''
})

const thumbnailSizeOptions = [
  { label: '500x500', value: '500' },
  { label: '800x800', value: '800' },
  { label: '1000x1000', value: '1000' },
  { label: '1500x1500', value: '1500' },
  { label: '2000x2000', value: '2000' }
]

watch(settings, (newVal) => {
  if (newVal) {
    const form = {...settingsForm.value, ...newVal}
    if (form['upload.max-file-size']) {
      const bytes = parseInt(form['upload.max-file-size'])
      if (!isNaN(bytes)) {
        form['upload.max-file-size'] = (bytes / (1024 * 1024)).toString()
      }
    }
    settingsForm.value = form
  }
}, { immediate: true })

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['settings']})
    message.success('设置已更新')
  }
})

function handleSaveSettings() {
  const form = {...settingsForm.value}
  if (form['upload.max-file-size']) {
    const mb = parseFloat(form['upload.max-file-size'])
    if (!isNaN(mb)) {
      form['upload.max-file-size'] = Math.floor(mb * 1024 * 1024).toString()
    }
  }
  updateSettingsMutation.mutate(form)
}

const clearCacheMutation = useMutation({
  mutationFn: systemApi.clearCache,
  onSuccess: () => {
    message.success('缓存已清空')
  },
  onError: () => {
    message.error('清空缓存失败')
  }
})

// --- 备份 ---
const restoreMutation = useMutation({
  mutationFn: systemApi.restoreBackup,
  onSuccess: () => {
    message.success('还原完成，请刷新页面。')
  },
  onError: () => {
    message.error('还原失败')
  }
})

// --- Password ---
const passwordForm = ref({
  password: '',
  confirmPassword: ''
})

const updatePasswordMutation = useMutation({
  mutationFn: authApi.updatePassword,
  onSuccess: () => {
    message.success('密码已更新')
    passwordForm.value.password = ''
    passwordForm.value.confirmPassword = ''
  },
  onError: () => {
    message.error('更新密码失败')
  }
})

function handleUpdatePassword() {
  if (passwordForm.value.password !== passwordForm.value.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  updatePasswordMutation.mutate(passwordForm.value.password)
}

const resetSystemMutation = useMutation({
  mutationFn: systemApi.resetSystem,
  onSuccess: () => {
    message.success('系统已重置，即将刷新页面。')
    setTimeout(() => window.location.reload(), 1500)
  },
  onError: () => {
    message.error('重置失败')
  }
})

</script>

<template>
  <div class="flex flex-col gap-4 h-full">

    <!-- System Settings -->
    <n-card title="系统设置">
      <n-form>
        <n-divider title-placement="left">Upload 设置</n-divider>

        <n-form-item label="最大上传大小 (MB)">
          <n-input v-model:value="settingsForm['upload.max-file-size']"/>
        </n-form-item>
        <n-form-item label="允许的扩展名 (逗号分隔)">
          <n-input v-model:value="settingsForm['upload.allowed-extensions']"/>
        </n-form-item>
        <n-form-item label="最大同时上传数量">
          <n-input v-model:value="settingsForm['upload.concurrency']" placeholder="3"/>
        </n-form-item>
        <n-form-item label="任务列表轮询间隔 (ms)">
          <n-input v-model:value="settingsForm['upload.poll-interval']" placeholder="1000"/>
        </n-form-item>
        <n-form-item label="缩略图质量 (1-100)">
          <n-input v-model:value="settingsForm['file.thumbnail.quality']" placeholder="80"/>
        </n-form-item>
        <n-form-item label="缩略图最大分辨率">
          <n-select v-model:value="settingsForm['file.thumbnail.max-size']" :options="thumbnailSizeOptions" />
        </n-form-item>

        <n-divider title-placement="left">Tag 设置</n-divider>

        <n-form-item label="阈值">
          <n-input v-model:value="settingsForm['tag.threshold']" placeholder="0.61"/>
        </n-form-item>

        <n-divider title-placement="left">LLM 设置</n-divider>
        <n-form-item label="API URL">
          <n-input v-model:value="settingsForm['llm.url']" placeholder="https://api.openai.com/v1/chat/completions" />
        </n-form-item>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <n-form-item label="API Key">
            <n-input v-model:value="settingsForm['llm.api-key']" type="password" show-password-on="click" />
          </n-form-item>
          <n-form-item label="Model Name">
            <n-input v-model:value="settingsForm['llm.model']" placeholder="gpt-3.5-turbo" />
          </n-form-item>
        </div>

        <n-space>
          <n-button type="primary" @click="handleSaveSettings">保存设置</n-button>
          <n-popconfirm @positive-click="clearCacheMutation.mutate()">
            <template #trigger>
              <n-button type="warning">清空缓存</n-button>
            </template>
            确定要清空临时文件缓存吗？
          </n-popconfirm>
        </n-space>
      </n-form>
    </n-card>

    <!-- Backup & Restore -->
    <n-card title="备份与还原">
      <n-space>
        <n-button @click="systemApi.downloadBackup">下载备份</n-button>
        <n-upload
            :custom-request="({ file }) => restoreMutation.mutate(file.file as File)"
            :show-file-list="false"
        >
          <n-button type="warning">还原备份</n-button>
        </n-upload>
        <n-popconfirm @positive-click="resetSystemMutation.mutate()">
          <template #trigger>
            <n-button type="error">删除所有数据</n-button>
          </template>
          确定要删除所有数据并重置系统吗？此操作不可恢复！
        </n-popconfirm>
      </n-space>
    </n-card>

    <!-- Update Password -->
    <n-card title="更新密码">
      <n-form>
        <n-form-item label="新密码">
          <n-input type="password" v-model:value="passwordForm.password"/>
        </n-form-item>
        <n-form-item label="确认密码">
          <n-input type="password" v-model:value="passwordForm.confirmPassword"/>
        </n-form-item>
        <n-space>
          <n-button type="primary" @click="handleUpdatePassword">更新密码</n-button>
        </n-space>
      </n-form>
    </n-card>

  </div>
</template>

