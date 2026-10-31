<template>
  <el-dialog
    :model-value="visible" title="创建进港预约" width="540px"
    @update:model-value="emit('update:visible', $event)" @open="onOpen"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <!-- 固定单证模式：只读展示（司机从单证页发起） -->
      <el-form-item v-if="fixedEir" label="预约单证">
        <span>
          {{ fixedEir.eirNo }} | {{ dictLabel(BUSINESS_TYPE, fixedEir.businessType) }} | {{ fixedEir.containerNo }}
        </span>
      </el-form-item>
      <!-- 选择模式：从已派单单证中选择（预约页发起） -->
      <el-form-item v-else label="选择单证" prop="eirId">
        <el-select
          v-model="form.eirId" placeholder="请选择已派单单证" filterable style="width: 100%"
          @change="onEirChange"
        >
          <el-option
            v-for="e in eirOptions" :key="e.id"
            :label="`${e.eirNo} | ${dictLabel(BUSINESS_TYPE, e.businessType)} | ${e.containerNo} | ${e.plateNo}`"
            :value="e.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="form.plateNo" placeholder="选择单证后自动带出，可修改" />
      </el-form-item>
      <el-form-item label="预约日期" prop="apptDate">
        <el-date-picker
          v-model="form.apptDate" type="date" value-format="YYYY-MM-DD"
          placeholder="选择预约日期" :disabled-date="disablePast" style="width: 100%"
          @change="loadSlots"
        />
      </el-form-item>
      <el-form-item label="预约时段" prop="timeSlot">
        <el-select v-model="form.timeSlot" placeholder="请先选择预约日期" style="width: 100%">
          <el-option
            v-for="option in slotOptions" :key="option.timeSlot"
            :label="slotLabel(option)"
            :value="option.timeSlot"
            :disabled="option.remain <= 0 || isPastSlot(option.timeSlot)"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认预约</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apptApi } from '@/api'
import { BUSINESS_TYPE, dictLabel } from '@/utils/dict'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** 固定单证模式：传入单证对象则隐藏选择器（司机单证页场景） */
  fixedEir: { type: Object, default: null },
  /** 选择模式：可预约单证列表（预约页场景） */
  eirOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:visible', 'success'])

const formRef = ref()
const submitting = ref(false)
const slotOptions = ref([])
const defaultForm = () => ({ eirId: null, plateNo: '', apptDate: '', timeSlot: '' })
const form = reactive(defaultForm())

const rules = {
  eirId: [{ required: true, message: '请选择单证', trigger: 'change' }],
  plateNo: [{ required: true, message: '车牌号不能为空', trigger: 'blur' }],
  apptDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择预约时段', trigger: 'change' }]
}

// 每次打开重置表单；固定单证模式自动回填
function onOpen() {
  Object.assign(form, defaultForm())
  slotOptions.value = []
  if (props.fixedEir) {
    form.eirId = props.fixedEir.id
    form.plateNo = props.fixedEir.plateNo || ''
  }
}

function onEirChange(eirId) {
  const eir = props.eirOptions.find((e) => e.id === eirId)
  if (eir) {
    form.plateNo = eir.plateNo || ''
  }
}

async function loadSlots() {
  slotOptions.value = []
  form.timeSlot = ''
  if (!form.apptDate) return
  slotOptions.value = await apptApi.slots({ date: form.apptDate })
}

function disablePast(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

// 当天已经开始的时段不允许再预约（后端也会兜底拦截）
function isPastSlot(timeSlot) {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const todayText = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  // 只有预约日期是今天时才可能过期
  if (form.apptDate !== todayText) {
    return false
  }
  const [hour, minute] = timeSlot.slice(0, 5).split(':').map(Number)
  return hour * 60 + minute <= now.getHours() * 60 + now.getMinutes()
}

function slotLabel(option) {
  if (isPastSlot(option.timeSlot)) {
    return `${option.timeSlot}（已过期）`
  }
  return `${option.timeSlot}（剩余 ${option.remain}）`
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await apptApi.create({
      eirId: form.eirId,
      plateNo: form.plateNo,
      apptDate: form.apptDate,
      timeSlot: form.timeSlot
    })
    ElMessage.success('预约成功')
    emit('update:visible', false)
    emit('success')
  } finally {
    submitting.value = false
  }
}
</script>
