<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../lib/api'

const records = ref([])
const devices = ref([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const notice = ref('')
const activeFilter = ref('全部')
const form = reactive({ deviceId: '', purpose: '', expectedReturnTime: '', remark: '' })
const returnRemarks = reactive({})
const returningId = ref(null)
const filters = ['全部', '待审核', '借用中', '逾期', '待归还确认', '已结束']
const visibleRecords = computed(() => records.value.filter((row) => activeFilter.value === '全部' || (activeFilter.value === '已结束' ? ['已归还', '已拒绝'].includes(row.status) : row.status === activeFilter.value)))
const availableDevices = computed(() => devices.value.filter((device) => device.status === '在库'))
const counts = computed(() => ({ waiting: records.value.filter((r) => r.status === '待审核').length, borrowing: records.value.filter((r) => ['借用中', '逾期'].includes(r.status)).length, returning: records.value.filter((r) => r.status === '待归还确认').length }))

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value).replace('T', ' ') : new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}
function stateClass(status) { return ({ 待审核: 'pending', 借用中: 'active', 逾期: 'danger', 待归还确认: 'pending', 已归还: 'done', 已拒绝: 'danger' })[status] || '' }
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [mine, deviceList] = await Promise.all([api.get('/borrow/my'), api.get('/device')])
    records.value = Array.isArray(mine) ? mine : []
    devices.value = Array.isArray(deviceList) ? deviceList : []
  } catch (e) { error.value = e.message || '借还记录加载失败。' }
  finally { loading.value = false }
}
async function apply() {
  notice.value = ''; error.value = ''
  if (!form.deviceId) { error.value = '请选择一台在库设备。'; return }
  if (form.purpose.trim().length < 2 || form.purpose.trim().length > 500) { error.value = '借用用途需填写 2–500 个字符。'; return }
  if (!form.expectedReturnTime || new Date(form.expectedReturnTime) <= new Date()) { error.value = '预计归还时间必须晚于现在。'; return }
  submitting.value = true
  try {
    await api.post('/borrow', { deviceId: Number(form.deviceId), purpose: form.purpose.trim(), expectedReturnTime: form.expectedReturnTime, remark: form.remark.trim() || null })
    form.deviceId = ''; form.purpose = ''; form.expectedReturnTime = ''; form.remark = ''
    notice.value = '申请已提交，等待管理员审核。'
    await load()
  } catch (e) { error.value = e.message || '提交申请失败。' }
  finally { submitting.value = false }
}
async function requestReturn(record) {
  if (!window.confirm(`确认申请归还“${record.deviceName || '该设备'}”吗？`)) return
  error.value = ''; notice.value = ''
  returningId.value = record.id
  try {
    const returnNote = (returnRemarks[record.id] || '').trim()
    const originalRemark = (record.remark || '').trim()
    const remark = returnNote
      ? [originalRemark, `归还说明：${returnNote}`].filter(Boolean).join('\n')
      : (record.remark ?? null)
    await api.post(`/borrow/${record.id}/return-request`, { remark })
    notice.value = '归还申请已提交，等待管理员验收。'
    await load()
  } catch (e) { error.value = e.message || '提交归还申请失败。' }
  finally { returningId.value = null }
}
onMounted(load)
</script>

<template>
  <main class="borrow-page">
    <header class="page-title"><div><p class="eyebrow">PERSONAL BORROWING RECORD</p><h1>我的借用</h1><p>查看申请、归还进度与历史记录。</p></div><button class="btn" :disabled="loading" @click="load">刷新记录</button></header>
    <p v-if="error" class="message error" role="alert">{{ error }}</p><p v-if="notice" class="message success" role="status">{{ notice }}</p>

    <section class="summary" aria-label="借还数量">
      <article class="panel metric"><span>待审核</span><strong>{{ counts.waiting }}</strong></article><article class="panel metric"><span>借用中 / 逾期</span><strong>{{ counts.borrowing }}</strong></article><article class="panel metric"><span>待归还确认</span><strong>{{ counts.returning }}</strong></article>
    </section>

    <section class="panel apply-panel">
      <div class="section-heading"><div><p class="eyebrow">NEW REQUEST</p><h2>提交借用申请</h2></div><span class="small-note">设备须处于在库状态</span></div>
      <form class="apply-form" @submit.prevent="apply">
        <label class="field">设备
          <select v-model="form.deviceId" class="input" required><option value="" disabled>选择设备</option><option v-for="device in availableDevices" :key="device.id" :value="device.id">{{ device.name }}{{ device.serialNo ? ` · ${device.serialNo}` : '' }}</option></select>
          <small v-if="!loading && availableDevices.length === 0">当前没有可申请的在库设备。</small>
        </label>
        <label class="field">预计归还时间<input v-model="form.expectedReturnTime" class="input" type="datetime-local" required></label>
        <label class="field wide">借用用途<textarea v-model="form.purpose" class="input" rows="3" minlength="2" maxlength="500" placeholder="说明实验或工作用途（2–500 字）" required></textarea></label>
        <label class="field wide">补充说明 <span class="optional">选填</span><textarea v-model="form.remark" class="input" rows="2" maxlength="500" placeholder="其他需要管理员了解的信息"></textarea></label>
        <div class="form-actions"><button class="btn btn-primary" type="submit" :disabled="submitting || loading || !availableDevices.length">{{ submitting ? '提交中…' : '提交申请' }}</button></div>
      </form>
    </section>

    <section class="panel records-panel">
      <div class="section-heading"><div><p class="eyebrow">YOUR RECORDS</p><h2>借还档案 <span class="count">{{ visibleRecords.length }}</span></h2></div></div>
      <nav class="filters" aria-label="按状态筛选"><button v-for="filter in filters" :key="filter" class="filter" :class="{ selected: activeFilter === filter }" @click="activeFilter = filter">{{ filter }}</button></nav>
      <div v-if="loading" class="empty">正在读取借还记录…</div>
      <div v-else-if="!visibleRecords.length" class="empty"><strong>{{ records.length ? '没有符合筛选条件的记录' : '暂无借还记录' }}</strong><span>{{ records.length ? '切换状态查看其他记录。' : '提交借用申请后，进度会显示在这里。' }}</span></div>
      <div v-else class="record-list">
        <article v-for="record in visibleRecords" :key="record.id" class="record">
          <div class="record-head"><div><h3>{{ record.deviceName || `设备 #${record.deviceId}` }}</h3><p class="muted">{{ record.deviceSerialNo || '未填写编号' }} · 申请于 {{ formatDate(record.createdAt || record.borrowTime) }}</p></div><span class="state" :class="stateClass(record.status)">{{ record.status }}</span></div>
          <dl class="details"><div><dt>借用用途</dt><dd>{{ record.purpose || '—' }}</dd></div><div><dt>预计归还</dt><dd>{{ formatDate(record.expectedReturnTime) }}</dd></div><div v-if="record.returnRequestedAt"><dt>归还申请</dt><dd>{{ formatDate(record.returnRequestedAt) }}</dd></div><div v-if="record.rejectReason"><dt>拒绝原因</dt><dd>{{ record.rejectReason }}</dd></div><div v-if="record.returnCheckNote"><dt>验收备注</dt><dd>{{ record.returnCheckNote }}</dd></div><div v-if="record.remark"><dt>借用备注 / 归还说明</dt><dd class="remark-text">{{ record.remark }}</dd></div></dl>
          <div v-if="['借用中', '逾期'].includes(record.status)" class="return-form"><label class="field">本次归还说明 <span class="optional">选填</span><textarea v-model="returnRemarks[record.id]" class="input" rows="2" maxlength="500" placeholder="说明归还时的情况；留空将保留原借用备注"></textarea></label><button class="btn btn-primary" :disabled="returningId === record.id" @click="requestReturn(record)">{{ returningId === record.id ? '提交中…' : '申请归还' }}</button></div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.borrow-page{display:grid;gap:22px;color:var(--color-text-primary,#171717)}.page-title{display:flex;justify-content:space-between;align-items:center;gap:16px}.page-title h1,.section-heading h2{margin:0;font-family:var(--font-display,inherit)}.page-title p:not(.eyebrow){margin:7px 0 0;color:var(--color-text-secondary,#3a3630)}.eyebrow{margin:0 0 5px;font-size:11px;font-weight:800;letter-spacing:.1em;color:var(--color-primary,#c82b27)}.panel{padding:20px;background:var(--color-surface,#f7f0e2);border:1px solid var(--color-ink,#171717);box-shadow:var(--shadow-panel,4px 4px 0 #171717)}.summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.metric{display:grid;gap:7px}.metric span,.small-note,.muted,.optional{color:var(--color-text-secondary,#3a3630);font-size:13px}.metric strong{font:800 28px var(--font-display,inherit)}.section-heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:18px}.section-heading h2{font-size:20px}.apply-form{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:15px}.field{display:grid;gap:7px;font-weight:700;font-size:14px}.field small{font-weight:400;color:var(--color-text-secondary,#3a3630)}.input{width:100%;box-sizing:border-box;padding:10px 11px;min-height:42px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-canvas,#f1e6cf);color:inherit;font:inherit}.input:focus{outline:3px solid #c82b274d;outline-offset:1px}.wide,.form-actions{grid-column:1/-1}.form-actions{display:flex;justify-content:flex-end}.btn{min-height:42px;padding:8px 14px;border:1px solid var(--color-ink,#171717);border-radius:0;background:var(--color-surface,#f7f0e2);color:inherit;font:700 14px inherit;cursor:pointer;box-shadow:3px 3px 0 var(--color-ink,#171717)}.btn:disabled{opacity:.55;cursor:not-allowed}.btn-primary{background:var(--color-primary,#c82b27);color:#fff}.filters{display:flex;gap:7px;overflow-x:auto;padding:2px 2px 8px}.filter{padding:8px 12px;border:1px solid var(--color-ink,#171717);background:transparent;color:inherit;white-space:nowrap;cursor:pointer}.filter.selected{background:var(--color-ink,#171717);color:var(--color-paper,#f7f0e2)}.count{display:inline-grid;place-items:center;min-width:24px;height:24px;margin-left:5px;background:var(--color-primary,#c82b27);color:white;font:700 13px sans-serif}.record-list{display:grid;gap:12px}.record{padding:16px;border:1px solid var(--color-border-soft,rgba(23,23,23,.22));background:#fff8e9}.record-head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px}.record h3{margin:0;font-size:17px}.muted{margin:5px 0 0}.state{display:inline-block;padding:4px 8px;border:1px solid currentColor;font-size:12px;font-weight:800;white-space:nowrap}.state.pending{color:#80520a;background:#ffefbd}.state.active{color:#245b38;background:#e1f0d9}.state.danger{color:#941f1c;background:#f8d8ce}.state.done{color:#394d51;background:#dce8e4}.details{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px 20px;margin:15px 0 0}.details div{min-width:0}.details dt{font-size:12px;color:var(--color-text-tertiary,#776c5c)}.details dd{margin:3px 0 0;overflow-wrap:anywhere}.record-actions{display:flex;justify-content:flex-end;margin-top:14px}.empty{display:grid;justify-items:center;gap:7px;padding:36px 12px;text-align:center;color:var(--color-text-secondary,#3a3630)}.empty strong{color:inherit}.message{margin:0;padding:11px 13px;border:1px solid currentColor}.message.error{color:#941f1c;background:#f8d8ce}.message.success{color:#245b38;background:#e1f0d9}@media(max-width:650px){.page-title{align-items:flex-start}.summary{grid-template-columns:1fr}.metric{grid-template-columns:1fr auto;align-items:center}.apply-form,.details{grid-template-columns:1fr}.wide,.form-actions{grid-column:auto}.panel{padding:15px}.section-heading{align-items:flex-start}.record-head{align-items:flex-start}.page-title .btn{flex-shrink:0}}
</style>

<style scoped>
.return-form{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:12px;align-items:end;margin-top:14px;padding-top:14px;border-top:1px solid var(--color-border-soft,rgba(23,23,23,.22))}
.remark-text{white-space:pre-line}
@media(max-width:650px){.return-form{grid-template-columns:1fr}}
</style>
