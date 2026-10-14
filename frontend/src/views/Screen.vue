<template>
  <div ref="containerRef" class="screen-container">
    <!-- 标题栏 -->
    <div class="screen-header">
      <h1>集装箱码头闸口数据可视化大屏</h1>
      <div class="header-right">
        <div class="ws-status" :class="{ online: wsConnected }">
          <span class="ws-dot"></span>{{ wsConnected ? '实时推送中' : '连接已断开' }}
        </div>
        <div class="header-time">{{ currentTime }}</div>
        <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏显示'" placement="left">
          <el-button class="fullscreen-btn" circle text @click="toggleFullscreen">
            <el-icon :size="20"><Aim v-if="isFullscreen" /><FullScreen v-else /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 顶部指标卡片 -->
    <div class="stat-cards">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon" :style="{ background: card.color }">
          <el-icon :size="28"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表网格 2x2 -->
    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">单证状态分布</div>
        <div ref="pieRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">最近7天进出闸趋势</div>
        <div ref="lineRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">业务类型分布</div>
        <div ref="barRef" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">收费统计</div>
        <div ref="gaugeRef" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { FullScreen, Aim } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api'

// refs for chart containers
const pieRef = ref(null)
const lineRef = ref(null)
const barRef = ref(null)
const gaugeRef = ref(null)

let pieChart, lineChart, barChart, gaugeChart
const screenData = ref({})

const currentTime = ref('')
let timeTimer = null

/* ==================== 全屏控制 ==================== */
const containerRef = ref(null)
const isFullscreen = ref(false)

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      // 全屏容器本身，脱离后台布局（侧边栏/顶栏），铺满整个视口
      await containerRef.value?.requestFullscreen()
    }
  } catch (e) {
    // 浏览器拒绝全屏请求时静默忽略
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  // 全屏切换后容器尺寸变化，需重排图表
  nextTick(() => resizeCharts())
  setTimeout(resizeCharts, 120)
}

/* ==================== WebSocket 实时推送 ==================== */
const wsConnected = ref(false)
let ws = null
let wsReconnectTimer = null
let destroyed = false

function connectWebSocket() {
  const token = localStorage.getItem('gss_token')
  if (!token) return
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  try {
    ws = new WebSocket(`${proto}://${location.host}/ws/screen?token=${encodeURIComponent(token)}`)
  } catch (e) {
    scheduleReconnect()
    return
  }
  ws.onopen = () => { wsConnected.value = true }
  ws.onmessage = (event) => {
    try {
      // 收到推送的最新统计数据，直接刷新卡片与图表
      screenData.value = JSON.parse(event.data)
      initCharts()
    } catch (e) {
      // 忽略无法解析的消息
    }
  }
  ws.onclose = () => {
    wsConnected.value = false
    if (!destroyed) scheduleReconnect()
  }
  ws.onerror = () => { ws?.close() }
}

function scheduleReconnect() {
  if (destroyed || wsReconnectTimer) return
  // 断线 5 秒后自动重连
  wsReconnectTimer = setTimeout(() => {
    wsReconnectTimer = null
    connectWebSocket()
  }, 5000)
}

const statusLabels = {
  CREATED: '已录入',
  DISPATCHED: '已派单',
  APPOINTED: '已预约',
  GATE_IN: '已进闸',
  GATE_OUT: '已出闸',
  DONE: '已完成',
  CANCELLED: '已作废'
}
const bizLabels = { 'PI-F': '提重', 'PI-E': '提空', 'GR-E': '还空', 'GR-F': '还重' }

// 顶部指标卡片
const statCards = computed(() => [
  {
    label: '今日进闸量',
    value: screenData.value.todayGateIn ?? 0,
    icon: 'Download',
    color: 'linear-gradient(135deg, #36d1dc, #5b86e5)'
  },
  {
    label: '今日出闸量',
    value: screenData.value.todayGateOut ?? 0,
    icon: 'Upload',
    color: 'linear-gradient(135deg, #f7971e, #ffd200)'
  },
  {
    label: '已收金额(元)',
    value: screenData.value.paidAmount ?? 0,
    icon: 'Coin',
    color: 'linear-gradient(135deg, #11998e, #38ef7d)'
  },
  {
    label: '未收金额(元)',
    value: screenData.value.unpaidAmount ?? 0,
    icon: 'Wallet',
    color: 'linear-gradient(135deg, #eb3349, #f4576c)'
  }
])

function updateTime() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  currentTime.value =
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(async () => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  try {
    screenData.value = await dashboardApi.screen()
  } catch (e) {
    screenData.value = {}
  }
  await nextTick()
  initCharts()
  connectWebSocket()
  window.addEventListener('resize', resizeCharts)
  document.addEventListener('fullscreenchange', onFullscreenChange)
})

onBeforeUnmount(() => {
  destroyed = true
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  ws?.close()
  if (timeTimer) clearInterval(timeTimer)
  window.removeEventListener('resize', resizeCharts)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  pieChart?.dispose()
  lineChart?.dispose()
  barChart?.dispose()
  gaugeChart?.dispose()
})

function initCharts() {
  initPieChart()
  initLineChart()
  initBarChart()
  initGaugeChart()
}

function resizeCharts() {
  pieChart?.resize()
  lineChart?.resize()
  barChart?.resize()
  gaugeChart?.resize()
}

// 单证状态分布 - 玫瑰图
function initPieChart() {
  if (!pieRef.value) return
  // 已初始化则复用实例，setOption 合并更新数据（WS 推送时重复调用）
  if (!pieChart) pieChart = echarts.init(pieRef.value, 'dark')
  const statusCountMap = screenData.value.statusCount || {}
  const data = Object.keys(statusLabels).map((key) => ({
    name: statusLabels[key],
    value: statusCountMap[key] ?? 0
  }))
  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      textStyle: { color: '#c5d2e3' },
      type: 'scroll'
    },
    series: [
      {
        name: '单证状态',
        type: 'pie',
        radius: ['20%', '70%'],
        center: ['50%', '45%'],
        roseType: 'area',
        itemStyle: { borderRadius: 6, borderColor: '#0c1e3e', borderWidth: 2 },
        label: { color: '#c5d2e3' },
        labelLine: { lineStyle: { color: '#5b7a99' } },
        data
      }
    ],
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452']
  })
}

// 最近7天进出闸趋势 - 折线图
function initLineChart() {
  if (!lineRef.value) return
  if (!lineChart) lineChart = echarts.init(lineRef.value, 'dark')
  const trend = screenData.value.dailyTrend || []
  lineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: {
      top: 0,
      textStyle: { color: '#c5d2e3' },
      data: ['进闸量', '出闸量']
    },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#5b7a99' } },
      axisLabel: { color: '#c5d2e3' },
      data: trend.map((d) => d.date)
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#5b7a99' } },
      axisLabel: { color: '#c5d2e3' },
      splitLine: { lineStyle: { color: 'rgba(91,122,153,0.2)' } }
    },
    series: [
      {
        name: '进闸量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#36d1dc' },
        lineStyle: { width: 3, color: '#36d1dc' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(54,209,220,0.5)' },
            { offset: 1, color: 'rgba(54,209,220,0.05)' }
          ])
        },
        data: trend.map((d) => d.gateIn ?? 0)
      },
      {
        name: '出闸量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#f7971e' },
        lineStyle: { width: 3, color: '#f7971e' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(247,151,30,0.5)' },
            { offset: 1, color: 'rgba(247,151,30,0.05)' }
          ])
        },
        data: trend.map((d) => d.gateOut ?? 0)
      }
    ]
  })
}

// 业务类型分布 - 柱状图
function initBarChart() {
  if (!barRef.value) return
  if (!barChart) barChart = echarts.init(barRef.value, 'dark')
  const bizCount = screenData.value.bizTypeCount || {}
  const keys = Object.keys(bizLabels)
  barChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      axisLine: { lineStyle: { color: '#5b7a99' } },
      axisLabel: { color: '#c5d2e3' },
      data: keys.map((k) => bizLabels[k])
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#5b7a99' } },
      axisLabel: { color: '#c5d2e3' },
      splitLine: { lineStyle: { color: 'rgba(91,122,153,0.2)' } }
    },
    series: [
      {
        name: '单证数量',
        type: 'bar',
        barWidth: '45%',
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5b86e5' },
            { offset: 1, color: '#36d1dc' }
          ])
        },
        label: { show: true, position: 'top', color: '#c5d2e3' },
        data: keys.map((k) => bizCount[k] ?? 0)
      }
    ]
  })
}

// 收费统计 - 环形图
function initGaugeChart() {
  if (!gaugeRef.value) return
  if (!gaugeChart) gaugeChart = echarts.init(gaugeRef.value, 'dark')
  const paid = Number(screenData.value.paidAmount ?? 0)
  const unpaid = Number(screenData.value.unpaidAmount ?? 0)
  const total = paid + unpaid
  gaugeChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c} 元 ({d}%)' },
    legend: {
      bottom: 0,
      textStyle: { color: '#c5d2e3' },
      data: ['已收', '未收']
    },
    series: [
      {
        name: '收费统计',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#0c1e3e', borderWidth: 2 },
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            const pct = total === 0 ? 0 : Math.round((paid / total) * 100)
            return `{a|收率 ${pct}%}\n{b|总计 ${total} 元}`
          },
          rich: {
            a: { fontSize: 22, color: '#36d1dc', fontWeight: 'bold', lineHeight: 32 },
            b: { fontSize: 13, color: '#c5d2e3' }
          }
        },
        data: [
          { name: '已收', value: paid, itemStyle: { color: '#11998e' } },
          { name: '未收', value: unpaid, itemStyle: { color: '#eb3349' } }
        ]
      }
    ]
  })
}
</script>

<style scoped>
.screen-container {
  width: 100%;
  min-height: calc(100vh - 92px);
  background: linear-gradient(135deg, #0c1e3e, #1a3a5c);
  color: #c5d2e3;
  padding: 16px 20px 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.screen-header {
  position: relative;
  text-align: center;
  padding: 10px 0 18px;
  border-bottom: 1px solid rgba(91, 122, 153, 0.3);
  margin-bottom: 16px;
}

.screen-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: bold;
  letter-spacing: 4px;
  background: linear-gradient(90deg, #36d1dc, #5b86e5, #36d1dc);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 0 20px rgba(54, 209, 220, 0.3);
}

.header-right {
  position: absolute;
  right: 0;
  top: 6px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-time {
  font-size: 14px;
  color: #8aa9c9;
  font-family: 'Courier New', monospace;
}

.fullscreen-btn {
  color: #8aa9c9;
}

.fullscreen-btn:hover {
  color: #36d1dc;
}

/* WebSocket 连接状态指示 */
.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8aa9c9;
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #eb3349;
}

.ws-status.online .ws-dot {
  background: #38ef7d;
  box-shadow: 0 0 6px rgba(56, 239, 125, 0.8);
  animation: ws-blink 2s ease-in-out infinite;
}

@keyframes ws-blink {
  50% { opacity: 0.4; }
}

/* 全屏时容器铺满整个视口，内容超出可滚动 */
.screen-container:fullscreen {
  height: 100vh;
  min-height: 100vh;
  overflow: auto;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: rgba(26, 58, 92, 0.5);
  border: 1px solid rgba(91, 122, 153, 0.3);
  border-radius: 8px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #fff;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #8aa9c9;
  margin-top: 4px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.chart-box {
  background: rgba(12, 30, 62, 0.6);
  border: 1px solid rgba(91, 122, 153, 0.25);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  min-height: 280px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.chart-title {
  font-size: 15px;
  font-weight: bold;
  color: #c5d2e3;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(91, 122, 153, 0.2);
  margin-bottom: 8px;
}

.chart {
  flex: 1;
  width: 100%;
  min-height: 220px;
}

@media (max-width: 992px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-grid {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }
}
</style>
