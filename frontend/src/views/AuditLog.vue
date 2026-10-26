<template>
  <el-card shadow="never">
    <!-- 筛选 -->
    <el-form :inline="true" :model="query">
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="操作内容">
        <el-input v-model="query.operation" placeholder="操作描述" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.success" placeholder="全部" clearable style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleDateChange"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetQuery(() => dateRange = null)">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="username" label="操作人" width="110" />
      <el-table-column label="角色" width="120" align="center">
        <template #default="{ row }">
          {{ dictLabel(ROLE, row.role) }}
        </template>
      </el-table-column>
      <el-table-column prop="operation" label="操作" width="130" />
      <el-table-column prop="method" label="方式" width="70" align="center" />
      <el-table-column prop="uri" label="请求地址" min-width="170" show-overflow-tooltip />
      <el-table-column prop="params" label="参数" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.params || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="120" />
      <el-table-column label="结果" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'">
            {{ row.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorMsg" label="失败原因" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.errorMsg || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="80" align="center">
        <template #default="{ row }">
          {{ row.costMs != null ? row.costMs + 'ms' : '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <AppPager :query="query" :total="total" @change="loadData" />
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { auditApi } from '@/api'
import { ROLE, dictLabel } from '@/utils/dict'
import { useCrudPage } from '@/utils/useCrudPage'
import AppPager from '@/components/AppPager.vue'

const { loading, tableData, total, query, loadData, resetQuery } = useCrudPage(auditApi, {
  defaultQuery: { username: '', operation: '', success: null, startTime: null, endTime: null }
})

const dateRange = ref(null)

function handleDateChange(val) {
  query.startTime = val?.[0] ?? null
  query.endTime = val?.[1] ?? null
  query.page = 1
  loadData()
}

loadData()
</script>
