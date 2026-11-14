<template>
  <el-card shadow="never">
    <el-form :inline="true">
      <el-form-item>
        <el-button v-if="isAdmin" type="success" :icon="Plus" @click="openDialog()">新增车道</el-button>
        <el-button :icon="RefreshLeft" @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="lanes" v-loading="loading" border stripe empty-text="暂无车道">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="gateNo" label="闸口号" width="120" />
      <el-table-column prop="laneNo" label="车道号" width="120" />
      <el-table-column prop="capacityPerSlot" label="默认容量/时段" width="130" align="center" />
      <el-table-column prop="capacitySummary" label="容量概要" min-width="160" />
      <el-table-column label="未完成预约" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.activeAppointments > 0 ? 'warning' : 'info'" size="small">
            {{ row.activeAppointments }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140">
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>
      <el-table-column v-if="isAdmin" label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">修改</el-button>
          <el-button
            link :type="row.status === 'ENABLED' ? 'danger' : 'success'"
            @click="handleToggle(row)"
          >{{ row.status === 'ENABLED' ? '停用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改车道' : '新增车道'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="闸口号" prop="gateNo">
          <el-input v-model="form.gateNo" placeholder="如 GATE-1" />
        </el-form-item>
        <el-form-item label="车道号" prop="laneNo">
          <el-input v-model="form.laneNo" placeholder="如 LANE-2" />
        </el-form-item>
        <el-form-item label="默认容量/时段" prop="capacityPerSlot">
          <el-input-number v-model="form.capacityPerSlot" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshLeft } from '@element-plus/icons-vue'
import { laneApi } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const lanes = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const defaultForm = () => ({ id: null, gateNo: '', laneNo: '', capacityPerSlot: 10, remark: '' })
const form = reactive(defaultForm())
const rules = {
  gateNo: [{ required: true, message: '请输入闸口号', trigger: 'blur' }],
  laneNo: [{ required: true, message: '请输入车道号', trigger: 'blur' }],
  capacityPerSlot: [{ required: true, message: '请输入默认容量', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    lanes.value = await laneApi.list()
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, defaultForm(), row || {})
  if (!row) {
    form.id = null
  }
  dialogVisible.value = true
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    if (form.id) {
      await laneApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await laneApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleToggle(row) {
  const enable = row.status !== 'ENABLED'
  await ElMessageBox.confirm(
    enable ? `确定启用车道 ${row.laneNo} 吗？` : `确定停用车道 ${row.laneNo} 吗？有未完成预约时会被拒绝。`,
    '启停车道', { type: 'warning' }
  )
  await laneApi.toggle(row.id, enable)
  ElMessage.success(enable ? '已启用' : '已停用')
  loadData()
}

onMounted(loadData)
</script>