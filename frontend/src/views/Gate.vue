<template>
  <!-- 单根节点：<Transition> 无法动画 fragment 根（多个顶层节点），会告警并跳过动画 -->
  <div class="gate-page">
  <el-row :gutter="16">
    <!-- 左：办单操作 -->
    <el-col :md="9" :xs="24">
      <el-card shadow="never" class="gate-card op-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><CreditCard /></el-icon>
            <span class="card-title">闸口办单（模拟刷卡）</span>
          </div>
        </template>

        <el-input
          v-model="icCardNo"
          size="large"
          placeholder="请输入 IC 卡号"
          clearable
          :prefix-icon="CreditCard"
          @keyup.enter="handleGateOp('checkin')"
        />
        <div class="quick-cards">
          <span class="quick-label">快捷选择</span>
          <el-tag
            v-for="card in exampleCards"
            :key="card"
            :effect="icCardNo === card ? 'dark' : 'plain'"
            class="quick-tag"
            @click="icCardNo = card"
          >
            {{ card }}
          </el-tag>
        </div>

        <div class="action-btns">
          <el-button type="success" size="large" :icon="Download" :loading="loading" class="op-btn" @click="handleGateOp('checkin')">
            进闸办单
          </el-button>
          <el-button type="warning" size="large" :icon="Upload" :loading="loading" class="op-btn" @click="handleGateOp('checkout')">
            出闸办单
          </el-button>
        </div>

        <div class="rules">
          <div class="rules-title">
            <el-icon><InfoFilled /></el-icon>办单规则
          </div>
          <div class="rule-item"><span class="dot dot-in" />进闸：拖车正常 + 单证已预约 + 存在有效预约</div>
          <div class="rule-item"><span class="dot dot-out" />出闸：费用须已缴清，未缴清禁止放行</div>
        </div>
      </el-card>
    </el-col>

    <!-- 右：最近办单结果 -->
    <el-col :md="15" :xs="24">
      <el-card shadow="never" class="gate-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">最近办单结果（近 5 条）</span>
            <el-button text :icon="Refresh" @click="loadRecent" />
          </div>
        </template>

        <el-table :data="records" v-loading="loadingRecent" border stripe empty-text="暂无办单记录">
          <el-table-column label="操作" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="opMap[row.opType]?.type">{{ opMap[row.opType]?.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="eirNo" label="单证编号" min-width="150" />
          <el-table-column prop="containerNo" label="箱号" width="125" />
          <el-table-column label="业务类型" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="dictType(BUSINESS_TYPE, row.businessType)">
                {{ dictLabel(BUSINESS_TYPE, row.businessType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="plateNo" label="车牌" width="105">
            <template #default="{ row }">{{ row.plateNo || '—' }}</template>
          </el-table-column>
          <el-table-column label="办单时间" width="165">
            <template #default="{ row }">{{ formatTime(row.opTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>

  <!-- M2：凭证核验 + 核验留痕 -->
  <el-row :gutter="16">
    <el-col :md="9" :xs="24">
      <el-card shadow="never" class="gate-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Ticket /></el-icon>
            <span class="card-title">预约凭证核验</span>
          </div>
        </template>

        <el-form label-width="78px" @submit.prevent>
          <el-form-item label="凭证号">
            <el-input v-model="voucherNo" placeholder="APT20261115000001" clearable @keyup.enter="handleVerify" />
          </el-form-item>
          <el-form-item label="IC 卡号">
            <el-input v-model="verifyIcCard" placeholder="与凭证号二选一，或同时填" clearable @keyup.enter="handleVerify" />
          </el-form-item>
          <el-form-item label="车道号">
            <el-input v-model="laneCode" placeholder="选填（本轮仅留痕，M3 接车道配额）" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="verifying" @click="handleVerify">核验并放行判定</el-button>
          </el-form-item>
        </el-form>

        <el-alert
          v-if="verifyResult"
          :type="verifyResult.result === 'PASS' ? 'success' : 'error'"
          :closable="false"
          show-icon
          :title="verifyResult.result === 'PASS'
            ? '核验通过'
            : '核验失败' + (verifyResult.abnormalType ? '（' + abnormalLabel(verifyResult.abnormalType) + '）' : '')"
          :description="verifyResult.message"
        />
      </el-card>
    </el-col>

    <el-col :md="15" :xs="24">
      <el-card shadow="never" class="gate-card">
        <template #header>
          <div class="card-header">
            <span class="card-title">核验记录</span>
            <div class="log-tools">
              <el-select v-model="logQuery.result" placeholder="全部结果" clearable style="width: 120px" @change="loadCheckLogs">
                <el-option label="通过" value="PASS" />
                <el-option label="失败" value="FAIL" />
              </el-select>
              <el-button text :icon="Refresh" @click="loadCheckLogs" />
            </div>
          </div>
        </template>

        <el-table :data="checkLogs" v-loading="loadingLogs" border stripe empty-text="暂无核验记录">
          <el-table-column label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'">{{ row.result === 'PASS' ? '通过' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="异常类型" width="130" align="center">
            <template #default="{ row }">
              <span v-if="row.abnormalType">{{ abnormalLabel(row.abnormalType) }}</span>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="voucherNo" label="凭证号" width="170">
            <template #default="{ row }">{{ row.voucherNo || '—' }}</template>
          </el-table-column>
          <el-table-column prop="eirNo" label="单证编号" width="160">
            <template #default="{ row }">{{ row.eirNo || '—' }}</template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="100">
            <template #default="{ row }">{{ row.operatorName || '—' }}</template>
          </el-table-column>
          <el-table-column label="核验时间" width="165">
            <template #default="{ row }">{{ formatTime(row.checkTime) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150">
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="logQuery.page"
          v-model:page-size="logQuery.size"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          class="log-pager"
          @size-change="loadCheckLogs"
          @current-change="loadCheckLogs"
        />
      </el-card>
    </el-col>
  </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, CreditCard, Download, Upload, InfoFilled, Ticket } from '@element-plus/icons-vue'
import { gateApi } from '@/api'
import { BUSINESS_TYPE, ABNORMAL_TYPE, dictLabel, dictType } from '@/utils/dict'
import { formatTime } from '@/utils/format'

const icCardNo = ref('')
const loading = ref(false)
const loadingRecent = ref(false)
const records = ref([])

const opMap = {
  GATE_IN: { label: '进闸', type: 'success' },
  GATE_OUT: { label: '出闸', type: 'warning' },
  AUDIT: { label: '审单', type: 'primary' }
}

const exampleCards = ['IC0001', 'IC0002', 'IC0003']

/* ---------- M2：凭证核验 ---------- */
const voucherNo = ref('')
const verifyIcCard = ref('')
const laneCode = ref('')
const verifying = ref(false)
const verifyResult = ref(null)
const checkLogs = ref([])
const loadingLogs = ref(false)
const logTotal = ref(0)
const logQuery = ref({ page: 1, size: 10, result: '' })

const abnormalLabel = (value) => dictLabel(ABNORMAL_TYPE, value)

async function handleVerify() {
  const voucher = voucherNo.value.trim()
  const icCard = verifyIcCard.value.trim()
  if (!voucher && !icCard) {
    ElMessage.warning('凭证号与 IC 卡号至少填一个')
    return
  }
  verifying.value = true
  try {
    verifyResult.value = await gateApi.verify({
      voucherNo: voucher || null,
      icCard: icCard || null,
      laneCode: laneCode.value.trim() || null
    })
    if (verifyResult.value.result !== 'PASS') {
      ElMessage.warning(verifyResult.value.message || '核验失败')
    }
    loadCheckLogs()
  } finally {
    verifying.value = false
  }
}

async function loadCheckLogs() {
  loadingLogs.value = true
  try {
    const data = await gateApi.checkLogs(logQuery.value)
    checkLogs.value = data.rows
    logTotal.value = data.total
  } finally {
    loadingLogs.value = false
  }
}

async function loadRecent() {
  loadingRecent.value = true
  try {
    records.value = await gateApi.recentOps()
  } finally {
    loadingRecent.value = false
  }
}

// 进闸与出闸走同一套刷卡流程，只有调用的接口和成功文案不同
async function handleGateOp(action) {
  const cardNo = icCardNo.value.trim()
  if (!cardNo) {
    ElMessage.warning('请输入或选择 IC 卡号')
    return
  }

  loading.value = true
  try {
    if (action === 'checkin') {
      await gateApi.checkin(cardNo)
      ElMessage.success('进闸办单成功')
    } else {
      await gateApi.checkout(cardNo)
      ElMessage.success('出闸办单成功')
    }
    loadRecent()
  } catch (e) {
    // 业务错误（如费用未缴清）由后端返回具体原因，优先展示它
    ElMessage.error(e.response?.data?.msg || e.message || '办单失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRecent()
  loadCheckLogs()
})
</script>

<style scoped>
.gate-card {
  height: 100%;
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.op-card .card-header {
  justify-content: flex-start;
  gap: 6px;
}

.header-icon {
  color: #409eff;
  font-size: 18px;
}

.op-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.quick-cards {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-label {
  font-size: 13px;
  color: #909399;
}

.quick-tag {
  cursor: pointer;
}

.action-btns {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.op-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-left: 0;
}

.rules {
  background: #f4f7fb;
  border-radius: 6px;
  padding: 12px 14px;
}

.rules-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

.rule-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
  line-height: 2;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-in {
  background: #67c23a;
}

.dot-out {
  background: #e6a23c;
}

.log-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
