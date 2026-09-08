<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  NAlert,
  NButton,
  NCard,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NScrollbar,
  NSpace,
  NSpin,
  NTag,
  useMessage
} from 'naive-ui'
import { aiModelApi, systemApi, type AiModelDto } from '../api/system'
import { authApi } from '../api/auth'

type SettingsForm = {
  'tag.threshold': number | null
  'ai-job.max-attempts': number | null
  'ai-job.retry-base-delay-seconds': number | null
  'ai-job.retry-max-delay-seconds': number | null
  'upload.completed-retention-days': number | null
  'ai.service-url': string
  'ai.inference-concurrency': number | null
}

const defaults = {
  'tag.threshold': 0.61,
  'ai-job.max-attempts': 5,
  'ai-job.retry-base-delay-seconds': 30,
  'ai-job.retry-max-delay-seconds': 1800,
  'upload.completed-retention-days': 7,
  'ai.service-url': 'http://ai-service:8000',
  'ai.inference-concurrency': 1
} satisfies SettingsForm

const message = useMessage()
const queryClient = useQueryClient()
const settingsForm = ref<SettingsForm>({ ...defaults })
const passwordForm = ref({ password: '', confirmPassword: '' })
const downloadingModelId = ref<string | null>(null)

const settingsQuery = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings,
  retry: 1
})

const aiModelsQuery = useQuery({
  queryKey: ['aiModels'],
  queryFn: aiModelApi.list,
  refetchInterval: 3000,
  retry: 1
})

function numberSetting(value: string | undefined, fallback: number) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

watch(settingsQuery.data, value => {
  if (!value) return
  settingsForm.value = {
    'tag.threshold': numberSetting(value['tag.threshold'], defaults['tag.threshold']),
    'ai-job.max-attempts': numberSetting(value['ai-job.max-attempts'], defaults['ai-job.max-attempts']),
    'ai-job.retry-base-delay-seconds': numberSetting(value['ai-job.retry-base-delay-seconds'], defaults['ai-job.retry-base-delay-seconds']),
    'ai-job.retry-max-delay-seconds': numberSetting(value['ai-job.retry-max-delay-seconds'], defaults['ai-job.retry-max-delay-seconds']),
    'upload.completed-retention-days': numberSetting(value['upload.completed-retention-days'], defaults['upload.completed-retention-days']),
    'ai.service-url': value['ai.service-url'] || defaults['ai.service-url'],
    'ai.inference-concurrency': numberSetting(value['ai.inference-concurrency'], defaults['ai.inference-concurrency'])
  }
}, { immediate: true })

function errorMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) return fallback
  return error.response?.data?.message || error.response?.data?.detail || fallback
}

function validateSettings() {
  const form = settingsForm.value
  const numericKeys: Array<keyof SettingsForm> = [
    'tag.threshold',
    'ai-job.max-attempts',
    'ai-job.retry-base-delay-seconds',
    'ai-job.retry-max-delay-seconds',
    'upload.completed-retention-days',
    'ai.inference-concurrency'
  ]
  if (numericKeys.some(key => form[key] === null || !Number.isFinite(Number(form[key])))) {
    message.error('请填写完整的数值设置')
    return false
  }
  if (!form['ai.service-url'].trim()) {
    message.error('AI 服务地址不能为空')
    return false
  }
  if (Number(form['ai-job.retry-max-delay-seconds']) < Number(form['ai-job.retry-base-delay-seconds'])) {
    message.error('最大重试间隔不能小于初始重试间隔')
    return false
  }
  return true
}

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['settings'] })
    message.success('设置已保存')
  },
  onError: (error: unknown) => message.error(errorMessage(error, '保存设置失败'))
})

function saveSettings() {
  if (!validateSettings()) return
  const payload = Object.fromEntries(
    Object.entries(settingsForm.value).map(([key, value]) => [key, String(value).trim()])
  )
  updateSettingsMutation.mutate(payload)
}

const tagModels = computed(() => (aiModelsQuery.data.value ?? []).filter(model => model.type === 'TAGGER'))
const clipModels = computed(() => (aiModelsQuery.data.value ?? []).filter(model => model.type === 'CLIP'))
const modelGroups = computed(() => [
  { title: '标签模型', models: tagModels.value },
  { title: 'CLIP 模型', models: clipModels.value }
])

const modelMutation = useMutation({
  mutationFn: (id: string) => aiModelApi.download(id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['aiModels'] })
    message.success('模型下载任务已提交，下载完成后请重启 AI 服务')
  },
  onError: (error: unknown) => message.error(errorMessage(error, '模型下载失败')),
  onSettled: () => {
    downloadingModelId.value = null
  }
})

function downloadModel(id: string) {
  if (modelMutation.isPending.value) return
  downloadingModelId.value = id
  modelMutation.mutate(id)
}

function modelStatusLabel(model: AiModelDto) {
  if (model.artifactState === 'READY') return '已就绪'
  if (model.artifactState === 'RESTART_REQUIRED') return '已下载，重启后可用'
  if (model.artifactState === 'DOWNLOADING') return '下载中'
  if (model.artifactState === 'FAILED') return '下载失败'
  return '未下载'
}

function modelStatusType(model: AiModelDto): 'success' | 'info' | 'warning' | 'error' | 'default' {
  if (model.artifactState === 'READY') return 'success'
  if (model.artifactState === 'RESTART_REQUIRED') return 'warning'
  if (model.artifactState === 'DOWNLOADING') return 'info'
  if (model.artifactState === 'FAILED') return 'error'
  return 'default'
}

const updatePasswordMutation = useMutation({
  mutationFn: authApi.updatePassword,
  onSuccess: () => {
    passwordForm.value = { password: '', confirmPassword: '' }
    message.success('密码已更新')
  },
  onError: (error: unknown) => message.error(errorMessage(error, '更新密码失败'))
})

function updatePassword() {
  if (!passwordForm.value.password.trim()) {
    message.error('密码不能为空')
    return
  }
  if (passwordForm.value.password !== passwordForm.value.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  updatePasswordMutation.mutate(passwordForm.value.password)
}
</script>

<template>
  <n-scrollbar class="h-full">
    <main class="mx-auto max-w-5xl space-y-4 p-4 sm:p-6">
      <n-alert v-if="settingsQuery.isError.value" type="error" title="设置加载失败">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span>当前显示默认值。请检查后端服务后重试，保存前请确认各项配置。</span>
          <n-button size="small" @click="settingsQuery.refetch()">重新加载</n-button>
        </div>
      </n-alert>

      <n-card size="small" title="分析设置">
        <n-spin :show="settingsQuery.isLoading.value">
          <n-form :model="settingsForm" label-placement="top" size="medium">
            <div class="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
              <n-form-item label="标签置信度阈值" path="tag.threshold">
                <n-input-number v-model:value="settingsForm['tag.threshold']" :min="0" :max="1" :step="0.01" :precision="2" class="w-full" placeholder="0.61" />
              </n-form-item>
              <n-form-item label="任务最大重试次数" path="ai-job.max-attempts">
                <n-input-number v-model:value="settingsForm['ai-job.max-attempts']" :min="1" :max="20" :precision="0" class="w-full" placeholder="5" />
              </n-form-item>
              <n-form-item label="重试初始间隔（秒）" path="ai-job.retry-base-delay-seconds">
                <n-input-number v-model:value="settingsForm['ai-job.retry-base-delay-seconds']" :min="1" :max="3600" :precision="0" class="w-full" placeholder="30" />
              </n-form-item>
              <n-form-item label="重试最大间隔（秒）" path="ai-job.retry-max-delay-seconds">
                <n-input-number v-model:value="settingsForm['ai-job.retry-max-delay-seconds']" :min="1" :max="86400" :precision="0" class="w-full" placeholder="1800" />
              </n-form-item>
              <n-form-item label="已完成上传任务保留天数" path="upload.completed-retention-days">
                <n-input-number v-model:value="settingsForm['upload.completed-retention-days']" :min="1" :max="365" :precision="0" class="w-full" placeholder="7" />
              </n-form-item>
            </div>
          </n-form>
        </n-spin>
        <template #footer>
          <n-space justify="end">
            <n-button type="primary" :loading="updateSettingsMutation.isPending.value" @click="saveSettings">保存设置</n-button>
          </n-space>
        </template>
      </n-card>

      <n-card size="small" title="AI 服务">
        <n-spin :show="settingsQuery.isLoading.value">
          <n-form :model="settingsForm" label-placement="top" size="medium">
            <div class="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
              <n-form-item label="服务地址" path="ai.service-url">
                <n-input v-model:value="settingsForm['ai.service-url']" placeholder="http://ai-service:8000" />
              </n-form-item>
              <n-form-item label="推理并发数" path="ai.inference-concurrency">
                <n-input-number v-model:value="settingsForm['ai.inference-concurrency']" :min="1" :max="64" :precision="0" class="w-full" placeholder="1" />
              </n-form-item>
              <n-form-item label="推理设备">
                <n-input value="CUDA（固定 GPU 推理）" readonly />
              </n-form-item>
              <n-form-item label="模型缓存目录">
                <n-input value="/model_cache（由容器配置管理）" readonly />
              </n-form-item>
            </div>
          </n-form>
        </n-spin>
        <template #footer>
          <n-space justify="end">
            <n-button type="primary" :loading="updateSettingsMutation.isPending.value" @click="saveSettings">保存设置</n-button>
          </n-space>
        </template>
      </n-card>

      <n-alert v-if="aiModelsQuery.isError.value" type="error" title="模型列表加载失败">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span>无法获取 AI 模型状态。</span>
          <n-button size="small" @click="aiModelsQuery.refetch()">重新加载</n-button>
        </div>
      </n-alert>

      <n-card v-for="group in modelGroups" :key="group.title" size="small" :title="group.title">
        <n-spin :show="aiModelsQuery.isLoading.value">
          <div v-if="group.models.length" class="divide-y divide-gray-100 dark:divide-gray-800">
            <div v-for="model in group.models" :key="model.id" class="flex min-h-16 flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium">{{ model.name }}</div>
                <div class="text-xs text-gray-500">版本 {{ model.version }}<span v-if="model.dimension"> · {{ model.dimension }} 维</span></div>
              </div>
              <n-space align="center" size="small">
                <n-tag size="small" :type="modelStatusType(model)">{{ modelStatusLabel(model) }}</n-tag>
                <n-button
                  v-if="model.artifactState === 'NOT_INSTALLED' || model.artifactState === 'FAILED'"
                  size="small"
                  :loading="downloadingModelId === model.id"
                  :disabled="modelMutation.isPending.value && downloadingModelId !== model.id"
                  @click="downloadModel(model.id)"
                >
                  {{ model.artifactState === 'FAILED' ? '重试下载' : '下载' }}
                </n-button>
              </n-space>
              <div v-if="model.errorMessage" class="basis-full break-words text-xs text-red-500">{{ model.errorMessage }}</div>
            </div>
          </div>
          <n-empty v-else-if="!aiModelsQuery.isLoading.value" size="small" description="暂无模型" />
        </n-spin>
      </n-card>

      <n-card size="small" title="安全设置">
        <n-form :model="passwordForm" label-placement="top" size="medium">
          <div class="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
            <n-form-item label="新密码" path="password">
              <n-input v-model:value="passwordForm.password" type="password" show-password-on="click" autocomplete="new-password" placeholder="输入新密码" />
            </n-form-item>
            <n-form-item label="确认密码" path="confirmPassword">
              <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" autocomplete="new-password" placeholder="再次输入新密码" @keyup.enter="updatePassword" />
            </n-form-item>
          </div>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button type="primary" :loading="updatePasswordMutation.isPending.value" @click="updatePassword">更新密码</n-button>
          </n-space>
        </template>
      </n-card>
    </main>
  </n-scrollbar>
</template>
