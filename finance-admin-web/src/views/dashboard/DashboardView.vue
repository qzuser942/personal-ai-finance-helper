<!--
  后台首页数据大屏仪表盘
  @author 胡宪棋 软件2413 202421332084
-->
<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="page-header glass-card" style="padding:20px 24px">
      <el-row :gutter="16" align="middle">
        <el-col :span="16">
          <h2 class="page-title" style="margin:0">📊 运营数据看板</h2>
          <p style="color:#999;margin:8px 0 0">实时监控全平台用户及账单数据</p>
        </el-col>
        <el-col :span="8" style="text-align:right">
          <el-tag type="info" size="large" effect="light">
            {{ currentMonth }} 月数据
          </el-tag>
        </el-col>
      </el-row>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="6" v-for="item in statCards" :key="item.label">
        <div class="stat-card">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
          <div style="display:flex;align-items:center;gap:6px;margin-top:8px">
            <el-icon :color="item.trend > 0 ? '#00B894' : '#FF7675'" :size="16">
              <CaretTop v-if="item.trend>0" /><CaretBottom v-else />
            </el-icon>
            <span :style="{color: item.trend>0?'#00B894':'#FF7675',fontSize:'12px'}">
              {{ item.trendText }}
            </span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="16">
        <el-card class="glass-card" shadow="never">
          <template #header><span>📈 近6个月平台数据趋势</span></template>
          <div ref="trendChartRef" style="width:100%;height:350px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="glass-card" shadow="never" style="height:100%">
          <template #header><span>⚡ 快捷功能入口</span></template>
          <div class="quick-actions">
            <el-button class="action-item" @click="$router.push('/users')" round>
              <el-icon><User /></el-icon> 用户管理
            </el-button>
            <el-button class="action-item" @click="$router.push('/bills')" round>
              <el-icon><List /></el-icon> 账单管理
            </el-button>
            <el-button class="action-item" @click="$router.push('/categories')" round>
              <el-icon><Grid /></el-icon> 分类管理
            </el-button>
            <el-button class="action-item" @click="$router.push('/ai-config')" round>
              <el-icon><Cpu /></el-icon> AI配置
            </el-button>
            <el-button class="action-item" @click="$router.push('/logs')" round>
              <el-icon><Document /></el-icon> 操作日志
            </el-button>
            <el-button class="action-item" @click="$router.push('/admins')" round v-if="isSuperAdmin">
              <el-icon><Setting /></el-icon> 管理员
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 消费统计排行 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="12">
        <el-card class="glass-card" shadow="never">
          <template #header><span>🏆 用户消费排行TOP10</span></template>
          <el-table :data="userRanking" stripe size="small" max-height="340">
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="username" label="用户" />
            <el-table-column prop="totalAmount" label="消费额">
              <template #default="{row}">{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="billCount" label="账单数" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="glass-card" shadow="never">
          <template #header><span>📊 分类消费分布</span></template>
          <div ref="pieChartRef" style="width:100%;height:340px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
/**
 * 后台总看板
 * @author 胡宪棋 软件2413 202421332084
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { getDashboard } from '@/api/dashboard'
import { getBillStats } from '@/api/bill'
import { formatMoney, getCurrentMonth } from '@/utils/date'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'

const isSuperAdmin = computed(() => useUserStore().isSuperAdmin)
const currentMonth = getCurrentMonth()
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const userRanking = ref([])
const loading = ref(false)

const statCards = ref([
  { label: '平台用户总数', value: '0', trend: 12, trendText: '较上月增长12%' },
  { label: '当月活跃用户', value: '0', trend: 8, trendText: '较上月增长8%' },
  { label: '当月账单总量', value: '0', trend: 15, trendText: '较上月增长15%' },
  { label: '当月交易总额', value: '¥0', trend: -3, trendText: '较上月减少3%' }
])

onMounted(async () => {
  loading.value = true
  try {
    const [dashRes, statRes] = await Promise.all([
      getDashboard(), getBillStats(currentMonth)
    ])
    if (dashRes.code === 200 && dashRes.data) {
      const d = dashRes.data
      statCards.value[0].value = d.totalUsers || 0
      statCards.value[1].value = d.activeUsers || 0
      statCards.value[2].value = d.currentMonthBillCount || 0
      statCards.value[3].value = formatMoney(d.currentMonthTotalAmount)

      await nextTick()
      if (d.recent6MonthTrend?.length) renderTrendChart(d.recent6MonthTrend)
    }
    if (statRes.code === 200 && statRes.data) {
      userRanking.value = statRes.data.userRanking || []
      await nextTick()
      if (statRes.data.categoryDistribution?.length) renderPieChart(statRes.data.categoryDistribution)
    }
  } catch (e) { /* ignore */ }
  loading.value = false
})

function renderTrendChart(data) {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['账单数', '活跃用户', '交易额(万)'] },
    grid: { left: 60, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: data.map(d => d.yearMonth), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [
      { name: '账单数', type: 'bar', data: data.map(d => d.billCount), itemStyle: { borderRadius: [8,8,0,0], color: '#6C5CE7' }, barWidth: 16 },
      { name: '活跃用户', type: 'bar', data: data.map(d => d.userCount), itemStyle: { borderRadius: [8,8,0,0], color: '#00CEC9' }, barWidth: 16 },
      { name: '交易额(万)', type: 'line', data: data.map(d => (d.totalAmount / 10000).toFixed(1)), lineStyle: { color: '#FF7675', width: 3 }, itemStyle: { color: '#FF7675' }, smooth: true }
    ]
  })
}

function renderPieChart(data) {
  if (!pieChartRef.value) return
  const chart = echarts.init(pieChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '75%'],
      center: ['50%', '55%'],
      data: data.map(d => ({ name: d.categoryName, value: d.totalAmount })),
      label: { formatter: '{b}\n{d}%' },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
    }],
    color: ['#6C5CE7','#00CEC9','#FF7675','#FDCB6E','#74B9FF','#A29BFE','#55EFC4','#FD79A8','#636E72','#E17055']
  })
}
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
  margin: 0 auto;
}
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.action-item {
  width: calc(50% - 6px);
  height: 48px;
  border: 1px solid rgba(108,92,231,0.15);
  background: rgba(108,92,231,0.04);
  transition: all 0.3s;
}
.action-item:hover {
  background: linear-gradient(135deg, rgba(108,92,231,0.1), rgba(0,206,201,0.08));
  border-color: #6C5CE7;
  color: #6C5CE7;
}
</style>
