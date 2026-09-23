import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api, request } from '../lib/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('labwaypoint_token') || '')
  const user = ref(null)
  const loading = ref(false)
  const isAuthenticated = computed(() => Boolean(token.value))
  const isAdmin = computed(() => ['admin', 'super_admin'].includes(user.value?.role))
  const isSuperAdmin = computed(() => user.value?.role === 'super_admin')

  async function login(username, password) {
    const body = new URLSearchParams({ username, password })
    const nextToken = await request('/user/login', { method: 'POST', body })
    token.value = nextToken
    localStorage.setItem('labwaypoint_token', nextToken)
    try {
      await refreshUser()
    } catch (error) {
      logout()
      throw error
    }
  }

  async function refreshUser() {
    if (!token.value) return null
    loading.value = true
    try {
      user.value = await api.get('/user/userInfo')
      return user.value
    } finally {
      loading.value = false
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('labwaypoint_token')
  }

  return { token, user, loading, isAuthenticated, isAdmin, isSuperAdmin, login, refreshUser, logout }
})
