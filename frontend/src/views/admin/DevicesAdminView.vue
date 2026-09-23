<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, upload } from '../../lib/api'
import { useAuthStore } from '../../stores/auth'
import DeviceQrCode from '../../components/DeviceQrCode.vue'

const auth = useAuthStore()
const rows = ref([])
const categories = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const saving = ref(false)
const search = ref('')
const filterStatus = ref('')
const dialogOpen = ref(false)
const editingId = ref(null)
const formError = ref('')
const imageFile = ref(null)
const qrDevice = ref(null)
const approvalDevice = ref(null)
const approvalStatus = ref('')
const approvalLoading = ref(false)
const approvalBusy = ref(false)
const approvalReason = ref('')
const approvalError = ref('')
const emptyForm = () => ({ categoryId: '', name: '', model: '', serialNo: '', location: '', description: '', imageUrl: '' })
const form = reactive(emptyForm())
const isAdmin = computed(() => auth.isAdmin)
const isSuperAdmin = computed(() => auth.isSuperAdmin)
const currentUserId = computed(() => auth.user?.id)
const visibleRows = computed(() => rows.value.filter((device) => {
  const q = search.value.trim().toLowerCase()
  const matchesText = !q || [device.name, device.model, device.serialNo, device.location].some((value) => String(value || '').toLowerCase().includes(q))
  return matchesText && (!filterStatus.value || device.status === filterStatus.value)
}))

function messageOf(err) { return err?.message || '操作失败，请稍后重试。' }
async function load() {
  loading.value = true; error.value = ''
  try {
    const [devices, cats] = await Promise.all([api.get('/device'), api.get('/category')])
    rows.value = Array.isArray(devices) ? devices : []
    categories.value = Array.isArray(cats) ? cats : []
  } catch (err) { error.value = messageOf(err) }
  finally { loading.value = false }
}
function categoryName(id) { return categories.value.find((item) => Number(item.id) === Number(id))?.name || '未分类' }
function openCreate() {
  editingId.value = null; Object.assign(form, emptyForm()); form.categoryId = categories.value[0]?.id || ''; formError.value = ''; imageFile.value = null; dialogOpen.value = true
}
function openEdit(device) {
  editingId.value = device.id
  Object.assign(form, { categoryId: device.categoryId || '', name: device.name || '', model: device.model || '', serialNo: device.serialNo || '', location: device.location || '', description: device.description || '', imageUrl: device.imageUrl || '' })
  imageFile.value = null; formError.value = ''; dialogOpen.value = true
}
const canEditDirectly = (device) => isSuperAdmin.value || Number(device.createdBy) === Number(currentUserId.value)
const canDelete = (device) => canEditDirectly(device)
async function prepareEdit(device) {
  if (canEditDirectly(device)) { openEdit(device); return }
  approvalDevice.value = device
  approvalStatus.value = ''
  approvalReason.value = ''
  approvalError.value = ''
  approvalLoading.value = true
  try {
    const permission = await api.get(`/edit-approval/permission?targetType=device&targetId=${encodeURIComponent(device.id)}`)
    approvalStatus.value = permission?.status || ''
    if (permission?.status === 'edit_once') {
      approvalDevice.value = null
      openEdit(device)
    }
  } catch (err) { approvalError.value = messageOf(err) }
  finally { approvalLoading.value = false }
}
async function refreshApproval() {
  const device = approvalDevice.value
  if (!device) return
  approvalLoading.value = true
  approvalError.value = ''
  try {
    const permission = await api.get(`/edit-approval/permission?targetType=device&targetId=${encodeURIComponent(device.id)}`)
    approvalStatus.value = permission?.status || ''
    if (permission?.status === 'edit_once') {
      approvalDevice.value = null
      openEdit(device)
    }
  } catch (err) { approvalError.value = messageOf(err) }
  finally { approvalLoading.value = false }
}
async function requestDeviceEdit() {
  const reason = approvalReason.value.trim()
  if (reason.length < 5 || reason.length > 500) { approvalError.value = '申请理由需为 5–500 个字符。'; return }
  approvalBusy.value = true
  approvalError.value = ''
  try {
    await api.post('/edit-approval', { targetType: 'device', targetId: Number(approvalDevice.value.id), reason })
    approvalStatus.value = 'pending'
    approvalReason.value = ''
    notice.value = '设备编辑授权申请已提交给创建者。'
  } catch (err) { approvalError.value = messageOf(err) }
  finally { approvalBusy.value = false }
}
async function save() {
  formError.value = ''
  if (!form.name.trim()) { formError.value = '请填写设备名称。'; return }
  if (!form.categoryId) { formError.value = '请选择设备分类。'; return }
  saving.value = true
  try {
    if (imageFile.value) {
      const uploaded = await upload(imageFile.value, 'equipment')
      form.imageUrl = typeof uploaded === 'string' ? uploaded : uploaded?.url || uploaded?.fileUrl || ''
      if (!form.imageUrl) throw new Error('图片上传响应中没有文件地址。')
    }
    const body = { ...form, id: editingId.value, categoryId: Number(form.categoryId), name: form.name.trim(), model: form.model.trim(), serialNo: form.serialNo.trim(), location: form.location.trim(), description: form.description.trim() }
    if (editingId.value) await api.put('/device', body)
    else await api.post('/device', body)
    dialogOpen.value = false; notice.value = editingId.value ? '设备档案已更新。' : '设备档案已新增。'; await load()
  } catch (err) { formError.value = messageOf(err) }
  finally { saving.value = false }
}
async function removeDevice(device) {
  if (!window.confirm(`确定删除“${device.name}”吗？此操作会由服务器校验权限。`)) return
  notice.value = ''; error.value = ''
  try { await api.delete(`/device/${device.id}`); notice.value = '设备已删除。'; await load() }
  catch (err) { error.value = messageOf(err) }
}
async function finishMaintenance(device) {
  const status = window.prompt('维修完成后的状态只能填写“在库”或“报废”。', '在库')
  if (!status) return
  if (!['在库', '报废'].includes(status.trim())) { error.value = '状态只能是“在库”或“报废”。'; return }
  error.value = ''; notice.value = ''
  try { await api.put(`/device/${device.id}/maintenance-status`, { status: status.trim() }); notice.value = `设备状态已调整为${status.trim()}。`; await load() }
  catch (err) { error.value = messageOf(err) }
}
function showQr(device) { qrDevice.value = device }
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <header class="page-head"><div><p class="eyebrow">04 / SYSTEM & REVIEW</p><h1 class="page-title">设备维护</h1><p class="intro">维护设备档案、库存状态与二维码信息。操作权限由服务器最终校验。</p></div><button v-if="isAdmin" class="btn btn-primary" @click="openCreate">＋ 新建设备</button></header>
    <div v-if="!isAdmin" class="notice error">此页面仅供管理员使用。</div>
    <template v-else>
      <p v-if="notice" class="notice success" role="status">{{ notice }}</p>
      <p v-if="error" class="notice error" role="alert">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
      <div class="panel filters"><label class="search-field">搜索设备<input v-model="search" class="input" type="search" placeholder="名称、型号、编号或位置"></label><label>设备状态<select v-model="filterStatus" class="input"><option value="">全部状态</option><option>在库</option><option>已借出</option><option>维修中</option><option>报废</option></select></label><button class="btn" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新' }}</button></div>
      <div v-if="loading && !rows.length" class="panel state">正在读取设备档案…</div>
      <div v-else-if="!error && !visibleRows.length" class="panel state"><strong>{{ rows.length ? '没有符合筛选条件的设备' : '还没有设备档案' }}</strong><span>{{ rows.length ? '调整关键词或状态后再试。' : '新增设备后会显示在这里。' }}</span></div>
      <div v-else class="device-list">
        <article v-for="device in visibleRows" :key="device.id" class="panel device-card">
          <div class="device-main"><div class="device-mark">{{ String(device.id).padStart(2, '0') }}</div><div><h2>{{ device.name }}</h2><p>{{ device.model || '型号未登记' }} · {{ categoryName(device.categoryId) }}</p><p class="muted">编号 {{ device.serialNo || '—' }} · {{ device.location || '位置未登记' }}</p><span class="state-tag" :class="`tag-${device.status}`">{{ device.status || '状态未知' }}</span></div></div>
          <p v-if="device.description" class="description">{{ device.description }}</p>
          <div class="actions"><button class="btn" @click="showQr(device)">二维码</button><button v-if="canEditDirectly(device)" class="btn" @click="prepareEdit(device)">编辑档案</button><button v-else class="btn" @click="prepareEdit(device)">申请编辑</button><button v-if="device.status === '维修中'" class="btn" @click="finishMaintenance(device)">维修完成</button><button v-if="canDelete(device)" class="btn danger-button" @click="removeDevice(device)">删除</button></div>
        </article>
      </div>
    </template>

    <div v-if="approvalDevice" class="modal-backdrop" @click.self="approvalDevice = null"><section class="modal approval-modal panel" role="dialog" aria-modal="true" aria-labelledby="approval-title"><div class="modal-head"><div><p class="eyebrow">DEVICE ACCESS / #{{ approvalDevice.id }}</p><h2 id="approval-title">设备编辑授权</h2></div><button class="close-button" aria-label="关闭授权申请" @click="approvalDevice = null">×</button></div><p class="approval-target">{{ approvalDevice.name }}</p><p v-if="approvalLoading" class="state">正在查询当前授权…</p><template v-else><p v-if="approvalStatus === 'pending'" class="notice">已有一条申请正在等待设备创建者处理。</p><form v-else-if="approvalStatus === 'can_request'" class="approval-form" @submit.prevent="requestDeviceEdit"><label>申请理由（5–500 个字符）<textarea v-model="approvalReason" class="input" rows="4" minlength="5" maxlength="500" required placeholder="说明需要修改设备档案的原因"></textarea></label><div class="modal-actions"><button type="button" class="btn" @click="approvalDevice = null">取消</button><button class="btn btn-primary" :disabled="approvalBusy">{{ approvalBusy ? '提交中…' : '提交授权申请' }}</button></div></form><p v-else-if="approvalStatus" class="notice">当前授权状态：{{ approvalStatus }}</p><button v-if="approvalStatus !== 'can_request'" class="btn" @click="refreshApproval">重新检查授权</button></template><p v-if="approvalError" class="notice error" role="alert">{{ approvalError }}</p></section></div>

    <div v-if="dialogOpen" class="modal-backdrop" @click.self="dialogOpen = false"><section class="modal panel" role="dialog" aria-modal="true" :aria-label="editingId ? '编辑设备档案' : '新建设备档案'"><div class="modal-head"><div><p class="eyebrow">DEVICE FILE / {{ editingId ? 'EDIT' : 'NEW' }}</p><h2>{{ editingId ? '编辑设备档案' : '新建设备档案' }}</h2></div><button class="close-button" aria-label="关闭" @click="dialogOpen = false">×</button></div>
      <p v-if="formError" class="notice error" role="alert">{{ formError }}</p>
      <form class="form-grid" @submit.prevent="save"><label>设备名称<input v-model="form.name" class="input" required maxlength="100"></label><label>分类<select v-model="form.categoryId" class="input" required><option value="" disabled>选择分类</option><option v-for="item in categories" :key="item.id" :value="item.id">{{ item.name }}</option></select></label><label>型号<input v-model="form.model" class="input"></label><label>设备编号<input v-model="form.serialNo" class="input"></label><label>存放位置<input v-model="form.location" class="input"></label><label>设备图片<input class="input" type="file" accept="image/*" @change="imageFile = $event.target.files?.[0] || null"><small>选择新图片后将上传并保存至设备档案。</small></label><label class="wide">说明<textarea v-model="form.description" class="input" rows="3"></textarea></label><label class="wide">图片地址<input v-model="form.imageUrl" class="input" placeholder="/uploads/... 或完整 URL"></label><div class="modal-actions wide"><button type="button" class="btn" @click="dialogOpen = false">取消</button><button class="btn btn-primary" :disabled="saving">{{ saving ? '保存中…' : '保存档案' }}</button></div></form>
    </section></div>
    <div v-if="qrDevice" class="modal-backdrop" @click.self="qrDevice = null"><section class="modal qr-modal panel" role="dialog" aria-modal="true" aria-label="设备二维码"><div class="modal-head"><div><p class="eyebrow">ASSET / {{ qrDevice.id }}</p><h2>{{ qrDevice.name }}</h2></div><button class="close-button" aria-label="关闭二维码" @click="qrDevice = null">×</button></div><DeviceQrCode :device-id="qrDevice.id" /></section></div>
  </section>
</template>

<style scoped>
.admin-page{--red:#d5261e;--ink:#171717;--paper:#f4eedf;color:var(--ink);max-width:1180px;margin:auto;padding:28px 24px 56px}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:24px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:var(--red);margin:0 0 8px}.page-title{font-size:clamp(30px,4vw,44px);line-height:1;margin:0;font-weight:900}.intro,.muted{color:#625d54}.intro{margin:12px 0 0}.panel{background:var(--paper);border:2px solid var(--ink);box-shadow:4px 4px 0 var(--red)}.filters{display:flex;align-items:end;gap:14px;padding:16px;margin-bottom:20px}.filters label,.form-grid label{display:grid;gap:7px;font-weight:700;font-size:14px}.search-field{flex:1}.input{width:100%;min-height:44px;border:2px solid var(--ink);border-radius:0;background:#fffaf0;padding:9px 11px;color:var(--ink);font:inherit}.btn{min-height:44px;border:2px solid var(--ink);border-radius:0;background:var(--paper);color:var(--ink);font-weight:800;padding:8px 14px;cursor:pointer}.btn:hover,.btn:focus-visible{outline:3px solid #171717;outline-offset:2px}.btn-primary{background:var(--red);color:#fff;border-color:var(--ink);box-shadow:3px 3px 0 var(--ink)}button:disabled{opacity:.6;cursor:wait}.notice{padding:12px 14px;border:2px solid var(--ink);margin:12px 0}.success{background:#e5ecd9}.error{background:#ffe1da}.text-button{border:0;text-decoration:underline;background:transparent;font-weight:800;cursor:pointer}.state{padding:28px;display:grid;gap:8px;text-align:center}.device-list{display:grid;gap:16px}.device-card{padding:18px;display:grid;gap:12px}.device-main{display:flex;gap:15px;align-items:flex-start}.device-mark{background:var(--red);color:white;border:2px solid var(--ink);font-weight:900;padding:10px;min-width:52px;text-align:center}.device-card h2{margin:0 0 5px;font-size:21px}.device-card p{margin:4px 0}.state-tag{display:inline-block;padding:4px 9px;border:1.5px solid var(--ink);font-weight:800;font-size:13px;margin-top:5px}.tag-在库{background:#e5ecd9}.tag-已借出{background:#f9d9a5}.tag-维修中{background:#f4c4b7}.tag-报废{background:#d7d2c8}.description{border-left:4px solid var(--red);padding-left:10px}.actions{display:flex;gap:8px;flex-wrap:wrap}.danger-button{background:#ffe1da}.modal-backdrop{position:fixed;inset:0;background:#171717aa;z-index:50;display:grid;place-items:center;padding:18px;overflow:auto}.modal{width:min(720px,100%);padding:22px;max-height:calc(100dvh - 36px);overflow:auto}.modal-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:18px}.modal-head h2{margin:0;font-size:26px}.close-button{width:44px;height:44px;border:2px solid var(--ink);border-radius:0;background:var(--red);color:white;font-size:28px;line-height:1;cursor:pointer}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-grid small{font-size:12px;font-weight:400}.wide{grid-column:1/-1}.modal-actions{display:flex;justify-content:flex-end;gap:10px}.approval-target{font-size:18px;font-weight:900}.approval-form label{display:grid;gap:8px;font-weight:800}.approval-form textarea{resize:vertical}.qr-modal{width:min(440px,100%)}.qr-image{text-align:center;background:#fff;padding:16px;border:2px solid var(--ink)}.qr-image img{max-width:100%;max-height:280px}.qr-modal .state{border:1px dashed #625d54}@media(max-width:620px){.admin-page{padding:22px 14px 42px}.page-head{align-items:flex-start;flex-direction:column}.filters{align-items:stretch;flex-direction:column}.form-grid{grid-template-columns:1fr}.wide{grid-column:auto}.actions .btn{flex:1}.modal{padding:16px}.device-card{padding:14px}}
</style>
