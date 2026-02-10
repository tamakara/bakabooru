<script setup lang="ts">
import { h } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { NLayout, NLayoutHeader, NLayoutContent, NMenu, NIcon, NDropdown, NButton } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import {
  Image24Regular as GalleryIcon,
  ArrowUpload24Regular as UploadIcon,
  Settings24Regular as SettingsIcon,
  Navigation24Regular as MenuIcon,
  WeatherMoon24Regular as MoonIcon,
  WeatherSunny24Regular as SunIcon,
  DoorArrowRight28Regular as LogoutIcon
} from '@vicons/fluent'
import { useThemeStore } from '../../stores/theme'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()

function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}

const menuOptions: MenuOption[] = [
  {
    label: () => h(RouterLink, { to: '/gallery' }, { default: () => '图库' }),
    key: 'gallery',
    icon: renderIcon(GalleryIcon)
  },
  {
    label: () => h(RouterLink, { to: '/upload' }, { default: () => '上传' }),
    key: 'upload',
    icon: renderIcon(UploadIcon)
  },
  {
    label: () => h(RouterLink, { to: '/settings' }, { default: () => '设置' }),
    key: 'settings',
    icon: renderIcon(SettingsIcon)
  }
]
</script>

<template>
  <n-layout class="h-screen">
    <n-layout-header v-if="route.name !== 'login'" bordered class="h-16 flex items-center px-4 justify-between">
      <div class="flex items-center shrink-0 mr-4">
        <span class="text-xl font-bold">BaKaBooru</span>
      </div>
      <div class="hidden sm:flex flex-1 min-w-0 justify-end">
        <n-menu
          mode="horizontal"
          :options="menuOptions"
          :value="String(route.name)"
          responsive
        />
        <div class="flex items-center ml-4 gap-2">
          <n-button circle quaternary @click="themeStore.toggleTheme">
            <template #icon>
              <n-icon>
                <MoonIcon v-if="themeStore.isDark" />
                <SunIcon v-else />
              </n-icon>
            </template>
          </n-button>
          <n-button circle quaternary @click="handleLogout">
            <template #icon>
              <n-icon>
                <LogoutIcon />
              </n-icon>
            </template>
          </n-button>
        </div>
      </div>
      <div class="sm:hidden flex items-center gap-2">
        <n-button circle quaternary @click="themeStore.toggleTheme">
          <template #icon>
            <n-icon>
              <MoonIcon v-if="themeStore.isDark" />
              <SunIcon v-else />
            </n-icon>
          </template>
        </n-button>
        <n-button circle quaternary @click="handleLogout">
          <template #icon>
            <n-icon>
              <LogoutIcon />
            </n-icon>
          </template>
        </n-button>
        <n-dropdown trigger="click" :options="menuOptions">
          <n-button text style="font-size: 24px">
            <n-icon>
              <MenuIcon />
            </n-icon>
          </n-button>
        </n-dropdown>
      </div>
    </n-layout-header>
    <n-layout-content
      class="bg-gray-50 dark:bg-gray-900"
      :style="route.name === 'login' ? 'height: 100vh' : 'height: calc(100vh - 64px)'"
    >
      <div
        class="h-full overflow-auto"
        :class="route.name === 'login' ? 'p-0' : 'p-2'"
      >
        <router-view />
      </div>
    </n-layout-content>
  </n-layout>
</template>

