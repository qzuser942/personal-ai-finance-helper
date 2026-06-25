<!--
  用户管理页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="user-manage">
    <div class="page-header glass-card"><h2 class="page-title">👥 用户管理</h2></div>
    <!-- 搜索区 -->
    <div class="search-bar glass-card">
      <el-input v-model="search.username" placeholder="用户名搜索" clearable style="width:180px" />
      <el-select v-model="search.status" placeholder="账号状态" clearable style="width:120px">
        <el-option label="正常" :value="1" /><el-option label="冻结" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData" :icon="Search">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div style="flex:1" />
      <!-- 关键修复：导出按钮加 v-permission（修复 P1-4），从后端 permissions 列表判断 -->
      <el-button class="gradient-btn" @click="handleExport" v-permission="'user:export'">📥 导出Excel</el-button>
    </div>

    <!-- 数据表格 -->
    <el-card class="glass-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.status===1?'success':'danger'" effect="light" size="small">
              {{ row.status===1?'正常':'冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="160" />
        <el-table-column prop="createdAt" label="注册时间" width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{row}">
            <el-button size="small" :type="row.status===1?'warning':'success'"
              @click="handleToggleStatus(row)">{{ row.status===1?'冻结':'解冻' }}</el-button>
            <!-- 关键修复：重置密码仅超管可见（运营不再有该权限），v-permission 指令从后端判断 -->
            <el-button size="small" type="info" @click="handleResetPwd(row)" v-permission="'user:resetPassword'">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[20,50,100]" layout="total,sizes,prev,pager,next"
          @change="fetchData" background />
      </div>
    </el-card>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="360px">
      <p>确定重置用户 <b>{{ resetUser?.username }}</b> 的密码？</p>
      <div v-if="newPassword" style="margin-top:12px;padding:12px;background:#E8F5E9;border-radius:8px">
        新密码：<b style="color:#2E7D32;font-size:16px">{{ newPassword }}</b>
      </div>
      <template #footer>
        <el-button @click="resetPwdVisible=false">关闭</el-button>
        <el-button type="primary" @click="confirmResetPwd">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 用户管理 - 列表/冻结/解冻/重置密码/导出
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { getUserPage, updateUserStatus, resetUserPassword } from '@/api/user'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { downloadFile } from '@/utils/download'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)

const loading = ref(false)
const tableData = ref([])
const search = reactive({ username: '', status: null })
const pagination = reactive({ page: 1, size: 20, total: 0 })
const resetPwdVisible = ref(false)
const resetUser = ref(null)
const newPassword = ref('')

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getUserPage({
      page: pagination.page, size: pagination.size,
      username: search.username || undefined,
      status: search.status !== null && search.status !== '' ? search.status : undefined
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

function resetSearch() {
  search.username = ''; search.status = null; pagination.page = 1; fetchData()
}

async function handleToggleStatus(row) {
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定${action}用户 "${row.username}"？`, '操作确认', { type: 'warning' })
    const newStatus = row.status === 1 ? 0 : 1
    const res = await updateUserStatus(row.userId, newStatus)
    if (res.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchData()
    }
  } catch (e) { /* cancel */ }
}

async function handleResetPwd(row) {
  resetUser.value = row
  newPassword.value = ''
  resetPwdVisible.value = true
}

async function confirmResetPwd() {
  try {
    const res = await resetUserPassword(resetUser.value.userId)
    if (res.code === 200 && res.data) {
      newPassword.value = res.data.newPassword
      ElMessage.success('密码重置成功')
    }
  } catch (e) { /* ignore */ }
}

// 关键修复：用 downloadFile 工具下载（带 Authorization header）
async function handleExport() {
  const params = {}
  if (search.username) params.username = search.username
  if (search.status !== null && search.status !== '') params.status = search.status
  await downloadFile('/api/admin/user/export', params, `users_${Date.now()}.xlsx`)
}
</script>
