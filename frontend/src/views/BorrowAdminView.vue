<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { api } from '../lib/api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)
const records = ref([])
const loading = ref(false)
const busyId = ref(null)
const error = ref('')
const notice = ref('')
const filter = ref('待处理')
const rejection = reactive({})
const checks = reactive({})
const visibleRecords = computed(() => records.value.filter((row) => filter.value === '全部' || (filter.value === '待处理' ? ['待审核', '待归还确认'].includes(row.status) : row.status === filter.value)))
const pendingCount = computed(() => records.value.filter((r) => ['待审核', '待归还确认'].includes(r.status)).length)

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value).replace('T', ' ') : new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}
function stateClass(status) { return ({ 待审核: 'pending', 借用中: 'active', 逾期: 'danger', 待归还确认: 'pending', 已归还: 'done', 已拒绝: 'danger' })[status] || '' }
async function load() {
  if (!isAdmin.value) return
  loading.value = true; error.value = ''
  try { const data = await api.get('/borrow/admin/list'); records.value = Array.isArray(data) ? data : []; for (const row of records.value) if (row.status === '待归还确认' && !checks[row.id]) checks[row.id] = { status: '在库', note: '' } }
  catch (e) { error.value = e.message || '借还队列加载失败。' }
  finally { loading.value = false }
}
async function act(record, action) {
  error.value = ''; notice.value = ''; busyId.value = record.id
  try {
    if (action === 'approve') await api.put(`/borrow/${record.id}/approve`)
    if (action === 'reject') {
      const reason = (rejection[record.id] || '').trim()
      if (reason.length < 2) { error.value = '请填写至少 2 个字符的拒绝原因。'; return }
      await api.put(`/borrow/${record.id}/reject`, { rejectReason: reason })
    }
    if (action === 'confirm') {
      const check = checks[record.id] || {}
      if (!['在库', '维修中', '报废'].includes(check.status)) { error.value = '请选择归还验收后的设备状态。'; return }
      await api.put(`/borrow/${record.id}/confirm-return`, { returnDeviceStatus: check.status, returnCheckNote: (check.note || '').trim() || null })
    }
    notice.value = action === 'approve' ? '借用申请已批准。' : action === 'reject' ? '借用申请已拒绝。' : '归还验收已完成。'
    await load()
  } catch (e) { error.value = e.message || '操作失败，请刷新后重试。' }
  finally { busyId.value = null }
}
watch(isAdmin, (allowed) => { if (allowed) load() }, { immediate: true })
</script>

<template>
  <main class="admin-page">
    <header class="page-title"><div><p class="eyebrow">BORROWING ADMINISTRATION</p><h1>借还管理</h1><p>审核借用申请，并记录归还验收结果。</p></div><button v-if="isAdmin" class="btn" :disabled="loading" @click="load">刷新队列</button></header>
    <p v-if="!isAdmin" class="message error" role="alert">此页面仅供管理员使用。</p>
    <template v-else>
      <p v-if="error" class="message error" role="alert">{{ error }}</p><p v-if="notice" class="message success" role="status">{{ notice }}</p>
      <section class="panel">
        <div class="heading"><div><p class="eyebrow">REVIEW QUEUE</p><h2>借还档案 <span class="count">{{ pendingCount }} 待处理</span></h2></div></div>
        <nav class="filters" aria-label="按状态筛选"><button v-for="item in ['待处理','待审核','待归还确认','借用中','逾期','已归还','已拒绝','全部']" :key="item" class="filter" :class="{ selected: filter === item }" @click="filter = item">{{ item }}</button></nav>
        <div v-if="loading" class="empty">正在读取借还队列…</div>
        <div v-else-if="!visibleRecords.length" class="empty"><strong>{{ records.length ? '没有符合筛选条件的记录' : '当前没有借还记录' }}</strong><span>{{ records.length ? '选择其他状态查看记录。' : '新申请和待验收归还会出现在这里。' }}</span></div>
        <div v-else class="record-list">
          <article v-for="record in visibleRecords" :key="record.id" class="record">
            <div class="record-head"><div><h3>{{ record.deviceName || `设备 #${record.deviceId}` }}</h3><p class="muted">{{ record.deviceSerialNo || '未填写编号' }} · 申请人：{{ record.borrowerName || record.applicantUsername || `用户 #${record.borrowerId}` }}</p></div><span class="state" :class="stateClass(record.status)">{{ record.status }}</span></div>
            <dl class="details"><div><dt>借用用途</dt><dd>{{ record.purpose || '—' }}</dd></div><div><dt>申请时间</dt><dd>{{ formatDate(record.createdAt || record.borrowTime) }}</dd></div><div><dt>预计归还</dt><dd>{{ formatDate(record.expectedReturnTime) }}</dd></div><div v-if="record.borrowerIdentity"><dt>身份信息</dt><dd>{{ record.borrowerIdentity }}</dd></div><div v-if="record.borrowerPhone"><dt>联系电话</dt><dd>{{ record.borrowerPhone }}</dd></div><div v-if="record.remark"><dt>申请备注</dt><dd>{{ record.remark }}</dd></div><div v-if="record.rejectReason"><dt>拒绝原因</dt><dd>{{ record.rejectReason }}</dd></div><div v-if="record.returnRequestedAt"><dt>归还申请时间</dt><dd>{{ formatDate(record.returnRequestedAt) }}</dd></div><div v-if="record.returnCheckNote"><dt>验收备注</dt><dd>{{ record.returnCheckNote }}</dd></div></dl>
            <div v-if="record.status === '待审核'" class="actions">
              <button class="btn btn-primary" :disabled="busyId === record.id" @click="act(record, 'approve')">{{ busyId === record.id ? '处理中…' : '批准借用' }}</button>
              <div class="reject-box"><label class="field">拒绝原因<textarea v-model="rejection[record.id]" class="input" rows="2" minlength="2" placeholder="说明拒绝原因（至少 2 字）"></textarea></label><button class="btn danger-btn" :disabled="busyId === record.id" @click="act(record, 'reject')">拒绝申请</button></div>
            </div>
            <div v-else-if="record.status === '待归还确认'" class="check-box">
              <p class="section-label">归还验收</p>
              <label class="field">设备最终状态<select v-model="checks[record.id].status" class="input"><option value="在库">在库，可继续借用</option><option value="维修中">维修中</option><option value="报废">报废</option></select></label>
              <label class="field">验收备注 <span class="optional">选填</span><textarea v-model="checks[record.id].note" class="input" rows="2" maxlength="500" placeholder="设备外观、配件或故障情况"></textarea></label>
              <button class="btn btn-primary" :disabled="busyId === record.id" @click="act(record, 'confirm')">{{ busyId === record.id ? '处理中…' : '完成归还验收' }}</button>
            </div>
          </article>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.admin-page{display:grid;gap:20px;color:var(--color-text-primary,#171717)}.page-title{display:flex;justify-content:space-between;align-items:center;gap:16px}.page-title h1,.heading h2{margin:0;font-family:var(--font-display,inherit)}.page-title p:not(.eyebrow){margin:7px 0 0;color:var(--color-text-secondary,#3a3630)}.eyebrow{margin:0 0 5px;font-size:11px;font-weight:800;letter-spacing:.1em;color:var(--color-primary,#c82b27)}.panel{padding:20px;background:var(--color-surface,#f7f0e2);border:1px solid var(--color-ink,#171717);box-shadow:var(--shadow-panel,4px 4px 0 #171717)}.heading{margin-bottom:16px}.heading h2{font-size:20px}.count{display:inline-block;margin-left:8px;padding:4px 8px;background:#f2d8c5;color:#79211c;font:700 12px sans-serif}.filters{display:flex;gap:7px;overflow-x:auto;padding:2px 2px 10px}.filter{padding:8px 11px;border:1px solid var(--color-ink,#171717);background:transparent;color:inherit;white-space:nowrap;cursor:pointer}.filter.selected{background:var(--color-ink,#171717);color:var(--color-paper,#f7f0e2)}.record-list{display:grid;gap:12px}.record{padding:16px;border:1px solid var(--color-border-soft,rgba(23,23,23,.22));background:#fff8e9}.record-head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px}.record h3{margin:0;font-size:17px}.muted{margin:5px 0 0;color:var(--color-text-secondary,#3a3630);font-size:13px}.state{display:inline-block;padding:4px 8px;border:1px solid currentColor;font-size:12px;font-weight:800;white-space:nowrap}.state.pending{color:#80520a;background:#ffefbd}.state.active{color:#245b38;background:#e1f0d9}.state.danger{color:#941f1c;background:#f8d8ce}.state.done{color:#394d51;background:#dce8e4}.details{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 20px;margin:15px 0}.details dt{font-size:12px;color:var(--color-text-tertiary,#776c5c)}.details dd{margin:3px 0 0;overflow-wrap:anywhere}.actions{display:grid;grid-template-columns:auto 1fr;gap:14px;align-items:end;padding-top:14px;border-top:1px solid var(--color-border-soft,rgba(23,23,23,.22))}.reject-box{display:grid;grid-template-columns:1fr auto;gap:10px;align-items:end}.check-box{display:grid;grid-template-columns:1fr 1fr auto;gap:12px;align-items:end;padding-top:14px;border-top:1px solid var(--color-border-soft,rgba(23,23,23,.22))}.section-label{grid-column:1/-1;margin:0;font-weight:800}.field{display:grid;gap:6px;font-size:13px;font-weight:700}.input{width:100%;box-sizing:border-box;padding:9px 10px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-canvas,#f1e6cf);color:inherit;font:inherit}.btn{min-height:42px;padding:8px 13px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-surface,#f7f0e2);color:inherit;font-weight:700;cursor:pointer;box-shadow:3px 3px 0 var(--color-ink,#171717);white-space:nowrap}.btn:disabled{opacity:.55;cursor:not-allowed}.btn-primary{background:var(--color-primary,#c82b27);color:#fff}.danger-btn{color:#941f1c}.empty{display:grid;justify-items:center;gap:7px;padding:36px 12px;text-align:center;color:var(--color-text-secondary,#3a3630)}.message{margin:0;padding:11px 13px;border:1px solid currentColor}.message.error{color:#941f1c;background:#f8d8ce}.message.success{color:#245b38;background:#e1f0d9}@media(max-width:720px){.panel{padding:15px}.page-title{align-items:flex-start}.details{grid-template-columns:1fr}.actions,.reject-box,.check-box{grid-template-columns:1fr}.section-label{grid-column:auto}.reject-box{align-items:stretch}}
</style>
