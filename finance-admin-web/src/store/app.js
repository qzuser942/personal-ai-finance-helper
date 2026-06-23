/**
 * 应用全局状态 - 主题、侧边栏
 * @author 胡宪棋 软件2413 202421332084
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const isDarkMode = ref(localStorage.getItem('admin_dark_mode') === 'true')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('admin_dark_mode', String(isDarkMode.value))
    applyTheme()
  }

  function initTheme() {
    applyTheme()
  }

  function applyTheme() {
    document.documentElement.classList.toggle('dark', isDarkMode.value)
  }

  return { sidebarCollapsed, isDarkMode, toggleSidebar, toggleDarkMode, initTheme }
})
