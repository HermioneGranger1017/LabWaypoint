<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, upload } from '../lib/api'
import { useAuthStore } from '../stores/auth'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isNew = computed(() => route.name === 'tutorial-new')
const form = ref({ title: '', stepOrder: 1, content: '' })
const permission = ref(null)
const reason = ref('')
const error = ref('')
const busy = ref(false)
const loading = ref(true)
const imageInput = ref(null)
const editorInstance = shallowRef(null)
const toolbarConfig = {}
const editorConfig = {
  placeholder: '请编写清晰、可操作的设备教程。',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        await uploadAndInsert(file, insertFn)
      },
    },
  },
}
const mayEdit = computed(() => auth.isSuperAdmin || Number(permission.value?.createdBy) === Number(auth.user?.id) || permission.value?.status === 'edit_direct' || permission.value?.status === 'edit_once')
const canRequest = computed(() => !isNew.value && permission.value?.status === 'can_request')

async function load() {
  loading.value = true; error.value = ''; permission.value = null
  try {
    if (isNew.value) { form.value = { title: '', stepOrder: 1, content: '' }; loading.value = false; return }
    const id = encodeURIComponent(route.params.id)
    const [instruction, access] = await Promise.all([
      api.get(`/instruction/${id}`),
      api.get(`/edit-approval/permission?targetType=instruction&targetId=${id}`),
    ])
    if (!instruction) throw new Error('没有找到这篇教程。')
    permission.value = { ...access, createdBy: instruction.createdBy }
    form.value = { title: instruction.title || '', stepOrder: instruction.stepOrder ?? 1, content: instruction.content || '' }
  } catch (err) { error.value = err.message || '读取教程失败。' }
  finally { loading.value = false }
}
function readableText(html) { return String(html || '').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&#39;|&apos;/g, "'").replace(/&quot;/g, '"').replace(/\s+/g, ' ').trim() }
function htmlEscape(value) { return String(value).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]) }
async function chooseImage() { imageInput.value?.click() }
function handleEditorCreated(editor) { editorInstance.value = editor }
function handleEditorDestroyed(editor) { if (editorInstance.value === editor) editorInstance.value = null }
async function uploadAndInsert(file, insertFn) {
  if (!file) return
  busy.value = true; error.value = ''
  try {
    const result = await upload(file, 'tutorial')
    if (!result?.url) throw new Error('图片上传成功，但服务器没有返回图片地址。')
    if (typeof insertFn === 'function') insertFn(result.url, file.name || '教程插图', result.url)
    else {
      const editor = editorInstance.value
      if (!editor || editor.isDestroyed) throw new Error('编辑器尚未准备好，请稍后重试。')
      editor.focus()
      editor.dangerouslyInsertHtml(`<p><img src="${htmlEscape(result.url)}" alt="${htmlEscape(file.name || '教程插图')}"></p>`)
    }
  } catch (err) {
    error.value = err.message || '图片上传失败。'
    editorInstance.value?.alert?.(error.value, 'error')
  } finally { busy.value = false }
}
async function insertImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  await uploadAndInsert(file)
}
async function requestEdit() {
  if (reason.value.trim().length < 5) { error.value = '申请理由至少填写 5 个字符。'; return }
  busy.value = true; error.value = ''
  try {
    await api.post('/edit-approval', { targetType: 'instruction', targetId: Number(route.params.id), reason: reason.value.trim() })
    await router.push(`/devices/${route.params.deviceId}/tutorials/${route.params.id}`)
  } catch (err) { error.value = err.message || '提交申请失败。' }
  finally { busy.value = false }
}
async function save() {
  error.value = ''
  if (form.value.title.trim().length < 2 || form.value.title.trim().length > 100) { error.value = '标题需要填写 2–100 个字符。'; return }
  const editor = editorInstance.value
  const content = editor && !editor.isDestroyed ? editor.getHtml() : form.value.content
  const bodyText = editor && !editor.isDestroyed ? editor.getText().trim() : readableText(content)
  if (bodyText.length < 10) { error.value = '教程正文去除格式后至少需要 10 个字符。'; return }
  busy.value = true
  try {
    const data = { deviceId: Number(route.params.deviceId), title: form.value.title.trim(), stepOrder: Number(form.value.stepOrder), content }
    if (isNew.value) {
      const created = await api.post('/instruction', data)
      await router.replace(created?.id
        ? `/devices/${route.params.deviceId}/tutorials/${created.id}`
        : `/devices/${route.params.deviceId}`)
    } else {
      await api.put('/instruction', { ...data, id: Number(route.params.id) })
      await router.push(`/devices/${route.params.deviceId}/tutorials/${route.params.id}`)
    }
  } catch (err) { error.value = err.message || '保存失败。' }
  finally { busy.value = false }
}
watch(() => [route.params.deviceId, route.params.id, route.name], load)
onMounted(load)
onBeforeUnmount(() => {
  const editor = editorInstance.value
  if (editor && !editor.isDestroyed) editor.destroy()
  editorInstance.value = null
})
</script>

<template>
  <section class="editor-page">
    <RouterLink class="back-link" :to="isNew ? `/devices/${route.params.deviceId}` : `/devices/${route.params.deviceId}/tutorials/${route.params.id}`">← 返回{{ isNew ? '设备详情' : '教程' }}</RouterLink>
    <header><p class="eyebrow">INSTRUCTIONS / EDIT DESK</p><h1 class="page-title">{{ isNew ? '编写教程' : '编辑教程' }}</h1><p class="intro">正文支持 HTML。插图使用教程图片上传接口，保存时由服务端清理 HTML。</p></header>
    <div v-if="loading" class="panel state">正在读取教程和编辑权限…</div>
    <div v-else-if="error && !permission && !isNew" class="panel state error-state" role="alert">{{ error }} <button class="btn" @click="load">重试</button></div>
    <template v-else-if="isNew || mayEdit">
      <p v-if="error" class="error-note" role="alert">{{ error }}</p>
      <form class="panel editor-form" @submit.prevent="save">
        <div class="form-row"><label class="field"><span>教程标题</span><input v-model="form.title" class="input" minlength="2" maxlength="100" required placeholder="例如：显微镜开机与关机" /></label><label class="field order-field"><span>步骤序号</span><input v-model.number="form.stepOrder" class="input" type="number" min="1" required /></label></div>
        <div class="field"><span>教程正文</span><div class="wang-editor"><Toolbar :editor="editorInstance" :defaultConfig="toolbarConfig" mode="default" /><Editor v-model="form.content" :defaultConfig="editorConfig" mode="default" @onCreated="handleEditorCreated" @onDestroyed="handleEditorDestroyed" /></div><small class="editor-help">纯文本至少 10 个字符。可使用工具栏插入图片，上传会走实验室教程图片接口。</small></div>
        <div class="editor-tools"><input ref="imageInput" class="visually-hidden" type="file" accept="image/jpeg,image/png,image/webp" @change="insertImage" /><button class="btn" type="button" :disabled="busy" @click="chooseImage">选择图片并插入</button><span>JPG / PNG / WebP，最大 10 MB。</span></div>
        <div class="actions"><button class="btn btn-primary" type="submit" :disabled="busy">{{ busy ? '处理中…' : '保存教程' }}</button><RouterLink class="btn" :to="isNew ? `/devices/${route.params.deviceId}` : `/devices/${route.params.deviceId}/tutorials/${route.params.id}`">取消</RouterLink></div>
      </form>
    </template>
    <div v-else-if="canRequest" class="panel request-panel"><h2>需要创建者授权</h2><p>管理员可向教程创建者申请一次性编辑授权。获批后首次保存会消耗该授权。</p><label class="field"><span>申请理由（至少 5 个字符）</span><textarea v-model="reason" class="input" minlength="5" maxlength="500" /></label><p v-if="error" class="error-note" role="alert">{{ error }}</p><button class="btn btn-primary" :disabled="busy" @click="requestEdit">{{ busy ? '提交中…' : '申请一次性编辑' }}</button></div>
    <div v-else-if="permission?.status === 'pending'" class="panel state">编辑申请正在等待创建者审批。</div>
    <div v-else class="panel state error-state">当前账号没有编辑权限。教程只能由创建者或超级管理员直接编辑；管理员需要先获得创建者的一次性授权。</div>
  </section>
</template>

<style scoped>
.editor-page{max-width:1040px;margin:auto;padding:12px 0 48px}.back-link{display:inline-block;margin-bottom:20px;font-weight:900;color:var(--deep-red)}header{margin-bottom:20px;padding-bottom:18px;border-bottom:3px solid var(--ink)}.eyebrow{margin:0;color:var(--deep-red);font-size:12px;font-weight:900;letter-spacing:.12em}.page-title{font-size:clamp(30px,5vw,48px);margin:6px 0}.intro{margin:8px 0 0;color:var(--soft)}.editor-form,.request-panel{padding:clamp(16px,3vw,28px)}.form-row{display:grid;grid-template-columns:1fr 180px;gap:16px}.field{display:grid;gap:7px;margin-bottom:17px;font-weight:800}.input{width:100%;min-height:46px;border:2px solid var(--ink);border-radius:0;background:var(--surface);padding:10px;font:inherit}.wang-editor{border:2px solid var(--ink);background:var(--surface);min-height:340px}.wang-editor :deep(.w-e-toolbar){border:0!important;border-bottom:2px solid var(--ink)!important;flex-wrap:wrap}.wang-editor :deep(.w-e-text-container){min-height:300px}.wang-editor :deep(.w-e-text-placeholder){font-weight:400}.editor-help{display:block;margin-top:7px;color:var(--soft);font-size:12px;font-weight:400}.editor-tools{display:flex;align-items:center;gap:14px;flex-wrap:wrap;margin:0 0 18px}.editor-tools span,.request-panel p{font-size:13px;color:var(--soft)}.actions{display:flex;gap:10px;padding-top:16px;border-top:2px solid var(--ink)}.state{padding:24px}.error-state{background:#ffe1da}.error-note{color:#9b201a}.visually-hidden{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap}@media(max-width:600px){.form-row{grid-template-columns:1fr}.actions{flex-direction:column}.actions .btn{width:100%}.wang-editor{min-height:290px}.wang-editor :deep(.w-e-text-container){min-height:250px}}
</style>
