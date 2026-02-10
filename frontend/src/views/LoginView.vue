<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import {
  NInput,
  NButton,
  NCard,
  NSpin,
  NAlert,
  NFormItem,
  NIcon
} from 'naive-ui'
import { LockClosedOutline, CheckmarkCircleOutline, LogInOutline, SettingsOutline, ArrowForwardOutline } from '@vicons/ionicons5'

const router = useRouter()
const mode = ref<'login' | 'setup' | 'loading'>('loading')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)

const checkStatus = async () => {
  try {
    const status = await authApi.getStatus()
    if (!status.initialized) {
      mode.value = 'setup'
    } else {
      mode.value = 'login'
    }
  } catch (e) {
    console.error(e)
    error.value = '无法连接到服务器'
  }
}

const handleLogin = async () => {
    loading.value = true;
    try {
        const res = await authApi.login(password.value)
        localStorage.setItem('token', res.token)
        router.push('/')
    } catch (e: any) {
        error.value = '登录失败'
    } finally {
        loading.value = false;
    }
}

const handleSetup = async (skip: boolean = false) => {
    const pwd = skip ? '' : password.value;

    if (!skip && pwd !== confirmPassword.value) {
        error.value = '两次输入的密码不一致'
        return
    }

    loading.value = true;
    try {
        await authApi.setup(pwd)
        // Login immediately with the set password
        const res = await authApi.login(pwd)
        localStorage.setItem('token', res.token)
        router.push('/')
    } catch (e: any) {
        error.value = '设置失败'
    } finally {
        loading.value = false;
    }
}


onMounted(() => {
    checkStatus()
})
</script>

<template>
  <div class="h-screen w-full flex items-center justify-center bg-gray-50 dark:bg-gray-950 p-4">
    <n-card
        class="w-full max-w-md shadow-lg"
        size="large"
        bordered
    >
      <template #header>
        <div class="text-center">
            <h1 class="text-2xl font-bold">
              {{ mode === 'setup' ? '初始化设置' : '欢迎回来' }}
            </h1>
        </div>
      </template>

      <div v-if="mode === 'loading'" class="flex flex-col items-center justify-center py-8">
        <n-spin size="large" />
        <p class="mt-4 text-gray-500">加载中...</p>
      </div>

      <div v-else>
         <n-alert v-if="error" type="error" class="mb-6" closable :on-close="() => error = ''">
            {{ error }}
        </n-alert>

        <form @submit.prevent="mode === 'setup' ? handleSetup() : handleLogin()">
            <n-form-item :label="mode === 'setup' ? '设置密码' : '密码'" path="password">
                <n-input
                    v-model:value="password"
                    type="password"
                    placeholder="请输入密码"
                    show-password-on="click"
                    :disabled="loading"
                    autofocus
                >
                    <template #prefix>
                        <n-icon :component="LockClosedOutline" />
                    </template>
                </n-input>
            </n-form-item>

            <n-form-item v-if="mode === 'setup'" label="确认密码" path="confirmPassword">
                <n-input
                    v-model:value="confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    show-password-on="click"
                    :disabled="loading"
                >
                     <template #prefix>
                        <n-icon :component="CheckmarkCircleOutline" />
                    </template>
                </n-input>
            </n-form-item>

            <div class="mt-4 grid grid-cols-2 gap-4" v-if="mode === 'setup'">
                <n-button
                    type="primary"
                    :loading="loading"
                    :disabled="loading"
                    attr-type="submit"
                    block
                >
                    <template #icon>
                        <n-icon :component="SettingsOutline" />
                    </template>
                    完成设置
                </n-button>
                <n-button
                    :loading="loading"
                    :disabled="loading"
                    @click="handleSetup(true)"
                    attr-type="button"
                    block
                >
                    <template #icon>
                        <n-icon :component="ArrowForwardOutline" />
                    </template>
                    跳过
                </n-button>
            </div>

            <div class="mt-4" v-else>
                <n-button
                    attr-type="submit"
                    type="primary"
                    block
                    :loading="loading"
                    :disabled="loading"
                >
                    <template #icon>
                        <n-icon :component="LogInOutline" />
                    </template>
                    立即登录
                </n-button>
            </div>
        </form>
      </div>
    </n-card>
  </div>
</template>

<style scoped>
</style>

