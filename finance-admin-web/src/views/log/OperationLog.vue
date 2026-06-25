<!--
  管理员操作日志页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="operation-log">
    <div class="page-header glass-card"><h2 class="page-title">📜 操作日志审计</h2></div>

    <!-- 搜索区 -->
    <div class="search-bar glass-card">
      <el-input v-if="isSuperAdmin" v-model="search.username" placeholder="操作人" clearable style="width:140px" />
      <el-input v-model="search.operation" placeholder="操作类型" clearable style="width:140px" />
      <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
        start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss"
        style="width:360px" />
      <el-button type="primary" @click="searchData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div style="flex:1" />
      <!-- 关键修复：日志导出仅超管（运营可看页面但不能导出），v-permission 指令从后端判断 -->
      <el-button class="gradient-btn" :loading="exporting" @click="handleExport" v-permission="'log:export'">📥 导出日志</el-button>
    </div>

    <!-- 日志表格 -->
    <el-card class="glass-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="adminUsername" label="操作人" width="120" />
        <el-table-column prop="operation" label="操作描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="方法" width="70">
          <template #default="{row}">
            <el-tag :type="row.method==='DELETE'?'danger':row.method==='PUT'?'warning':'info'" size="small">
              {{ row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求路径" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="70">
          <template #default="{row}">
            <el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'成功':'失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="createdAt" label="操作时间" width="160" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[20,50,100]" layout="total,sizes,prev,pager,next"
          @current-change="fetchData" @size-change="onSizeChange" background />
      </div>
    </el-card>

    <!-- 日志详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="600px" top="5vh">
      <el-descriptions v-if="detailRow" :column="2" border size="small">
        <el-descriptions-item label="日志ID">{{ detailRow.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ detailRow.adminUsername }}
          <el-tag :type="detailRow.adminRole==='SUPER_ADMIN'?'danger':'warning'" size="small" style="margin-left:6px">
            {{ detailRow.adminRole==='SUPER_ADMIN'?'超管':'运营' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ detailRow.operation }}</el-descriptions-item>
        <el-descriptions-item label="HTTP方法">
          <el-tag :type="detailRow.method==='DELETE'?'danger':detailRow.method==='PUT'?'warning':'info'" size="small">
            {{ detailRow.method }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求路径" :span="2">{{ detailRow.requestUrl }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.resourceId" label="关联资源">
          <el-tag size="small" effect="plain">{{ detailRow.resourceId }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <el-tag :type="detailRow.status===1?'success':'danger'" size="small">
            {{ detailRow.status===1?'成功':'失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">
          <span>{{ detailRow.ipAddress }}</span>
          <span v-if="geoLoading" style="color:#909399;margin-left:6px;font-size:12px">查询中...</span>
          <el-tag v-else-if="geoInfo" size="small" effect="plain" style="margin-left:6px">{{ geoInfo }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ detailRow.createdAt }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.errorMsg" label="错误信息" :span="2">
          <span style="color:#FF7675">{{ detailRow.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 操作日志 - 所有管理员增删改留痕
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, computed } from 'vue'
import { getLogPage, exportLogs } from '@/api/log'
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)

const loading = ref(false)
const exporting = ref(false)
const detailVisible = ref(false)
const detailRow = ref(null)
const geoLoading = ref(false)
const geoInfo = ref('')
const tableData = ref([])
const search = reactive({ username: '', operation: '' })
const timeRange = ref([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

onMounted(() => fetchData())

function searchData() {
  pagination.page = 1
  fetchData()
}

function onSizeChange() {
  pagination.page = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLogPage({
      page: pagination.page, size: pagination.size,
      username: search.username || undefined,
      operation: search.operation || undefined,
      startTime: timeRange.value?.[0] || undefined,
      endTime: timeRange.value?.[1] || undefined
    })
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

function resetSearch() {
  search.username = ''; search.operation = ''; timeRange.value = []; pagination.page = 1; fetchData()
}

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
  // 异步查询IP归属地
  if (row.ipAddress && row.ipAddress !== '0:0:0:0:0:0:0:1' && row.ipAddress !== '127.0.0.1') {
    fetchGeo(row.ipAddress)
  } else {
    geoInfo.value = ''
    geoLoading.value = false
  }
}

async function fetchGeo(ip) {
  geoLoading.value = true
  geoInfo.value = ''
  try {
    const res = await fetch(`https://api.ip.sb/geoip/${ip}`)
    if (!res.ok) throw new Error('查询失败')
    const data = await res.json()
    const parts = []
    if (data.country) parts.push(data.country)
    if (data.region) parts.push(data.region)
    if (data.city) parts.push(data.city)
    if (data.isp) parts.push(data.isp)
    geoInfo.value = parts.length > 0 ? parts.join(' ') : '未知'
  } catch {
    geoInfo.value = ''
  } finally {
    geoLoading.value = false
  }
}

// 关键修复：使用 axios + responseType: 'blob' 下载，确保携带 Authorization header
// 原代码用 window.open() 打开 URL，浏览器无法附加 token header，所以一直返回 10004「请先登录」
async function handleExport() {
  exporting.value = true
  try {
    const params = {}
    if (search.username) params.username = search.username
    if (search.operation) params.operation = search.operation
    if (timeRange.value?.[0]) params.startTime = timeRange.value[0]
    if (timeRange.value?.[1]) params.endTime = timeRange.value[1]
    const blob = await exportLogs(params)
    if (!blob) {
      ElMessage.error('导出失败：未获取到数据')
      return
    }
    // 用 Blob 触发浏览器下载
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    const ts = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
    link.download = `operation_logs_${ts}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('[OperationLog] export failed', e)
    ElMessage.error('导出失败：' + (e.message || '网络异常'))
  } finally {
    exporting.value = false
  }
}
</script>