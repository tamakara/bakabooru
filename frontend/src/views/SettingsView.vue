<script setup lang="ts">
import {ref, watch} from 'vue'
import axios from 'axios'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {aiModelApi, systemApi, type AiModelDto} from '../api/system'
import {authApi} from '../api/auth'
import {
  NButton,
  NCard,
  NForm,
  NFormItemGi,
  NGrid,
  NInput,
  NInputNumber,
  NScrollbar,
  useMessage
} from 'naive-ui'

const message = useMessage()
const queryClient = useQueryClient()

const {data: aiModels} = useQuery({queryKey: ['aiModels'], queryFn: aiModelApi.list})
const modelMutation = useMutation({
  mutationFn: ({id, action}: {id: string, action: 'download' | 'enable' | 'disable'}) => aiModelApi[action](id),
  onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiModels']}),
  onError: () => message.error('模型操作失败')
})

function modelAction(model: AiModelDto) {
  if (model.status === 'READY' || model.status === 'AVAILABLE') {
    modelMutation.mutate({id: model.id, action: model.status === 'READY' ? 'disable' : 'download'})
  } else if (model.status === 'DISABLED') {
    modelMutation.mutate({id: model.id, action: 'enable'})
  }
}

function modelActionLabel(model: AiModelDto) {
  if (model.status === 'AVAILABLE') return '下载'
  if (model.status === 'DOWNLOADING') return '下载中'
  if (model.status === 'DISABLED') return '启用'
  if (model.status === 'READY') return '停用'
  return '重试'
}

// --- 系统设置 ---
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings
})

// 根据数据库配置表定义表单字段
const settingsForm = ref({
  'tag.threshold': 0.61,
  'ai-job.max-attempts': 5,
  'ai-job.retry-base-delay-seconds': 30,
  'ai-job.retry-max-delay-seconds': 1800,
  'upload.completed-retention-days': 7,
  'ai.default-vector-models': 'clip-vit-base-patch32'
})

watch(settings, (newVal) => {
  if (newVal) {
    settingsForm.value = {
      'tag.threshold': Number(newVal['tag.threshold']),
      'ai-job.max-attempts': Number(newVal['ai-job.max-attempts']),
      'ai-job.retry-base-delay-seconds': Number(newVal['ai-job.retry-base-delay-seconds']),
      'ai-job.retry-max-delay-seconds': Number(newVal['ai-job.retry-max-delay-seconds']),
      'upload.completed-retention-days': Number(newVal['upload.completed-retention-days']),
      'ai.default-vector-models': newVal['ai.default-vector-models'] || 'clip-vit-base-patch32'
    }
  }
}, {immediate: true})

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['settings']})
    message.success('设置已保存')
  },
  onError: (error: unknown) => {
    const detail = axios.isAxiosError(error) ? error.response?.data?.message : undefined
    message.error(detail || '保存失败')
  }
})

function handleSaveSettings() {
  updateSettingsMutation.mutate(
    Object.fromEntries(
      Object.entries(settingsForm.value).map(([key, value]) => [key, String(value)])
    )
  )
}

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
  if (!passwordForm.value.password.trim()) {
    message.error('密码不能为空')
    return
  }
  updatePasswordMutation.mutate(passwordForm.value.password)
}
</script>

<template>
  <n-scrollbar class="h-full">
    <div class="p-4 max-w-2xl mx-auto space-y-4">

      <!-- 系统设置 -->
      <n-card size="small">
        <template #header>
          <span class="text-sm font-medium">系统设置</span>
        </template>
        <n-form label-placement="left" label-width="100" size="small">
          <n-grid :cols="2" :x-gap="16" :y-gap="8">
            <n-form-item-gi label="标签阈值">
              <n-input-number
                  v-model:value="settingsForm['tag.threshold']"
                  :min="0"
                  :max="1"
                  :step="0.01"
              />
            </n-form-item-gi>
            <n-form-item-gi label="AI 重试次数">
              <n-input-number
                  v-model:value="settingsForm['ai-job.max-attempts']"
                  :min="1"
                  :max="20"
              />
            </n-form-item-gi>
            <n-form-item-gi label="重试初始延迟">
              <n-input-number
                  v-model:value="settingsForm['ai-job.retry-base-delay-seconds']"
                  :min="1"
                  :max="3600"
              >
                <template #suffix>s</template>
              </n-input-number>
            </n-form-item-gi>
            <n-form-item-gi label="重试最大延迟">
              <n-input-number
                  v-model:value="settingsForm['ai-job.retry-max-delay-seconds']"
                  :min="1"
                  :max="86400"
              >
                <template #suffix>s</template>
              </n-input-number>
            </n-form-item-gi>
            <n-form-item-gi label="任务保留时间">
              <n-input-number
                  v-model:value="settingsForm['upload.completed-retention-days']"
                  :min="1"
                  :max="365"
              >
                <template #suffix>天</template>
              </n-input-number>
            </n-form-item-gi>
            <n-form-item-gi label="默认向量模型">
              <n-input v-model:value="settingsForm['ai.default-vector-models']" size="small" />
            </n-form-item-gi>
          </n-grid>
        </n-form>
        <template #action>
          <n-button
              type="primary"
              size="small"
              @click="handleSaveSettings"
              :loading="updateSettingsMutation.isPending.value"
          >
            保存设置
          </n-button>
        </template>
      </n-card>

      <n-card size="small">
        <template #header><span class="text-sm font-medium">AI 模型</span></template>
        <div class="space-y-2">
          <div v-for="model in aiModels || []" :key="model.id"
               class="flex items-center justify-between gap-3 border-b border-gray-100 dark:border-gray-800 pb-2 last:border-0">
            <div class="min-w-0">
              <div class="text-sm truncate">{{ model.name }}</div>
              <div class="text-xs text-gray-500">{{ model.capability }} · {{ model.status }}</div>
            </div>
            <n-button size="small" :disabled="model.status === 'DOWNLOADING'" @click="modelAction(model)">
              {{ modelActionLabel(model) }}
            </n-button>
          </div>
        </div>
      </n-card>

      <!-- 安全设置 -->
      <n-card size="small">
        <template #header>
          <span class="text-sm font-medium">安全设置</span>
        </template>
        <n-form label-placement="left" label-width="100" size="small">
          <n-grid :cols="2" :x-gap="16" :y-gap="8">
            <n-form-item-gi label="新密码">
              <n-input
                  type="password"
                  v-model:value="passwordForm.password"
                  placeholder="输入新密码"
              />
            </n-form-item-gi>
            <n-form-item-gi label="确认密码">
              <n-input
                  type="password"
                  v-model:value="passwordForm.confirmPassword"
                  placeholder="再次输入"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>
        <template #action>
          <n-button
              type="primary"
              size="small"
              @click="handleUpdatePassword"
              :loading="updatePasswordMutation.isPending.value"
          >
            更新密码
          </n-button>
        </template>
      </n-card>

    </div>
  </n-scrollbar>
</template>

