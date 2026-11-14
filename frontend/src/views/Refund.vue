<template>
  <el-card shadow="never">
    <!-- 筛选 -->
    <el-form :inline="true" :model="query">
      <el-form-item label="退款状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="s in REFUND_STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="所属公司">
        <el-select v-model="query.companyId" placeholder="全部公司" clearable style="width: 170px">
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="refundNo" label="退款编号" width="170">
        <template #default="{ row }">{{ row.refundNo || '—' }}</template>
      </el-table-column>
      <el-table-column prop="eirNo" label="单证编号" width="165" />
      <el-table-column v-if="isAdmin" prop="companyName" label="缴费公司" min-width="160" />
      <el-table-column label="退款金额(元)" width="120" align="right">
        <template #default="{ row }">
          <span class="amount">¥ {{ row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(REFUND_STATUS, row.status)">
            {{ dictLabel(REFUND_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicant" label="申请人" width="110" />
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.appliedAt) }}</template>
      </el-table-column>
      <el-table-column prop="approver" label="审批人" width="110">
        <template #default="{ row }">{{ row.approver || '—' }}</template>
      </el-table-column>
      <el-table-column label="审批时间" width="170">
        <template #default="{ row }">{{ formatTime(row.approvedAt) }}</template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="驳回原因" min-width="150">
        <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
      </el-table-column>
      <el-table-column v-if="isAdmin" label="操作" width="130" fixed="right" align="center">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />
  </el-card>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { refundApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'
import { REFUND_STATUS, dictLabel, dictType } from '@/utils/dict'
import { formatTime } from '@/utils/format'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const companies = ref([])

const { loading, tableData, total, query, loadData, resetQuery } = useCrudPage(refundApi, {
  defaultQuery: { status: null, companyId: null }
})

async function handleApprove(row) {
  await ElMessageBox.confirm(
    `确认通过单证 ${row.eirNo} 的退款 ¥${row.amount} 吗？通过后将同时作废该单证。`,
    '退款审批', { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' }
  )
  const outcome = await refundApi.approve(row.id)
  if (outcome === 'REJECTED_GATE_IN') {
    ElMessage.warning('单证已进闸，退款申请已自动驳回')
  } else {
    ElMessage.success('退款完成，单证已作废')
  }
  loadData()
}

async function handleReject(row) {
  const { value } = await ElMessageBox.prompt(
    `请填写单证 ${row.eirNo} 的驳回原因`, '驳回退款申请',
    {
      confirmButtonText: '确认驳回', cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => (v && v.trim() ? true : '驳回原因不能为空')
    }
  )
  await refundApi.reject(row.id, value.trim())
  ElMessage.success('已驳回退款申请')
  loadData()
}

async function loadCompanies() {
  if (!isAdmin.value) {
    return
  }
  companies.value = await companyApi.all()
}

loadCompanies()
loadData()
</script>

<style scoped>
.amount {
  font-weight: bold;
  color: #f56c6c;
}
</style>
