<template>
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
          @keyup.enter="doCheckin"
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
          <el-button type="success" size="large" :icon="Download" :loading="loading" class="op-btn" @click="doCheckin">
            进闸办单
          </el-button>
          <el-button type="warning" size="large" :icon="Upload" :loading="loading" class="op-btn" @click="doCheckout">
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, CreditCard, Download, Upload, InfoFilled } from '@element-plus/icons-vue'
import { gateApi } from '@/api'
import { BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
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

async function loadRecent() {
  loadingRecent.value = true
  try {
    records.value = await gateApi.recentOps()
  } finally {
    loadingRecent.value = false
  }
}

async function handle(action) {
  if (!icCardNo.value.trim()) {
    ElMessage.warning('请输入或选择 IC 卡号')
    return
  }
  loading.value = true
  try {
    if (action === 'checkin') {
      await gateApi.checkin(icCardNo.value.trim())
      ElMessage.success('进闸办单成功')
    } else {
      await gateApi.checkout(icCardNo.value.trim())
      ElMessage.success('出闸办单成功')
    }
    loadRecent()
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || e.message || '办单失败')
  } finally {
    loading.value = false
  }
}

function doCheckin() {
  handle('checkin')
}

function doCheckout() {
  handle('checkout')
}

onMounted(loadRecent)
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
</style>
