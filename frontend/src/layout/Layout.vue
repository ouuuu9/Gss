<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo-bar">
        <el-icon :size="26" color="#fff"><Ship /></el-icon>
        <span v-show="!collapsed" class="logo-text">闸口办单系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :default-openeds="openeds"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        background-color="#1f3b5e"
        text-color="#c5d2e3"
        active-text-color="#ffd04b"
      >
        <el-sub-menu v-for="group in menuGroups" :key="group.title" :index="group.title">
          <template #title>
            <el-icon><component :is="groupIcons[group.title] || 'Menu'" /></el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: homePath }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="30" class="avatar">{{ user?.name?.charAt(0) || 'U' }}</el-avatar>
            <span class="user-name">{{ user?.name }}</span>
            <el-tag size="small" type="warning" effect="plain">{{ roleName }}</el-tag>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const user = computed(() => userStore.user)

const roleNames = {
  ADMIN: '码头管理员',
  GATE: '闸口办单员',
  COMPANY: '运输公司',
  DRIVER: '司机'
}
const roleName = computed(() => roleNames[user.value?.role] || '')

// 用户的首页路径：取第一个有权限访问的菜单（看板仅 ADMIN/GATE 可见，公司/司机落到各自业务页）
const homePath = computed(() => {
  const rootRoute = router.options.routes.find((r) => r.path === '/')
  const children = rootRoute?.children ?? []
  const first = children.find((child) => {
    const roles = child.meta?.roles
    return child.meta?.title && (!roles || roles.includes(user.value?.role))
  })
  return first ? '/' + first.path : '/dashboard'
})

// 菜单由路由表单一数据源生成：读取主布局下带标题的子路由，
// 按 meta.roles 过滤当前用户有权限访问的模块（无 roles 表示所有角色可见），
// 再按 meta.group 归组渲染（组顺序即路由表中的出现顺序）
const menuGroups = computed(() => {
  const rootRoute = router.options.routes.find((r) => r.path === '/')
  const children = rootRoute?.children ?? []
  const groups = []
  for (const child of children) {
    if (!child.meta?.title) continue
    const roles = child.meta.roles
    if (roles && !roles.includes(user.value?.role)) continue
    const groupTitle = child.meta.group || '其他'
    let group = groups.find((g) => g.title === groupTitle)
    if (!group) {
      group = { title: groupTitle, items: [] }
      groups.push(group)
    }
    group.items.push({
      path: '/' + child.path,
      title: child.meta.title,
      icon: child.meta.icon
    })
  }
  return groups
})

// 分组图标与默认展开状态（点击组名可折叠/展开）
const groupIcons = {
  数据洞察: 'DataLine',
  作业中心: 'SetUp',
  基础档案: 'Files',
  系统管理: 'Setting'
}
const openeds = computed(() => menuGroups.value.map((g) => g.title))

async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    try {
      await authApi.logout()
    } catch (e) {
      // 即使登出接口异常也继续本地清理
    }
    userStore.clear()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background-color: #1f3b5e;
  transition: width 0.25s;
  overflow-x: hidden;
}

.logo-bar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background-color: #16324f;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
}

.aside :deep(.el-menu) {
  border-right: none;
}

/* 分组标题（可折叠）与菜单项统一风格 */
.aside :deep(.el-sub-menu__title) {
  height: 46px;
  margin: 2px 8px;
  border-radius: 6px;
}

.aside :deep(.el-sub-menu__title:hover) {
  background-color: #2a4d75;
}

.aside :deep(.el-menu-item) {
  height: 46px;
  margin: 2px 8px;
  border-radius: 6px;
}

.aside :deep(.el-menu-item:hover) {
  background-color: #2a4d75;
}

.aside :deep(.el-menu-item.is-active) {
  background-color: #16324f;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a6a7a;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  background: #2b6cb0;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.main {
  background: #f0f2f5;
  padding: 16px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
