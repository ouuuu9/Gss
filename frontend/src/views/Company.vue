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

    <AppPager :query="query" :total="total" @change="loadData" />

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
import { Search, RefreshLeft, Plus } from '@element-plus/icons-vue'
import { companyApi } from '@/api'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const {
  loading, submitting, tableData, total, dialogVisible, formRef, query, form,
  loadData, resetQuery, openDialog, handleSubmit, handleDelete
} = useCrudPage(companyApi, {
  defaultQuery: { name: '', code: '' },
  defaultForm: () => ({ id: null, code: '', name: '', contact: '', phone: '', status: 1 })
})

const rules = {
  code: [{ required: true, message: '请输入公司编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}

loadData()
</script>
