<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query">
      <el-form-item label="车牌号">
        <el-input v-model="query.plateNo" placeholder="车牌号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="IC卡号">
        <el-input v-model="query.icCardNo" placeholder="IC卡编号" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item v-if="isAdmin" label="所属公司">
        <el-select v-model="query.companyId" placeholder="全部公司" clearable style="width: 170px">
          <el-option v-for="c in companies" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增拖车</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="plateNo" label="车牌号" width="130" />
      <el-table-column prop="frameNo" label="车架号" min-width="170" />
      <el-table-column prop="icCardNo" label="IC卡编号" width="120" />
      <el-table-column prop="axleNum" label="轴数" width="80" align="center" />
      <el-table-column v-if="isAdmin" prop="companyName" label="所属公司" min-width="180" />
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
          <el-popconfirm title="确定删除该拖车吗？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑拖车' : '新增拖车'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="form.plateNo" />
        </el-form-item>
        <el-form-item label="车架号" prop="frameNo">
          <el-input v-model="form.frameNo" />
        </el-form-item>
        <el-form-item label="IC卡编号" prop="icCardNo">
          <el-input v-model="form.icCardNo" placeholder="模拟刷卡编号，如 IC0004" />
        </el-form-item>
        <el-form-item label="轴数" prop="axleNum">
          <el-input-number v-model="form.axleNum" :min="2" :max="10" />
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
import { tractorApi, companyApi } from '@/api'
import { useUserStore } from '@/store/user'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.role === 'ADMIN')

const companies = ref([])

const {
  loading, submitting, tableData, total, dialogVisible, formRef, query, form,
  loadData, resetQuery, openDialog, handleSubmit, handleDelete
} = useCrudPage(tractorApi, {
  defaultQuery: { plateNo: '', icCardNo: '', companyId: null },
  defaultForm: () => ({
    id: null, plateNo: '', frameNo: '', icCardNo: '', axleNum: 4,
    companyId: isAdmin.value ? null : userStore.user.companyId, status: 1
  })
})

const rules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  icCardNo: [{ required: true, message: '请输入IC卡编号', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择所属公司', trigger: 'change' }]
}

async function loadCompanies() {
  companies.value = await companyApi.all()
}

loadCompanies()
loadData()
</script>
