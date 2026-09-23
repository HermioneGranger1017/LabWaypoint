<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../lib/api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const rows = ref([])
const loading = ref(false)
const busyId = ref(null)
const error = ref('')
const notice = ref('')
const statusFilter = ref('待审核')
const canReview = computed(() => auth.isAdmin)
const visibleRows = computed(() => rows.value.filter((item) => !statusFilter.value || item.registrationStatus === statusFilter.value))
function messageOf(err) { return err?.message || '操作失败，请稍后重试。' }
async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.get('/user/registration/list')
    rows.value = Array.isArray(data) ? data : []
  } catch (err) { error.value = messageOf(err) }
  finally { loading.value = false }
}
async function approve(item) {
  if (!window.confirm(`批准账号“${item.username}”的注册申请？`)) return
  busyId.value = item.id; error.value = ''; notice.value = ''
  try { await api.put(`/user/registration/${item.id}/approve`); notice.value = `已批准 ${item.username} 的注册申请。`; await load() }
  catch (err) { error.value = messageOf(err) }
  finally { busyId.value = null }
}
async function reject(item) {
  const reason = window.prompt('请输入拒绝原因（2–500 个字符）。')
  if (reason === null) return
  if (reason.trim().length < 2 || reason.trim().length > 500) { error.value = '拒绝原因需为 2–500 个字符。'; return }
  busyId.value = item.id; error.value = ''; notice.value = ''
  try { await api.put(`/user/registration/${item.id}/reject`, { reason: reason.trim() }); notice.value = `已拒绝 ${item.username} 的注册申请。`; await load() }
  catch (err) { error.value = messageOf(err) }
  finally { busyId.value = null }
}
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <header class="page-head"><div><p class="eyebrow">04 / SYSTEM & REVIEW</p><h1 class="page-title">注册审核</h1><p class="intro">核对申请者的账号、学号与专业，并记录审核决定。</p></div><button class="btn" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新申请' }}</button></header>
    <p v-if="!canReview" class="notice error">此页面仅供管理员审核。</p>
    <template v-else>
      <p v-if="notice" class="notice success" role="status">{{ notice }}</p><p v-if="error" class="notice error" role="alert">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
      <div class="toolbar panel"><label>申请状态<select v-model="statusFilter" class="input"><option value="待审核">待审核</option><option value="已通过">已通过</option><option value="已拒绝">已拒绝</option><option value="">全部状态</option></select></label><span>共 {{ visibleRows.length }} 条记录</span></div>
      <div v-if="loading && !rows.length" class="panel state">正在读取注册申请…</div>
      <div v-else-if="!error && !visibleRows.length" class="panel state"><strong>{{ rows.length ? '该状态下没有申请' : '当前没有注册申请' }}</strong><span>新的注册申请会出现在这里。</span></div>
      <div v-else class="application-list"><article v-for="item in visibleRows" :key="item.id" class="panel application-card"><header class="card-head"><div><p class="eyebrow">APPLICATION / {{ String(item.id).padStart(4, '0') }}</p><h2>{{ item.username }}</h2></div><span class="state-tag" :class="item.registrationStatus === '待审核' ? 'pending' : ''">{{ item.registrationStatus || '状态未知' }}</span></header><dl class="details"><div><dt>学号</dt><dd>{{ item.studentNo || '—' }}</dd></div><div><dt>专业</dt><dd>{{ item.major || '—' }}</dd></div><div><dt>提交时间</dt><dd>{{ item.createdAt || '—' }}</dd></div><div v-if="item.registrationReviewedAt"><dt>审核时间</dt><dd>{{ item.registrationReviewedAt }}</dd></div><div v-if="item.registrationRejectReason" class="wide"><dt>拒绝原因</dt><dd>{{ item.registrationRejectReason }}</dd></div></dl><footer v-if="item.registrationStatus === '待审核'" class="actions"><button class="btn btn-primary" :disabled="busyId === item.id" @click="approve(item)">{{ busyId === item.id ? '处理中…' : '批准申请' }}</button><button class="btn danger-button" :disabled="busyId === item.id" @click="reject(item)">拒绝并填写原因</button></footer></article></div>
    </template>
  </section>
</template>

<style scoped>
.admin-page{--red:#d5261e;--ink:#171717;--paper:#f4eedf;color:var(--ink);max-width:1050px;margin:auto;padding:28px 24px 56px}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:24px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:var(--red);margin:0 0 8px}.page-title{font-size:clamp(30px,4vw,44px);line-height:1;margin:0;font-weight:900}.intro{color:#625d54;margin:12px 0 0}.panel{background:var(--paper);border:2px solid var(--ink);box-shadow:4px 4px 0 var(--red)}.toolbar{padding:14px 16px;display:flex;justify-content:space-between;align-items:end;gap:14px;margin-bottom:18px}.toolbar label{display:grid;gap:6px;font-weight:800}.input{min-width:180px;min-height:44px;border:2px solid var(--ink);border-radius:0;background:#fffaf0;padding:8px;font:inherit}.application-list{display:grid;gap:16px}.application-card{padding:18px}.card-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.card-head h2{font-size:23px;margin:0}.details{display:grid;grid-template-columns:1fr 1fr;gap:12px 22px;margin:20px 0}.details div{min-width:0}.details dt{font-size:12px;font-weight:800;color:#625d54;text-transform:uppercase;letter-spacing:.06em}.details dd{margin:3px 0 0;overflow-wrap:anywhere}.wide{grid-column:1/-1}.state-tag{padding:5px 9px;border:2px solid var(--ink);font-weight:800;background:#e5ecd9}.state-tag.pending{background:#f9d9a5}.actions{display:flex;gap:10px;flex-wrap:wrap;border-top:1px solid #aaa18f;padding-top:14px}.btn{min-height:44px;border:2px solid var(--ink);border-radius:0;background:var(--paper);font-weight:800;padding:8px 14px;cursor:pointer;color:var(--ink)}.btn-primary{background:var(--red);color:white;box-shadow:3px 3px 0 var(--ink)}.danger-button{background:#ffe1da}.btn:focus-visible{outline:3px solid var(--ink);outline-offset:2px}.btn:disabled{opacity:.6}.state{text-align:center;padding:30px;display:grid;gap:8px}.notice{padding:12px 14px;border:2px solid var(--ink);margin:12px 0}.success{background:#e5ecd9}.error{background:#ffe1da}.text-button{border:0;background:none;text-decoration:underline;font-weight:800;cursor:pointer}@media(max-width:620px){.admin-page{padding:22px 14px 42px}.page-head{align-items:flex-start;flex-direction:column}.toolbar{align-items:stretch;flex-direction:column}.details{grid-template-columns:1fr}.wide{grid-column:auto}.actions .btn{flex:1}}
</style>
