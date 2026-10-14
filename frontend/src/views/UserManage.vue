<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="登录用户名" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 150px">
          <el-option v-for="r in ROLE" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增用户</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="name" label="真实姓名" width="120" />
      <el-table-column label="角色" width="130" align="center">
        <template #default="{ row }">
          <el-tag>{{ dictLabel(ROLE, row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="companyName" label="所属公司" min-width="170">
        <template #default="{ row }">{{ row.companyName || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" @click="openPwdDialog(row)">重置密码</el-button>
          <el-popconfirm title="确定删除该用户吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in ROLE" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needCompany" label="所属公司" prop="companyId">
          <el-select v-model="form.companyId" placeholder="请选择公司" style="width: 100%">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.role === 'DRIVER'" label="关联司机">
          <el-select v-model="form.driverId" placeholder="可选，关联司机档案" clearable filterable style="width: 100%">
            <el-option
              v-for="d in drivers.filter((x) => !form.companyId || x.companyId === form.companyId)"
              :key="d.id"
              :label="`${d.name}（${d.phone}）`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form :model="pwdForm" label-width="90px">
        <el-form-item label="用户">
          <span>{{ pwdForm.name }}（{{ pwdForm.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Plus } from '@element-plus/icons-vue'
import { userApi, companyApi, driverApi } from '@/api'
import { ROLE, dictLabel } from '@/utils/dict'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const pwdVisible = ref(false)
const formRef = ref()
const companies = ref([])
const drivers = ref([])

const query = reactive({ page: 1, size: 10, username: '', role: '' })
const defaultForm = () => ({
  id: null, username: '', password: '', name: '', role: '',
  companyId: null, driverId: null, status: 1
})
const form = reactive(defaultForm())
const pwdForm = reactive({ id: null, username: '', name: '', password: '' })

const needCompany = computed(() => form.role === 'COMPANY' || form.role === 'DRIVER')

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  companyId: [{ required: true, message: '请选择所属公司', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const data = await userApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.role = ''
  query.page = 1
  loadData()
}

function openDialog(row) {
  Object.assign(form, defaultForm())
  if (row) {
    Object.assign(form, {
      id: row.id, username: row.username, name: row.name, role: row.role,
      companyId: row.companyId, driverId: row.driverId, status: row.status
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (form.id) {
        await userApi.update(form.id, form)
        ElMessage.success('修改成功')
      } else {
        await userApi.create(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  await userApi.delete(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function openPwdDialog(row) {
  pwdForm.id = row.id
  pwdForm.username = row.username
  pwdForm.name = row.name
  pwdForm.password = ''
  pwdVisible.value = true
}

async function handleResetPwd() {
  if (!pwdForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  await userApi.resetPassword(pwdForm.id, pwdForm.password)
  ElMessage.success('密码重置成功')
  pwdVisible.value = false
}

async function loadBaseData() {
  companies.value = await companyApi.all()
  drivers.value = await driverApi.all()
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
