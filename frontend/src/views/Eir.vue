<template>
  <el-card shadow="never">
    <!-- 筛选 -->
    <el-form :inline="true" :model="query">
      <el-form-item label="单证编号">
        <el-input v-model="query.eirNo" placeholder="单证编号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="query.businessType" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="b in BUSINESS_TYPE" :key="b.value" :label="b.label" :value="b.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option v-for="s in EIR_STATUS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isAdmin" label="作业公司">
        <el-select v-model="query.companyId" placeholder="全部公司" clearable style="width: 160px">
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button v-if="isAdmin" type="success" :icon="Plus" @click="openEirDialog()">录入单证</el-button>
        <el-button v-if="isCompany" type="success" :icon="EditPen" @click="openApplyDialog()">办单申请</el-button>
        <el-button
          v-if="isCompany" type="primary" :icon="Promotion"
          :disabled="selectedRows.length === 0" @click="openBatchDispatch()"
        >批量派单（{{ selectedRows.length }}）</el-button>
        <el-button type="warning" :icon="Download" :loading="exporting" @click="handleExport">导出Excel</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="tableData" v-loading="loading" border stripe @selection-change="onSelectionChange">
      <el-table-column v-if="isCompany" type="selection" width="48" :selectable="canSelectCreated" />
      <el-table-column prop="eirNo" label="单证编号" width="165" fixed />
      <el-table-column label="业务类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(BUSINESS_TYPE, row.businessType)">
            {{ dictLabel(BUSINESS_TYPE, row.businessType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="containerNo" label="集装箱号" width="130" />
      <el-table-column label="箱型尺寸" width="100" align="center">
        <template #default="{ row }">{{ row.containerSize }}{{ row.containerType }}</template>
      </el-table-column>
      <el-table-column v-if="!isCompany" prop="companyName" label="作业公司" min-width="160" />
      <el-table-column prop="plateNo" label="派单车牌" width="100">
        <template #default="{ row }">{{ row.plateNo || '—' }}</template>
      </el-table-column>
      <el-table-column prop="driverName" label="派单司机" width="100">
        <template #default="{ row }">{{ row.driverName || '—' }}</template>
      </el-table-column>
      <el-table-column label="缴费" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.payStatus === 1" type="success" size="small">已缴</el-tag>
          <el-tag v-else-if="row.payStatus === 0" type="danger" size="small">未缴</el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="办单审核" width="110" align="center">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.reviewStatus === 'REJECTED' && row.rejectReason"
            :content="'驳回原因：' + row.rejectReason" placement="top"
          >
            <el-tag type="danger" size="small">{{ dictLabel(REVIEW_STATUS, row.reviewStatus) }}</el-tag>
          </el-tooltip>
          <el-tag v-else :type="dictType(REVIEW_STATUS, row.reviewStatus)" size="small">
            {{ dictLabel(REVIEW_STATUS, row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictType(EIR_STATUS, row.status)">
            {{ dictLabel(EIR_STATUS, row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="isAdmin || isCompany || isDriver" label="操作" width="230" fixed="right" align="center">
        <template #default="{ row }">
          <!-- ADMIN -->
          <el-button v-if="isAdmin && row.status === 'GATE_OUT'" link type="success" @click="handleAudit(row)">审单</el-button>
          <el-button
            v-if="isAdmin && ['APPROVED', 'DISPATCHED', 'APPOINTED'].includes(row.status)"
            link type="danger" @click="handleRevokeReview(row)"
          >撤销审核</el-button>
          <el-button v-if="isAdmin" link type="primary" @click="openEirDialog(row)">修改</el-button>
          <!-- COMPANY -->
          <el-button v-if="isCompany && row.status === 'APPROVED'" link type="primary" @click="openDispatch(row)">派单</el-button>
          <el-button v-if="isCompany && row.status === 'REJECTED'" link type="warning" @click="handleResubmit(row)">重新提交</el-button>
          <el-button v-if="isCompany && row.status === 'DISPATCHED'" link type="warning" @click="handleCancelDispatch(row)">取消派单</el-button>
          <!-- DRIVER -->
          <el-button v-if="isDriver && row.status === 'DISPATCHED'" link type="warning" @click="openBook(row)">预约</el-button>
          <el-button v-if="isDriver && row.status === 'APPOINTED'" link type="danger" @click="handleCancelAppt(row)">取消预约</el-button>
          <!-- 作废 -->
          <el-button
            v-if="(isAdmin || isCompany) && ['CREATED', 'SUBMITTED', 'APPROVED', 'REJECTED', 'DISPATCHED', 'APPOINTED'].includes(row.status)"
            link type="danger" @click="handleCancel(row)"
          >作废</el-button>
        </template>
      </el-table-column>
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />

    <!-- 录入/修改单证弹窗 -->
    <el-dialog v-model="eirDialog" :title="eirForm.id ? '修改单证' : '录入EIR单证'" width="560px">
      <el-form ref="eirFormRef" :model="eirForm" :rules="eirRules" label-width="100px">
        <el-form-item v-if="eirForm.id" label="单证编号">
          <el-input :model-value="eirForm.eirNo" disabled />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-radio-group v-model="eirForm.businessType">
            <el-radio v-for="b in BUSINESS_TYPE" :key="b.value" :value="b.value">{{ b.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="集装箱号" prop="containerNo">
          <el-input v-model="eirForm.containerNo" placeholder="如 CSNU1000003" />
        </el-form-item>
        <el-form-item label="箱尺寸" prop="containerSize">
          <el-radio-group v-model="eirForm.containerSize">
            <el-radio :value="20">20尺</el-radio>
            <el-radio :value="40">40尺</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="箱型" prop="containerType">
          <el-radio-group v-model="eirForm.containerType">
            <el-radio value="GP">GP 普通箱</el-radio>
            <el-radio value="HQ">HQ 高箱</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作业公司" prop="companyId">
          <el-select v-model="eirForm.companyId" placeholder="请选择作业公司" style="width: 100%">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="eirForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="eirDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEir">确定</el-button>
      </template>
    </el-dialog>

    <!-- 客户办单申请弹窗（M1）：提交后进入待审核，审核通过才能派单 -->
    <el-dialog v-model="applyDialog" title="提交办单申请" width="520px">
      <el-alert
        title="提交后由码头管理员审核；审核通过后方可派单、司机方可预约。驳回会显示原因，可修改后重新提交。"
        type="info" :closable="false" show-icon style="margin-bottom: 16px"
      />
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="applyForm.businessType" style="width: 100%">
            <el-option v-for="b in BUSINESS_TYPE" :key="b.value" :label="b.label" :value="b.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="集装箱号" prop="containerNo">
          <el-input v-model="applyForm.containerNo" placeholder="如 CSNU1000003" />
        </el-form-item>
        <el-form-item label="箱尺寸" prop="containerSize">
          <el-radio-group v-model="applyForm.containerSize">
            <el-radio :value="20">20</el-radio>
            <el-radio :value="40">40</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="箱型" prop="containerType">
          <el-radio-group v-model="applyForm.containerType">
            <el-radio value="GP">GP</el-radio>
            <el-radio value="HQ">HQ</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="applyForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 派单弹窗（单条/批量共用） -->
    <el-dialog v-model="dispatchDialog" :title="dispatchEirIds.length > 1 ? '批量派单' : '单证派单'" width="520px">
      <el-alert
        title="派单后将通知司机；还重单生成 50 元还重费（进闸前缴清），提重单生成 80 元提重费（出闸前缴清）"
        type="info" :closable="false" show-icon style="margin-bottom: 16px"
      />
      <el-form :model="dispatchForm" label-width="90px">
        <el-form-item label="派单单证">
          <span>{{ dispatchLabel }}</span>
        </el-form-item>
        <el-form-item label="派单司机" required>
          <el-select v-model="dispatchForm.driverId" placeholder="请选择本公司司机" filterable style="width: 100%">
            <el-option
              v-for="d in companyDrivers" :key="d.id"
              :label="`${d.name}（${d.phone}）`" :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="派单拖车" required>
          <el-select v-model="dispatchForm.tractorId" placeholder="请选择本公司拖车" filterable style="width: 100%">
            <el-option
              v-for="t in companyTractors" :key="t.id"
              :label="`${t.plateNo}（IC卡：${t.icCardNo}）`" :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDispatch">确认派单</el-button>
      </template>
    </el-dialog>
    <!-- 司机预约弹窗（共用组件，单证固定） -->
    <appt-book-dialog v-model:visible="bookVisible" :fixed-eir="bookEir" @success="loadData" />
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Download, Promotion, EditPen } from '@element-plus/icons-vue'
import { eirApi, companyApi, driverApi, tractorApi, gateApi, apptApi } from '@/api'
import { useUserStore } from '@/store/user'
import { EIR_STATUS, REVIEW_STATUS, BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
import { exportExcel } from '@/utils/download'
import ApptBookDialog from '@/components/ApptBookDialog.vue'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isCompany = computed(() => userStore.role === 'COMPANY')
const isDriver = computed(() => userStore.role === 'DRIVER')

const submitting = ref(false)
const exporting = ref(false)
const companies = ref([])

const { loading, tableData, total, query, loadData, resetQuery } = useCrudPage(eirApi, {
  defaultQuery: { eirNo: '', status: '', businessType: '', companyId: null }
})

/* ---------- 单证弹窗 ---------- */
const eirDialog = ref(false)
const eirFormRef = ref()
const defaultEirForm = () => ({
  id: null, eirNo: '', businessType: 'PI-E', containerNo: '',
  containerSize: 20, containerType: 'GP', companyId: null, remark: ''
})
const eirForm = reactive(defaultEirForm())
const eirRules = {
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  containerNo: [{ required: true, message: '请输入集装箱号', trigger: 'blur' }],
  containerSize: [{ required: true, message: '请选择箱尺寸', trigger: 'change' }],
  containerType: [{ required: true, message: '请选择箱型', trigger: 'change' }],
  companyId: [{ required: true, message: '请选择作业公司', trigger: 'change' }]
}

/* ---------- 办单申请（COMPANY，M1） ---------- */
const applyDialog = ref(false)
const applyFormRef = ref()
const applyForm = reactive({
  businessType: 'PI-E', containerNo: '', containerSize: 20, containerType: 'GP', remark: ''
})
const applyRules = {
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  containerNo: [{ required: true, message: '请输入集装箱号', trigger: 'blur' }],
  containerSize: [{ required: true, message: '请选择箱尺寸', trigger: 'change' }],
  containerType: [{ required: true, message: '请选择箱型', trigger: 'change' }]
}

function openApplyDialog() {
  Object.assign(applyForm, { businessType: 'PI-E', containerNo: '', containerSize: 20, containerType: 'GP', remark: '' })
  applyDialog.value = true
}

async function submitApply() {
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await eirApi.apply(applyForm)
    ElMessage.success('办单申请已提交，等待管理员审核')
    applyDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleResubmit(row) {
  await ElMessageBox.confirm(
    `单证 ${row.eirNo} 已被驳回${row.rejectReason ? '（原因：' + row.rejectReason + '）' : ''}，确定重新提交审核吗？`,
    '重新提交', { confirmButtonText: '重新提交', cancelButtonText: '取消', type: 'warning' }
  )
  await eirApi.resubmit(row.id)
  ElMessage.success('已重新提交，等待管理员审核')
  loadData()
}

/* ---------- 派单弹窗（单条/批量共用） ---------- */
const dispatchDialog = ref(false)
const dispatchEirIds = ref([])
const dispatchLabel = ref('')
const dispatchForm = reactive({ driverId: null, tractorId: null })
const companyDrivers = ref([])
const companyTractors = ref([])
const selectedRows = ref([])

// 仅审核通过（APPROVED）的单证可勾选参与批量派单（M1：未审核/已驳回不可派单）
function canSelectCreated(row) {
  return row.status === 'APPROVED'
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function openEirDialog(row) {
  Object.assign(eirForm, defaultEirForm())
  if (row) {
    Object.assign(eirForm, {
      id: row.id, eirNo: row.eirNo, businessType: row.businessType, containerNo: row.containerNo,
      containerSize: row.containerSize, containerType: row.containerType,
      companyId: row.companyId, remark: row.remark
    })
  }
  eirDialog.value = true
}

async function submitEir() {
  const valid = await eirFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    if (eirForm.id) {
      await eirApi.update(eirForm.id, eirForm)
      ElMessage.success('修改成功')
    } else {
      await eirApi.create(eirForm)
      ElMessage.success('单证录入成功')
    }
    eirDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function openDispatch(row) {
  dispatchEirIds.value = [row.id]
  dispatchLabel.value = row.eirNo
  prepareDispatch()
}

function openBatchDispatch() {
  dispatchEirIds.value = selectedRows.value.map((r) => r.id)
  dispatchLabel.value = `已选 ${dispatchEirIds.value.length} 条单证（派给同一司机与拖车）`
  prepareDispatch()
}

async function prepareDispatch() {
  dispatchForm.driverId = null
  dispatchForm.tractorId = null
  const companyId = userStore.user.companyId
  const [dData, tData] = await Promise.all([
    driverApi.page({ page: 1, size: 200, companyId }),
    tractorApi.page({ page: 1, size: 200, companyId })
  ])
  companyDrivers.value = dData.rows.filter((d) => d.status === 1)
  companyTractors.value = tData.rows.filter((t) => t.status === 1)
  dispatchDialog.value = true
}

async function submitDispatch() {
  if (!dispatchForm.driverId || !dispatchForm.tractorId) {
    ElMessage.warning('请选择派单司机和拖车')
    return
  }
  submitting.value = true
  try {
    const payload = { driverId: dispatchForm.driverId, tractorId: dispatchForm.tractorId }
    if (dispatchEirIds.value.length === 1) {
      await eirApi.dispatch(dispatchEirIds.value[0], payload)
      ElMessage.success('派单成功')
    } else {
      const res = await eirApi.batchDispatch({ ids: dispatchEirIds.value, ...payload })
      if (res.failed > 0) {
        ElMessageBox.alert(
          res.failures.join('\n'),
          `批量派单完成：成功 ${res.success} 单，失败 ${res.failed} 单`,
          { type: 'warning' }
        )
      } else {
        ElMessage.success(`批量派单成功，共 ${res.success} 单`)
      }
    }
    dispatchDialog.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------- 司机预约 ---------- */
const bookVisible = ref(false)
const bookEir = ref(null)

function openBook(row) {
  bookEir.value = row
  bookVisible.value = true
}

async function handleCancelAppt(row) {
  await ElMessageBox.confirm('确定取消该单证的进港预约吗？取消后单证将回到已派单状态。', '提示', {
    type: 'warning'
  })
  await apptApi.cancelByEir(row.id)
  ElMessage.success('预约已取消')
  loadData()
}

async function handleCancelDispatch(row) {
  await ElMessageBox.confirm('取消派单将清除派单信息及收费记录，确定继续吗？', '提示', {
    type: 'warning'
  })
  await eirApi.cancelDispatch(row.id)
  ElMessage.success('已取消派单')
  loadData()
}

async function handleCancel(row) {
  await ElMessageBox.confirm(`确定作废单证 ${row.eirNo} 吗？`, '作废确认', {
    confirmButtonText: '确定作废', cancelButtonText: '取消', type: 'warning'
  })
  const outcome = await eirApi.cancel(row.id)
  ElMessage.success(outcome === 'REFUND_PENDING' ? '已提交退款申请，等待管理员审批' : '单证已作废')
  loadData()
}

/**
 * 撤销办单审核（ADMIN）：已通过/已派单/已预约 → 驳回。
 * 已预约的会同时取消预约并回补名额，已派单的会清空派单信息，确认框里说明清楚影响。
 */
async function handleRevokeReview(row) {
  const impact = row.status === 'APPOINTED'
    ? '将同时取消该单证的有效预约并回补时段名额'
    : row.status === 'DISPATCHED'
      ? '将同时清空派单司机与拖车信息'
      : '该单证将退回未通过状态'
  const { value } = await ElMessageBox.prompt(
    `确定撤销单证 ${row.eirNo} 的办单审核吗？${impact}。请填写撤销原因（1-200 字）：`,
    '撤销办单审核',
    {
      confirmButtonText: '确认撤销', cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => {
        const text = (v || '').trim()
        if (!text) {
          return '撤销原因不能为空'
        }
        return text.length <= 200 ? true : '撤销原因不能超过 200 字'
      }
    }
  )
  await eirApi.revokeReview(row.id, value.trim())
  ElMessage.success('已撤销审核，单证退回驳回状态')
  loadData()
}

async function handleAudit(row) {
  await ElMessageBox.confirm(`确定审单通过吗？单证 ${row.eirNo}`, '审单确认', {
    type: 'warning'
  })
  await gateApi.audit(row.id)
  ElMessage.success('审单完成')
  loadData()
}

async function handleExport() {
  exporting.value = true
  await exportExcel(eirApi.export, {
    status: query.status, bizType: query.businessType, companyId: query.companyId
  }, 'EIR单证导出.xlsx')
  exporting.value = false
}

async function loadBaseData() {
  companies.value = await companyApi.all()
}

loadBaseData()
loadData()
</script>
