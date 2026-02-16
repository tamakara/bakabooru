<script setup lang="ts">
import {ref, watch} from 'vue'
import {useMutation, useQuery, useQueryClient} from '@tanstack/vue-query'
import {systemApi} from '../api/system'
import {authApi} from '../api/auth'
import {
  NButton,
  NCard,
  NForm,
  NFormItemGi,
  NGrid,
  NInput,
  NScrollbar,
  NSelect,
  useMessage
} from 'naive-ui'

const message = useMessage()
const queryClient = useQueryClient()

// --- 系统设置 ---
const {data: settings} = useQuery({
  queryKey: ['settings'],
  queryFn: systemApi.getSettings
})

// 缩略图尺寸选项
const thumbnailSizeOptions = [
  {label: '500x500', value: '500'},
  {label: '800x800', value: '800'},
  {label: '1000x1000', value: '1000'},
  {label: '1500x1500', value: '1500'},
  {label: '2000x2000', value: '2000'}
]

// 根据数据库配置表定义表单字段
const settingsForm = ref<Record<string, string>>({
  'upload.poll-interval': '1000',
  'file.thumbnail.size': '800',
  'tag.threshold': '0.61',
  'llm.url': '',
  'llm.api-key': '',
  'llm.model': ''
})

watch(settings, (newVal) => {
  if (newVal) {
    settingsForm.value = {...settingsForm.value, ...newVal}
  }
}, {immediate: true})

const updateSettingsMutation = useMutation({
  mutationFn: systemApi.updateSettings,
  onSuccess: () => {
    queryClient.invalidateQueries({queryKey: ['settings']})
    message.success('设置已保存')
  },
  onError: () => {
    message.error('保存失败')
  }
})

function handleSaveSettings() {
  updateSettingsMutation.mutate(settingsForm.value)
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
            <n-form-item-gi label="轮询间隔">
              <n-input v-model:value="settingsForm['upload.poll-interval']" placeholder="1000">
                <template #suffix>ms</template>
              </n-input>
            </n-form-item-gi>
            <n-form-item-gi label="缩略图尺寸">
              <n-select
                  v-model:value="settingsForm['file.thumbnail.size']"
                  :options="thumbnailSizeOptions"
                  placeholder="选择尺寸"
              />
            </n-form-item-gi>
            <n-form-item-gi label="标签阈值">
              <n-input v-model:value="settingsForm['tag.threshold']" placeholder="0.61"/>
            </n-form-item-gi>
            <n-form-item-gi label="LLM Model">
              <n-input v-model:value="settingsForm['llm.model']" placeholder="gpt-4o"/>
            </n-form-item-gi>
            <n-form-item-gi :span="2" label="LLM URL">
              <n-input v-model:value="settingsForm['llm.url']" placeholder="https://api.openai.com/v1"/>
            </n-form-item-gi>
            <n-form-item-gi :span="2" label="API Key">
              <n-input
                  v-model:value="settingsForm['llm.api-key']"
                  type="password"
                  show-password-on="click"
                  placeholder="sk-..."
              />
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

