<template>
  <el-card shadow="never">
    <!-- 搜索栏 -->
    <el-form :inline="true" :model="query">
      <el-form-item label="公司名称">
        <el-input v-model="query.name" placeholder="请输入公司名称" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="公司编码">
        <el-input v-model="query.code" placeholder="请输入公司编码" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增公司</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="code" label="公司编码" width="140" />
      <el-table-column prop="name" label="公司名称" min-width="200" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="150" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该公司吗？" @confirm="handleDelete(row)">
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
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑运输公司' : '新增运输公司'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公司编码" prop="code">
          <el-input v-model="form.code" placeholder="如 COMP003" />
        </el-form-item>
        <el-form-item label="公司名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Plus } from '@element-plus/icons-vue'
import { companyApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, name: '', code: '' })
const defaultForm = () => ({ id: null, code: '', name: '', contact: '', phone: '', status: 1 })
const form = reactive(defaultForm())

const rules = {
  code: [{ required: true, message: '请输入公司编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const data = await companyApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.name = ''
  query.code = ''
  query.page = 1
  loadData()
}

function openDialog(row) {
  Object.assign(form, defaultForm())
  if (row) {
    Object.assign(form, {
      id: row.id,
      code: row.code,
      name: row.name,
      contact: row.contact,
      phone: row.phone,
      status: row.status
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
        await companyApi.update(form.id, form)
        ElMessage.success('修改成功')
      } else {
        await companyApi.create(form)
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
  await companyApi.delete(row.id)
  ElMessage.success('删除成功')
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
