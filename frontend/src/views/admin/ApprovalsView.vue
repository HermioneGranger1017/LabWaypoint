<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../../lib/api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const activeTab = ref('inbox')
const inbox = ref([])
const outbox = ref([])
const loading = ref(false)
const busyId = ref(null)
const error = ref('')
const notice = ref('')
const filterStatus = ref('')
const canReview = computed(() => auth.isAdmin)
const currentRows = computed(() => (activeTab.value === 'inbox' ? inbox.value : outbox.value).filter((item) => !filterStatus.value || item.status === filterStatus.value))
function messageOf(err) { return err?.message || '操作失败，请稍后重试。' }
async function load() {
  loading.value = true; error.value = ''
  const [received, sent] = await Promise.allSettled([api.get('/edit-approval/inbox'), api.get('/edit-approval/outbox')])
  if (received.status === 'fulfilled') inbox.value = Array.isArray(received.value) ? received.value : []
  if (sent.status === 'fulfilled') outbox.value = Array.isArray(sent.value) ? sent.value : []
  const failures = [received, sent].filter((item) => item.status === 'rejected').map((item) => messageOf(item.reason))
  if (failures.length) error.value = failures.join(' ')
  loading.value = false
}
async function decide(item, status) {
  const action = status === 'approved' ? '批准' : '拒绝'
  if (!window.confirm(`确定${action}这条编辑授权申请吗？`)) return
  busyId.value = item.id; error.value = ''; notice.value = ''
  try { await api.put(`/edit-approval/${item.id}`, { status }); notice.value = `申请已${status === 'approved' ? '批准' : '拒绝'}。`; await load() }
  catch (err) { error.value = messageOf(err) }
  finally { busyId.value = null }
}
function labelTarget(item) { return item.targetType === 'device' ? '设备' : item.targetType === 'instruction' ? '教程' : '未知对象' }
function labelStatus(value) { return ({ pending: '待审核', approved: '已批准', rejected: '已拒绝' })[value] || value || '状态未知' }
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <header class="page-head"><div><p class="eyebrow">04 / SYSTEM & REVIEW</p><h1 class="page-title">编辑审核</h1><p class="intro">分别查看收到的授权申请与自己发起的申请。批准后授权规则由服务端执行。</p></div><button class="btn" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新记录' }}</button></header>
    <p v-if="!canReview" class="notice error">编辑授权申请仅对管理员开放。</p>
    <template v-else>
      <p v-if="notice" class="notice success" role="status">{{ notice }}</p><p v-if="error" class="notice error" role="alert">{{ error }} <button class="text-button" @click="load">重试读取</button></p>
      <nav class="tabs" aria-label="编辑申请列表"><button :class="{ selected: activeTab === 'inbox' }" @click="activeTab = 'inbox'">待我审核 <span>{{ inbox.length }}</span></button><button :class="{ selected: activeTab === 'outbox' }" @click="activeTab = 'outbox'">我发起的 <span>{{ outbox.length }}</span></button></nav>
      <div class="toolbar"><label>审核状态<select v-model="filterStatus" class="input"><option value="">全部状态</option><option value="pending">待审核</option><option value="approved">已批准</option><option value="rejected">已拒绝</option></select></label><span>{{ activeTab === 'inbox' ? '由内容所有者处理收到的申请' : '你提交的授权申请记录' }}</span></div>
      <div v-if="loading && !inbox.length && !outbox.length" class="panel state">正在读取编辑授权记录…</div>
      <div v-else-if="!error && !currentRows.length" class="panel state"><strong>{{ activeTab === 'inbox' ? '没有收到的申请' : '还没有发起申请' }}</strong><span>申请记录会显示目标对象、申请人、理由和当前状态。</span></div>
      <div v-else class="approval-list"><article v-for="item in currentRows" :key="item.id" class="panel approval-card"><header class="card-head"><div><p class="eyebrow">{{ labelTarget(item) }} / {{ String(item.targetId || item.deviceId || item.instructionId || '—').padStart(3, '0') }}</p><h2>{{ item.targetTitle || `${labelTarget(item)} #${item.targetId || item.deviceId || item.instructionId || '—'}` }}</h2></div><span class="state-tag" :class="`status-${item.status}`">{{ labelStatus(item.status) }}</span></header><dl class="details"><div><dt>申请人</dt><dd>{{ item.applicantName || `用户 #${item.applicantId ?? '—'}` }}</dd></div><div><dt>内容所有者</dt><dd>{{ item.ownerName || `用户 #${item.ownerId ?? '—'}` }}</dd></div><div><dt>提交时间</dt><dd>{{ item.createdAt || '—' }}</dd></div><div v-if="item.deviceId"><dt>关联设备</dt><dd>#{{ item.deviceId }}</dd></div><div class="wide"><dt>申请理由</dt><dd class="reason">{{ item.reason || '未提供理由' }}</dd></div></dl><footer v-if="activeTab === 'inbox' && item.status === 'pending'" class="actions"><button class="btn btn-primary" :disabled="busyId === item.id" @click="decide(item, 'approved')">批准一次性编辑</button><button class="btn danger-button" :disabled="busyId === item.id" @click="decide(item, 'rejected')">拒绝申请</button></footer></article></div>
    </template>
  </section>
</template>

<style scoped>
.admin-page{--red:#d5261e;--ink:#171717;--paper:#f4eedf;color:var(--ink);max-width:1080px;margin:auto;padding:28px 24px 56px}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:24px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:var(--red);margin:0 0 8px}.page-title{font-size:clamp(30px,4vw,44px);line-height:1;margin:0;font-weight:900}.intro{color:#625d54;margin:12px 0 0}.panel{background:var(--paper);border:2px solid var(--ink);box-shadow:4px 4px 0 var(--red)}.tabs{display:flex;border-bottom:3px solid var(--ink);gap:4px}.tabs button{min-height:48px;border:2px solid var(--ink);border-bottom:0;border-radius:0;background:#e5dece;padding:8px 16px;font:inherit;font-weight:800;cursor:pointer}.tabs button.selected{background:var(--red);color:white}.tabs span{display:inline-block;padding-left:8px}.toolbar{display:flex;justify-content:space-between;align-items:end;gap:12px;padding:14px 0}.toolbar label{display:grid;gap:6px;font-weight:800}.input{min-width:180px;min-height:44px;border:2px solid var(--ink);border-radius:0;background:#fffaf0;padding:8px;font:inherit}.approval-list{display:grid;gap:16px}.approval-card{padding:18px}.card-head{display:flex;justify-content:space-between;align-items:center;gap:12px}.card-head h2{font-size:22px;margin:0}.state-tag{padding:5px 9px;border:2px solid var(--ink);background:#e5ecd9;font-weight:800;white-space:nowrap}.status-pending{background:#f9d9a5}.status-rejected{background:#ffe1da}.details{display:grid;grid-template-columns:1fr 1fr;gap:12px 20px;margin:20px 0}.details dt{font-size:12px;font-weight:800;color:#625d54;letter-spacing:.05em}.details dd{margin:3px 0 0;overflow-wrap:anywhere}.wide{grid-column:1/-1}.reason{padding:10px;background:#fffaf0;border-left:4px solid var(--red);white-space:pre-wrap}.actions{display:flex;gap:10px;border-top:1px solid #aaa18f;padding-top:14px}.btn{min-height:44px;border:2px solid var(--ink);border-radius:0;background:var(--paper);font-weight:800;padding:8px 14px;cursor:pointer;color:var(--ink)}.btn-primary{background:var(--red);color:white;box-shadow:3px 3px 0 var(--ink)}.danger-button{background:#ffe1da}.btn:focus-visible,.tabs button:focus-visible{outline:3px solid var(--ink);outline-offset:2px}.btn:disabled{opacity:.6}.state{text-align:center;padding:30px;display:grid;gap:8px}.notice{padding:12px 14px;border:2px solid var(--ink);margin:12px 0}.success{background:#e5ecd9}.error{background:#ffe1da}.text-button{border:0;background:none;text-decoration:underline;font-weight:800;cursor:pointer}@media(max-width:620px){.admin-page{padding:22px 14px 42px}.page-head{align-items:flex-start;flex-direction:column}.tabs button{flex:1;padding:8px}.toolbar{align-items:stretch;flex-direction:column}.details{grid-template-columns:1fr}.wide{grid-column:auto}.actions{flex-direction:column}.actions .btn{width:100%}}
</style>
