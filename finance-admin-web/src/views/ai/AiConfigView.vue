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
            <el-form-item label="API Key">
              <el-tag v-if="apiKeyConfigured" type="success" effect="dark" size="large">已配置 ✓</el-tag>
              <el-tag v-else type="danger" effect="dark" size="large">未配置 ✗</el-tag>
              <el-tooltip content="API Key由环境变量 DEEPSEEK_API_KEY 注入，不落库存储，安全管理" placement="top" :show-after="300">
                <span style="margin-left:8px;color:#909399;cursor:help;font-size:13px">ⓘ</span>
              </el-tooltip>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模型名称">
              <el-input v-model="paramsForm.model_name" placeholder="deepseek-chat" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Base URL">
              <el-input v-model="paramsForm.model_base_url" placeholder="https://api.deepseek.com/v1" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
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
          <el-col :span="8">
            <el-form-item label="Top P">
              <el-slider v-model="paramsForm.model_top_p" :min="0" :max="1" :step="0.05" show-input />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="24">
            <el-button type="primary" @click="saveParams" :loading="saving">💾 保存参数</el-button>
            <el-button type="success" @click="testConnection" :loading="testing" style="margin-left:12px">
              🔗 测试连接
            </el-button>
            <el-popconfirm title="确认重置为YAML默认值？此操作不可撤销" @confirm="resetDefaults" width="320">
              <template #reference>
                <el-button type="warning" :loading="resetting" style="margin-left:12px">🔄 重置默认值</el-button>
              </template>
            </el-popconfirm>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- Prompt模板 -->
    <el-card class="glass-card" shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span>📝 Prompt模板管理</span>
        </div>
      </template>
      <el-tabs v-model="promptTab">
        <el-tab-pane label="分析Prompt" name="analysis">
          <el-input v-model="promptAnalysis" type="textarea" :rows="8"
            placeholder="输入分析Prompt模板，支持 {{变量名}} 占位符" />
        </el-tab-pane>
        <el-tab-pane label="分类Prompt" name="classify">
          <el-input v-model="promptClassify" type="textarea" :rows="6"
            placeholder="输入分类Prompt模板，支持 {{变量名}} 占位符" />
        </el-tab-pane>
        <el-tab-pane label="特征提取Prompt" name="extraction">
          <el-input v-model="promptFeatureExtraction" type="textarea" :rows="8"
            placeholder="输入特征提取Prompt模板，支持 {{变量名}} 占位符" />
        </el-tab-pane>
      </el-tabs>
      <div style="display:flex;align-items:center;gap:12px;margin-top:12px;flex-wrap:wrap">
        <el-button type="primary" @click="savePrompt" :loading="saving">💾 保存模板</el-button>
        <el-popconfirm title="确认将当前标签页模板恢复为classpath默认值？" @confirm="resetCurrentPrompt" width="320">
          <template #reference>
            <el-button type="warning" :loading="resettingPrompt">🔄 重置当前模板</el-button>
          </template>
        </el-popconfirm>
        <el-button v-if="hasPromptHistory()" type="info" @click="rollbackPrompt" plain>
          ⏪ 恢复上次保存
        </el-button>
        <el-button @click="previewPrompt" plain>👁️ 变量预览</el-button>
      </div>
      <!-- 变量预览弹窗（全功能版） -->
      <el-dialog v-model="previewVisible" title="模板变量预览" width="900px" top="3vh">
        <div v-if="previewLoading" v-loading="previewLoading" style="min-height:200px"></div>
        <template v-else>
          <!-- 对比模式：左模板 | 右渲染 -->
          <el-row :gutter="12" style="margin-bottom:12px">
            <el-col :span="12">
              <div class="preview-panel">
                <div class="preview-panel-title">📄 模板原文</div>
                <pre class="preview-text">{{ currentTemplate }}</pre>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="preview-panel">
                <div class="preview-panel-title">🔍 渲染结果</div>
                <pre class="preview-text rendered">{{ previewContent }}</pre>
              </div>
            </el-col>
          </el-row>
          <!-- Token统计 -->
          <div style="display:flex;gap:16px;margin-bottom:12px;color:#606266;font-size:13px">
            <span>📊 模板字符: <b>{{ currentTemplate.length }}</b></span>
            <span>📊 渲染字符: <b>{{ previewContent.length }}</b></span>
            <span>🪙 估算Token: <b>~{{ Math.ceil(previewContent.length / 4) }}</b></span>
            <el-tag v-if="Math.ceil(previewContent.length / 4) > paramsForm.model_max_tokens" type="danger" size="small">
              ⚠️ 超出Max Tokens({{ paramsForm.model_max_tokens }})
            </el-tag>
          </div>
          <!-- 变量清单 + 自定义输入 -->
          <el-divider content-position="left">📋 变量清单（可自定义值）</el-divider>
          <div v-if="variableList.length === 0" style="color:#909399;font-size:13px">当前模板无变量</div>
          <el-form v-else label-width="160px" size="small">
            <el-form-item v-for="v in variableList" :key="v.name" :label="'{{'+v.name+'}}'">
              <el-input v-model="v.value" @input="onVariableChanged" />
            </el-form-item>
          </el-form>
          <!-- 数据来源 -->
          <div style="color:#909399;font-size:12px;margin-top:8px">
            数据来源: {{ previewSource === 'db' ? `真实AI分析记录（${previewYearMonth}）` : '默认示例数据' }}
            <el-button size="small" text type="primary" @click="refreshPreview">🔄 刷新真实数据</el-button>
          </div>
        </template>
      </el-dialog>
      <!-- Diff对比弹窗 -->
      <el-dialog v-model="diffVisible" title="模板变更对比" width="900px" top="3vh">
        <div v-if="diffLines.length === 0" style="color:#909399;text-align:center;padding:40px">无变更</div>
        <div v-else class="diff-container">
          <div v-for="(line, i) in diffLines" :key="i" :class="'diff-line diff-' + line.type">
            <span class="diff-sign">{{ line.type === 'add' ? '+' : line.type === 'del' ? '-' : ' ' }}</span>
            <span>{{ line.text }}</span>
          </div>
        </div>
        <template #footer>
          <el-button @click="diffVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSaveWithDiff">确认保存</el-button>
        </template>
      </el-dialog>
    </el-card>

    <!-- AI分析记录 -->
    <el-card class="glass-card" shadow="never">
      <template #header><span>📊 全平台AI分析记录</span></template>
      <div class="search-bar" style="box-shadow:none;padding:0 0 12px;margin-bottom:0">
        <el-input v-model="aiSearch.username" placeholder="用户名搜索" clearable style="width:160px" />
        <el-date-picker v-model="aiSearch.dateRange" type="daterange"
          range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
          value-format="YYYY-MM-DD" style="width:260px" />
        <el-button type="primary" @click="searchAiRecords">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
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
          :total="aiPage.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next"
          @current-change="fetchAiRecords" @size-change="onSizeChange" background small />
      </div>
    </el-card>

    <!-- AI详情弹窗 -->
    <el-dialog v-model="detailVisible" title="AI分析详情" width="700px" top="5vh">
      <div v-if="detailData" v-loading="detailLoading">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="用户">{{ detailData.username }}</el-descriptions-item>
          <el-descriptions-item label="月份">{{ detailData.yearMonth }}</el-descriptions-item>
          <el-descriptions-item label="总收入">{{ formatMoney(detailData.overview?.totalIncome) }}</el-descriptions-item>
          <el-descriptions-item label="总支出">{{ formatMoney(detailData.overview?.totalExpense) }}</el-descriptions-item>
          <el-descriptions-item label="结余">{{ formatMoney(detailData.overview?.balance) }}</el-descriptions-item>
          <el-descriptions-item label="健康评分">{{ detailData.overview?.healthScore ?? '-' }}分</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ (detailData.processingTimeMs/1000).toFixed(1) }}s</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">📝 月度复盘</h4>
        <p style="white-space:pre-wrap;line-height:1.8;color:#555">{{ detailData.overview?.summary || '暂无' }}</p>
        <h4 style="margin:16px 0 8px">⚠️ 冗余消费项</h4>
        <div v-for="(item,i) in (detailData.wasteItems||[])" :key="i" style="padding:8px;background:#FFF5F5;border-radius:6px;margin-bottom:6px">
          <b>{{ item.name }}</b> - {{ formatMoney(item.amount) }}<br>
          <span style="font-size:12px;color:#999">{{ item.reason }}</span>
        </div>
        <h4 style="margin:16px 0 8px">💡 省钱方案</h4>
        <div v-for="(item,i) in (detailData.suggestions||[])" :key="i" style="padding:8px;background:#E8F5E9;border-radius:6px;margin-bottom:6px">
          <b>{{ item.plan }}</b><br>
          <span style="font-size:12px;color:#999">{{ item.description }} — {{ item.estimatedMonthlySave }}</span>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { getAiConfig, updateAiConfig, testAiConnection, resetAiConfig, getPromptTemplateDefault, getPromptPreviewData, getPromptVariables, getAiRecords, getAiRecordDetail } from '@/api/ai'
import { formatMoney } from '@/utils/date'
import { ElMessage } from 'element-plus'

const loading = ref(false), saving = ref(false), testing = ref(false), resetting = ref(false), resettingPrompt = ref(false)
const apiKeyConfigured = ref(false)
const paramsForm = reactive({
  model_name: 'deepseek-chat',
  model_temperature: 0.7,
  model_max_tokens: 2048,
  model_top_p: 0.9,
  model_base_url: ''
})
const promptTab = ref('analysis')
const promptAnalysis = ref('')
const promptClassify = ref('')
const promptFeatureExtraction = ref('')

// 模板历史：保存上一次提交的内容，支持回滚
const promptHistory = reactive({ analysis: '', classify: '', extraction: '' })

// Diff对比
const diffVisible = ref(false)
const diffLines = ref([])  // [{type:'add'|'del'|'same', text}]
let pendingSaveConfigs = null  // 待保存的配置，diff确认后使用

// 当前模板内容（computed）
const currentTemplate = computed(() => {
  const map = { analysis: promptAnalysis, classify: promptClassify, extraction: promptFeatureExtraction }
  return map[promptTab.value]?.value || ''
})

// 变量预览（全功能版）
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewContent = ref('')
const previewSource = ref('default')
const previewYearMonth = ref('')
const variableList = ref([])  // [{name, value}]
const variableSampleData = ref({})  // 后端返回的原始样本数据

// AI记录
const aiLoading = ref(false)
const aiRecords = ref([])
const aiSearch = reactive({ username: '', dateRange: null })
const aiPage = reactive({ page: 1, size: 20, total: 0 })

// 详情
const detailVisible = ref(false), detailLoading = ref(false)
const detailData = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAiConfig()
    if (res.code === 200 && res.data?.configs) {
      apiKeyConfigured.value = res.data.apiKeyConfigured || false
      for (const c of res.data.configs) {
        const key = c.configKey
        if (key === 'prompt_template_analysis') promptAnalysis.value = c.configValue
        else if (key === 'prompt_template_classify') promptClassify.value = c.configValue
        else if (key === 'prompt_template_feature_extraction') promptFeatureExtraction.value = c.configValue
        else if (key === 'model_temperature') paramsForm.model_temperature = Number(c.configValue)
        else if (key === 'model_max_tokens') paramsForm.model_max_tokens = Number(c.configValue)
        else if (key === 'model_top_p') paramsForm.model_top_p = Number(c.configValue)
        else if (key === 'model_base_url') paramsForm.model_base_url = c.configValue
        else if (key === 'model_name') paramsForm.model_name = c.configValue
      }
      // 初始化历史记录为当前DB值（作为脏检测基准）
      promptHistory.analysis = promptAnalysis.value
      promptHistory.classify = promptClassify.value
      promptHistory.extraction = promptFeatureExtraction.value
    }
  } catch (e) {
    ElMessage.error('加载AI配置失败: ' + (e.message || '网络异常'))
  }
  loading.value = false
  fetchAiRecords()
})

/** 客户端参数校验 */
function validateParams() {
  const errors = []
  if (!paramsForm.model_name || !paramsForm.model_name.trim()) {
    errors.push('模型名称不能为空')
  }
  if (!paramsForm.model_base_url || !paramsForm.model_base_url.trim()) {
    errors.push('Base URL不能为空')
  } else if (!paramsForm.model_base_url.startsWith('http://') && !paramsForm.model_base_url.startsWith('https://')) {
    errors.push('Base URL必须以 http:// 或 https:// 开头')
  }
  if (paramsForm.model_temperature < 0 || paramsForm.model_temperature > 2) {
    errors.push('Temperature必须在 0~2 之间')
  }
  if (paramsForm.model_max_tokens < 256 || paramsForm.model_max_tokens > 8192) {
    errors.push('Max Tokens必须在 256~8192 之间')
  }
  if (paramsForm.model_top_p < 0 || paramsForm.model_top_p > 1) {
    errors.push('Top P必须在 0~1 之间')
  }
  return errors
}

async function saveParams() {
  const errors = validateParams()
  if (errors.length > 0) {
    ElMessage.warning(errors.join('; '))
    return
  }

  saving.value = true
  try {
    const configs = [
      { configKey: 'model_name', configValue: paramsForm.model_name },
      { configKey: 'model_temperature', configValue: String(paramsForm.model_temperature) },
      { configKey: 'model_max_tokens', configValue: String(paramsForm.model_max_tokens) },
      { configKey: 'model_top_p', configValue: String(paramsForm.model_top_p) },
      { configKey: 'model_base_url', configValue: paramsForm.model_base_url }
    ]
    const res = await updateAiConfig({ configs })
    if (res.code === 200) {
      ElMessage.success(res.message || '参数已更新，即时生效')
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  saving.value = false
}

async function testConnection() {
  testing.value = true
  try {
    const res = await testAiConnection()
    if (res.code === 200 && res.data) {
      const d = res.data
      if (d.success) {
        ElMessage.success(`连接成功！模型: ${d.modelName}, 耗时: ${d.elapsedMs}ms`)
      } else {
        ElMessage.warning(`连接测试完成但响应异常: ${d.error || '未知'}`)
      }
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  testing.value = false
}

async function resetDefaults() {
  resetting.value = true
  try {
    const res = await resetAiConfig()
    if (res.code === 200) {
      ElMessage.success(res.message || '已重置为默认值，请刷新页面查看')
      const cfgRes = await getAiConfig()
      if (cfgRes.code === 200 && cfgRes.data?.configs) {
        apiKeyConfigured.value = cfgRes.data.apiKeyConfigured || false
        for (const c of cfgRes.data.configs) {
          const key = c.configKey
          if (key === 'model_temperature') paramsForm.model_temperature = Number(c.configValue)
          else if (key === 'model_max_tokens') paramsForm.model_max_tokens = Number(c.configValue)
          else if (key === 'model_top_p') paramsForm.model_top_p = Number(c.configValue)
          else if (key === 'model_base_url') paramsForm.model_base_url = c.configValue
          else if (key === 'model_name') paramsForm.model_name = c.configValue
        }
      }
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  resetting.value = false
}

/** 校验 {{ }} 占位符是否配对 */
function validateTemplateBraces(template, name) {
  const open = (template.match(/\{\{/g) || []).length
  const close = (template.match(/\}\}/g) || []).length
  if (open !== close) {
    return `${name}: 占位符 {{ }} 未配对（{{ 出现${open}次，}} 出现${close}次）`
  }
  return null
}

async function savePrompt() {
  const templates = [
    { key: 'prompt_template_analysis', value: promptAnalysis.value, name: '分析Prompt' },
    { key: 'prompt_template_classify', value: promptClassify.value, name: '分类Prompt' },
    { key: 'prompt_template_feature_extraction', value: promptFeatureExtraction.value, name: '特征提取Prompt' }
  ]

  // 校验 {{ }} 配对
  const braceErrors = []
  for (const t of templates) {
    const err = validateTemplateBraces(t.value, t.name)
    if (err) braceErrors.push(err)
  }
  if (braceErrors.length > 0) {
    ElMessage.warning(braceErrors.join('; '))
    return
  }

  // 校验至少有一个非空
  if (templates.every(t => !t.value.trim())) {
    ElMessage.warning('至少需要一个Prompt模板')
    return
  }

  saving.value = true
  try {
    // 保存历史版本
    promptHistory.analysis = promptAnalysis.value
    promptHistory.classify = promptClassify.value
    promptHistory.extraction = promptFeatureExtraction.value

    const configs = templates
      .filter(t => t.value.trim())
      .map(t => ({ configKey: t.key, configValue: t.value }))

    // 先展示Diff对比
    const before = getCurrentTemplateForDiff()
    const after = currentTemplate.value
    diffLines.value = computeDiff(before, after)
    pendingSaveConfigs = configs
    diffVisible.value = true
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  saving.value = false
}

/** 获取当前模板用于Diff对比 */
function getCurrentTemplateForDiff() {
  const map = { analysis: promptAnalysis, classify: promptClassify, extraction: promptFeatureExtraction }
  return map[promptTab.value]?.value || ''
}

/** 计算文本Diff */
function computeDiff(oldText, newText) {
  const lines = []
  if (oldText === newText) return lines

  const oldLines = oldText.split('\n')
  const newLines = newText.split('\n')
  const maxLen = Math.max(oldLines.length, newLines.length)

  for (let i = 0; i < maxLen; i++) {
    const o = i < oldLines.length ? oldLines[i] : null
    const n = i < newLines.length ? newLines[i] : null
    if (o === n) {
      lines.push({ type: 'same', text: o || '' })
    } else {
      if (o !== null) lines.push({ type: 'del', text: o })
      if (n !== null) lines.push({ type: 'add', text: n })
    }
  }
  return lines
}

/** 确认保存（Diff对比后） */
async function confirmSaveWithDiff() {
  diffVisible.value = false
  if (!pendingSaveConfigs) return

  saving.value = true
  try {
    const res = await updateAiConfig({ configs: pendingSaveConfigs })
    if (res.code === 200) {
      ElMessage.success(res.message || '模板已保存，即时生效')
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  pendingSaveConfigs = null
  saving.value = false
}

/** 当前标签页是否有历史版本 */
function hasPromptHistory() {
  const map = { analysis: 'analysis', classify: 'classify', extraction: 'extraction' }
  const key = map[promptTab.value]
  if (!key) return false
  return !!promptHistory[key]
}

/** 恢复当前标签页到上次保存的版本 */
function rollbackPrompt() {
  const map = { analysis: promptAnalysis, classify: promptClassify, extraction: promptFeatureExtraction }
  const histKeyMap = { analysis: 'analysis', classify: 'classify', extraction: 'extraction' }
  const ref = map[promptTab.value]
  const histKey = histKeyMap[promptTab.value]
  if (ref && histKey && promptHistory[histKey]) {
    ref.value = promptHistory[histKey]
    ElMessage.success('已恢复上次保存的模板内容')
  }
}

/** 重置当前标签页模板为classpath默认值 */
async function resetCurrentPrompt() {
  const keyMap = {
    analysis: 'prompt_template_analysis',
    classify: 'prompt_template_classify',
    extraction: 'prompt_template_feature_extraction'
  }
  const key = keyMap[promptTab.value]
  if (!key) return

  resettingPrompt.value = true
  try {
    const res = await getPromptTemplateDefault(key)
    if (res.code === 200 && res.data?.content) {
      const map = { analysis: promptAnalysis, classify: promptClassify, extraction: promptFeatureExtraction }
      const ref = map[promptTab.value]
      if (ref) {
        ref.value = res.data.content
        ElMessage.success('已加载classpath默认模板，请点击保存以生效')
      }
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  resettingPrompt.value = false
}

/** 变量预览：从DB加载真实数据 + 左右对比 + Token统计 + 自定义输入 */
async function previewPrompt() {
  if (!currentTemplate.value.trim()) {
    ElMessage.warning('当前模板为空，无法预览')
    return
  }
  previewVisible.value = true
  await loadPreviewData()
}

/** 加载预览数据 */
async function loadPreviewData() {
  previewLoading.value = true
  const keyMap = {
    analysis: 'prompt_template_analysis',
    classify: 'prompt_template_classify',
    extraction: 'prompt_template_feature_extraction'
  }
  const key = keyMap[promptTab.value]
  if (!key) { previewLoading.value = false; return }

  try {
    const dataRes = await getPromptPreviewData(key).catch(() => null)

    if (dataRes && dataRes.code === 200 && dataRes.data) {
      variableSampleData.value = dataRes.data
      const hasRealData = dataRes.data.yearMonth && dataRes.data.yearMonth !== '无真实数据'
      previewSource.value = hasRealData ? 'db' : 'default'
      previewYearMonth.value = hasRealData ? dataRes.data.yearMonth : ''
    }

    // 始终从当前文本域内容提取变量名（而非DB），确保用户修改的变量都能被替换
    const varNames = extractVarNames(currentTemplate.value)

    variableList.value = varNames.map(name => ({
      name,
      value: (variableSampleData.value && variableSampleData.value[name]) || `[${name}示例值]`
    }))

    renderPreview()
  } catch (_) {
    // 降级：使用硬编码变量
    variableList.value = extractVarNames(currentTemplate.value).map(name => ({
      name, value: `[${name}示例值]`
    }))
    renderPreview()
  }
  previewLoading.value = false
}

/** 从模板提取变量名 */
function extractVarNames(template) {
  const names = []
  const re = /\{\{(\w+)\}\}/g
  let m
  while ((m = re.exec(template)) !== null) {
    if (!names.includes(m[1])) names.push(m[1])
  }
  return names
}

/** 渲染预览 */
function renderPreview() {
  let result = currentTemplate.value
  for (const v of variableList.value) {
    result = result.replace(new RegExp('\\{\\{' + v.name + '\\}\\}', 'g'), v.value)
  }
  previewContent.value = result
}

/** 变量值改变时重新渲染 */
function onVariableChanged() {
  renderPreview()
}

/** 刷新真实数据 */
async function refreshPreview() {
  await loadPreviewData()
}

/** 搜索按钮：重置页码后查询 */
function searchAiRecords() {
  aiPage.page = 1
  fetchAiRecords()
}

/** 重置搜索条件 */
function resetSearch() {
  aiSearch.username = ''
  aiSearch.dateRange = null
  aiPage.page = 1
  fetchAiRecords()
}

/** 每页条数变更：重置页码后查询 */
function onSizeChange() {
  aiPage.page = 1
  fetchAiRecords()
}

async function fetchAiRecords() {
  aiLoading.value = true
  try {
    const params = {
      page: aiPage.page, size: aiPage.size,
      username: aiSearch.username || undefined
    }
    if (aiSearch.dateRange && aiSearch.dateRange.length === 2) {
      params.startDate = aiSearch.dateRange[0]
      params.endDate = aiSearch.dateRange[1]
    }
    const res = await getAiRecords(params)
    if (res.code === 200 && res.data) {
      aiRecords.value = res.data.records || []
      aiPage.total = res.data.total || 0
    }
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  aiLoading.value = false
}

async function showDetail(row) {
  detailVisible.value = true; detailData.value = null
  detailLoading.value = true
  try {
    const res = await getAiRecordDetail(row.id)
    if (res.code === 200 && res.data) detailData.value = res.data
  } catch (_) {
    // 拦截器已统一处理错误提示
  }
  detailLoading.value = false
}
</script>

<style scoped>
.preview-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  height: 300px;
  display: flex;
  flex-direction: column;
}
.preview-panel-title {
  background: #f5f7fa;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}
.preview-text {
  flex: 1;
  margin: 0;
  padding: 12px;
  font-size: 12px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
  overflow-y: auto;
  background: #fff;
  color: #333;
  font-family: 'Courier New', monospace;
}
.preview-text.rendered {
  background: #f0f9eb;
  color: #1a5e2a;
}

/* Diff对比 */
.diff-container {
  max-height: 500px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.diff-line {
  padding: 2px 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
.diff-add {
  background: #e6ffec;
  color: #1a5e2a;
}
.diff-del {
  background: #ffebe9;
  color: #c62828;
}
.diff-same {
  color: #909399;
}
.diff-sign {
  display: inline-block;
  width: 20px;
  font-weight: bold;
  user-select: none;
}
</style>