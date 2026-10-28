<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="司机姓名" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item v-if="isAdmin" label="所属公司">
        <el-select v-model="query.companyId" placeholder="全部公司" clearable style="width: 170px">
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增司机</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="司机姓名" width="110" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="licenseNo" label="驾驶证号" min-width="150" />
      <el-table-column prop="plateNo" label="绑定车牌" width="120" />
      <el-table-column v-if="isAdmin" prop="companyName" label="所属公司" min-width="170" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-popconfirm title="确定删除该司机吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑司机' : '新增司机'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="司机姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="司机登录账号将使用该手机号" />
        </el-form-item>
        <el-form-item label="驾驶证号" prop="licenseNo">
          <el-input v-model="form.licenseNo" />
        </el-form-item>
        <el-form-item label="绑定拖车" prop="tractorId">
          <el-select v-model="form.tractorId" placeholder="请选择拖车" clearable filterable style="width: 100%">
            <el-option
              v-for="t in tractors"
              :key="t.id"
              :label="`${t.plateNo}（${t.icCardNo}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属公司" prop="companyId">
          <el-select v-model="form.companyId" :disabled="!isAdmin" placeholder="请选择公司" style="width: 100%">
            <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
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
import { ref, computed } from 'vue'
import { Search, RefreshLeft, Plus } from '@element-plus/icons-vue'
import { driverApi, tractorApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const companies = ref([])
const tractors = ref([])

const {
  loading, submitting, tableData, total, dialogVisible, formRef, query, form,
  loadData, resetQuery, openDialog, handleSubmit, handleDelete
} = useCrudPage(driverApi, {
  defaultQuery: { name: '', phone: '', companyId: null },
  defaultForm: () => ({
    id: null, name: '', phone: '', licenseNo: '', tractorId: null,
    companyId: isAdmin.value ? null : userStore.user.companyId, status: 1
  })
})

const rules = {
  name: [{ required: true, message: '请输入司机姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择所属公司', trigger: 'change' }]
}

async function loadBaseData() {
  companies.value = await companyApi.all()
  // 拖车数据较多，ADMIN 全部加载供选择
  const data = await tractorApi.page({ page: 1, size: 200 })
  // 已停用的拖车不可再被绑定
  tractors.value = data.rows.filter((t) => t.status === 1)
}

loadBaseData()
loadData()
</script>
