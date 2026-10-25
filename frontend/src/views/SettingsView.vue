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
import { modelManagerApi, systemApi, type ModelDto } from '../api/system'
import { authApi } from '../api/auth'

type SettingsForm = {
  'analysis.tag-threshold': number | null
  'analysis.max-attempts': number | null
  'analysis.retry-base-delay-seconds': number | null
  'analysis.retry-max-delay-seconds': number | null
  'upload.completed-retention-days': number | null
  'inference.concurrency': number | null
}

const defaults = {
  'analysis.tag-threshold': 0.61,
  'analysis.max-attempts': 5,
  'analysis.retry-base-delay-seconds': 30,
  'analysis.retry-max-delay-seconds': 1800,
  'upload.completed-retention-days': 7,
  'inference.concurrency': 1
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

const modelsQuery = useQuery({
  queryKey: ['models'],
  queryFn: modelManagerApi.list,
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
    'analysis.tag-threshold': numberSetting(value['analysis.tag-threshold'], defaults['analysis.tag-threshold']),
    'analysis.max-attempts': numberSetting(value['analysis.max-attempts'], defaults['analysis.max-attempts']),
    'analysis.retry-base-delay-seconds': numberSetting(value['analysis.retry-base-delay-seconds'], defaults['analysis.retry-base-delay-seconds']),
    'analysis.retry-max-delay-seconds': numberSetting(value['analysis.retry-max-delay-seconds'], defaults['analysis.retry-max-delay-seconds']),
    'upload.completed-retention-days': numberSetting(value['upload.completed-retention-days'], defaults['upload.completed-retention-days']),
    'inference.concurrency': numberSetting(value['inference.concurrency'], defaults['inference.concurrency'])
  }
}, { immediate: true })

function errorMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) return fallback
  return error.response?.data?.message || error.response?.data?.detail || fallback
}

function validateSettings() {
  const form = settingsForm.value
  const numericKeys: Array<keyof SettingsForm> = [
    'analysis.tag-threshold',
    'analysis.max-attempts',
    'analysis.retry-base-delay-seconds',
    'analysis.retry-max-delay-seconds',
    'upload.completed-retention-days',
    'inference.concurrency'
  ]
  if (numericKeys.some(key => form[key] === null || !Number.isFinite(Number(form[key])))) {
    message.error('请填写完整的数值设置')
    return false
  }
  if (Number(form['analysis.retry-max-delay-seconds']) < Number(form['analysis.retry-base-delay-seconds'])) {
    message.error('最大重试间隔不能小于初始重试间隔')
    return false
  }
  return true
}

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: async (result: { restartRequired?: boolean, restartServices?: string[], message?: string }) => {
    await queryClient.invalidateQueries({ queryKey: ['settings'] })
    if (result.restartRequired) {
      message.warning(`${result.message || '设置已保存'}，请重启${(result.restartServices || []).join('、') || '相关服务'}后生效`)
    } else {
      message.success(result.message || '设置已保存')
    }
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

const tagModels = computed(() => (modelsQuery.data.value ?? []).filter(model => model.type === 'TAGGER'))
const clipModels = computed(() => (modelsQuery.data.value ?? []).filter(model => model.type === 'CLIP'))
const modelGroups = computed(() => [...tagModels.value, ...clipModels.value])

const modelMutation = useMutation({
  mutationFn: (id: string) => modelManagerApi.download(id),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['models'] })
    message.success('模型下载任务已提交')
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

const enableModelMutation = useMutation({
  mutationFn: modelManagerApi.enable,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['models'] })
    message.success('模型已启用')
  },
  onError: (error: unknown) => message.error(errorMessage(error, '启用模型失败'))
})

function modelStatusLabel(model: ModelDto) {
  if (model.artifactState === 'READY') return '已就绪'
  if (model.artifactState === 'RESTART_REQUIRED') return '已下载，重启后可用'
  if (model.artifactState === 'DOWNLOADING') return '下载中'
  if (model.artifactState === 'FAILED') return '下载失败'
  return '未下载'
}

function modelStatusType(model: ModelDto): 'success' | 'info' | 'warning' | 'error' | 'default' {
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
              <n-form-item label="标签置信度阈值" path="analysis.tag-threshold">
                <n-input-number v-model:value="settingsForm['analysis.tag-threshold']" :min="0" :max="1" :step="0.01" :precision="2" class="w-full" placeholder="0.61" />
              </n-form-item>
              <n-form-item label="任务最大重试次数" path="analysis.max-attempts">
                <n-input-number v-model:value="settingsForm['analysis.max-attempts']" :min="1" :max="20" :precision="0" class="w-full" placeholder="5" />
              </n-form-item>
              <n-form-item label="重试初始间隔（秒）" path="analysis.retry-base-delay-seconds">
                <n-input-number v-model:value="settingsForm['analysis.retry-base-delay-seconds']" :min="1" :max="3600" :precision="0" class="w-full" placeholder="30" />
              </n-form-item>
              <n-form-item label="重试最大间隔（秒）" path="analysis.retry-max-delay-seconds">
                <n-input-number v-model:value="settingsForm['analysis.retry-max-delay-seconds']" :min="1" :max="86400" :precision="0" class="w-full" placeholder="1800" />
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

      <n-card size="small" title="推理设置">
        <n-spin :show="settingsQuery.isLoading.value">
          <n-form :model="settingsForm" label-placement="top" size="medium">
            <div class="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
              <n-form-item label="推理并发数" path="inference.concurrency">
                <n-input-number v-model:value="settingsForm['inference.concurrency']" :min="1" :max="64" :precision="0" class="w-full" placeholder="1" />
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

      <n-alert v-if="modelsQuery.isError.value" type="error" title="模型列表加载失败">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <span>无法获取 AI 模型状态。</span>
          <n-button size="small" @click="modelsQuery.refetch()">重新加载</n-button>
        </div>
      </n-alert>

      <n-card size="small" title="模型管理">
        <n-spin :show="modelsQuery.isLoading.value">
          <div v-if="modelGroups.length" class="divide-y divide-gray-100 dark:divide-gray-800">
            <div v-for="model in modelGroups" :key="model.id" class="flex min-h-16 flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0">
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-medium">{{ model.name }}</div>
                <div class="text-xs text-gray-500">{{ model.type === 'TAGGER' ? '标签模型' : 'CLIP 模型' }} · 版本 {{ model.version }}<span v-if="model.dimension"> · {{ model.dimension }} 维</span></div>
              </div>
              <n-space align="center" size="small">
                <n-tag size="small" :type="modelStatusType(model)">{{ modelStatusLabel(model) }}</n-tag>
                <n-tag v-if="model.enabled" size="small" type="success">当前启用</n-tag>
                <n-button v-else-if="model.artifactState === 'READY'" size="small" :loading="enableModelMutation.isPending.value" @click="enableModelMutation.mutate(model.id)">启用</n-button>
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
          <n-empty v-else-if="!modelsQuery.isLoading.value" size="small" description="暂无模型" />
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
