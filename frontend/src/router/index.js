import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppShell from '../components/AppShell.vue'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/AuthView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('../views/AuthView.vue'), meta: { public: true } },
  {
    path: '/', component: AppShell, redirect: '/home', children: [
      { path: 'home', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'devices', name: 'devices', component: () => import('../views/DevicesView.vue') },
      { path: 'devices/:id', name: 'device-detail', component: () => import('../views/DeviceDetailView.vue') },
      { path: 'devices/:deviceId/tutorials/new', name: 'tutorial-new', component: () => import('../views/TutorialEditorView.vue'), meta: { admin: true } },
      { path: 'devices/:deviceId/tutorials/:id/edit', name: 'tutorial-edit', component: () => import('../views/TutorialEditorView.vue'), meta: { admin: true } },
      { path: 'devices/:deviceId/tutorials/:id', name: 'tutorial', component: () => import('../views/TutorialView.vue') },
      { path: 'wiki', name: 'wiki', component: () => import('../views/WikiView.vue') },
      { path: 'borrow/my', name: 'borrow-my', component: () => import('../views/BorrowMyView.vue') },
      { path: 'scan', name: 'scan', component: () => import('../views/ScanView.vue') },
      { path: 'admin/borrow', name: 'admin-borrow', component: () => import('../views/BorrowAdminView.vue'), meta: { admin: true } },
      { path: 'admin/devices', name: 'admin-devices', component: () => import('../views/admin/DevicesAdminView.vue'), meta: { admin: true } },
      { path: 'admin/categories', name: 'admin-categories', component: () => import('../views/admin/CategoriesView.vue'), meta: { admin: true } },
      { path: 'admin/registrations', name: 'admin-registrations', component: () => import('../views/admin/RegistrationsView.vue'), meta: { admin: true } },
      { path: 'admin/approvals', name: 'admin-approvals', component: () => import('../views/admin/ApprovalsView.vue'), meta: { admin: true } },
      { path: 'admin/users', name: 'admin-users', component: () => import('../views/admin/UsersView.vue'), meta: { superAdmin: true } },
      { path: 'account', name: 'account', component: () => import('../views/AccountView.vue') },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({ history: createWebHashHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) return auth.isAuthenticated ? { name: 'home' } : true
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (!auth.user) {
    try { await auth.refreshUser() } catch { auth.logout(); return { name: 'login' } }
  }
  if (to.meta.superAdmin && !auth.isSuperAdmin) return { name: 'home' }
  if (to.meta.admin && !auth.isAdmin) return { name: 'home' }
  return true
})

window.addEventListener('labwaypoint:unauthorized', () => {
  const auth = useAuthStore()
  auth.logout()
  if (router.currentRoute.value.name !== 'login') router.replace({ name: 'login' })
})

export default router
