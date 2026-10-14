<template>
  <el-card shadow="never">
    <!-- 筛选 -->
    <el-form :inline="true" :model="query">
      <el-form-item label="单证编号">
        <el-input v-model="query.eirNo" placeholder="单证编号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="缴费状态">
        <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 120px">
          <el-option label="未缴" :value="0" />
          <el-option label="已缴" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="所属公司">
        <el-select v-model="query.companyId" placeholder="全部公司" clearable style="width: 160px">
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button
          type="warning" :icon="Coin" :disabled="!isCompany || selectedIds.length === 0"
          @click="handleBatchPay"
        >批量缴费（{{ selectedIds.length }}）</el-button>
        <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table
      :data="tableData" v-loading="loading" border stripe
      @selection-change="onSelectionChange"
    >
      <el-table-column
        v-if="isCompany" type="selection" width="48" :selectable="canSelect"
      />
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="eirNo" label="单证编号" width="165" fixed />
      <el-table-column label="业务类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="dictType(BUSINESS_TYPE, row.businessType)">
            {{ dictLabel(BUSINESS_TYPE, row.businessType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="plateNo" label="车牌号" width="110" />
      <el-table-column v-if="isAdmin" prop="companyName" label="缴费公司" min-width="170" />
      <el-table-column prop="amount" label="应收金额(元)" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">¥ {{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="缴费状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.payStatus === 1 ? 'success' : 'danger'">
            {{ row.payStatus === 1 ? '已缴费' : '未缴费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="payTime" label="缴费时间" width="170">
        <template #default="{ row }">{{ formatTime(row.payTime) }}</template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="110">
        <template #default="{ row }">{{ row.operator || '—' }}</template>
      </el-table-column>
      <el-table-column v-if="isCompany" label="操作" width="110" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.payStatus === 0"
            link type="warning" @click="handlePay(row)"
          >立即缴费</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="pager"
      @size-change="loadData"
      @current-change="loadData"
    />
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Coin, Download } from '@element-plus/icons-vue'
import { feeApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'
import { BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
import { exportExcel } from '@/utils/download'
import { formatTime } from '@/utils/format'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isCompany = computed(() => userStore.role === 'COMPANY')

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const companies = ref([])
const selectedRows = ref([])

const query = reactive({
  page: 1, size: 10, eirNo: '', payStatus: null, companyId: null
})

const selectedIds = computed(() => selectedRows.value.map((r) => r.id))

// 只有未缴费记录允许勾选
function canSelect(row) {
  return row.payStatus === 0
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const data = await feeApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.eirNo = ''
  query.payStatus = null
  query.companyId = null
  query.page = 1
  loadData()
}

async function handlePay(row) {
  await ElMessageBox.confirm(`确认为单证 ${row.eirNo} 缴纳 ¥${row.amount} 吗？`, '缴费确认', {
    confirmButtonText: '确认缴费', cancelButtonText: '取消', type: 'warning'
  })
  await feeApi.pay(row.id)
  ElMessage.success('缴费成功')
  loadData()
}

async function handleBatchPay() {
  const ids = selectedIds.value
  const totalAmount = selectedRows.value.reduce((sum, r) => sum + Number(r.amount), 0)
  await ElMessageBox.confirm(
    `本次共缴纳 ${ids.length} 笔，合计 ¥${totalAmount.toFixed(2)}，确认缴费吗？`,
    '批量缴费确认', { confirmButtonText: '确认缴费', cancelButtonText: '取消', type: 'warning' }
  )
  const data = await feeApi.batchPay(ids)
  ElMessage.success(`批量缴费成功，共 ${data.count} 笔`)
  loadData()
}

async function handleExport() {
  exporting.value = true
  await exportExcel(feeApi.export, { payStatus: query.payStatus, companyId: query.companyId }, '收费记录导出.xlsx')
  exporting.value = false
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
.amount {
  font-weight: bold;
  color: #f56c6c;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
