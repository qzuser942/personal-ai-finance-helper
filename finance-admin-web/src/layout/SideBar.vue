<!--
  侧边栏导航 - 毛玻璃风格
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="sidebar-wrapper">
    <!-- Logo -->
    <div class="sidebar-logo">
      <el-icon :size="28"><Coin /></el-icon>
      <span v-show="!collapsed" class="logo-text">智能理财</span>
    </div>
    <div v-show="!collapsed" class="logo-subtitle">管理后台 v1.0</div>

    <!-- 导航菜单 -->
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :unique-opened="true"
      router
      class="sidebar-menu"
      background-color="transparent"
      text-color="rgba(255,255,255,0.7)"
      active-text-color="#FFFFFF"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataLine /></el-icon>
        <span>首页看板</span>
      </el-menu-item>

      <el-menu-item index="/users">
        <el-icon><User /></el-icon>
        <span>用户管理</span>
      </el-menu-item>

      <el-menu-item index="/bills">
        <el-icon><List /></el-icon>
        <span>账单管理</span>
      </el-menu-item>

      <el-menu-item index="/categories">
        <el-icon><Grid /></el-icon>
        <span>分类管理</span>
      </el-menu-item>

      <el-menu-item index="/budgets">
        <el-icon><PieChart /></el-icon>
        <span>预算目标管理</span>
      </el-menu-item>

      <el-menu-item index="/ai-config" v-if="isSuperAdmin">
        <el-icon><Cpu /></el-icon>
        <span>AI系统配置</span>
      </el-menu-item>

      <el-menu-item index="/logs">
        <el-icon><Document /></el-icon>
        <span>操作日志</span>
      </el-menu-item>

      <el-menu-item index="/admins" v-if="isSuperAdmin">
        <el-icon><Setting /></el-icon>
        <span>管理员账号</span>
      </el-menu-item>

      <el-menu-item index="/files" v-if="isSuperAdmin">
        <el-icon><Folder /></el-icon>
        <span>文件管理</span>
      </el-menu-item>
    </el-menu>

    <!-- 底部信息 -->
    <div class="sidebar-footer" v-show="!collapsed">
      <div class="footer-info">
        <span class="admin-role">{{ isSuperAdmin ? '超级管理员' : '运营管理员' }}</span>
        <span class="admin-name">{{ username }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 侧边栏 - 导航菜单+角色信息
 * @author 胡宪棋 软件2413 202421332084
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

defineProps({ collapsed: { type: Boolean, default: false } })

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const username = computed(() => userStore.username)
</script>

<style scoped>
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 16px 4px;
  color: #fff;
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #A29BFE, #55EFC4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.logo-subtitle {
  text-align: center;
  color: rgba(255,255,255,0.35);
  font-size: 11px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  margin: 0 16px 8px;
}
.sidebar-menu {
  flex: 1;
  border-right: none;
  padding: 8px 0;
}
.sidebar-menu .el-menu-item,
.sidebar-menu .el-sub-menu__title {
  margin: 2px 8px;
  border-radius: 10px;
  transition: all 0.3s;
}
.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.45), rgba(74, 61, 182, 0.35)) !important;
  box-shadow: 0 4px 12px rgba(108, 92, 231, 0.3);
  border-left: 3px solid #A29BFE;
}
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.footer-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.admin-role {
  font-size: 11px;
  color: rgba(255,255,255,0.5);
}
.admin-name {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  font-weight: 500;
}
</style>