/**
 * Vue Router - 管理员后台路由权限体系
 * @author 胡宪棋 软件2413 202421332084
 */
import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '管理员登录', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页看板', icon: 'DataLine' } },
      { path: 'users', name: 'UserManage', component: () => import('@/views/user/UserListView.vue'), meta: { title: '用户管理', icon: 'User' } },
      { path: 'bills', name: 'BillManage', component: () => import('@/views/bill/BillListView.vue'), meta: { title: '账单管理', icon: 'List' } },
      { path: 'categories', name: 'CategoryManage', component: () => import('@/views/category/CategoryManage.vue'), meta: { title: '分类管理', icon: 'Grid' } },
      { path: 'budgets', name: 'BudgetManage', component: () => import('@/views/budget/BudgetManage.vue'), meta: { title: '预算目标管理', icon: 'PieChart' } },
      { path: 'ai-config', name: 'AiConfig', component: () => import('@/views/ai/AiConfigView.vue'), meta: { title: 'AI系统配置', icon: 'Cpu', needSuper: true } },
      { path: 'logs', name: 'OperationLog', component: () => import('@/views/log/OperationLog.vue'), meta: { title: '操作日志', icon: 'Document' } },
      { path: 'admins', name: 'AdminAccount', component: () => import('@/views/system/AdminAccount.vue'), meta: { title: '管理员账号', icon: 'Setting', needSuper: true } },
      { path: 'files', name: 'FileManager', component: () => import('@/views/system/FileManager.vue'), meta: { title: '文件管理', icon: 'Folder', needSuper: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 路由守卫 - 权限拦截 + 拉取最新管理员信息
let lastFetchTime = 0
const FETCH_COOLDOWN = 30000 // 30秒内不重复请求

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智能理财后台` : '智能理财管理后台'

  const userStore = useUserStore()
  const token = userStore.token
  let role = userStore.role

  // 登录页放行
  if (to.meta.noAuth) {
    if (token) return next('/dashboard')
    return next()
  }

  // 未登录拦截
  if (!token) {
    return next('/login')
  }

  // 关键修复：30秒内不重复请求 fetchInfo，避免运营管理员每次路由切换都触发 403
  const now = Date.now()
  if (now - lastFetchTime > FETCH_COOLDOWN) {
    await userStore.fetchInfo()
    lastFetchTime = now
  }
  role = userStore.role

  // 超级管理员权限检查
  if (to.meta.needSuper && role !== 'SUPER_ADMIN') {
    ElMessage.warning('需要超级管理员权限')
    return next('/dashboard')
  }

  next()
})

export default router