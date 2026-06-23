<!--
  管理员账号管理 - 仅超级管理员可访问
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="admin-account">
    <div class="page-header glass-card">
      <h2 class="page-title">👑 管理员账号管理</h2>
      <el-button class="gradient-btn" @click="openAddDialog" v-if="isSuperAdmin">+ 新增管理员</el-button>
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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑管理员':'新增管理员'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="isEdit" /></el-form-item>
        <el-form-item label="密码" v-if="!isEdit"><el-input v-model="form.password" type="password" placeholder="输入初始密码" /></el-form-item>
        <el-form-item label="密码" v-if="isEdit"><el-input v-model="form.password" type="password" placeholder="留空则不修改" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="运营管理员" value="OPERATOR" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码结果 -->
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
import { ref, reactive, computed } from 'vue'
import { getAdminPage, addAdmin, updateAdmin, deleteAdmin, resetAdminPassword } from '@/api/system'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted } from 'vue'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)
const loading = ref(false), saving = ref(false)
const tableData = ref([])
const dialogVisible = ref(false), isEdit = ref(false), editId = ref(0)
const form = reactive({ username: '', password: '', role: 'OPERATOR' })

const pwdVisible = ref(false), newPassword = ref('')

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
  isEdit.value = false; editId.value = 0
  Object.assign(form, { username: '', password: '', role: 'OPERATOR' })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, { username: row.username, password: '', role: row.role })
  dialogVisible.value = true
}

async function confirmSave() {
  if (!form.username) { ElMessage.warning('请输入账号'); return }
  if (!isEdit.value && !form.password) { ElMessage.warning('请输入密码'); return }
  saving.value = true
  try {
    const data = { role: form.role }
    if (form.password) data.password = form.password
    const res = isEdit.value ? await updateAdmin(editId.value, data) : await addAdmin({ username: form.username, password: form.password, role: form.role })
    if (res.code === 200) { ElMessage.success(isEdit.value?'修改成功':'创建成功'); dialogVisible.value = false; fetchData() }
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function handleResetPwd(row) {
  try {
    await ElMessageBox.confirm(`确定重置管理员"${row.username}"的密码？`, '操作确认', { type: 'warning' })
    const res = await resetAdminPassword(row.id)
    if (res.code === 200 && res.data) { newPassword.value = res.data.newPassword; pwdVisible.value = true }
  } catch (e) { /* cancel */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除管理员"${row.username}"？`, '危险操作', { type: 'error' })
    const res = await deleteAdmin(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); fetchData() }
  } catch (e) { /* cancel */ }
}
</script>
