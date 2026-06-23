<!--
  后台主布局容器
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <el-container class="admin-container">
    <!-- 粒子背景 -->
    <div class="particles-bg">
      <div v-for="i in 30" :key="i" class="particle"
        :style="{
          left: Math.random() * 100 + '%',
          animationDelay: Math.random() * 10 + 's',
          animationDuration: (6 + Math.random() * 12) + 's'
        }"></div>
    </div>

    <!-- 侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="admin-aside">
      <SideBar :collapsed="sidebarCollapsed" />
    </el-aside>

    <!-- 右侧内容 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="admin-header">
        <HeaderBar
          :collapsed="sidebarCollapsed"
          @toggle="sidebarCollapsed = !sidebarCollapsed"
        />
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 后台主布局 - 侧边栏+顶栏+内容区
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref } from 'vue'
import SideBar from './SideBar.vue'
import HeaderBar from './HeaderBar.vue'

const sidebarCollapsed = ref(false)
</script>

<style scoped>
.admin-container {
  height: 100vh;
  background: linear-gradient(135deg, #F8F9FD 0%, #F0F1F7 100%);
  position: relative;
}
.admin-aside {
  background: linear-gradient(180deg, #2D1B69 0%, #1A0A3E 100%) !important;
  box-shadow: 2px 0 24px rgba(0, 0, 0, 0.15);
  transition: width 0.3s ease;
  position: relative;
  z-index: 10;
  overflow: hidden;
}
.admin-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(108, 92, 231, 0.1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 5;
}
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
:deep(.el-main) {
  padding: 20px;
  overflow-y: auto;
  background: #F8F9FD;
}

/* 暗黑模式 */
:global(.dark) .admin-header {
  background: rgba(36, 40, 54, 0.9);
  border-bottom: 1px solid #3A3D4A;
}
:global(.dark) :deep(.el-main) {
  background: #1A1D29;
}
</style>
