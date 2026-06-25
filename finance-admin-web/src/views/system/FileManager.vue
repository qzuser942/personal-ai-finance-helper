<!--
  文件管理页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="file-manager">
    <div class="page-header glass-card"><h2 class="page-title">🗂️ 文件存储管理</h2></div>

    <el-row :gutter="16">
      <!-- 文件概览 -->
      <el-col :span="12">
        <div class="overview-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <span style="font-weight:600;color:#6C5CE7">📦 备份文件概览</span>
          <el-button
            size="small"
            type="primary"
            :loading="loading"
            @click="fetchOverview"
            data-testid="refresh-button"
          >🔄 刷新</el-button>
        </div>
        <el-card class="glass-card stat-card" shadow="never" v-loading="loading">
          <div class="stat-value">{{ overview.totalFileCount || 0 }}</div>
          <div class="stat-label">备份文件总数</div>
          <div style="margin-top:12px;color:#666">
            总占用空间：<b>{{ overview.totalSizeFormatted || '0 B' }}</b>
          </div>
          <div style="color:#999;font-size:12px;margin-top:4px;word-break:break-all">
            存储目录：{{ overview.storageDir }}
          </div>
        </el-card>
      </el-col>

      <!-- 数据库备份 -->
      <el-col :span="12">
        <el-card class="glass-card stat-card" shadow="never">
          <template #header><span>💾 数据库备份</span></template>
          <div style="margin-bottom:12px;color:#666">
            备份文件将保存至服务端本地磁盘，用于数据恢复。
          </div>
          <!-- 关键修复：备份按钮加 v-permission 指令（运营仅可读，不能备份/清理） -->
          <el-button type="primary" @click="handleBackup" :loading="backingUp" v-permission="'database:backup'">
            🔄 一键备份数据库
          </el-button>
          <el-tag v-if="!hasPerm('database:backup')" type="info" size="small">仅超级管理员可执行备份</el-tag>

          <div v-if="backupResult" style="margin-top:12px;padding:12px;background:#E8F5E9;border-radius:8px">
            <div>文件名：{{ backupResult.fileName }}</div>
            <div>大小：{{ formatSize(backupResult.fileSize) }}</div>
          </div>

          <!-- 备份历史 -->
          <div style="margin-top:16px">
            <h4 style="margin-bottom:8px">备份历史</h4>
            <el-table :data="backupLogs" size="small" max-height="200" stripe>
              <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
              <el-table-column label="大小" width="90">
                <template #default="{row}">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="60">
                <template #default="{row}">
                  <el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'成功':'失败' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="时间" width="150" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 关键修复：补"清理文件"按钮（仅超管），使用 v-permission 指令 -->
    <el-row v-permission="'file:clean'" :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card class="glass-card" shadow="never">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>🧹 备份文件清理（仅超管）</span>
              <el-button type="danger" size="small" :loading="cleaning" @click="handleCleanFiles">
                🗑️ 清理所有备份文件
              </el-button>
            </div>
          </template>
          <div style="color:#999;font-size:13px">将删除 backups 目录下的所有文件，此操作不可恢复，请谨慎！</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 文件管理 - 存储概览+数据库备份+清理
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, computed, onMounted } from 'vue'
import { getFileOverview, cleanFiles, databaseBackup, getBackupLog } from '@/api/system'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false), backingUp = ref(false), cleaning = ref(false)
const overview = ref({})
const backupResult = ref(null)
const backupLogs = ref([])
const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
/** 关键修复：从 store 的 permissions 列表判断（v-permission 指令同源） */
function hasPerm(perm) { return userStore.hasPermission(perm) }

async function fetchOverview(silent = false) {
  loading.value = true
  try {
    const res = await getFileOverview()
    if (res.code === 200 && res.data) {
      overview.value = res.data
      if (!silent) {
        const count = res.data.totalFileCount || 0
        const size = res.data.totalSizeFormatted || '0 B'
        if (count === 0) {
          ElMessage.warning(`刷新成功，目录 ${res.data.storageDir} 当前为空`)
        } else {
          ElMessage.success(`刷新成功：${count} 个文件，共 ${size}`)
        }
      }
    } else {
      ElMessage.error(res.message || '获取概览失败')
    }
  } catch (e) {
    console.error('[FileManager] fetchOverview failed', e)
    ElMessage.error('刷新失败：' + (e.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchOverview(true)
  // 仅超管才拉备份历史（运营也无需看）
  if (userStore.hasPermission('database:backup')) fetchBackupLogs()
})

async function handleCleanFiles() {
  try {
    await ElMessageBox.confirm('确定清理所有备份文件？此操作不可恢复！', '危险操作', { type: 'error' })
    cleaning.value = true
    const res = await cleanFiles()
    if (res.code === 200) {
      ElMessage.success(`已清理${res.data?.deletedCount || 0}个文件，释放${res.data?.freedSpaceFormatted || '0'}`)
      await fetchOverview()
      await fetchBackupLogs()
    }
  } catch (e) { /* cancel */ }
  cleaning.value = false
}

async function handleBackup() {
  backingUp.value = true
  try {
    const res = await databaseBackup()
    if (res.code === 200 && res.data) {
      backupResult.value = res.data
      ElMessage.success('备份成功')
      await fetchOverview()
      await fetchBackupLogs()
    }
  } catch (e) { /* ignore */ }
  backingUp.value = false
}

async function fetchBackupLogs() {
  try {
    const res = await getBackupLog({ page: 1, size: 10 })
    if (res.code === 200 && res.data) backupLogs.value = res.data.records || []
  } catch (e) { /* ignore */ }
}

function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}
</script>
