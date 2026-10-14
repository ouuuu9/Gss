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
          <el-button v-if="isAdmin" link type="primary" @click="openEirDialog(row)">修改</el-button>
          <!-- COMPANY -->
          <el-button v-if="isCompany && row.status === 'CREATED'" link type="primary" @click="openDispatch(row)">派单</el-button>
          <el-button v-if="isCompany && row.status === 'DISPATCHED'" link type="warning" @click="handleCancelDispatch(row)">取消派单</el-button>
          <!-- DRIVER -->
          <el-button v-if="isDriver && row.status === 'DISPATCHED'" link type="warning" @click="openBook(row)">预约</el-button>
          <el-button v-if="isDriver && row.status === 'APPOINTED'" link type="danger" @click="handleCancelAppt(row)">取消预约</el-button>
          <!-- 作废 -->
          <el-button
            v-if="(isAdmin || isCompany) && ['CREATED', 'DISPATCHED', 'APPOINTED'].includes(row.status)"
            link type="danger" @click="handleCancel(row)"
          >作废</el-button>
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

    <!-- 派单弹窗（单条/批量共用） -->
    <el-dialog v-model="dispatchDialog" :title="dispatchTargets.length > 1 ? '批量派单' : '单证派单'" width="520px">
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Download, Promotion } from '@element-plus/icons-vue'
import { eirApi, companyApi, driverApi, tractorApi, gateApi, apptApi } from '@/api'
import { useUserStore } from '@/store/user'
import { EIR_STATUS, BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
import { exportExcel } from '@/utils/download'
import ApptBookDialog from '@/components/ApptBookDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')
const isCompany = computed(() => userStore.role === 'COMPANY')
const isDriver = computed(() => userStore.role === 'DRIVER')

const loading = ref(false)
const submitting = ref(false)
const exporting = ref(false)
const tableData = ref([])
const total = ref(0)
const companies = ref([])

const query = reactive({
  page: 1, size: 10, eirNo: '', status: '', businessType: '', companyId: null
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

/* ---------- 派单弹窗（单条/批量共用） ---------- */
const dispatchDialog = ref(false)
const dispatchTargets = ref([])
const dispatchLabel = ref('')
const dispatchForm = reactive({ driverId: null, tractorId: null })
const companyDrivers = ref([])
const companyTractors = ref([])
const selectedRows = ref([])

// 仅 CREATED 状态可勾选参与批量派单
function canSelectCreated(row) {
  return row.status === 'CREATED'
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

async function loadData() {
  loading.value = true
  try {
    const data = await eirApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.eirNo = ''
  query.status = ''
  query.businessType = ''
  query.companyId = null
  query.page = 1
  loadData()
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
  await eirFormRef.value.validate(async (valid) => {
    if (!valid) return
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
  })
}

function openDispatch(row) {
  dispatchTargets.value = [row.id]
  dispatchLabel.value = row.eirNo
  prepareDispatch()
}

function openBatchDispatch() {
  dispatchTargets.value = selectedRows.value.map((r) => r.id)
  dispatchLabel.value = `已选 ${dispatchTargets.value.length} 条单证（派给同一司机与拖车）`
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
  companyDrivers.value = dData.rows
  companyTractors.value = tData.rows
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
    if (dispatchTargets.value.length === 1) {
      await eirApi.dispatch(dispatchTargets.value[0], payload)
      ElMessage.success('派单成功')
    } else {
      const res = await eirApi.batchDispatch({ ids: dispatchTargets.value, ...payload })
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
  await eirApi.cancel(row.id)
  ElMessage.success('单证已作废')
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

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
