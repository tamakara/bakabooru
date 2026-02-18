<script setup lang="ts">
import { NConfigProvider, NGlobalStyle, NMessageProvider, NDialogProvider } from 'naive-ui'
import { onMounted, onUnmounted } from 'vue'
import MainLayout from './components/layout/MainLayout.vue'
import { useThemeStore } from './stores/theme'
import { authApi } from './api/auth'
import { useRouter } from 'vue-router'

const themeStore = useThemeStore()
const router = useRouter()

const preventDefaultContextMenu = (e: MouseEvent) => {
  e.preventDefault()
}

const preventZoom = (e: Event) => {
  e.preventDefault()
}

onMounted(async () => {
  try {
     const status = await authApi.getStatus()
     if (!status.initialized) {
       await router.push('/login')
     }
  } catch (e) {
      console.error('Failed to check initialization status', e)
  }

  document.addEventListener('contextmenu', preventDefaultContextMenu)
  document.addEventListener('gesturestart', preventZoom)
})

onUnmounted(() => {
  document.removeEventListener('contextmenu', preventDefaultContextMenu)
  document.removeEventListener('gesturestart', preventZoom)
})
</script>

<template>
  <n-config-provider :theme="themeStore.theme" :theme-overrides="themeStore.themeOverrides">
    <n-global-style/>
    <n-message-provider>
      <n-dialog-provider>
        <MainLayout/>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
