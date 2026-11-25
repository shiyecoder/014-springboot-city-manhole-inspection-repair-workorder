<template>
  <div class="layout" :style="{ '--role-color': roleColor }">
    <!-- 后台形态：侧边栏 -->
    <aside v-if="!isPortal" class="sidebar">
      <div class="brand">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.6">
          <circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4" stroke-dasharray="2 2" />
        </svg>
        <span>井盖巡检管理</span>
      </div>
      <div class="role-label">{{ roleName }} 端</div>
      <nav>
        <router-link v-for="m in menus" :key="m.id" :to="m.path" :class="{ active: isActive(m.path) }">
          <el-icon><component :is="iconOf(m.icon)" /></el-icon>
          <span>{{ m.name }}</span>
        </router-link>
      </nav>
      <div class="side-foot">v1.0 · {{ user?.roleName }}</div>
    </aside>

    <div class="main">
      <!-- 门户形态：顶栏导航 -->
      <header v-if="isPortal" class="topbar portal">
        <div class="portal-brand">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="1.6">
            <circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4" stroke-dasharray="2 2" />
          </svg>
          井盖巡检 · {{ roleName }}端
        </div>
        <nav class="portal-nav">
          <router-link v-for="m in menus" :key="m.id" :to="m.path" :class="{ active: isActive(m.path) }">
            <el-icon><component :is="iconOf(m.icon)" /></el-icon>{{ m.name }}
          </router-link>
        </nav>
        <span class="grow" />
        <MessageBell />
        <UserMenu />
      </header>

      <header v-else class="topbar">
        <el-icon class="collapse" @click="collapsed = !collapsed"><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        <span class="crumb">{{ user?.deptName || '' }} / <b>{{ currentTitle }}</b></span>
        <span class="grow" />
        <MessageBell />
        <UserMenu />
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <keep-alive :max="6"><component :is="Component" /></keep-alive>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElBadge, ElDropdown, ElDropdownItem, ElDropdownMenu, ElIcon, ElMessage } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import * as Icons from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getMe, unreadCount } from '@/api'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const unread = ref(0)

const user = computed(() => store.user)
const menus = computed(() => (store.menus || []).flatMap((m) => (m.children && m.children.length ? m.children.filter(c => c.path) : [m])).filter((m) => m.path))
const isPortal = computed(() => store.isPortal)
const roleName = computed(() => user.value?.roleName || '')
const currentTitle = computed(() => menus.value.find((m) => isActive(m.path))?.name || route.meta?.title || '')

const ROLE_COLOR = { ADMIN: '#234E3B', DISPATCHER: '#1F4A52', ACCEPTOR: '#2B5140', INSPECTOR: '#3A7D5A', REPAIRER: '#4A5B3C' }
const roleColor = computed(() => ROLE_COLOR[user.value?.roleCode] || '#234E3B')

const ICONS = { dashboard: 'Odometer', cover: 'Aim', route: 'Guide', task: 'Calendar', alert: 'Warning', order: 'Document', check: 'CircleCheck', box: 'Box', rule: 'Operation', chart: 'TrendCharts', msg: 'ChatDotRound', setting: 'Setting', user: 'User', menu: 'Menu', tool: 'Tools', upload: 'Upload' }
function iconOf(name) {
  const key = ICONS[name] || 'Grid'
  return Icons[key] || Icons.Grid
}

const isActive = (path) => route.path === path || (path !== '/' && route.path.startsWith(path + '/'))

// 顶栏消息铃铛
const MessageBell = {
  setup() {
    onMounted(async () => {
      try {
        const res = await unreadCount()
        unread.value = res.data || 0
      } catch (e) { /* 忽略未读数失败 */ }
    })
    return () =>
      h(ElBadge, { value: unread.value, hidden: !unread.value, max: 99, class: 'bell' }, () =>
        h(ElIcon, { style: 'font-size:18px;cursor:pointer', onClick: () => router.push('/messages') }, () => h(Icons.ChatDotRound))
      )
  }
}

// 顶栏用户菜单
const UserMenu = {
  setup() {
    const onCommand = (cmd) => {
      if (cmd === 'profile') router.push('/profile')
      if (cmd === 'logout') {
        store.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      }
    }
    return () =>
      h(ElDropdown, { onCommand }, () => [
        h('div', { class: 'avatar' }, [
          h('img', {
            src: user.value?.avatar || '/avatar-default.svg',
            onError: (e) => { e.target.style.visibility = 'hidden' }
          }),
          h('span', {}, [
            (user.value?.realName || '未登录'),
            h('small', {}, user.value?.roleName || '')
          ])
        ]),
        h(ElDropdownMenu, {}, () => [
          h(ElDropdownItem, { command: 'profile' }, () => '个人中心'),
          h(ElDropdownItem, { command: 'logout', divided: true }, () => '退出登录')
        ])
      ])
  }
}

onMounted(async () => {
  if (!store.user) {
    try {
      await store.loadMe()
    } catch (e) { /* 拦截器已跳转登录 */ }
  }
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar {
  width: 220px; flex: 0 0 220px; background: var(--role-color); color: #DCE9E1;
  display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh;
}
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 18px; color: #fff; font-weight: 600; font-size: 15px; border-bottom: 1px solid rgba(255,255,255,.09); }
.role-label { margin: 12px 16px 4px; font-size: 11px; letter-spacing: 1px; color: rgba(255,255,255,.45); }
nav { padding: 4px 10px; overflow: auto; flex: 1; }
nav a {
  display: flex; align-items: center; gap: 10px; height: 40px; padding: 0 12px; border-radius: 6px;
  color: #CBDCD2; font-size: 13.5px; margin-bottom: 2px;
}
nav a:hover { background: rgba(255,255,255,.08); color: #fff; }
nav a.active { background: rgba(255,255,255,.15); color: #fff; font-weight: 500; box-shadow: inset 3px 0 0 #8FC7A8; }
.side-foot { padding: 12px 18px; font-size: 11px; color: rgba(255,255,255,.38); border-top: 1px solid rgba(255,255,255,.09); }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid var(--line); display: flex; align-items: center; gap: 14px; padding: 0 20px; position: sticky; top: 0; z-index: 20; }
.topbar.portal { background: var(--role-color); border-bottom: none; color: #fff; gap: 20px; }
.portal-brand { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 14px; }
.portal-nav { display: flex; gap: 6px; }
.portal-nav a { height: 34px; padding: 0 14px; border-radius: 6px; color: rgba(255,255,255,.82); font-size: 13.5px; display: inline-flex; align-items: center; gap: 7px; }
.portal-nav a:hover { background: rgba(255,255,255,.12); color: #fff; }
.portal-nav a.active { background: rgba(255,255,255,.2); color: #fff; font-weight: 500; }
.collapse { font-size: 18px; cursor: pointer; color: var(--text-2); }
.crumb { font-size: 12.5px; color: var(--text-3); }
.crumb b { color: var(--text); font-weight: 500; }
.grow { flex: 1; }
.avatar { display: flex; align-items: center; gap: 8px; cursor: pointer; color: inherit; }
.avatar img { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
.avatar span { font-size: 13px; line-height: 15px; }
.avatar small { display: block; font-size: 11px; opacity: .7; }
.content { flex: 1; background: var(--bg); min-height: calc(100vh - 56px); }
:deep(.bell) { margin-right: 4px; }
:deep(.topbar.portal .avatar img) { border-color: rgba(255,255,255,.35); }
</style>
