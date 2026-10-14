<template>
  <div>
    <!-- 欢迎卡片 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <el-avatar :size="46" class="welcome-avatar">{{ user?.name?.charAt(0) }}</el-avatar>
        <div>
          <div class="welcome-title">你好，{{ user?.name }}，欢迎使用闸口办单管理系统</div>
          <div class="welcome-sub">
            当前身份：<el-tag size="small" type="warning">{{ roleName }}</el-tag>
            <span style="margin-left: 12px">{{ today }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in topCards" :key="card.label" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="26"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-meta">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 单证状态分布 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <template #header>
            <span>单证状态分布</span>
          </template>
          <div v-for="item in distribution" :key="item.label" class="dist-row">
            <span class="dist-label">
              <el-tag size="small" :type="item.type" effect="plain">{{ item.label }}</el-tag>
            </span>
            <el-progress
              :percentage="item.percent"
              :format="() => item.value + ' 单'"
              :color="item.barColor"
              class="dist-progress"
            />
          </div>
          <el-empty v-if="distribution.length === 0" description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card shadow="never">
          <template #header><span>业务流程说明</span></template>
          <el-steps direction="vertical" :active="0" :process-status="'finish'">
            <el-step title="单证录入" description="码头管理员录入 EIR 单证" />
            <el-step title="派单" description="运输公司派单给司机/拖车（还重单生成费用）" />
            <el-step title="进港预约" description="司机/公司创建进港预约" />
            <el-step title="刷卡进闸" description="校验预约与缴费后进闸办单" />
            <el-step title="出闸办单" description="作业完成刷卡出闸" />
            <el-step title="审单完成" description="管理员审单，流程结束" />
          </el-steps>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { dashboardApi } from '@/api'
import { useUserStore } from '@/store/user'
import { EIR_STATUS } from '@/utils/dict'

const userStore = useUserStore()
const user = computed(() => userStore.user)

const stat = ref({})

const roleNames = {
  ADMIN: '码头管理员',
  GATE: '闸口办单员',
  COMPANY: '运输公司用户',
  DRIVER: '司机'
}
const roleName = computed(() => roleNames[user.value?.role])

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

// 顶部指标卡（看板仅 ADMIN / GATE 可见）
const topCards = computed(() => {
  return [
    { label: '单证总数', value: stat.value.totalEir ?? 0, icon: 'Document', color: '#409eff' },
    { label: '有效预约', value: stat.value.activeApptCount ?? 0, icon: 'Calendar', color: '#67c23a' },
    { label: '场内车辆', value: stat.value.gateInCount ?? 0, icon: 'Van', color: '#e6a23c' },
    { label: '已收金额(元)', value: stat.value.paidAmount ?? 0, icon: 'Coin', color: '#f56c6c' }
  ]
})

// 状态分布（含百分比）
const distribution = computed(() => {
  const total = Number(stat.value.totalEir || 0)
  const rows = EIR_STATUS.map((s) => {
    const keyMap = {
      CREATED: 'createdCount',
      DISPATCHED: 'dispatchedCount',
      APPOINTED: 'appointedCount',
      GATE_IN: 'gateInCount',
      GATE_OUT: 'gateOutCount',
      DONE: 'doneCount',
      CANCELLED: 'cancelledCount'
    }
    const colors = {
      CREATED: '#909399',
      DISPATCHED: '#409eff',
      APPOINTED: '#e6a23c',
      GATE_IN: '#8e44ad',
      GATE_OUT: '#16a085',
      DONE: '#67c23a',
      CANCELLED: '#f56c6c'
    }
    const value = Number(stat.value[keyMap[s.value]] || 0)
    return {
      label: s.label,
      value,
      type: s.type,
      barColor: colors[s.value],
      percent: total === 0 ? 0 : Math.round((value / total) * 100)
    }
  })
  return rows
})

async function load() {
  stat.value = await dashboardApi.get()
}

onMounted(load)
</script>

<style scoped>
.welcome-card {
  margin-bottom: 16px;
  background: linear-gradient(90deg, #eef5ff, #ffffff);
}

.welcome {
  display: flex;
  align-items: center;
  gap: 14px;
}

.welcome-avatar {
  background: #2b6cb0;
  font-size: 20px;
}

.welcome-title {
  font-size: 17px;
  font-weight: bold;
  color: #1f2d3d;
}

.welcome-sub {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.dist-row {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}

.dist-label {
  width: 80px;
  flex-shrink: 0;
}

.dist-progress {
  flex: 1;
  margin-right: 10px;
}
</style>
