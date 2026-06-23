<!--
  管理员登录页面 - 毛玻璃流光效果
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="login-container">
    <!-- 粒子背景 -->
    <div class="particles-bg">
      <div v-for="i in 40" :key="i" class="particle"
        :style="{ left: Math.random()*100+'%', animationDelay: Math.random()*10+'s', animationDuration: (5+Math.random()*15)+'s' }"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card shimmer-border">
      <div class="glass-card login-inner">
        <div class="login-header">
          <el-icon :size="48" color="#6C5CE7"><Coin /></el-icon>
          <h2>智能理财管理后台</h2>
          <p>Personal Finance Admin Console</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="管理员账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="管理员密码"
              :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button class="gradient-btn login-btn" :loading="loading"
              @click="handleLogin" round>登 录 后 台</el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span>胡宪棋 | 软件2413 | 202421332084</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 管理员登录
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref()

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const ok = await userStore.login(form.username, form.password)
    if (ok) {
      ElMessage.success('登录成功，欢迎回来！')
      router.replace('/dashboard')
    }
  } catch (e) {
    // 错误已由pincatch拦截器处理
  }
  loading.value = false
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0F0A2E 0%, #1A1045 30%, #2D1B69 60%, #1A0A3E 100%);
  position: relative;
  overflow: hidden;
}
.login-card {
  position: relative;
  z-index: 1;
  border-radius: 24px;
  padding: 3px;
}
.login-inner {
  width: 400px;
  padding: 48px 40px;
  border-radius: 22px;
}
.login-header {
  text-align: center;
  margin-bottom: 36px;
}
.login-header h2 {
  margin: 16px 0 8px;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #6C5CE7, #55EFC4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.login-header p {
  color: #B2BEC3;
  font-size: 13px;
  letter-spacing: 2px;
  text-transform: uppercase;
}
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  margin-top: 12px;
}
.login-footer {
  text-align: center;
  margin-top: 24px;
  color: #B2BEC3;
  font-size: 12px;
}
:deep(.el-input__wrapper) {
  background: rgba(108, 92, 231, 0.04);
  box-shadow: 0 0 0 1px rgba(108, 92, 231, 0.15);
  border-radius: 12px;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(108, 92, 231, 0.3);
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(108, 92, 231, 0.4);
}
</style>
