<template>
  <el-card shadow="never">
    <el-form :inline="true">
      <el-form-item label="日期">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" @change="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button :icon="RefreshLeft" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存本页容量</el-button>
      </el-form-item>
      <el-form-item>
        <span class="tip">绿色=有余量，红色=已约满；修改后点「保存」按格子批量提交，整批成功或整批回滚。</span>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border stripe empty-text="当天没有启用中的车道">
      <el-table-column prop="gateNo" label="闸口号" width="110" fixed />
      <el-table-column prop="laneNo" label="车道号" width="110" fixed />
      <el-table-column v-for="slot in slots" :key="slot" :label="slot" min-width="150" align="center">
        <template #default="{ row }">
          <div class="cell">
            <el-input-number
              v-model="edits[cellKey(row.laneId, slot)]"
              :min="0" :max="999" size="small" controls-position="right"
            />
            <div class="cell-meta">
              已占 {{ bookedCount(row.laneId, slot) }} /
              剩余 <span :class="remainOf(row.laneId, slot) > 0 ? 'ok' : 'full'">{{ remainOf(row.laneId, slot) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshLeft, Check } from '@element-plus/icons-vue'
import { laneApi } from '@/api'

const SLOTS = ['06:00-08:00', '08:00-10:00', '10:00-12:00', '12:00-14:00',
  '14:00-16:00', '16:00-18:00', '18:00-20:00', '20:00-22:00']

const today = new Date()
const pad = (n) => String(n).padStart(2, '0')
const date = ref(`${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`)
const slots = SLOTS
const cells = ref([])
const rows = ref([])
const edits = reactive({})
const loading = ref(false)
const saving = ref(false)

const cellKey = (laneId, slot) => `${laneId}::${slot}`

function current(laneId, slot) {
  return cells.value.find((c) => c.laneId === laneId && c.timeSlot === slot)
}

const bookedCount = (laneId, slot) => current(laneId, slot)?.bookedCount ?? 0
const remainOf = (laneId, slot) => {
  const c = current(laneId, slot)
  const cap = edits[cellKey(laneId, slot)] ?? c?.capacity ?? 0
  return Math.max(cap - (c?.bookedCount ?? 0), 0)
}

async function loadData() {
  if (!date.value) return
  loading.value = true
  try {
    cells.value = await laneApi.capacity({ date: date.value })
    // 行 = 车道；格子 = edits
    const byLane = new Map()
    for (const c of cells.value) {
      if (!byLane.has(c.laneId)) {
        byLane.set(c.laneId, { laneId: c.laneId, gateNo: c.gateNo, laneNo: c.laneNo })
      }
      edits[cellKey(c.laneId, c.timeSlot)] = c.capacity
    }
    rows.value = Array.from(byLane.values())
  } finally {
    loading.value = false
  }
}

async function save() {
  const payload = []
  for (const row of rows.value) {
    for (const slot of slots) {
      const capacity = edits[cellKey(row.laneId, slot)]
      if (capacity === undefined || capacity === null) continue
      payload.push({ laneId: row.laneId, timeSlot: slot, capacity })
    }
  }
  if (payload.length === 0) {
    ElMessage.warning('没有可保存的容量')
    return
  }
  saving.value = true
  try {
    await laneApi.batchCapacity({ date: date.value, cells: payload })
    ElMessage.success(`已保存 ${payload.length} 个格子`)
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.cell-meta {
  font-size: 12px;
  color: #909399;
}
.ok {
  color: #67c23a;
}
.full {
  color: #f56c6c;
}
.tip {
  font-size: 12px;
  color: #909399;
}
</style>