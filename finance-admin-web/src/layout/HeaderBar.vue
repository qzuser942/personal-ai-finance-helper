<!--
  顶部栏 - 面包屑、主题切换、用户信息
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="header-bar">
    <!-- 折叠按钮 -->
    <el-icon class="toggle-btn" :size="22" @click="$emit('toggle')">
      <Fold v-if="!collapsed" /><Expand v-else />
    </el-icon>

    <!-- 面包屑 -->
    <el-breadcrumb class="breadcrumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="header-right">
      <!-- 主题切换 -->
      <el-tooltip :content="isDark ? '切换浅色' : '切换深色'" placement="bottom">
        <el-button class="action-btn" circle @click="toggleDark">
          <el-icon :size="18"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
      </el-tooltip>

      <!-- 用户信息 -->
      <el-dropdown trigger="click">
        <div class="user-info">
          <el-avatar :size="32" class="avatar">
            {{ username.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="username">{{ username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <el-icon><User /></el-icon> {{ roleText }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
/**
 * 顶栏 - 面包屑+主题切换+用户下拉
 * @author 胡宪棋 软件2413 202421332084
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { ElMessageBox } from 'element-plus'

defineProps({ collapsed: Boolean })
defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const username = computed(() => userStore.username)
const isDark = computed(() => appStore.isDarkMode)
const roleText = computed(() => userStore.isSuperAdmin ? '超级管理员' : '运营管理员')

function toggleDark() { appStore.toggleDarkMode() }

function handleLogout() {
  ElMessageBox.confirm('确定退出管理后台？', '提示', { type: 'warning' })
    .then(() => { userStore.logout(); router.replace('/login') })
    .catch(() => {})
}
</script>

<style scoped>
.header-bar {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}
.toggle-btn { cursor: pointer; color: #636E72; transition: all 0.3s; }
.toggle-btn:hover { color: #6C5CE7; }
.breadcrumb { flex: 1; }
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.action-btn {
  background: transparent;
  border: 1px solid #E8ECF1;
  transition: all 0.3s;
}
.action-btn:hover { border-color: #6C5CE7; color: #6C5CE7; }
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.avatar {
  background: linear-gradient(135deg, #6C5CE7, #A29BFE);
}
.username {
  font-size: 14px;
  color: #2D3436;
  font-weight: 500;
}

:global(.dark) {
  .header-bar { background: transparent; }
  .toggle-btn { color: #B2BEC3; }
  .username { color: #E4E6EB; }
  .action-btn { border-color: #3A3D4A; }
}
</style>
