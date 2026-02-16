import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { useOsTheme, darkTheme } from 'naive-ui'
import type { GlobalTheme } from 'naive-ui'

export const useThemeStore = defineStore('theme', () => {
  const osTheme = useOsTheme()
  const savedTheme = localStorage.getItem('theme') as 'dark' | 'light' | null

  const isDark = ref(savedTheme
    ? savedTheme === 'dark'
    : osTheme.value === 'dark'
  )

  const theme = computed<GlobalTheme | null>(() => isDark.value ? darkTheme : null)

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
    toggleTheme
  }
})

