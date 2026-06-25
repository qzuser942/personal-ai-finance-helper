<!--
  后台首页数据大屏仪表盘 - 企业级优化版
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="page-header glass-card" style="padding:20px 24px">
      <el-row :gutter="16" align="middle">
        <el-col :span="16">
          <h2 class="page-title" style="margin:0">📊 运营数据看板</h2>
          <p style="color:#999;margin:8px 0 0">
            实时监控全平台用户及账单数据 · 30秒自动刷新
            <span v-if="lastFetchTime" style="margin-left:8px;font-size:12px;color:#bbb">
              更新于 {{ lastFetchTime }}
            </span>
          </p>
        </el-col>
        <el-col :span="8" style="text-align:right">
          <el-tag type="info" size="large" effect="light" style="margin-right:8px">
            {{ currentMonth }} 月
          </el-tag>
          <el-button type="primary" plain size="small" @click="handleExport" :loading="exporting">
            <el-icon><Download /></el-icon> 导出报表
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 异常告警 -->
    <div v-if="alerts.length > 0" style="margin-top:16px">
      <el-alert v-for="(a, i) in alerts" :key="i" :type="a.type" :closable="false" show-icon style="margin-bottom:8px">
        <template #title>{{ a.message }}</template>
      </el-alert>
    </div>

    <!-- 骨架屏 -->
    <template v-if="loading">
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="6" v-for="i in 4" :key="i">
          <el-skeleton animated style="padding:20px">
            <template #template>
              <el-skeleton-item variant="text" style="width:60%;margin-bottom:12px" />
              <el-skeleton-item variant="text" style="width:40%;height:32px;margin-bottom:8px" />
              <el-skeleton-item variant="text" style="width:50%" />
            </template>
          </el-skeleton>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="16">
          <el-card class="glass-card" shadow="never">
            <template #header><span>📈 平台数据趋势</span></template>
            <el-skeleton animated style="padding:20px">
              <template #template>
                <el-skeleton-item variant="rect" style="width:100%;height:350px" />
              </template>
            </el-skeleton>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="glass-card" shadow="never">
            <template #header><span>⚡ 快捷功能入口</span></template>
            <div class="quick-actions">
              <el-button class="action-item" round disabled v-for="t in quickActions" :key="t.label">
                <el-icon><component :is="t.icon" /></el-icon> {{ t.label }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <template v-else>
      <!-- 统计卡片 -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="6" v-for="item in statCards" :key="item.label">
          <div class="stat-card">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ item.value }}</div>
            <div style="display:flex;align-items:center;gap:6px;margin-top:8px">
              <el-icon :color="item.trend >= 0 ? '#00B894' : '#FF7675'" :size="16">
                <CaretTop v-if="item.trend >= 0" /><CaretBottom v-else />
              </el-icon>
              <span :style="{color: item.trend >= 0 ? '#00B894' : '#FF7675', fontSize:'12px'}">
                {{ item.trendText }}
              </span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 趋势图区：方案A 双图表分离 -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="16">
          <el-card class="glass-card" shadow="never">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>📈 平台数据趋势分析</span>
                <el-radio-group v-model="trendInterval" size="small" @change="onIntervalChange">
                  <el-radio-button :value="3">近3月</el-radio-button>
                  <el-radio-button :value="6">近6月</el-radio-button>
                  <el-radio-button :value="12">近12月</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <!-- 上图：业务规模（数量类） -->
            <div ref="scaleChartRef" style="width:100%;height:220px"></div>
            <!-- 下图：营收指标（金额类） -->
            <div ref="revenueChartRef" style="width:100%;height:200px;margin-top:8px"></div>
            <div style="text-align:center;margin-top:4px">
              <span style="font-size:11px;color:#999">点击图形可下钻至对应月份账单详情</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="glass-card" shadow="never" style="height:100%">
            <template #header><span>⚡ 快捷功能入口</span></template>
            <div class="quick-actions">
              <el-button
                v-for="t in visibleQuickActions" :key="t.label"
                class="action-item" round @click="$router.push(t.path)"
              >
                <el-icon><component :is="t.icon" /></el-icon> {{ t.label }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 消费统计排行 -->
      <el-row :gutter="16" style="margin-top:20px">
        <el-col :span="12">
          <el-card class="glass-card" shadow="never">
            <template #header>
              <span>🏆 用户消费排行TOP10</span>
              <span style="font-size:12px;color:#999;margin-left:8px">点击查看用户详情</span>
            </template>
            <el-table :data="userRanking" stripe size="small" max-height="340" @row-click="drillToUser">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="username" label="用户" />
              <el-table-column prop="totalAmount" label="消费额">
                <template #default="{row}">{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
              <el-table-column prop="billCount" label="账单数" width="70" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="glass-card" shadow="never">
            <template #header>
              <span>📊 分类消费分布</span>
              <span style="font-size:12px;color:#999;margin-left:8px">点击切片跳转账单</span>
            </template>
            <div ref="pieChartRef" style="width:100%;height:340px"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
/**
 * 后台总看板 - 方案A：双图表分离（业务规模 + 营收趋势）
 * 核心优化：
 *   - 双图表分离数量/金额，消除双Y轴混淆
 *   - 补齐缺失月份（后端 fillMissingMonths）
 *   - 人均账单、客单价衍生指标
 *   - 区间切换 3/6/12 月
 *   - 数据标签 + 均值参考线 + 空月标注
 *   - 数据缓存 TTL 5分钟 + KeepAlive 保活
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard, getDashboardAlerts, exportDashboard, getBillStatistics } from '@/api/dashboard'
import { formatMoney, getCurrentMonth } from '@/utils/date'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'

const CACHE_TTL = 5 * 60 * 1000

const router = useRouter()
const userStore = useUserStore()
const isSuperAdmin = computed(() => userStore.isSuperAdmin)
const currentMonth = getCurrentMonth()

const scaleChartRef = ref(null)
const revenueChartRef = ref(null)
const pieChartRef = ref(null)
const userRanking = ref([])
const loading = ref(true)
const exporting = ref(false)
const alerts = ref([])
const lastFetchTime = ref('')
const trendInterval = ref(6)

let scaleChart = null
let revenueChart = null
let pieChart = null
let refreshTimer = null
let scaleObserver = null
let revenueObserver = null
let pieObserver = null
let cacheTs = 0
let cachedDashboard = null
let cachedStats = null

const statCards = ref([
  { label: '平台用户总数', value: '0', trend: 0, trendText: '--' },
  { label: '当月活跃用户', value: '0', trend: 0, trendText: '--' },
  { label: '当月账单总量', value: '0', trend: 0, trendText: '--' },
  { label: '当月交易总额', value: '¥0', trend: 0, trendText: '--' }
])

const quickActions = [
  { label: '用户管理', path: '/users', icon: 'User', needSuper: false },
  { label: '账单管理', path: '/bills', icon: 'List', needSuper: false },
  { label: '分类管理', path: '/categories', icon: 'Grid', needSuper: false },
  { label: 'AI配置', path: '/ai-config', icon: 'Cpu', needSuper: true },
  { label: '操作日志', path: '/logs', icon: 'Document', needSuper: false },
  { label: '管理员', path: '/admins', icon: 'Setting', needSuper: true },
  { label: '数据库备份', path: '/database', icon: 'Coin', needSuper: true }
]

const visibleQuickActions = computed(() =>
  quickActions.filter(a => !a.needSuper || isSuperAdmin.value)
)

onMounted(() => { initFromCache(); startRefresh() })

onActivated(() => {
  if (Date.now() - cacheTs > CACHE_TTL) { fetchAll(); fetchAlerts() }
  startRefresh()
  nextTick(() => { scaleChart?.resize(); revenueChart?.resize(); pieChart?.resize() })
})

onDeactivated(() => stopRefresh())

onUnmounted(() => {
  stopRefresh()
  disposeAllCharts()
})

function disposeAllCharts() {
  scaleObserver?.disconnect(); revenueObserver?.disconnect(); pieObserver?.disconnect()
  scaleChart?.dispose(); scaleChart = null
  revenueChart?.dispose(); revenueChart = null
  pieChart?.dispose(); pieChart = null
}

function startRefresh() { stopRefresh(); refreshTimer = setInterval(() => { fetchAll(); fetchAlerts() }, 30_000) }
function stopRefresh() { clearInterval(refreshTimer); refreshTimer = null }

function initFromCache() {
  const raw = localStorage.getItem('dashboard_cache')
  if (raw) {
    try {
      const c = JSON.parse(raw)
      if (Date.now() - c.ts < CACHE_TTL) {
        if (c.dashboard) {
          applyDashboardData(c.dashboard)
          nextTick(() => {
            if (c.dashboard.recent6MonthTrend?.length) renderCharts(c.dashboard.recent6MonthTrend)
          })
        }
        if (c.stats) {
          userRanking.value = c.stats.userRanking || []
          if (c.stats.categoryDistribution?.length) nextTick(() => renderPieChart(c.stats.categoryDistribution))
        }
        cacheTs = c.ts; cachedDashboard = c.dashboard; cachedStats = c.stats
        lastFetchTime.value = c.timeLabel || ''
        loading.value = false
      }
    } catch { /* ignore */ }
  }
  fetchAll(); fetchAlerts()
}

function persistCache(dashboard, stats) {
  cacheTs = Date.now(); cachedDashboard = dashboard; cachedStats = stats
  const now = new Date()
  lastFetchTime.value = now.getHours().toString().padStart(2, '0') + ':' +
    now.getMinutes().toString().padStart(2, '0') + ':' + now.getSeconds().toString().padStart(2, '0')
  try { localStorage.setItem('dashboard_cache', JSON.stringify({ ts: cacheTs, timeLabel: lastFetchTime.value, dashboard, stats })) } catch { /* quota */ }
}

async function fetchAll() {
  try {
    const [dashRes, statRes] = await Promise.all([
      getDashboard(trendInterval.value), getBillStatistics(currentMonth)
    ])
    if (dashRes.code === 200 && dashRes.data) {
      applyDashboardData(dashRes.data)
      persistCache(dashRes.data, statRes.code === 200 && statRes.data ? statRes.data : cachedStats)
      await nextTick()
      if (dashRes.data.recent6MonthTrend?.length) renderCharts(dashRes.data.recent6MonthTrend)
    }
    if (statRes.code === 200 && statRes.data) {
      userRanking.value = statRes.data.userRanking || []
      await nextTick()
      if (statRes.data.categoryDistribution?.length) renderPieChart(statRes.data.categoryDistribution)
    }
  } catch { /* 静默 */ }
  loading.value = false
}

async function fetchAlerts() {
  try { const res = await getDashboardAlerts(); if (res.code === 200) alerts.value = res.data || [] } catch { /* 静默 */ }
}

function onIntervalChange() { fetchAll() }

function applyDashboardData(d) {
  statCards.value[0].value = d.totalUsers || 0
  statCards.value[0].trend = 0; statCards.value[0].trendText = '累计值'

  const activeMoM = d.activeUsersMoM != null ? d.activeUsersMoM : 0
  statCards.value[1].value = d.activeUsers || 0
  statCards.value[1].trend = activeMoM; statCards.value[1].trendText = formatMoM(activeMoM)

  const billMoM = d.billCountMoM != null ? d.billCountMoM : 0
  statCards.value[2].value = d.currentMonthBillCount || 0
  statCards.value[2].trend = billMoM; statCards.value[2].trendText = formatMoM(billMoM)

  const amountMoM = d.totalAmountMoM != null ? d.totalAmountMoM : 0
  statCards.value[3].value = formatMoney(d.currentMonthTotalAmount)
  statCards.value[3].trend = amountMoM; statCards.value[3].trendText = formatMoM(amountMoM)
}

function formatMoM(mom) {
  if (mom === 0) return '与上月持平'
  return `较上月${mom > 0 ? '+' : ''}${mom.toFixed(1)}%`
}

async function handleExport() {
  exporting.value = true
  try {
    const result = await exportDashboard(trendInterval.value)
    if (!result) { exporting.value = false; return }
    const url = URL.createObjectURL(result.blob)
    const a = document.createElement('a')
    a.href = url
    a.download = result.fileName
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('报表导出成功')
  } catch { /* ignore */ }
  exporting.value = false
}

/* ===================== 双图表渲染 ===================== */

function renderCharts(data) {
  renderScaleChart(data)
  renderRevenueChart(data)
}

function renderScaleChart(data) {
  if (!scaleChartRef.value) return
  const months = data.map(d => d.yearMonth)
  const billData = data.map(d => d.billCount)
  const userData = data.map(d => d.userCount)
  const avgBill = (billData.reduce((a, b) => a + b, 0) / billData.length).toFixed(0)
  const avgUser = (userData.reduce((a, b) => a + b, 0) / userData.length).toFixed(0)
  const isNew = !scaleChart

  if (isNew) {
    scaleChart = echarts.init(scaleChartRef.value)
    scaleChart.showLoading({ text: '加载中...', color: '#6C5CE7', maskColor: 'rgba(255,255,255,0.85)', textColor: '#6C5CE7' })
  }

  const emptyMonths = months.map((m, i) => billData[i] === 0 && userData[i] === 0 ? m : null).filter(Boolean)

  scaleChart.setOption({
    color: ['#6C5CE7', '#00CEC9'],
    legend: {
      data: ['账单数', '活跃用户'],
      top: 0, left: 'center',
      textStyle: { fontSize: 12, color: '#666' },
      itemWidth: 12, itemHeight: 12, itemGap: 32
    },
    grid: { left: 70, right: 20, top: 40, bottom: emptyMonths.length > 0 ? 40 : 20 },
    xAxis: {
      type: 'category', data: months,
      axisLabel: { color: '#666', fontSize: 11 },
      axisLine: { lineStyle: { color: '#E0E0E0' } },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '数量（个）',
      nameTextStyle: { color: '#999', fontSize: 11 },
      axisLabel: { color: '#999', fontSize: 11 },
      splitLine: { lineStyle: { type: 'dashed', color: '#F0F0F0' } },
      axisLine: { show: false }, axisTick: { show: false }
    },
    series: [
      {
        name: '账单数', type: 'bar', data: billData, barWidth: 20, barGap: '20%',
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#A29BFE' }, { offset: 1, color: '#6C5CE7' }])
        },
        label: { show: true, position: 'top', color: '#6C5CE7', fontSize: 11, fontWeight: 'bold', formatter: p => p.value > 0 ? p.value : '' },
        markLine: {
          silent: true, symbol: 'none',
          lineStyle: { type: 'dashed', color: '#A29BFE', width: 1 },
          label: { color: '#A29BFE', fontSize: 10, formatter: `近${trendInterval.value}月均账单 {c} 笔` },
          data: [{ yAxis: +avgBill }]
        },
        animationDuration: 800, animationDelay: idx => idx * 80
      },
      {
        name: '活跃用户', type: 'bar', data: userData, barWidth: 20,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#55EFC4' }, { offset: 1, color: '#00CEC9' }])
        },
        label: { show: true, position: 'top', color: '#00CEC9', fontSize: 11, fontWeight: 'bold', formatter: p => p.value > 0 ? p.value : '' },
        markLine: {
          silent: true, symbol: 'none',
          lineStyle: { type: 'dashed', color: '#55EFC4', width: 1 },
          label: { color: '#55EFC4', fontSize: 10, formatter: `近${trendInterval.value}月均用户 {c} 人` },
          data: [{ yAxis: +avgUser }]
        },
        animationDuration: 800, animationDelay: idx => idx * 80 + 150
      }
    ]
  }, true)

  if (isNew) {
    scaleChart.hideLoading()
    scaleChart.on('click', p => { if (p.componentType === 'series') router.push({ path: '/bills', query: { month: p.name } }) })
    scaleObserver = new ResizeObserver(() => scaleChart?.resize())
    scaleObserver.observe(scaleChartRef.value)
  }
}

function renderRevenueChart(data) {
  if (!revenueChartRef.value) return
  const months = data.map(d => d.yearMonth)
  const amountData = data.map(d => +(d.totalAmount / 10000).toFixed(1))
  const avgAmount = (amountData.reduce((a, b) => a + b, 0) / amountData.length).toFixed(1)
  const isNew = !revenueChart

  if (isNew) {
    revenueChart = echarts.init(revenueChartRef.value)
    revenueChart.showLoading({ text: '加载中...', color: '#FF7675', maskColor: 'rgba(255,255,255,0.85)', textColor: '#FF7675' })
  }

  const hasData = amountData.some(v => v > 0)

  revenueChart.setOption({
    color: ['#FF7675'],
    legend: {
      data: ['交易额'],
      top: 0, left: 'center',
      textStyle: { fontSize: 12, color: '#666' },
      itemWidth: 12, itemHeight: 12
    },
    grid: { left: 70, right: 20, top: 40, bottom: 20 },
    xAxis: {
      type: 'category', data: months,
      axisLabel: { color: '#666', fontSize: 11 },
      axisLine: { lineStyle: { color: '#E0E0E0' } },
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '交易额（万元）',
      nameTextStyle: { color: '#999', fontSize: 11 },
      axisLabel: { color: '#999', fontSize: 11, formatter: '¥{value}万' },
      splitLine: { lineStyle: { type: 'dashed', color: '#F0F0F0' } },
      axisLine: { show: false }, axisTick: { show: false }
    },
    series: [{
      name: '交易额', type: 'line', data: amountData,
      smooth: true, symbol: 'circle', symbolSize: 8,
      lineStyle: { color: '#FF7675', width: 3 },
      itemStyle: { color: '#FF7675', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(255,118,117,0.25)' },
          { offset: 1, color: 'rgba(255,118,117,0.02)' }
        ])
      },
      label: {
        show: true, position: 'top', distance: 12,
        color: '#FF7675', fontSize: 11, fontWeight: 'bold',
        formatter: p => p.value > 0 ? '¥' + p.value + '万' : ''
      },
      markLine: {
        silent: true, symbol: 'none',
        lineStyle: { type: 'dashed', color: '#FAB1A0', width: 1 },
        label: { color: '#FAB1A0', fontSize: 10, formatter: `近${trendInterval.value}月均交易额 ¥{c}万` },
        data: [{ yAxis: +avgAmount }]
      },
      animationDuration: 1000, animationDelay: idx => idx * 80 + 300
    }]
  }, true)

  if (isNew) {
    revenueChart.hideLoading()
    revenueChart.on('click', p => { if (p.componentType === 'series') router.push({ path: '/bills', query: { month: p.name } }) })
    revenueObserver = new ResizeObserver(() => revenueChart?.resize())
    revenueObserver.observe(revenueChartRef.value)
  }
}

/* ===================== 饼图（不变） ===================== */

function renderPieChart(data) {
  if (!pieChartRef.value) return
  const isNew = !pieChart
  if (isNew) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.showLoading({ text: '加载中...', color: '#6C5CE7', maskColor: 'rgba(255,255,255,0.85)', textColor: '#6C5CE7' })
  }
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['45%', '75%'], center: ['50%', '55%'],
      data: data.map(d => ({ name: d.categoryName, value: d.totalAmount })),
      label: { formatter: '{b}\n{d}%' },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
    }],
    color: ['#6C5CE7','#00CEC9','#FF7675','#FDCB6E','#74B9FF','#A29BFE','#55EFC4','#FD79A8','#636E72','#E17055']
  }, true)
  if (isNew) {
    pieChart.hideLoading()
    pieChart.on('click', p => router.push({ path: '/bills', query: { category: p.name } }))
    pieObserver = new ResizeObserver(() => pieChart?.resize())
    pieObserver.observe(pieChartRef.value)
  }
}

function drillToUser(row) { router.push({ path: '/users', query: { username: row.username } }) }
</script>

<style scoped>
.dashboard { max-width: 1400px; margin: 0 auto; }
.quick-actions { display: flex; flex-wrap: wrap; gap: 12px; }
.action-item {
  width: calc(50% - 6px); height: 48px;
  border: 1px solid rgba(108,92,231,0.15);
  background: rgba(108,92,231,0.04);
  transition: all 0.3s;
}
.action-item:hover {
  background: linear-gradient(135deg, rgba(108,92,231,0.1), rgba(0,206,201,0.08));
  border-color: #6C5CE7; color: #6C5CE7;
}
</style>