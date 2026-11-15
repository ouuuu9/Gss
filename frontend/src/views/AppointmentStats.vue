<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="日期区间">
          <el-date-picker
            v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
            range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
            style="width: 260px" @change="onDateChange"
          />
        </el-form-item>
        <el-form-item label="车道">
          <el-select v-model="query.laneId" placeholder="全部车道" clearable style="width: 170px">
            <el-option v-for="l in lanes" :key="l.id" :label="`${l.gateNo} / ${l.laneNo}`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="query.timeSlot" placeholder="全部时段" clearable style="width: 150px">
            <el-option v-for="s in SLOTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="客户">
          <el-select v-model="query.companyId" placeholder="全部客户" clearable style="width: 170px">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="query.groupBy" placeholder="不分组" clearable style="width: 130px">
            <el-option label="按车道" value="LANE" />
            <el-option label="按时段" value="SLOT" />
            <el-option label="按状态" value="STATUS" />
            <el-option label="按客户" value="COMPANY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button type="warning" :icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片 -->
    <el-row :gutter="12" class="summary-row">
      <el-col v-for="item in summaryCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="sum-card">
          <div class="sum-value">{{ item.value }}</div>
          <div class="sum-label">{{ item.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      type="info" :closable="false" show-icon class="rule-tip"
      title="口径：失约率 = 失约数 /(预约总数−取消数)；准点率 = 准时核验数 / 核验通过数（verify_time 不晚于时段起点）；核验通过数已排除「重复核验」记录。"
    />

    <el-card v-if="rows.length > 0" shadow="never" class="table-card">
      <el-table :data="rows" border stripe>
        <el-table-column prop="groupKey" label="分组" min-width="160" />
        <el-table-column prop="total" label="预约总数" width="100" align="center" />
        <el-table-column prop="cancelled" label="取消" width="80" align="center" />
        <el-table-column prop="noShow" label="失约" width="80" align="center" />
        <el-table-column prop="released" label="放行" width="80" align="center" />
        <el-table-column prop="passCount" label="核验通过" width="100" align="center" />
        <el-table-column prop="failCount" label="核验失败" width="100" align="center" />
        <el-table-column prop="onTimeCount" label="准时核验" width="100" align="center" />
        <el-table-column label="失约率" width="90" align="center">
          <template #default="{ row }">{{ row.noShowRate }}%</template>
        </el-table-column>
        <el-table-column label="准点率" width="90" align="center">
          <template #default="{ row }">{{ row.punctualRate }}%</template>
        </el-table-column>
        <el-table-column label="放行率" width="90" align="center">
          <template #default="{ row }">{{ row.releaseRate }}%</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { statsApi, laneApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'
import { exportExcel } from '@/utils/download'

const SLOTS = ['06:00-08:00', '08:00-10:00', '10:00-12:00', '12:00-14:00',
  '14:00-16:00', '16:00-18:00', '18:00-20:00', '20:00-22:00']

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const today = new Date()
const weekAgo = new Date(today.getTime() - 6 * 24 * 3600 * 1000)

const dateRange = ref([fmt(weekAgo), fmt(today)])
const query = reactive({
  startDate: fmt(weekAgo), endDate: fmt(today),
  laneId: null, timeSlot: null, companyId: null, groupBy: null
})
const summary = ref({})
const rows = ref([])
const lanes = ref([])
const companies = ref([])
const exporting = ref(false)

const summaryCards = computed(() => [
  { label: '预约总数', value: summary.value.total ?? 0 },
  { label: '取消', value: summary.value.cancelled ?? 0 },
  { label: '失约', value: summary.value.noShow ?? 0 },
  { label: '放行', value: summary.value.released ?? 0 },
  { label: '核验通过', value: summary.value.passCount ?? 0 },
  { label: '失约率', value: (summary.value.noShowRate ?? 0) + '%' },
  { label: '准点率', value: (summary.value.punctualRate ?? 0) + '%' },
  { label: '放行率', value: (summary.value.releaseRate ?? 0) + '%' }
])

function onDateChange(range) {
  query.startDate = range?.[0] || null
  query.endDate = range?.[1] || null
}

async function loadData() {
  if (!query.startDate || !query.endDate) {
    ElMessage.warning('请选择日期区间')
    return
  }
  const data = await statsApi.appointment(query)
  summary.value = data.summary || {}
  rows.value = data.rows || []
}

async function handleExport() {
  exporting.value = true
  try {
    await exportExcel(statsApi.export, { ...query }, '预约执行统计.xlsx')
  } finally {
    exporting.value = false
  }
}

async function loadBase() {
  lanes.value = await laneApi.list().catch(() => [])
  if (isAdmin.value) {
    companies.value = await companyApi.all().catch(() => [])
  }
}

onMounted(() => {
  loadBase()
  loadData()
})
</script>

<style scoped>
.summary-row {
  margin: 16px 0;
}

.sum-card {
  text-align: center;
}

.sum-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.sum-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.rule-tip {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}
</style>