<template>
  <el-card shadow="never">
    <!-- 说明：办单审核为管理员高频独立动作，ADMIN 单独鉴权，待审列表按申请时间先来先审 -->
    <el-alert
      title="客户提交办单申请后在此审核；通过后客户方可派单、司机方可预约。驳回需填写原因，并会自动取消该单证的有效预约、回补时段名额。"
      type="info" :closable="false" show-icon style="margin-bottom: 16px"
    />

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="eirNo" label="单证编号" width="165" />
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
      <el-table-column prop="companyName" label="作业公司" min-width="160" />
      <el-table-column prop="applicantName" label="申请人" width="100">
        <template #default="{ row }">{{ row.applicantName || '—' }}</template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140">
        <template #default="{ row }">{{ row.remark || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" align="center">
        <template #default="{ row }">
          <el-button link type="success" @click="handleApprove(row)">通过</el-button>
          <el-button link type="danger" @click="handleReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />
  </el-card>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { eirApi } from '@/api'
import { BUSINESS_TYPE, dictLabel, dictType } from '@/utils/dict'
import { formatTime } from '@/utils/format'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

// 审核待办走独立接口：useCrudPage 只依赖 api.page(query)，这里做一层适配
const reviewApi = {
  page: (params) => eirApi.reviewPending(params)
}

const { loading, tableData, total, query, loadData } = useCrudPage(reviewApi, {
  defaultQuery: {}
})

async function handleApprove(row) {
  await ElMessageBox.confirm(
    `确认通过单证 ${row.eirNo} 的办单申请吗？通过后客户可派单、司机可预约。`,
    '办单审核', { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'success' }
  )
  await eirApi.approve(row.id)
  ElMessage.success('已通过，客户可继续派单')
  loadData()
}

async function handleReject(row) {
  const { value } = await ElMessageBox.prompt(
    `请填写单证 ${row.eirNo} 的驳回原因（1-200 字），客户会看到该原因`,
    '驳回办单申请',
    {
      confirmButtonText: '确认驳回', cancelButtonText: '取消',
      inputType: 'textarea',
      inputValidator: (v) => {
        const text = (v || '').trim()
        if (!text) {
          return '驳回原因不能为空'
        }
        return text.length <= 200 ? true : '驳回原因不能超过 200 字'
      }
    }
  )
  await eirApi.reject(row.id, value.trim())
  ElMessage.success('已驳回，关联预约已取消、名额已回补')
  loadData()
}

loadData()
</script>