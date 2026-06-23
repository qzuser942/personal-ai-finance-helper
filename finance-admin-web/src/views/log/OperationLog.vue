<!--
  管理员操作日志页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="operation-log">
    <div class="page-header glass-card"><h2 class="page-title">📜 操作日志审计</h2></div>

    <!-- 搜索区 -->
    <div class="search-bar glass-card">
      <el-input v-model="search.username" placeholder="操作人" clearable style="width:140px" />
      <el-input v-model="search.operation" placeholder="操作类型" clearable style="width:140px" />
      <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
        start-placeholder="开始时间" end-placeholder="结束时间" value-format="YYYY-MM-DD HH:mm:ss"
        style="width:360px" />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div style="flex:1" />
      <el-button class="gradient-btn" @click="handleExport">📥 导出日志</el-button>
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
      </el-table>

      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[20,50,100]" layout="total,sizes,prev,pager,next"
          @change="fetchData" background />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 操作日志 - 所有管理员增删改留痕
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive } from 'vue'
import { getLogPage } from '@/api/log'
import { onMounted } from 'vue'

const loading = ref(false)
const tableData = ref([])
const search = reactive({ username: '', operation: '' })
const timeRange = ref([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

onMounted(() => fetchData())

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

function handleExport() {
  const params = new URLSearchParams()
  if (search.username) params.append('username', search.username)
  if (timeRange.value?.[0]) params.append('startTime', timeRange.value[0])
  if (timeRange.value?.[1]) params.append('endTime', timeRange.value[1])
  params.append('_t', String(Date.now()))
  window.open(`/api/admin/log/export?${params.toString()}`)
}
</script>
