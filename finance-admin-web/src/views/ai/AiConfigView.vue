<!--
  AI系统配置页面 - 高分特色功能
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="ai-config">
    <div class="page-header glass-card"><h2 class="page-title">🤖 AI系统配置</h2></div>

    <!-- 模型参数配置 -->
    <el-card class="glass-card" shadow="never" style="margin-bottom:16px">
      <template #header><span>⚙️ 模型调用参数</span></template>
      <el-form :model="paramsForm" label-width="140px" v-loading="loading">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="模型名称">
              <el-input v-model="paramsForm.model_name" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Temperature">
              <el-slider v-model="paramsForm.model_temperature" :min="0" :max="2" :step="0.1" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Max Tokens">
              <el-input-number v-model="paramsForm.model_max_tokens" :min="256" :max="8192" :step="256" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="Top P">
              <el-slider v-model="paramsForm.model_top_p" :min="0" :max="1" :step="0.05" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Base URL">
              <el-input v-model="paramsForm.model_base_url" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-button type="primary" style="margin-top:4px" @click="saveParams" :loading="saving">💾 保存参数</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- Prompt模板 -->
    <el-card class="glass-card" shadow="never" style="margin-bottom:16px">
      <template #header><span>📝 Prompt模板管理</span></template>
      <el-tabs v-model="promptTab">
        <el-tab-pane label="分析Prompt" name="analysis">
          <el-input v-model="promptAnalysis" type="textarea" :rows="8" />
        </el-tab-pane>
        <el-tab-pane label="分类Prompt" name="classify">
          <el-input v-model="promptClassify" type="textarea" :rows="6" />
        </el-tab-pane>
      </el-tabs>
      <el-button type="primary" @click="savePrompt" :loading="saving" style="margin-top:12px">💾 保存模板</el-button>
    </el-card>

    <!-- AI分析记录 -->
    <el-card class="glass-card" shadow="never">
      <template #header><span>📊 全平台AI分析记录</span></template>
      <div class="search-bar" style="box-shadow:none;padding:0 0 12px;margin-bottom:0">
        <el-input v-model="aiSearch.username" placeholder="用户名" clearable style="width:160px" />
        <el-input v-model="aiSearch.yearMonth" placeholder="月份(YYYY-MM)" clearable style="width:140px" />
        <el-button type="primary" @click="fetchAiRecords">搜索</el-button>
      </div>
      <el-table :data="aiRecords" v-loading="aiLoading" stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="yearMonth" label="分析月份" width="100" />
        <el-table-column prop="modelName" label="模型" width="100" />
        <el-table-column prop="processingTimeMs" label="耗时" width="90">
          <template #default="{row}">{{ (row.processingTimeMs/1000).toFixed(1) }}s</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="分析时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="showDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;margin-top:12px">
        <el-pagination v-model:current-page="aiPage.page" v-model:page-size="aiPage.size"
          :total="aiPage.total" :page-sizes="[10,20,50]" layout="total,prev,pager,next"
          @change="fetchAiRecords" background small />
      </div>
    </el-card>

    <!-- AI详情弹窗 -->
    <el-dialog v-model="detailVisible" title="AI分析详情" width="700px" top="5vh">
      <div v-if="detailData" v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="月份">{{ detailData.yearMonth }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ (detailData.processingTimeMs/1000).toFixed(1) }}s</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">📝 月度复盘</h4>
        <p style="white-space:pre-wrap;line-height:1.8;color:#555">{{ detailData.monthlyReview }}</p>
        <h4 style="margin:16px 0 8px">⚠️ 冗余消费项</h4>
        <div v-for="(item,i) in (detailData.redundantItems||[])" :key="i" style="padding:8px;background:#FFF5F5;border-radius:6px;margin-bottom:6px">
          <b>{{ item.name }}</b> - {{ formatMoney(item.amount) }}<br>
          <span style="font-size:12px;color:#999">{{ item.reason }}</span>
        </div>
        <h4 style="margin:16px 0 8px">💡 省钱方案</h4>
        <div v-for="(item,i) in (detailData.savingPlans||[])" :key="i" style="padding:8px;background:#E8F5E9;border-radius:6px;margin-bottom:6px">
          <b>{{ item.plan }}</b><br>
          <span style="font-size:12px;color:#999">{{ item.description }} — {{ item.estimatedSave }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * AI系统配置 - Prompt管理+分析记录
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, reactive, onMounted } from 'vue'
import { getAiConfig, updateAiConfig, getAiRecords, getAiRecordDetail } from '@/api/ai'
import { formatMoney } from '@/utils/date'
import { ElMessage } from 'element-plus'

const loading = ref(false), saving = ref(false)
const paramsForm = reactive({ model_name: 'deepseek-chat', model_temperature: 0.7, model_max_tokens: 2048, model_top_p: 0.9, model_base_url: '' })
const promptTab = ref('analysis')
const promptAnalysis = ref('')
const promptClassify = ref('')

// AI记录
const aiLoading = ref(false)
const aiRecords = ref([])
const aiSearch = reactive({ username: '', yearMonth: '' })
const aiPage = reactive({ page: 1, size: 20, total: 0 })

// 详情
const detailVisible = ref(false), detailLoading = ref(false)
const detailData = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200 && res.data?.configs) {
      for (const c of res.data.configs) {
        const key = c.configKey
        if (key === 'prompt_template_analysis') promptAnalysis.value = c.configValue
        else if (key === 'prompt_template_classify') promptClassify.value = c.configValue
        else if (key === 'model_temperature') paramsForm.model_temperature = Number(c.configValue)
        else if (key === 'model_max_tokens') paramsForm.model_max_tokens = Number(c.configValue)
        else if (key === 'model_top_p') paramsForm.model_top_p = Number(c.configValue)
        else if (key === 'model_base_url') paramsForm.model_base_url = c.configValue
        else if (key === 'model_name') paramsForm.model_name = c.configValue
      }
    }
  } catch (e) { /* ignore */ }
  loading.value = false
  fetchAiRecords()
})

async function saveParams() {
  saving.value = true
  try {
    const configs = [
      { configKey: 'model_temperature', configValue: String(paramsForm.model_temperature) },
      { configKey: 'model_max_tokens', configValue: String(paramsForm.model_max_tokens) },
      { configKey: 'model_top_p', configValue: String(paramsForm.model_top_p) },
      { configKey: 'model_base_url', configValue: paramsForm.model_base_url }
    ]
    const res = await updateAiConfig({ configs })
    if (res.code === 200) ElMessage.success('参数已更新，即时生效')
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function savePrompt() {
  saving.value = true
  try {
    const configs = [
      { configKey: 'prompt_template_analysis', configValue: promptAnalysis.value },
      { configKey: 'prompt_template_classify', configValue: promptClassify.value }
    ]
    const res = await updateAiConfig({ configs })
    if (res.code === 200) ElMessage.success('模板已保存')
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function fetchAiRecords() {
  aiLoading.value = true
  try {
    const res = await getAiRecords({
      page: aiPage.page, size: aiPage.size,
      username: aiSearch.username || undefined,
      yearMonth: aiSearch.yearMonth || undefined
    })
    if (res.code === 200 && res.data) {
      aiRecords.value = res.data.records || []
      aiPage.total = res.data.total || 0
    }
  } catch (e) { /* ignore */ }
  aiLoading.value = false
}

async function showDetail(row) {
  detailVisible.value = true; detailData.value = null
  detailLoading.value = true
  try {
    const res = await getAiRecordDetail(row.id)
    if (res.code === 200 && res.data) detailData.value = res.data
  } catch (e) { /* ignore */ }
  detailLoading.value = false
}
</script>
