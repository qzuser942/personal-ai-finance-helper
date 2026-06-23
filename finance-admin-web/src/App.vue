<template>
  <router-view />
</template>

<script setup>
/**
 * 根组件 - 个人智能理财系统管理后台
 * @author 胡宪棋 软件2413 202421332084
 */
import { useAppStore } from '@/store/app'
import { onMounted } from 'vue'

const appStore = useAppStore()

onMounted(() => {
  appStore.initTheme()
})
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
