<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileOpen = ref(false)
watch(() => route.fullPath, () => { mobileOpen.value = false })
const roleLabel = computed(() => ({ super_admin: '超级管理员', admin: '管理员', user: '普通用户' })[auth.user?.role] || '成员')
const nav = computed(() => [
  { index: '00', label: '控制中心', to: '/home', caption: 'COMMAND' },
  { index: '01', label: '设备中心', to: '/devices', caption: 'EQUIPMENT' },
  { index: '02', label: '实验室 Wiki', to: '/wiki', caption: 'KNOWLEDGE' },
  { index: '03', label: '我的借用', to: '/borrow/my', caption: 'BORROWING' },
  { index: '03', label: '借还管理', to: '/admin/borrow', caption: 'REVIEW', admin: true },
  { index: '04', label: '设备维护', to: '/admin/devices', caption: 'MAINTENANCE', admin: true },
  { index: '04', label: '分类管理', to: '/admin/categories', caption: 'CATEGORIES', admin: true },
  { index: '04', label: '注册审核', to: '/admin/registrations', caption: 'REGISTRATION', admin: true },
  { index: '04', label: '编辑审核', to: '/admin/approvals', caption: 'APPROVALS', admin: true },
  { index: '04', label: '用户管理', to: '/admin/users', caption: 'USERS', superAdmin: true },
].filter(item => (!item.admin || auth.isAdmin) && (!item.superAdmin || auth.isSuperAdmin)))

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="app-frame">
    <div v-if="mobileOpen" class="drawer-scrim" @click="mobileOpen = false"></div>
    <aside class="app-sidebar" :class="{ 'is-open': mobileOpen }" aria-label="主导航">
      <div class="sidebar-brand">
        <div class="brand-symbol" aria-hidden="true"><i></i><i></i><i></i></div>
        <div><strong>LAB<br>WAYPOINT</strong><span>实验室工作平台</span></div>
      </div>
      <div class="sidebar-caption">NAVIGATION / 导航索引</div>
      <nav class="side-nav">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" class="nav-link" :class="{ active: route.path === item.to || (item.to === '/devices' && route.path.startsWith('/devices/')) }">
          <span class="nav-index">{{ item.index }}</span>
          <span class="nav-copy"><b>{{ item.label }}</b><small>{{ item.caption }}</small></span>
          <span class="nav-arrow" aria-hidden="true">↗</span>
        </RouterLink>
      </nav>
      <div class="sidebar-bottom">
        <RouterLink to="/scan" class="scan-link"><span aria-hidden="true">⌗</span> 扫码查询设备 <b>↗</b></RouterLink>
        <div class="sidebar-stamp">LABWAYPOINT <span>·</span> SYSTEM 01</div>
      </div>
    </aside>

    <div class="app-content">
      <header class="topbar">
        <button class="mobile-menu icon-button" type="button" aria-label="打开导航菜单" @click="mobileOpen = !mobileOpen">☰</button>
        <div class="topbar-kicker"><span class="signal-dot"></span> LABWAYPOINT / WORKSPACE <span class="topbar-slash">///</span> {{ route.meta.section || '实验室设备与知识工作台' }}</div>
        <div class="topbar-actions">
          <span class="role-badge">{{ roleLabel }}</span>
          <RouterLink class="user-chip" to="/account" :title="auth.user?.username || '个人设置'"><span class="user-avatar">{{ (auth.user?.username || 'U').slice(0, 1).toUpperCase() }}</span><span class="user-name">{{ auth.user?.username || '账号' }}</span></RouterLink>
          <button type="button" class="logout-button" @click="logout" aria-label="退出登录">退出 ↗</button>
        </div>
      </header>
      <main class="main-content"><RouterView /></main>
    </div>
  </div>
</template>
