<!--
  管理员账号管理 - 仅超级管理员可访问
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="admin-account">
    <div class="page-header glass-card">
      <h2 class="page-title">👑 管理员账号管理</h2>
      <el-button class="gradient-btn" @click="openAddDialog" v-permission="'account:manage'">+ 新增管理员</el-button>
    </div>

    <el-card class="glass-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="管理员账号" />
        <el-table-column label="角色" width="140">
          <template #default="{row}">
            <el-tag :type="row.role==='SUPER_ADMIN'?'danger':'info'" effect="light" size="small">
              {{ row.role==='SUPER_ADMIN'?'超级管理员':'运营管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="160" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{row}">
            <!-- 关键修复：编辑/改密/重置/删除按钮均通过 v-permission 指令从后端 permissions 列表判断 -->
            <el-button size="small" type="primary" @click="openEditDialog(row)" v-permission="'account:manage'">编辑</el-button>
            <el-button size="small" type="warning" @click="openChangePwdDialog(row)" v-permission="'account:manage'">改密</el-button>
            <el-button size="small" type="info" @click="handleResetPwd(row)" v-permission="'account:manage'">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.id === currentAdminId" v-permission="'account:manage'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增管理员弹窗 -->
    <el-dialog v-model="addVisible" title="新增管理员" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" placeholder="至少8位" show-password /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="运营管理员" value="OPERATOR" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmAdd" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑角色弹窗 -->
    <el-dialog v-model="editVisible" title="编辑管理员角色" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" disabled /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="运营管理员" value="OPERATOR" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmEditRole" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗（Bug 3 修复：可改自己密码） -->
    <el-dialog v-model="pwdChangeVisible" title="修改密码" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" disabled /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.password" type="password" placeholder="至少8位" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdChangeVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmChangePwd" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码结果弹窗 -->
    <el-dialog v-model="pwdVisible" title="密码重置结果" width="360px">
      <div style="padding:12px;background:#E8F5E9;border-radius:8px">
        新密码：<b style="color:#2E7D32;font-size:18px">{{ newPassword }}</b>
      </div>
      <template #footer><el-button @click="pwdVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 管理员账号管理 - 仅超级管理员
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getAdminPage, addAdmin,
  updateAdminRole, updateAdminPassword,
  deleteAdmin, resetAdminPassword
} from '@/api/system'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const currentAdminId = computed(() => userStore.adminId)
const loading = ref(false), saving = ref(false)
const tableData = ref([])

const addVisible = ref(false)
const editVisible = ref(false)
const pwdChangeVisible = ref(false)
const editId = ref(0)
const pwdVisible = ref(false), newPassword = ref('')
const form = reactive({ username: '', password: '', role: 'OPERATOR' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminPage({ page: 1, size: 100 })
    if (res.code === 200 && res.data) tableData.value = res.data.records || []
  } catch (e) { /* ignore */ }
  loading.value = false
}

function openAddDialog() {
  Object.assign(form, { username: '', password: '', role: 'OPERATOR' })
  addVisible.value = true
}

function openEditDialog(row) {
  editId.value = row.id
  Object.assign(form, { username: row.username, password: '', role: row.role })
  editVisible.value = true
}

function openChangePwdDialog(row) {
  editId.value = row.id
  Object.assign(form, { username: row.username, password: '' })
  pwdChangeVisible.value = true
}

async function confirmAdd() {
  if (!form.username) return ElMessage.warning('请输入账号')
  if (!form.password || form.password.length < 8) return ElMessage.warning('密码至少8位')
  saving.value = true
  try {
    const res = await addAdmin({ username: form.username, password: form.password, role: form.role })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      addVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function confirmEditRole() {
  saving.value = true
  try {
    // 关键修复：使用新接口 PUT /api/admin/account/{id}/role（拆分后的 endpoint）
    const res = await updateAdminRole(editId.value, { role: form.role })
    if (res.code === 200) {
      ElMessage.success('角色已修改')
      editVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function confirmChangePwd() {
  if (!form.password || form.password.length < 8) return ElMessage.warning('密码至少8位')
  saving.value = true
  try {
    // 关键修复：使用新接口 PUT /api/admin/account/{id}/password（可改自己）
    const res = await updateAdminPassword(editId.value, { password: form.password })
    if (res.code === 200) {
      ElMessage.success(editId.value === currentAdminId.value ? '密码已修改，请重新登录' : '密码已修改')
      pwdChangeVisible.value = false
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定重置管理员"${row.username}"的密码？系统将生成8位强密码`, '操作确认', { type: 'warning' })
    const res = await resetAdminPassword(row.id)
    if (res.code === 200 && res.data) { newPassword.value = res.data.newPassword; pwdVisible.value = true }
  } catch (e) { /* cancel */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除管理员"${row.username}"？`, '危险操作', { type: 'error' })
    const res = await deleteAdmin(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) { /* cancel */ }
}
</script>