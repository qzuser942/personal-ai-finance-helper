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
        <el-card class="glass-card stat-card" shadow="never" v-loading="loading">
          <template #header><span>📦 存储概览</span></template>
          <div class="stat-value">{{ overview.totalFileCount || 0 }}</div>
          <div class="stat-label">文件总数</div>
          <div style="margin-top:12px;color:#666">
            总占用空间：<b>{{ overview.totalSizeFormatted || '0 B' }}</b>
          </div>
          <div style="color:#999;font-size:12px;margin-top:4px">
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
          <el-button type="primary" @click="handleBackup" :loading="backingUp">
            🔄 一键备份数据库
          </el-button>
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
  </div>
</template>

<script setup>
/**
 * 文件管理 - 存储概览+数据库备份
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, onMounted } from 'vue'
import { getFileOverview, cleanFiles, databaseBackup, getBackupLog } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false), backingUp = ref(false)
const overview = ref({})
const backupResult = ref(null)
const backupLogs = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await getFileOverview()
    if (res.code === 200 && res.data) overview.value = res.data
  } catch (e) { /* ignore */ }
  loading.value = false
  fetchBackupLogs()
})

async function handleCleanFiles() {
  try {
    await ElMessageBox.confirm('确定清理所有无效文件？此操作不可恢复！', '危险操作', { type: 'error' })
    const res = await cleanFiles()
    if (res.code === 200) { ElMessage.success(`已清理${res.data?.deletedCount||0}个文件，释放${res.data?.freedSpaceFormatted||'0'}`) }
  } catch (e) { /* cancel */ }
}

async function handleBackup() {
  backingUp.value = true
  try {
    const res = await databaseBackup()
    if (res.code === 200 && res.data) { backupResult.value = res.data; ElMessage.success('备份成功'); fetchBackupLogs() }
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
