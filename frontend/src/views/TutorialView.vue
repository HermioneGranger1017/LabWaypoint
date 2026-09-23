<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import DOMPurify from 'dompurify'
import { api, assetUrl } from '../lib/api'
import { formatDate } from '../lib/format'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const item = ref(null)
const permission = ref(null)
const loading = ref(false)
const error = ref('')
const approvalOpen = ref(false)
const reason = ref('')
const busy = ref(false)
const notice = ref('')
const isOwner = computed(() => Number(item.value?.createdBy) === Number(auth.user?.id))
const mayEdit = computed(() => auth.isSuperAdmin || isOwner.value || permission.value?.status === 'edit_once')

function displayHtml(html) {
  const safe = DOMPurify.sanitize(String(html || ''), {
    ALLOWED_TAGS: ['p', 'br', 'h1', 'h2', 'h3', 'h4', 'strong', 'em', 'u', 'ul', 'ol', 'li', 'blockquote', 'pre', 'code', 'a', 'img'],
    ALLOWED_ATTR: ['href', 'title', 'src', 'alt', 'class'],
    ALLOW_DATA_ATTR: false,
  })
  const document = new DOMParser().parseFromString(safe, 'text/html')
  document.querySelectorAll('img').forEach((img) => {
    const src = img.getAttribute('src') || ''
    if (!/^\/uploads\/tutorial\//.test(src) && !/^https?:\/\//i.test(src)) img.remove()
    else img.setAttribute('src', assetUrl(src))
  })
  return document.body.innerHTML
}
function statusLabel(value) { return ({ edit_direct: '可直接编辑', edit_once: '已获一次性编辑授权', pending: '授权申请待审批', can_request: '可申请一次性编辑' })[value] || '编辑状态未知' }

async function load() {
  loading.value = true; error.value = ''; notice.value = ''; item.value = null; permission.value = null
  try {
    item.value = await api.get(`/instruction/${encodeURIComponent(route.params.id)}`)
    if (!item.value) throw new Error('没有找到这篇教程。')
    if (auth.isAdmin) permission.value = await api.get(`/edit-approval/permission?targetType=instruction&targetId=${encodeURIComponent(route.params.id)}`)
  } catch (err) { error.value = err.message || '读取教程失败。' }
  finally { loading.value = false }
}
async function requestEdit() {
  if (reason.value.trim().length < 5) { error.value = '申请理由至少填写 5 个字符。'; return }
  busy.value = true; error.value = ''
  try {
    await api.post('/edit-approval', { targetType: 'instruction', targetId: Number(route.params.id), reason: reason.value.trim() })
    approvalOpen.value = false; reason.value = ''; notice.value = '编辑申请已提交给教程创建者。'; await load()
  } catch (err) { error.value = err.message || '提交申请失败。' }
  finally { busy.value = false }
}
watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <section class="tutorial-page">
    <RouterLink class="back-link" :to="`/devices/${route.params.deviceId}`">← 返回设备详情</RouterLink>
    <div v-if="loading" class="panel state" role="status">正在读取教程…</div>
    <div v-else-if="error && !item" class="panel state error-state" role="alert">{{ error }} <button class="btn" @click="load">重试</button></div>
    <template v-else-if="item">
      <header class="tutorial-head"><div><p class="eyebrow">INSTRUCTIONS / 教程档案 · #{{ item.id }}</p><h1 class="page-title">{{ item.title }}</h1><p class="meta">设备 #{{ item.deviceId }} · {{ item.authorName || '实验室成员' }} · 更新于 {{ formatDate(item.updatedAt || item.createdAt) }} · 步骤 {{ item.stepOrder ?? '—' }}</p></div><RouterLink v-if="mayEdit" class="btn btn-primary" :to="`/devices/${item.deviceId}/tutorials/${item.id}/edit`">编辑教程 ↗</RouterLink></header>
      <div v-if="auth.isAdmin && !mayEdit" class="panel permission"><strong>{{ statusLabel(permission?.status) }}</strong><button v-if="permission?.status === 'can_request'" class="btn" @click="approvalOpen = !approvalOpen">申请编辑</button><span v-if="permission?.status === 'pending'">请等待创建者处理。</span></div>
      <form v-if="approvalOpen" class="panel request-form" @submit.prevent="requestEdit"><label class="field"><span>申请理由（至少 5 个字符）</span><textarea v-model="reason" class="input" minlength="5" maxlength="500" required /></label><button class="btn btn-primary" :disabled="busy">{{ busy ? '提交中…' : '提交申请' }}</button><button class="btn" type="button" @click="approvalOpen = false">取消</button></form>
      <p v-if="error && item" class="error-note" role="alert">{{ error }}</p><p v-if="notice" class="success-note" role="status">{{ notice }}</p>
      <article class="panel tutorial-content"><div v-html="displayHtml(item.content)" /></article>
    </template>
  </section>
</template>

<style scoped>
.tutorial-page{max-width:1000px;margin:auto;padding:12px 0 48px}.back-link{display:inline-block;margin-bottom:20px;font-weight:900;color:var(--deep-red)}.tutorial-head{display:flex;align-items:end;justify-content:space-between;gap:20px;padding-bottom:20px;border-bottom:3px solid var(--ink);margin-bottom:20px}.page-title{margin:5px 0;font-size:clamp(30px,5vw,54px);line-height:1.1}.eyebrow{margin:0;color:var(--deep-red);font-size:12px;font-weight:900;letter-spacing:.1em}.meta{font-size:13px;color:var(--soft);margin:10px 0 0}.tutorial-content{padding:clamp(18px,4vw,40px);line-height:1.85;overflow-wrap:anywhere}.tutorial-content :deep(img){display:block;max-width:100%;height:auto;margin:20px auto;border:1px solid var(--line)}.tutorial-content :deep(h1),.tutorial-content :deep(h2),.tutorial-content :deep(h3){line-height:1.3}.tutorial-content :deep(pre){overflow:auto;padding:14px;background:var(--muted)}.permission,.request-form{display:flex;align-items:center;gap:12px;padding:14px;margin-bottom:16px;flex-wrap:wrap}.request-form .field{flex:1 1 100%}.state{padding:24px;display:flex;align-items:center;justify-content:center;gap:12px}.error-state{background:#ffe1da}.error-note{color:#9b201a}.success-note{padding:10px;border:2px solid var(--ink);background:#e5ecd9}.btn{min-height:44px}.input{width:100%;min-height:110px}@media(max-width:620px){.tutorial-head{align-items:flex-start;flex-direction:column}.permission{align-items:flex-start;flex-direction:column}}
</style>
