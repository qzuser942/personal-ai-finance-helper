/**
 * Vue3应用入口 - 个人智能理财系统管理后台
 * @author 胡宪棋 软件2413 202421332084
 */
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import App from './App.vue'
import router from './router'
import pinia from './store'
import permissionDirective from './directives/permission'
import './assets/styles/global.scss'

const app = createApp(App)

// 关键修复：注册 v-permission 指令，所有按钮统一从后端 /api/admin/info 拉权限位渲染
app.use(permissionDirective)

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn, size: 'default' })
app.use(router)
app.use(pinia)
app.mount('#app')
