<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../lib/api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const user = computed(() => auth.user || {})
const submitting = ref(false)
const loadingProfile = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const roleLabels = { super_admin: '超级管理员', admin: '管理员', user: '普通用户' }
const profileFields = computed(() => [
  { label: '用户名', value: user.value.username },
  { label: '姓名', value: user.value.realName || user.value.nickname },
  { label: '学号', value: user.value.studentNo },
  { label: '专业', value: user.value.major },
  { label: '联系电话', value: user.value.phone },
  { label: '电子邮箱', value: user.value.email },
  { label: '账号角色', value: roleLabels[user.value.role] || user.value.role },
  { label: '注册状态', value: user.value.registrationStatus },
])

async function refreshProfile() {
  if (auth.user) return
  loadingProfile.value = true
  try { await auth.refreshUser() }
  catch (e) { error.value = e.message || '无法读取当前用户资料。' }
  finally { loadingProfile.value = false }
}

async function updatePassword() {
  error.value = ''
  notice.value = ''
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    error.value = '请填写旧密码、新密码和确认密码。'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '新密码与确认密码不一致。'
    return
  }
  submitting.value = true
  try {
    await api.patch('/user/updatePwd', {
      old_pwd: form.oldPassword,
      new_pwd: form.newPassword,
      re_pwd: form.confirmPassword,
    })
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    notice.value = '密码已更新。'
  } catch (e) {
    error.value = e.message || '密码更新失败，请重试。'
  } finally {
    submitting.value = false
  }
}

onMounted(refreshProfile)
</script>

<template>
  <main class="account-page">
    <header class="page-title"><div><p class="eyebrow">ACCOUNT RECORD</p><h1>账号资料</h1><p>查看当前账号信息，或更新登录密码。</p></div><span class="account-stamp">个人<br>档案</span></header>

    <p v-if="error" class="message error" role="alert">{{ error }}</p>
    <p v-if="notice" class="message success" role="status">{{ notice }}</p>

    <section class="panel profile-panel">
      <div class="section-heading"><div><p class="eyebrow">CURRENT USER</p><h2>账号信息</h2></div><span v-if="user.role" class="role-tag">{{ roleLabels[user.role] || user.role }}</span></div>
      <div v-if="loadingProfile" class="empty">正在读取账号资料…</div>
      <div v-else-if="!user.username" class="empty">当前没有可显示的用户资料，请重新登录后再试。</div>
      <dl v-else class="profile-grid"><div v-for="field in profileFields" :key="field.label"><dt>{{ field.label }}</dt><dd>{{ field.value || '未填写' }}</dd></div></dl>
    </section>

    <section class="panel password-panel">
      <div class="section-heading"><div><p class="eyebrow">PASSWORD SECURITY</p><h2>修改密码</h2></div></div>
      <p class="helper">输入当前密码后设置新密码。忘记旧密码时，请联系系统管理员。</p>
      <form class="password-form" @submit.prevent="updatePassword">
        <label class="field">旧密码<input v-model="form.oldPassword" class="input" type="password" autocomplete="current-password" required></label>
        <label class="field">新密码<input v-model="form.newPassword" class="input" type="password" autocomplete="new-password" required></label>
        <label class="field">确认新密码<input v-model="form.confirmPassword" class="input" type="password" autocomplete="new-password" required></label>
        <div class="form-actions"><button class="btn btn-primary" type="submit" :disabled="submitting">{{ submitting ? '保存中…' : '更新密码' }}</button></div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.account-page{display:grid;gap:22px;color:var(--color-text-primary,#171717)}.page-title{display:flex;align-items:center;justify-content:space-between;gap:16px}.page-title h1,.section-heading h2{margin:0;font-family:var(--font-display,inherit)}.page-title p:not(.eyebrow){margin:7px 0 0;color:var(--color-text-secondary,#3a3630)}.eyebrow{margin:0 0 5px;color:var(--color-primary,#c82b27);font-size:11px;font-weight:800;letter-spacing:.11em}.account-stamp{display:grid;place-items:center;width:54px;height:54px;transform:rotate(3deg);background:var(--color-primary,#c82b27);color:white;text-align:center;font-weight:800;line-height:1.05}.panel{padding:20px;border:1px solid var(--color-ink,#171717);background:var(--color-surface,#f7f0e2);box-shadow:var(--shadow-panel,4px 4px 0 #171717)}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:17px}.section-heading h2{font-size:20px}.role-tag{padding:5px 9px;border:1px solid var(--color-ink,#171717);background:var(--color-surface-muted,#e9d8b9);font-size:12px;font-weight:800}.profile-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 24px;margin:0}.profile-grid div{padding:13px 0;border-top:1px solid var(--color-border-soft,rgba(23,23,23,.22));min-width:0}.profile-grid dt{margin-bottom:5px;color:var(--color-text-tertiary,#776c5c);font-size:12px}.profile-grid dd{margin:0;overflow-wrap:anywhere;font-weight:650}.helper{margin:-7px 0 17px;color:var(--color-text-secondary,#3a3630);font-size:14px}.password-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:15px}.field{display:grid;gap:7px;font-size:14px;font-weight:700}.input{box-sizing:border-box;width:100%;min-height:42px;padding:10px 11px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-canvas,#f1e6cf);color:inherit;font:inherit}.input:focus{outline:3px solid #c82b274d;outline-offset:1px}.form-actions{grid-column:1/-1;display:flex;justify-content:flex-end}.btn{min-height:42px;padding:8px 14px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-surface,#f7f0e2);color:inherit;font-size:14px;font-weight:700;cursor:pointer;box-shadow:3px 3px 0 var(--color-ink,#171717)}.btn:disabled{opacity:.55;cursor:not-allowed}.btn-primary{background:var(--color-primary,#c82b27);color:white}.message{margin:0;padding:11px 13px;border:1px solid currentColor}.message.error{color:#941f1c;background:#f8d8ce}.message.success{color:#245b38;background:#e1f0d9}.empty{padding:22px 12px;text-align:center;color:var(--color-text-secondary,#3a3630)}@media(max-width:620px){.panel{padding:15px}.page-title{align-items:flex-start}.profile-grid,.password-form{grid-template-columns:1fr}.form-actions{grid-column:auto}}
</style>
