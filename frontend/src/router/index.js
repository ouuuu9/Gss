import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: () => {
      // 按角色决定登录后落地页：管理员/闸口进看板，公司/司机进 EIR 单证管理
      const user = JSON.parse(localStorage.getItem('gss_user') || 'null')
      return firstAccessiblePath(user?.role)
    },
    children: [
      // —— 数据洞察 ——
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页看板', icon: 'Odometer', roles: ['ADMIN', 'GATE'], group: '数据洞察' }
      },
      {
        path: 'screen',
        name: 'Screen',
        component: () => import('@/views/Screen.vue'),
        meta: { title: '数据大屏', icon: 'DataAnalysis', roles: ['ADMIN', 'GATE'], group: '数据洞察' }
      },
      // —— 作业中心 ——
      {
        path: 'eir',
        name: 'Eir',
        component: () => import('@/views/Eir.vue'),
        meta: { title: 'EIR单证管理', icon: 'Document', roles: ['ADMIN', 'GATE', 'COMPANY', 'DRIVER'], group: '作业中心' }
      },
      {
        path: 'appointments',
        name: 'Appointment',
        component: () => import('@/views/Appointment.vue'),
        meta: { title: '进港预约', icon: 'Calendar', roles: ['COMPANY', 'DRIVER'], group: '作业中心' }
      },
      {
        path: 'gate',
        name: 'Gate',
        component: () => import('@/views/Gate.vue'),
        meta: { title: '闸口办单', icon: 'CreditCard', roles: ['ADMIN', 'GATE'], group: '作业中心' }
      },
      {
        path: 'fees',
        name: 'Fee',
        component: () => import('@/views/Fee.vue'),
        meta: { title: '收费管理', icon: 'Wallet', roles: ['ADMIN', 'COMPANY'], group: '作业中心' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/Report.vue'),
        meta: { title: '收费报表', icon: 'TrendCharts', roles: ['ADMIN', 'COMPANY'], group: '作业中心' }
      },
      // —— 基础档案 ——
      {
        path: 'tractors',
        name: 'Tractor',
        component: () => import('@/views/Tractor.vue'),
        meta: { title: '拖车管理', icon: 'Van', roles: ['ADMIN', 'COMPANY'], group: '基础档案' }
      },
      {
        path: 'drivers',
        name: 'Driver',
        component: () => import('@/views/Driver.vue'),
        meta: { title: '司机管理', icon: 'User', roles: ['ADMIN', 'COMPANY'], group: '基础档案' }
      },
      {
        path: 'companies',
        name: 'Company',
        component: () => import('@/views/Company.vue'),
        meta: { title: '运输公司管理', icon: 'OfficeBuilding', roles: ['ADMIN'], group: '基础档案' }
      },
      // —— 系统管理 ——
      {
        path: 'users',
        name: 'User',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '系统用户管理', icon: 'Avatar', roles: ['ADMIN'], group: '系统管理' }
      },
      {
        path: 'audit-logs',
        name: 'AuditLog',
        component: () => import('@/views/AuditLog.vue'),
        meta: { title: '操作日志', icon: 'Memo', roles: ['ADMIN'], group: '系统管理' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：认证校验 + 角色校验
router.beforeEach((to, from, next) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 闸口预约与办单管理系统`
    : '集装箱码头闸口预约与办单管理系统'

  if (to.path === '/login') {
    return next()
  }

  const token = localStorage.getItem('gss_token')
  if (!token) {
    return next('/login')
  }

  // 角色校验：无权限时跳转到该用户第一个有权限的页面（避免固定 /dashboard 造成死循环）
  if (to.meta.roles) {
    const user = JSON.parse(localStorage.getItem('gss_user') || 'null')
    if (!user || !to.meta.roles.includes(user.role)) {
      return next(firstAccessiblePath(user?.role))
    }
  }
  next()
})

/**
 * 根据角色返回其第一个有权限访问的路径，作为无权限跳转的兜底目标。
 * 菜单单一数据源原则：直接从路由表读取 meta.roles 判断。
 */
function firstAccessiblePath(role) {
  const root = routes.find((r) => r.path === '/')
  const children = root?.children ?? []
  for (const child of children) {
    const roles = child.meta?.roles
    // 仅考虑有菜单标题的路由，与 Layout 菜单过滤逻辑保持一致
    if (child.meta?.title && (!roles || roles.includes(role))) {
      return '/' + child.path
    }
  }
  return '/login'
}

export default router
export { routes }
