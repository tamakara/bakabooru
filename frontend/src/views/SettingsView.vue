<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import axios from 'axios'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NGrid,
  NInput,
  NInputNumber,
  NScrollbar,
  NSelect,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui'
import { aiModelApi, systemApi, type AiModelDto } from '../api/system'
import { authApi } from '../api/auth'

type SettingsForm = {
  'tag.threshold': number
  'ai-job.max-attempts': number
  'ai-job.retry-base-delay-seconds': number
  'ai-job.retry-max-delay-seconds': number
  'upload.completed-retention-days': number
  'ai.service-url': string
  'ai.inference-concurrency': number
  'ai.device-mode': string
  'ai.model-cache-dir': string
}

const message = useMessage()
const queryClient = useQueryClient()
const { data: settings } = useQuery({ queryKey: ['settings'], queryFn: systemApi.getSettings })
const { data: aiModels } = useQuery({ queryKey: ['aiModels'], queryFn: aiModelApi.list, refetchInterval: 3000 })

const defaults: SettingsForm = {
  'tag.threshold': 0.61,
  'ai-job.max-attempts': 5,
  'ai-job.retry-base-delay-seconds': 30,
  'ai-job.retry-max-delay-seconds': 1800,
  'upload.completed-retention-days': 7,
  'ai.service-url': 'http://ai-service:8000',
  'ai.inference-concurrency': 1,
  'ai.device-mode': 'auto',
  'ai.model-cache-dir': '/model_cache'
}
const settingsForm = ref<SettingsForm>({ ...defaults })

watch(settings, value => {
  if (!value) return
  settingsForm.value = {
    'tag.threshold': Number(value['tag.threshold'] ?? defaults['tag.threshold']),
    'ai-job.max-attempts': Number(value['ai-job.max-attempts'] ?? defaults['ai-job.max-attempts']),
    'ai-job.retry-base-delay-seconds': Number(value['ai-job.retry-base-delay-seconds'] ?? defaults['ai-job.retry-base-delay-seconds']),
    'ai-job.retry-max-delay-seconds': Number(value['ai-job.retry-max-delay-seconds'] ?? defaults['ai-job.retry-max-delay-seconds']),
    'upload.completed-retention-days': Number(value['upload.completed-retention-days'] ?? defaults['upload.completed-retention-days']),
    'ai.service-url': value['ai.service-url'] ?? defaults['ai.service-url'],
    'ai.inference-concurrency': Number(value['ai.inference-concurrency'] ?? defaults['ai.inference-concurrency']),
    'ai.device-mode': value['ai.device-mode'] ?? defaults['ai.device-mode'],
    'ai.model-cache-dir': value['ai.model-cache-dir'] ?? defaults['ai.model-cache-dir']
  }
}, { immediate: true })

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['settings'] })
    message.success('设置已保存')
  },
  onError: (error: unknown) => {
    const detail = axios.isAxiosError(error) ? error.response?.data?.message : undefined
    message.error(detail || '保存设置失败')
  }
})

function saveSettings() {
  updateSettingsMutation.mutate(Object.fromEntries(
    Object.entries(settingsForm.value).map(([key, value]) => [key, String(value)])
  ))
}

const tagModels = computed(() => (aiModels.value ?? []).filter(model => model.type === 'TAGGER'))
const clipModels = computed(() => (aiModels.value ?? []).filter(model => model.type === 'CLIP'))
const modelMutation = useMutation({
  mutationFn: (id: string) => aiModelApi.download(id),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['aiModels'] }),
  onError: () => message.error('模型下载失败')
})

function modelStatusLabel(model: AiModelDto) {
  if (model.artifactState === 'READY') return '已就绪'
  if (model.artifactState === 'DOWNLOADING') return '下载中'
  if (model.artifactState === 'FAILED') return '下载失败'
  return '未安装'
}

function modelStatusType(model: AiModelDto): 'success' | 'info' | 'warning' | 'error' | 'default' {
  if (model.artifactState === 'READY') return 'success'
  if (model.artifactState === 'DOWNLOADING') return 'info'
  if (model.artifactState === 'FAILED') return 'error'
  return 'default'
}

const passwordForm = ref({ password: '', confirmPassword: '' })
const updatePasswordMutation = useMutation({
  mutationFn: authApi.updatePassword,
  onSuccess: () => {
    passwordForm.value = { password: '', confirmPassword: '' }
    message.success('密码已更新')
  },
  onError: () => message.error('更新密码失败')
})

function updatePassword() {
  if (!passwordForm.value.password.trim()) return message.error('密码不能为空')
  if (passwordForm.value.password !== passwordForm.value.confirmPassword) return message.error('两次输入的密码不一致')
  updatePasswordMutation.mutate(passwordForm.value.password)
}
</script>

<template>
  <n-scrollbar class="h-full">
    <div class="mx-auto max-w-4xl space-y-4 p-4">
      <n-card size="small" title="分析设置">
        <n-form label-placement="top" size="small">
          <n-grid :cols="2" :x-gap="16" :y-gap="8" responsive="screen">
            <n-form-item label="标签阈值"><n-input-number v-model:value="settingsForm['tag.threshold']" :min="0" :max="1" :step="0.01" /></n-form-item>
            <n-form-item label="任务最大重试次数"><n-input-number v-model:value="settingsForm['ai-job.max-attempts']" :min="1" :max="20" /></n-form-item>
            <n-form-item label="重试初始间隔（秒）"><n-input-number v-model:value="settingsForm['ai-job.retry-base-delay-seconds']" :min="1" :max="3600" /></n-form-item>
            <n-form-item label="重试最大间隔（秒）"><n-input-number v-model:value="settingsForm['ai-job.retry-max-delay-seconds']" :min="1" :max="86400" /></n-form-item>
            <n-form-item label="已完成上传任务保留（天）"><n-input-number v-model:value="settingsForm['upload.completed-retention-days']" :min="1" :max="365" /></n-form-item>
          </n-grid>
        </n-form>
        <template #footer><n-space justify="end"><n-button type="primary" size="small" :loading="updateSettingsMutation.isPending.value" @click="saveSettings">保存设置</n-button></n-space></template>
      </n-card>

      <n-card size="small" title="AI 服务">
        <n-form label-placement="top" size="small">
          <n-grid :cols="2" :x-gap="16" :y-gap="8" responsive="screen">
            <n-form-item label="服务地址"><n-input v-model:value="settingsForm['ai.service-url']" /></n-form-item>
            <n-form-item label="推理并发"><n-input-number v-model:value="settingsForm['ai.inference-concurrency']" :min="1" :max="64" /></n-form-item>
            <n-form-item label="设备模式"><n-select v-model:value="settingsForm['ai.device-mode']" :options="[{ label: '自动', value: 'auto' }, { label: 'CPU', value: 'cpu' }, { label: 'CUDA', value: 'cuda' }]" /></n-form-item>
            <n-form-item label="模型缓存目录"><n-input v-model:value="settingsForm['ai.model-cache-dir']" /></n-form-item>
          </n-grid>
        </n-form>
        <template #footer><n-space justify="end"><span class="text-xs text-gray-500">设备模式和缓存目录保存后会重载模型</span><n-button type="primary" size="small" :loading="updateSettingsMutation.isPending.value" @click="saveSettings">保存设置</n-button></n-space></template>
      </n-card>

      <n-card v-for="group in [{ title: '标签模型', models: tagModels }, { title: 'CLIP 模型', models: clipModels }]" :key="group.title" size="small" :title="group.title">
        <div v-if="group.models.length" class="space-y-2">
          <div v-for="model in group.models" :key="model.id" class="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 pb-2 last:border-0 dark:border-gray-800">
            <div class="min-w-0"><div class="truncate text-sm">{{ model.name }}</div><div class="text-xs text-gray-500">{{ model.version }}<span v-if="model.dimension"> · {{ model.dimension }} 维</span></div></div>
            <n-space align="center" size="small"><n-tag size="small" :type="modelStatusType(model)">{{ modelStatusLabel(model) }}</n-tag><n-button v-if="model.artifactState === 'NOT_INSTALLED' || model.artifactState === 'FAILED'" size="small" :loading="modelMutation.isPending.value" @click="modelMutation.mutate(model.id)">{{ model.artifactState === 'FAILED' ? '重试下载' : '下载' }}</n-button></n-space>
            <div v-if="model.errorMessage" class="basis-full break-words text-xs text-red-500">{{ model.errorMessage }}</div>
          </div>
        </div>
        <div v-else class="text-sm text-gray-500">暂无模型</div>
      </n-card>

      <n-card size="small" title="安全设置">
        <n-form label-placement="top" size="small"><n-grid :cols="2" :x-gap="16" responsive="screen"><n-form-item label="新密码"><n-input v-model:value="passwordForm.password" type="password" /></n-form-item><n-form-item label="确认密码"><n-input v-model:value="passwordForm.confirmPassword" type="password" /></n-form-item></n-grid></n-form>
        <template #footer><n-button type="primary" size="small" :loading="updatePasswordMutation.isPending.value" @click="updatePassword">更新密码</n-button></template>
      </n-card>
    </div>
  </n-scrollbar>
</template>
