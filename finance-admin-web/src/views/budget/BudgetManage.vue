<!--
  预算与存钱目标管理页面
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="budget-manage">
    <div class="page-header glass-card"><h2 class="page-title">🎯 预算与存钱目标管理</h2></div>

    <!-- 用户搜索 -->
    <div class="search-bar glass-card">
      <el-input v-model="searchUserId" placeholder="输入用户ID查询" clearable style="width:200px" />
      <el-input v-model="searchUsername" placeholder="或输入用户名" clearable style="width:200px" />
      <el-input v-model="searchMonth" placeholder="月份(YYYY-MM)" clearable style="width:160px" />
      <el-button type="primary" @click="searchByUser">查询</el-button>
    </div>

    <el-row :gutter="16">
      <!-- 预算信息 -->
      <el-col :span="12">
        <el-card class="glass-card" shadow="never">
          <template #header><span>📊 用户预算</span></template>
          <div v-if="budgetData">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="月份">{{ budgetData.yearMonth }}</el-descriptions-item>
              <el-descriptions-item label="总预算">{{ formatMoney(budgetData.totalBudget) }}</el-descriptions-item>
              <el-descriptions-item label="已消费">{{ formatMoney(budgetData.totalSpent) }}</el-descriptions-item>
              <el-descriptions-item label="剩余">{{ formatMoney(budgetData.remaining) }}</el-descriptions-item>
              <el-descriptions-item label="使用率">
                <el-progress :percentage="Number(budgetData.usagePercent||0)" :color="progressColor(budgetData.usagePercent)" />
              </el-descriptions-item>
            </el-descriptions>
            <el-button style="margin-top:12px" type="primary" size="small" @click="openBudgetEdit">修正预算</el-button>
          </div>
          <el-empty v-else description="请先查询用户" />
        </el-card>
      </el-col>

      <!-- 存钱目标 -->
      <el-col :span="12">
        <el-card class="glass-card" shadow="never">
          <template #header><span>💰 存钱目标</span></template>
          <el-table :data="targets" size="small" max-height="400" stripe>
            <el-table-column prop="name" label="目标名称" />
            <el-table-column label="进度" min-width="180">
              <template #default="{row}">
                <el-progress :percentage="Number(row.progressPercent||0)" :status="row.status===1?'success':''" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="140">
              <template #default="{row}">{{ formatMoney(row.savedAmount) }}/{{ formatMoney(row.targetAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{row}">
                <el-tag :type="row.status===1?'success':'warning'" size="small">{{ row.status===1?'完成':'进行中' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预算修正弹窗 -->
    <el-dialog v-model="budgetEditVisible" title="修正预算" width="400px">
      <el-form label-width="100px">
        <el-form-item label="总预算"><el-input-number v-model="budgetEditForm.totalBudget" :min="0" :precision="2" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="budgetEditVisible=false">取消</el-button>
        <el-button type="primary" @click="confirmBudgetEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 预算与存钱目标管理
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive } from 'vue'
import { getUserBudget, updateUserBudget, getUserTargets } from '@/api/budget'
import { getCurrentMonth, formatMoney } from '@/utils/date'
import { ElMessage } from 'element-plus'

const searchUserId = ref('')
const searchUsername = ref('')
const searchMonth = ref(getCurrentMonth())
const budgetData = ref(null)
const targets = ref([])
const budgetEditVisible = ref(false)
const budgetEditForm = reactive({ id: 0, totalBudget: 0 })
const currentBudgetId = ref(0)

async function searchByUser() {
  // 先通过用户名找userId（简化：直接用输入的userId）
  const userId = Number(searchUserId.value)
  if (!userId) { ElMessage.warning('请输入有效用户ID'); return }
  try {
    const [budgetRes, targetRes] = await Promise.all([
      getUserBudget(userId, searchMonth.value),
      getUserTargets(userId)
    ])
    if (budgetRes.code === 200 && budgetRes.data) {
      budgetData.value = budgetRes.data
      currentBudgetId.value = budgetRes.data.id
    } else { budgetData.value = null }
    if (targetRes.code === 200 && targetRes.data) {
      targets.value = Array.isArray(targetRes.data) ? targetRes.data : []
    }
  } catch (e) { /* ignore */ }
}

function openBudgetEdit() {
  if (!budgetData.value) return
  budgetEditForm.id = budgetData.value.id
  budgetEditForm.totalBudget = budgetData.value.totalBudget
  budgetEditVisible.value = true
}

async function confirmBudgetEdit() {
  try {
    const res = await updateUserBudget(budgetEditForm.id, { totalBudget: budgetEditForm.totalBudget, yearMonth: searchMonth.value })
    if (res.code === 200) { ElMessage.success('预算已修正'); budgetEditVisible.value = false; searchByUser() }
  } catch (e) { /* ignore */ }
}

function progressColor(pct) { const v = Number(pct||0); return v>=100?'#FF7675':v>=80?'#FDCB6E':'#6C5CE7' }
</script>
