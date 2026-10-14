<template>
  <el-card shadow="never">
    <!-- 筛选 -->
    <el-form :inline="true" :model="query">
      <el-form-item v-if="!isDriver" label="单证编号">
        <el-input v-model="query.eirNo" placeholder="单证编号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="s in APPT_STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预约日期">
        <el-date-picker v-model="query.apptDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openCreateDialog">创建预约</el-button>
        <el-button type="warning" :icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="eirNo" label="单证编号" width="165" fixed />
      <el-table-column label="业务类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(BUSINESS_TYPE, row.businessType)">
            {{ dictLabel(BUSINESS_TYPE, row.businessType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="containerNo" label="集装箱号" width="130" />
      <el-table-column prop="plateNo" label="车牌号" width="110" />
      <el-table-column v-if="!isDriver" prop="driverName" label="司机" width="100" />
      <el-table-column v-if="isAdmin" prop="companyName" label="所属公司" min-width="160" />
      <el-table-column prop="apptDate" label="预约日期" width="120" />
      <el-table-column prop="timeSlot" label="预约时段" width="110" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(APPT_STATUS, row.status)">
            {{ dictLabel(APPT_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'" link type="danger" @click="handleCancel(row)"
          >取消预约</el-button>
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

    <!-- 创建预约弹窗（共用组件，从可预约单证中选择） -->
    <appt-book-dialog v-model:visible="createVisible" :eir-options="bookableEirs" @success="loadData" />
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Download } from '@element-plus/icons-vue'
import { apptApi, eirApi } from '@/api'
import { useUserStore } from '@/store/user'
import { APPT_STATUS, BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
import { exportExcel } from '@/utils/download'
import ApptBookDialog from '@/components/ApptBookDialog.vue'

const userStore = useUserStore()
const isDriver = computed(() => userStore.role === 'DRIVER')
const isAdmin = computed(() => userStore.role === 'ADMIN')

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const createVisible = ref(false)
const bookableEirs = ref([])

const query = reactive({ page: 1, size: 10, eirNo: '', status: '', apptDate: '' })

async function handleExport() {
  exporting.value = true
  await exportExcel(apptApi.export, { status: query.status }, '预约记录导出.xlsx')
  exporting.value = false
}

async function loadData() {
  loading.value = true
  try {
    const data = await apptApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.eirNo = ''
  query.status = ''
  query.apptDate = ''
  query.page = 1
  loadData()
}

async function openCreateDialog() {
  // 查询可预约（已派单）单证；后端按角色自动过滤
  const data = await eirApi.page({ page: 1, size: 200, status: 'DISPATCHED' })
  bookableEirs.value = data.rows
  if (bookableEirs.value.length === 0) {
    ElMessage.info('暂无可预约的已派单单证')
    return
  }
  createVisible.value = true
}

async function handleCancel(row) {
  await ElMessageBox.confirm('确定取消该预约吗？取消后单证将回到已派单状态。', '提示', {
    type: 'warning'
  })
  await apptApi.cancel(row.id)
  ElMessage.success('预约已取消')
  loadData()
}

loadData()
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
