<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, assetUrl } from '../lib/api'
import { formatDate, statusTone } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import DeviceQrCode from '../components/DeviceQrCode.vue'

const route = useRoute()
const auth = useAuthStore()
const device = ref(null)
const instructions = ref([])
const activeBorrow = ref(null)
const loading = ref(true)
const error = ref('')
const imageFailed = ref(false)
const borrowOpen = ref(false)
const submitting = ref(false)
const borrowError = ref('')
const borrowSuccess = ref('')
const borrowForm = ref({ purpose: '', expectedReturnTime: '', remark: '' })
const canBorrow = computed(() => device.value?.status === '在库' && !activeBorrow.value)

async function load() {
  loading.value = true
  error.value = ''
  imageFailed.value = false
  try {
    device.value = await api.get(`/device/${route.params.id}`)
    const [tutorialResult, borrowResult] = await Promise.allSettled([
      api.get(`/instruction?deviceId=${encodeURIComponent(route.params.id)}`),
      api.get(`/borrow/device/${encodeURIComponent(route.params.id)}/active`),
    ])
    instructions.value = tutorialResult.status === 'fulfilled' && Array.isArray(tutorialResult.value) ? tutorialResult.value : []
    activeBorrow.value = borrowResult.status === 'fulfilled' ? borrowResult.value : null
  } catch (err) { error.value = err.message } finally { loading.value = false }
}

async function applyBorrow() {
  borrowError.value = ''
  borrowSuccess.value = ''
  if (borrowForm.value.purpose.trim().length < 2 || borrowForm.value.purpose.trim().length > 500) { borrowError.value = '用途需要填写 2–500 个字符。'; return }
  if (!borrowForm.value.expectedReturnTime || new Date(borrowForm.value.expectedReturnTime).getTime() <= Date.now()) { borrowError.value = '请选择未来的预计归还时间。'; return }
  submitting.value = true
  try {
    await api.post('/borrow', { deviceId: Number(route.params.id), purpose: borrowForm.value.purpose.trim(), expectedReturnTime: borrowForm.value.expectedReturnTime, remark: borrowForm.value.remark.trim() })
    borrowSuccess.value = '借用申请已提交，请等待管理员审核。'
    borrowOpen.value = false
    borrowForm.value = { purpose: '', expectedReturnTime: '', remark: '' }
    await load()
  } catch (err) { borrowError.value = err.message } finally { submitting.value = false }
}
watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div>
    <RouterLink class="back-link" to="/devices">← 返回设备中心</RouterLink>
    <div v-if="loading" class="loading-line" role="status" aria-label="正在加载设备详情"></div>
    <div v-if="error" class="error-note">{{ error }} <button class="inline-retry" type="button" @click="load">重试</button></div>
    <template v-if="device && !loading && !error">
      <div class="detail-header"><div><div class="eyebrow">01 / EQUIPMENT RECORD · #{{ device.id }}</div><h1>{{ device.name }}</h1><div class="detail-sub"><span class="status-pill" :class="statusTone(device.status)">{{ device.status || '未知状态' }}</span><span>{{ device.model || '型号未填写' }}</span><span class="mono">{{ device.serialNo || '无编号' }}</span></div></div><span class="detail-mark" aria-hidden="true">↗</span></div>
      <div class="detail-grid"><div class="detail-photo"><img v-if="device.imageUrl && !imageFailed" :src="assetUrl(device.imageUrl)" :alt="`${device.name}照片`" @error="imageFailed = true" /><div v-else class="photo-placeholder" aria-label="设备照片占位"><span></span></div><span class="detail-photo-label">LABWAYPOINT / EQUIPMENT 0{{ device.id }}</span></div><div class="panel detail-info"><div class="mini-label">ARCHIVE / 设备档案</div><div class="data-list"><div class="data-line"><span>设备型号</span><b>{{ device.model || '—' }}</b></div><div class="data-line"><span>设备编号</span><b class="mono">{{ device.serialNo || '—' }}</b></div><div class="data-line"><span>存放位置</span><b>{{ device.location || '—' }}</b></div><div class="data-line"><span>购置日期</span><b>{{ device.purchaseDate || '—' }}</b></div><div class="data-line"><span>归档时间</span><b>{{ formatDate(device.createdAt) }}</b></div></div><div class="detail-actions"><button v-if="canBorrow" class="btn btn-primary" type="button" @click="borrowOpen = !borrowOpen">申请借用 ↗</button><RouterLink v-if="auth.isAdmin" class="btn" to="/admin/devices">管理设备 ↗</RouterLink><RouterLink class="btn btn-quiet" to="/borrow/my">我的借还记录</RouterLink></div><div v-if="activeBorrow" class="hint">这台设备当前有进行中的借用记录，暂不可再次申请。</div></div></div>
      <div v-if="borrowSuccess" class="success-note after-detail" role="status">{{ borrowSuccess }}</div>
      <section v-if="borrowOpen" class="panel borrow-form"><div class="section-label">借用申请 <span>/ REQUEST TO BORROW</span></div><p class="hint">提交后需要管理员审核。请填写用途和预计归还时间。</p><div v-if="borrowError" class="error-note" role="alert">{{ borrowError }}</div><form @submit.prevent="applyBorrow"><div class="form-grid"><label class="field"><span>借用用途</span><textarea v-model="borrowForm.purpose" required minlength="2" maxlength="500" placeholder="说明使用这台设备的任务" /></label><div class="stack"><label class="field"><span>预计归还时间</span><input v-model="borrowForm.expectedReturnTime" type="datetime-local" required /></label><label class="field"><span>备注（可选）</span><input v-model="borrowForm.remark" maxlength="500" placeholder="补充说明" /></label></div></div><div class="form-actions"><button class="btn btn-primary" :disabled="submitting" type="submit">{{ submitting ? '提交中…' : '提交申请' }}</button><button class="btn" type="button" @click="borrowOpen = false">取消</button></div></form></section>
      <section class="panel description-panel"><span class="mini-label">DESCRIPTION / 设备说明</span><p>{{ device.description || '暂无设备说明。' }}</p></section>
      <div class="detail-bottom"><section class="panel tutorial-panel"><div class="section-heading"><div><span class="mini-label">INSTRUCTIONS / 使用说明</span><h2>操作与教程</h2></div><RouterLink v-if="auth.isAdmin" class="btn btn-small" :to="`/devices/${device.id}/tutorials/new`">编写教程 ↗</RouterLink></div><div v-if="!instructions.length" class="empty-state"><strong>暂无教程</strong>这台设备的使用说明正在整理中。</div><RouterLink v-for="(tutorial, index) in instructions" :key="tutorial.id" class="tutorial-link" :to="`/devices/${device.id}/tutorials/${tutorial.id}`"><span class="tutorial-number">{{ String(index + 1).padStart(2, '0') }}</span><div><b>{{ tutorial.title }}</b><small>{{ tutorial.authorName || '实验室成员' }} · {{ formatDate(tutorial.updatedAt || tutorial.createdAt) }}</small></div><span aria-hidden="true">↗</span></RouterLink></section><section class="panel qr-panel"><span class="mini-label">QUICK ACCESS / 快速访问</span><h2>设备二维码</h2><DeviceQrCode :device-id="device.id" /><RouterLink class="btn btn-small" to="/scan">打开扫码查询 ↗</RouterLink></section></div>
    </template>
  </div>
</template>

<style scoped>
.back-link { display: inline-block; margin-bottom: 21px; font-size: 12px; font-weight: 900; color: var(--deep-red); }
.detail-header { display: flex; justify-content: space-between; align-items: end; gap: 20px; border-bottom: 3px solid var(--ink); padding-bottom: 19px; margin-bottom: 20px; }
.detail-header h1 { font: 900 clamp(34px, 4.3vw, 62px)/1.1 "Arial Black", Impact, "Microsoft YaHei", sans-serif; letter-spacing: -.06em; margin: 7px 0 15px; }
.detail-sub { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; font-size: 13px; font-weight: 800; }
.detail-mark { font: 900 95px/1 Impact, sans-serif; color: var(--red); }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(310px, .85fr); gap: 19px; }
.detail-photo { min-height: 360px; border: 2px solid var(--ink); position: relative; background: var(--muted); overflow: hidden; }
.detail-photo > img, .detail-photo .photo-placeholder { width: 100%; height: 100%; position: absolute; inset: 0; object-fit: cover; }
.detail-photo-label { position: absolute; bottom: 0; left: 0; background: var(--ink); color: var(--surface); padding: 8px 13px; font-size: 10px; letter-spacing: .11em; font-weight: 900; }
.detail-info { display: flex; flex-direction: column; }
.detail-info .data-list { margin-top: 15px; }
.detail-actions { display: flex; flex-wrap: wrap; gap: 9px; margin-top: auto; padding-top: 24px; }
.detail-info .hint { margin-top: 10px; }
.after-detail, .borrow-form, .description-panel { margin-top: 18px; }
.borrow-form .section-label { margin-top: 0; }
.borrow-form .error-note { margin: 10px 0; }
.description-panel p { margin: 10px 0 0; line-height: 1.8; white-space: pre-wrap; font-size: 14px; }
.detail-bottom { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(260px, .5fr); gap: 18px; margin-top: 18px; }
.section-heading { display: flex; justify-content: space-between; gap: 12px; align-items: end; border-bottom: 2px solid var(--ink); padding-bottom: 12px; margin-bottom: 12px; }
.section-heading h2, .qr-panel h2 { margin: 6px 0 0; font-size: 22px; }
.tutorial-link { display: grid; grid-template-columns: 36px 1fr 15px; align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid var(--line); }
.tutorial-link:last-child { border: 0; }
.tutorial-link:hover b { color: var(--red); }
.tutorial-number { font: 900 23px Impact, sans-serif; color: var(--red); }
.tutorial-link b { display: block; font-size: 14px; }
.tutorial-link small { display: block; margin-top: 4px; font-size: 11px; color: var(--soft); }
.qr-panel { display: flex; flex-direction: column; align-items: start; gap: 11px; }
.qr-panel h2 { margin: 0; }
.qr-frame { width: 180px; height: 180px; border: 2px solid var(--ink); padding: 9px; background: white; }
.qr-frame img { width: 100%; height: 100%; object-fit: contain; }
.qr-empty { display: grid; place-items: center; text-align: center; height: 145px; width: 100%; border: 2px dashed var(--line); color: var(--soft); font-size: 13px; }
.inline-retry { background: none; border: 0; font-weight: 900; color: var(--deep-red); text-decoration: underline; }
@media (max-width: 1100px) { .detail-grid, .detail-bottom { grid-template-columns: 1fr; } .detail-photo { min-height: 300px; } }
@media (max-width: 580px) { .detail-mark { display: none; } .detail-photo { min-height: 230px; } .detail-actions .btn { width: 100%; } }
</style>
