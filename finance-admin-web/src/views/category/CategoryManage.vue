<!--
  分类管理页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="category-manage">
    <div class="page-header glass-card">
      <h2 class="page-title">🏷️ 分类管理</h2>
      <el-button class="gradient-btn" @click="openAddDialog">+ 新增全局分类</el-button>
    </div>

    <el-card class="glass-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="支出分类" name="expense" />
        <el-tab-pane label="收入分类" name="income" />
      </el-tabs>

      <el-table :data="filteredData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="类型" width="100">
          <template #default="{row}">
            <el-tag :type="row.isSystem?'info':'warning'" size="small">{{ row.isSystem?'系统内置':'用户自定义' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.isSystem && !isSuperAdmin">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑分类':'新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称"><el-input v-model="form.name" placeholder="如：健身" /></el-form-item>
        <el-form-item label="图标标识"><el-input v-model="form.icon" placeholder="如：icon-fitness" /></el-form-item>
        <el-form-item label="分类类型">
          <el-radio-group v-model="form.type" :disabled="isEdit">
            <el-radio label="expense">支出</el-radio><el-radio label="income">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 分类管理 - CRUD
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/category'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)
const loading = ref(false), saving = ref(false)
const allData = ref([])
const activeTab = ref('expense')
const dialogVisible = ref(false), isEdit = ref(false), editId = ref(0)
const form = reactive({ name: '', icon: '', type: 'expense', sortOrder: 0 })

const filteredData = computed(() => allData.value.filter(c => c.type === activeTab.value))

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getCategoryList()
    if (res.code === 200 && res.data) allData.value = res.data
  } catch (e) { /* ignore */ }
  loading.value = false
}

function openAddDialog() {
  isEdit.value = false; editId.value = 0
  Object.assign(form, { name: '', icon: '', type: activeTab.value, sortOrder: 0 })
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, { name: row.name, icon: row.icon||'', type: row.type, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

async function confirmSave() {
  if (!form.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    const data = { name: form.name, icon: form.icon, type: form.type, sortOrder: form.sortOrder }
    const res = isEdit.value ? await updateCategory(editId.value, data) : await addCategory(data)
    if (res.code === 200) { ElMessage.success(isEdit.value?'修改成功':'创建成功'); dialogVisible.value = false; fetchData() }
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类"${row.name}"？`, '删除确认', { type: 'warning' })
    const res = await deleteCategory(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); fetchData() }
  } catch (e) { /* cancel */ }
}
</script>

<style scoped>
.category-manage .page-header { display: flex; justify-content: space-between; align-items: center; }
</style>
