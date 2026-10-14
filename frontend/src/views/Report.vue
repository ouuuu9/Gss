<template>
  <el-card shadow="never">
    <template #header>
      <div class="header-bar">
        <span>
          <el-icon><TrendCharts /></el-icon>
          收费汇总报表（按运输公司）
        </span>
        <el-space>
          <el-select
            v-if="isAdmin" v-model="companyId" placeholder="全部公司" clearable
            style="width: 180px" @change="loadData"
          >
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </el-space>
      </div>
    </template>

    <!-- 汇总指标 -->
    <el-row :gutter="16" class="sum-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="sum-card">
          <div class="sum-value">{{ summary.totalCount }}</div>
          <div class="sum-label">收费总笔数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="sum-card">
          <div class="sum-value green">{{ summary.paidCount }}</div>
          <div class="sum-label">已缴费笔数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="sum-card">
          <div class="sum-value red">{{ summary.unpaidCount }}</div>
          <div class="sum-label">未缴费笔数</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="sum-card">
          <div class="sum-value blue">¥ {{ summary.paidAmount }}</div>
          <div class="sum-label">已收总金额</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 明细报表 -->
    <el-table :data="reportData" v-loading="loading" border stripe show-summary :summary-method="getSummary">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="companyCode" label="公司编码" width="120" />
      <el-table-column prop="companyName" label="运输公司" min-width="180" />
      <el-table-column prop="totalCount" label="收费笔数" width="100" align="center" />
      <el-table-column prop="paidCount" label="已缴笔数" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="success" size="small">{{ row.paidCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unpaidCount" label="未缴笔数" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.unpaidCount > 0" type="danger" size="small">{{ row.unpaidCount }}</el-tag>
          <span v-else>0</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="应收金额(元)" width="130" align="right" />
      <el-table-column prop="paidAmount" label="已收金额(元)" width="130" align="right">
        <template #default="{ row }">
          <span class="paid">¥ {{ row.paidAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收缴率" width="180">
        <template #default="{ row }">
          <el-progress :percentage="rate(row)" :stroke-width="14" />
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { TrendCharts, Refresh } from '@element-plus/icons-vue'
import { feeApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const loading = ref(false)
const reportData = ref([])
const companies = ref([])
const companyId = ref(null)

const summary = reactive({
  totalCount: 0,
  paidCount: 0,
  unpaidCount: 0,
  paidAmount: '0.00'
})

function rate(row) {
  if (!row.totalCount) return 0
  return Math.round((row.paidCount / row.totalCount) * 100)
}

async function loadData() {
  loading.value = true
  try {
    const data = await feeApi.report(isAdmin.value ? { companyId: companyId.value } : {})
    reportData.value = data || []

    // 汇总计算
    summary.totalCount = reportData.value.reduce((s, r) => s + Number(r.totalCount || 0), 0)
    summary.paidCount = reportData.value.reduce((s, r) => s + Number(r.paidCount || 0), 0)
    summary.unpaidCount = reportData.value.reduce((s, r) => s + Number(r.unpaidCount || 0), 0)
    summary.paidAmount = reportData.value
      .reduce((s, r) => s + Number(r.paidAmount || 0), 0)
      .toFixed(2)
  } finally {
    loading.value = false
  }
}

// 合计行
function getSummary({ columns, data }) {
  return columns.map((col, index) => {
    if (index === 0 || index === 1 || index === 2) {
      return index === 2 ? '合计' : ''
    }
    const prop = col.property
    if (!prop) return ''
    if (['totalCount', 'paidCount', 'unpaidCount'].includes(prop)) {
      return data.reduce((s, r) => s + Number(r[prop] || 0), 0)
    }
    if (prop === 'totalAmount' || prop === 'paidAmount') {
      return data.reduce((s, r) => s + Number(r[prop] || 0), 0).toFixed(2)
    }
    return ''
  })
}

async function loadCompanies() {
  if (isAdmin.value) {
    companies.value = await companyApi.all()
  }
}

loadCompanies()
loadData()
</script>

<style scoped>
.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sum-row {
  margin-bottom: 18px;
}

.sum-card {
  text-align: center;
}

.sum-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.sum-value.green {
  color: #67c23a;
}

.sum-value.red {
  color: #f56c6c;
}

.sum-value.blue {
  color: #409eff;
}

.sum-label {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.paid {
  color: #67c23a;
  font-weight: bold;
}
</style>
