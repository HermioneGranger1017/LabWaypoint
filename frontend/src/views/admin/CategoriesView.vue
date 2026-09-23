<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../lib/api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const rows = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const saving = ref(false)
const modal = ref(false)
const editingId = ref(null)
const formError = ref('')
const form = reactive({ name: '', description: '' })
const canManage = computed(() => auth.isAdmin)
function messageOf(err) { return err?.message || '操作失败，请稍后重试。' }
async function load() {
  loading.value = true; error.value = ''
  try { const data = await api.get('/category'); rows.value = Array.isArray(data) ? data : [] }
  catch (err) { error.value = messageOf(err) }
  finally { loading.value = false }
}
function openCreate() { editingId.value = null; form.name = ''; form.description = ''; formError.value = ''; modal.value = true }
function openEdit(item) { editingId.value = item.id; form.name = item.name || ''; form.description = item.description || ''; formError.value = ''; modal.value = true }
async function save() {
  formError.value = ''
  if (!form.name.trim()) { formError.value = '分类名称不能为空。'; return }
  saving.value = true
  try {
    const body = { id: editingId.value, name: form.name.trim(), description: form.description.trim() }
    if (editingId.value) await api.put('/category', body); else await api.post('/category', body)
    modal.value = false; notice.value = editingId.value ? '分类已更新。' : '分类已新增。'; await load()
  } catch (err) { formError.value = messageOf(err) }
  finally { saving.value = false }
}
async function remove(item) {
  if (!window.confirm(`确定删除分类“${item.name}”吗？若分类仍被设备使用，服务器可能拒绝删除。`)) return
  error.value = ''; notice.value = ''
  try { await api.delete(`/category/${item.id}`); notice.value = '分类已删除。'; await load() }
  catch (err) { error.value = messageOf(err) }
}
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <header class="page-head"><div><p class="eyebrow">04 / SYSTEM & REVIEW</p><h1 class="page-title">分类管理</h1><p class="intro">管理设备档案使用的分类名称与说明。</p></div><button v-if="canManage" class="btn btn-primary" @click="openCreate">＋ 新增分类</button></header>
    <p v-if="!canManage" class="notice error">此页面仅供管理员使用。</p>
    <template v-else>
      <p v-if="notice" class="notice success" role="status">{{ notice }}</p><p v-if="error" class="notice error" role="alert">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
      <div class="toolbar"><span>分类档案 <b>{{ rows.length }}</b></span><button class="btn" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新' }}</button></div>
      <div v-if="loading && !rows.length" class="panel state">正在读取分类…</div>
      <div v-else-if="!error && !rows.length" class="panel state"><strong>还没有设备分类</strong><span>新增分类后，可用于设备档案和设备筛选。</span></div>
      <div v-else class="category-list"><article v-for="item in rows" :key="item.id" class="panel category-card"><div class="category-no">{{ String(item.id).padStart(2, '0') }}</div><div class="category-copy"><h2>{{ item.name }}</h2><p>{{ item.description || '暂无分类说明' }}</p></div><div v-if="canManage" class="actions"><button class="btn" @click="openEdit(item)">编辑</button><button class="btn danger-button" @click="remove(item)">删除</button></div></article></div>
    </template>
    <div v-if="modal" class="modal-backdrop" @click.self="modal = false"><section class="modal panel" role="dialog" aria-modal="true" :aria-label="editingId ? '编辑分类' : '新增分类'"><header class="modal-head"><div><p class="eyebrow">CATEGORY / {{ editingId ? 'EDIT' : 'NEW' }}</p><h2>{{ editingId ? '编辑分类' : '新增分类' }}</h2></div><button class="close-button" aria-label="关闭" @click="modal = false">×</button></header><p v-if="formError" class="notice error" role="alert">{{ formError }}</p><form class="form-grid" @submit.prevent="save"><label>分类名称<input v-model="form.name" class="input" maxlength="100" required></label><label>说明<textarea v-model="form.description" class="input" rows="4"></textarea></label><div class="modal-actions"><button type="button" class="btn" @click="modal = false">取消</button><button class="btn btn-primary" :disabled="saving">{{ saving ? '保存中…' : '保存分类' }}</button></div></form></section></div>
  </section>
</template>

<style scoped>
.admin-page{--red:#d5261e;--ink:#171717;--paper:#f4eedf;color:var(--ink);max-width:1050px;margin:auto;padding:28px 24px 56px}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:24px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:var(--red);margin:0 0 8px}.page-title{font-size:clamp(30px,4vw,44px);line-height:1;margin:0;font-weight:900}.intro{color:#625d54;margin:12px 0 0}.panel{background:var(--paper);border:2px solid var(--ink);box-shadow:4px 4px 0 var(--red)}.toolbar{display:flex;justify-content:space-between;align-items:center;padding:12px 0}.state{text-align:center;padding:30px;display:grid;gap:8px}.category-list{display:grid;gap:14px}.category-card{display:grid;grid-template-columns:58px 1fr auto;gap:16px;align-items:center;padding:17px}.category-no{background:var(--red);border:2px solid var(--ink);color:white;font-weight:900;text-align:center;padding:13px 8px}.category-copy h2{font-size:21px;margin:0 0 5px}.category-copy p{margin:0;color:#625d54}.actions{display:flex;gap:8px}.btn{min-height:44px;border:2px solid var(--ink);border-radius:0;background:var(--paper);font-weight:800;padding:8px 14px;cursor:pointer;color:var(--ink)}.btn-primary{background:var(--red);color:white;box-shadow:3px 3px 0 var(--ink)}.danger-button{background:#ffe1da}.btn:focus-visible,.close-button:focus-visible{outline:3px solid var(--ink);outline-offset:2px}.notice{padding:12px 14px;border:2px solid var(--ink);margin:12px 0}.success{background:#e5ecd9}.error{background:#ffe1da}.text-button{border:0;background:none;text-decoration:underline;font-weight:800;cursor:pointer}.modal-backdrop{position:fixed;inset:0;background:#171717aa;z-index:50;display:grid;place-items:center;padding:18px;overflow:auto}.modal{width:min(560px,100%);padding:22px;max-height:calc(100dvh - 36px);overflow:auto}.modal-head{display:flex;justify-content:space-between;margin-bottom:18px}.modal-head h2{margin:0;font-size:26px}.close-button{width:44px;height:44px;border:2px solid var(--ink);background:var(--red);color:white;font-size:28px;cursor:pointer}.form-grid{display:grid;gap:14px}.form-grid label{display:grid;gap:7px;font-size:14px;font-weight:700}.input{width:100%;min-height:44px;border:2px solid var(--ink);border-radius:0;background:#fffaf0;padding:9px 11px;font:inherit}.modal-actions{display:flex;justify-content:flex-end;gap:10px}.notice.error{background:#ffe1da}@media(max-width:620px){.admin-page{padding:22px 14px 42px}.page-head{align-items:flex-start;flex-direction:column}.category-card{grid-template-columns:48px 1fr}.actions{grid-column:1/-1}.actions .btn{flex:1}}
</style>
