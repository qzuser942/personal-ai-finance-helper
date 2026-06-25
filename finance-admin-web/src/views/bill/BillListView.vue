<!--
  全局账单管理页面 - 核心运营功能
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="bill-manage">
    <div class="page-header glass-card"><h2 class="page-title">📋 全平台账单管理</h2></div>

    <!-- 搜索区 -->
    <div class="search-bar glass-card">
      <el-input v-model="search.username" placeholder="用户名搜索" clearable style="width:140px" />
      <el-input v-model="search.userId" placeholder="用户ID" clearable style="width:120px" />
      <el-select v-model="search.type" placeholder="收支类型" clearable style="width:120px">
        <el-option label="支出" value="expense" /><el-option label="收入" value="income" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
        style="width:240px" />
      <el-input v-model="search.minAmount" placeholder="最小金额" clearable style="width:110px" />
      <el-input v-model="search.maxAmount" placeholder="最大金额" clearable style="width:110px" />
      <el-button type="primary" @click="searchData" :icon="Search">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div style="flex:1" />
      <!-- 关键修复：导出按钮加 v-permission（修复 P1-3），从后端 permissions 列表判断 -->
      <el-button class="gradient-btn" @click="handleExportAll" v-permission="'bill:export'">📥 导出全量</el-button>
    </div>

    <!-- 数据表格 -->
    <el-card class="glass-card" shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="账单ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column label="金额" width="110">
          <template #default="{row}">
            <span :style="{color:row.type==='income'?'#00B894':'#FF7675',fontWeight:'600'}">
              {{ row.type==='income'?'+':'-' }}{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="70">
          <template #default="{row}">
            <el-tag :type="row.type==='income'?'success':'danger'" size="small" effect="light">
              {{ row.type==='income'?'收入':'支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="小票" width="70">
          <template #default="{row}">
            <el-tag v-if="row.hasImage" type="info" size="small">📷 有</el-tag>
            <span v-else style="color:#ccc">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="consumeTime" label="消费时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <!-- 关键修复：编辑/删除仅超管可见（运营仅可读），使用 v-permission 指令从后端 permissions 列表判断 -->
            <el-button size="small" type="primary" @click="openEditDialog(row)" v-permission="'bill:write'">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" v-permission="'bill:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:16px">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[20,50,100]" layout="total,sizes,prev,pager,next"
          @current-change="fetchData" @size-change="onSizeChange" background />
      </div>
    </el-card>

    <!-- 编辑抽屉 -->
    <el-drawer v-model="editVisible" title="编辑账单" size="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="金额"><el-input-number v-model="editForm.amount" :min="0.01" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="分类ID"><el-input-number v-model="editForm.categoryId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="消费时间"><el-input v-model="editForm.consumeTime" /></el-form-item>
        <el-form-item><el-button type="primary" @click="confirmEdit" :loading="saving">保存修改</el-button></el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
/**
 * 全局账单管理
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { getBillPage, updateBill, deleteBill } from '@/api/bill'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { downloadFile } from '@/utils/download'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])
const search = reactive({ username: '', userId: '', type: '', minAmount: '', maxAmount: '' })
const dateRange = ref([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const editVisible = ref(false)
const editForm = reactive({ id: 0, amount: 0, categoryId: 0, remark: '', consumeTime: '' })

function getParams() {
  return {
    page: pagination.page, size: pagination.size,
    username: search.username || undefined, userId: search.userId || undefined,
    type: search.type || undefined,
    startDate: dateRange.value?.[0] || undefined, endDate: dateRange.value?.[1] || undefined,
    minAmount: search.minAmount || undefined, maxAmount: search.maxAmount || undefined
  }
}

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
    const res = await getBillPage(getParams())
    if (res.code === 200 && res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('[BillListView] fetchData failed', e)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  Object.assign(search, { username: '', userId: '', type: '', minAmount: '', maxAmount: '' })
  dateRange.value = []
  pagination.page = 1
  fetchData()
}

function openEditDialog(row) {
  Object.assign(editForm, {
    id: row.id, amount: row.amount, categoryId: row.categoryId,
    remark: row.remark || '', consumeTime: row.consumeTime || ''
  })
  editVisible.value = true
}

async function confirmEdit() {
  saving.value = true
  try {
    const res = await updateBill(editForm.id, {
      amount: editForm.amount, categoryId: editForm.categoryId,
      remark: editForm.remark, consumeTime: editForm.consumeTime
    })
    if (res.code === 200) {
      ElMessage.success('修改成功')
      editVisible.value = false
      fetchData()
    }
  } catch (e) {
    console.error('[BillListView] confirmEdit failed', e)
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户"${row.username}"的账单 #${row.id}？`, '删除确认', { type: 'warning' })
    const res = await deleteBill(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      fetchData()
    }
  } catch (e) { /* 用户取消或接口失败 */ }
}

// 关键修复：用 downloadFile 工具下载（带 Authorization header），替换原 window.open
// 原代码 window.open() 浏览器无法附加 token，导致后端 10004「请先登录」
async function handleExportAll() {
  const p = getParams()
  delete p.page
  delete p.size
  await downloadFile('/api/admin/bill/export-all', p, `bills_${Date.now()}.xlsx`)
}

// 关键修复：仅在挂载时拉取一次数据，移除错误的 onActivated 同步调用
onMounted(() => {
  fetchData()
})
</script>