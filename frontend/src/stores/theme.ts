import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useOsTheme, darkTheme } from 'naive-ui'
import type { GlobalTheme, GlobalThemeOverrides } from 'naive-ui'

export const useThemeStore = defineStore('theme', () => {
  const osTheme = useOsTheme()
  const savedTheme = localStorage.getItem('theme') as 'dark' | 'light' | null

  const isDark = ref(savedTheme
    ? savedTheme === 'dark'
    : osTheme.value === 'dark'
  )

  const theme = computed<GlobalTheme | null>(() => isDark.value ? darkTheme : null)

  const themeOverrides = computed<GlobalThemeOverrides | null>(() => {
    if (isDark.value) return null
    return {
      common: {
        borderColor: '#d1d5db', // gray-300
        dividerColor: '#e5e7eb', // gray-200
        bodyColor: '#ffffff',
        modalColor: '#ffffff',
        popoverColor: '#ffffff',
        cardColor: '#ffffff'
      },
      Card: {
        borderColor: '#d1d5db' // gray-300
      },
      Layout: {
        headerBorderColor: '#d1d5db',
        siderBorderColor: '#d1d5db'
      },
      DataTable: {
        borderColor: '#d1d5db'
      },
      List: {
        borderColor: '#d1d5db'
      }
    }
  })

  watch(isDark, (val) => {
    if (val) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }, { immediate: true })

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  return {
    isDark,
    theme,
    themeOverrides,
    toggleTheme
  }
})
