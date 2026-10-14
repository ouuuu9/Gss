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
        <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
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
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { auditApi } from '@/api'
import { ROLE, dictLabel } from '@/utils/dict'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref(null)
const query = reactive({ page: 1, size: 10, username: '', operation: '', success: null, startTime: null, endTime: null })

function handleDateChange(val) {
  if (val && val.length === 2) {
    query.startTime = val[0]
    query.endTime = val[1]
  } else {
    query.startTime = null
    query.endTime = null
  }
  query.page = 1
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const data = await auditApi.page(query)
    tableData.value = data.rows
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.username = ''
  query.operation = ''
  query.success = null
  query.startTime = null
  query.endTime = null
  query.page = 1
  dateRange.value = null
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
