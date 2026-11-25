import { createRouter, createWebHistory } from 'vue-router'

// 路由与后端菜单 path 一一对应；角色越权由后端接口再校验一次
const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'covers', name: 'covers', component: () => import('@/views/CoverList.vue'), meta: { title: '井盖台账' } },
      { path: 'routes', name: 'routes', component: () => import('@/views/RouteList.vue'), meta: { title: '巡检路线' } },
      { path: 'tasks', name: 'tasks', component: () => import('@/views/TaskList.vue'), meta: { title: '巡检任务' } },
      { path: 'issues', name: 'issues', component: () => import('@/views/IssueList.vue'), meta: { title: '问题上报' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/OrderList.vue'), meta: { title: '维修工单' } },
      { path: 'acceptances', name: 'acceptances', component: () => import('@/views/AcceptanceList.vue'), meta: { title: '验收记录' } },
      { path: 'parts', name: 'parts', component: () => import('@/views/PartList.vue'), meta: { title: '备件库存' } },
      { path: 'risk/rules', name: 'riskRules', component: () => import('@/views/RiskRule.vue'), meta: { title: '规则配置' } },
      { path: 'risk/scores', name: 'riskScores', component: () => import('@/views/RiskScore.vue'), meta: { title: '计算记录' } },
      { path: 'analysis', name: 'analysis', component: () => import('@/views/Analysis.vue'), meta: { title: '统计分析' } },
      { path: 'messages', name: 'messages', component: () => import('@/views/MessageList.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人中心' } },
      // 巡检员端
      { path: 'my/tasks', name: 'myTasks', component: () => import('@/views/MyTask.vue'), meta: { title: '我的任务' } },
      { path: 'my/report', name: 'myReport', component: () => import('@/views/PatrolReport.vue'), meta: { title: '巡检上报' } },
      // 维修工端
      { path: 'my/orders', name: 'myOrders', component: () => import('@/views/MyOrder.vue'), meta: { title: '我的工单' } },
      // 系统管理
      { path: 'system/users', name: 'sysUsers', component: () => import('@/views/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'system/roles', name: 'sysRoles', component: () => import('@/views/RoleList.vue'), meta: { title: '角色管理' } },
      { path: 'system/menus', name: 'sysMenus', component: () => import('@/views/MenuList.vue'), meta: { title: '菜单管理' } },
      { path: 'system/dicts', name: 'sysDicts', component: () => import('@/views/DictList.vue'), meta: { title: '字典管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('@/views/NotFound.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('mh_token')
  if (!to.meta?.public && !token) return { path: '/login', query: { redirect: to.fullPath } }
  if (to.path === '/login' && token) return { path: '/dashboard' }
  return true
})

export default router
