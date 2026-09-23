<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../../lib/api'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const rows = ref([])
const roleDrafts = reactive({})
const savingId = ref(null)
const loading = ref(false)
const error = ref('')
const notice = ref('')
const query = ref('')
const canManage = computed(() => auth.isSuperAdmin)
const selfId = computed(() => auth.user?.id)
const visibleRows = computed(() => rows.value.filter((user) => [user.username, user.studentNo, user.major, user.role, user.registrationStatus].some((value) => String(value || '').toLowerCase().includes(query.value.trim().toLowerCase()))))
const roleLabel = (role) => ({ user: '普通用户', admin: '管理员', super_admin: '超级管理员' })[role] || role || '未知角色'
function messageOf(err) { return err?.message || '操作失败，请稍后重试。' }
async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await api.get('/user/list')
    rows.value = Array.isArray(data) ? data : []
    for (const user of rows.value) roleDrafts[user.id] = user.role
  } catch (err) { error.value = messageOf(err) }
  finally { loading.value = false }
}
async function saveRole(user) {
  if (Number(user.id) === Number(selfId.value)) { roleDrafts[user.id] = user.role; error.value = '不能调整自己的角色。'; return }
  if (!['user', 'admin', 'super_admin'].includes(roleDrafts[user.id])) { roleDrafts[user.id] = user.role; error.value = '请选择有效角色。'; return }
  if (roleDrafts[user.id] === user.role) return
  if (!window.confirm(`将 ${user.username} 的角色从“${roleLabel(user.role)}”调整为“${roleLabel(roleDrafts[user.id])}”？`)) { roleDrafts[user.id] = user.role; return }
  savingId.value = user.id; error.value = ''; notice.value = ''
  try { await api.put(`/user/${user.id}/role`, { role: roleDrafts[user.id] }); notice.value = `${user.username} 的角色已更新。`; await load() }
  catch (err) { roleDrafts[user.id] = user.role; error.value = messageOf(err) }
  finally { savingId.value = null }
}
onMounted(load)
</script>

<template>
  <section class="admin-page">
    <header class="page-head"><div><p class="eyebrow">04 / SYSTEM & REVIEW</p><h1 class="page-title">用户管理</h1><p class="intro">查看账号资料与注册状态，调整其他用户的系统角色。</p></div><button v-if="canManage" class="btn" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新用户' }}</button></header>
    <p v-if="!canManage" class="notice error">仅超级管理员可以查看用户列表和调整角色。</p>
    <template v-else>
      <p v-if="notice" class="notice success" role="status">{{ notice }}</p><p v-if="error" class="notice error" role="alert">{{ error }} <button class="text-button" @click="load">重新加载</button></p>
      <div class="toolbar panel"><label>查找用户<input v-model="query" class="input" type="search" placeholder="账号、学号、专业或角色"></label><span>显示 {{ visibleRows.length }} / {{ rows.length }}</span></div>
      <div v-if="loading && !rows.length" class="panel state">正在读取用户列表…</div>
      <div v-else-if="!error && !visibleRows.length" class="panel state"><strong>{{ rows.length ? '没有匹配的用户' : '用户列表为空' }}</strong><span>检查关键词或稍后刷新。</span></div>
      <div v-else class="user-list"><article v-for="user in visibleRows" :key="user.id" class="panel user-card"><header class="user-head"><div class="user-id">{{ String(user.id).padStart(3, '0') }}</div><div class="identity"><h2>{{ user.username }} <span v-if="Number(user.id) === Number(selfId)" class="self-tag">当前账号</span></h2><p>{{ user.realName || user.nickname || '未填写姓名' }} · {{ user.email || user.phone || '未留联系方式' }}</p></div><span class="registration-tag" :class="`registration-${user.registrationStatus}`">{{ user.registrationStatus || '注册状态未知' }}</span></header><dl class="details"><div><dt>学号</dt><dd>{{ user.studentNo || '—' }}</dd></div><div><dt>专业</dt><dd>{{ user.major || '—' }}</dd></div><div><dt>创建时间</dt><dd>{{ user.createdAt || '—' }}</dd></div><div><dt>当前角色</dt><dd>{{ roleLabel(user.role) }}</dd></div></dl><footer class="role-control"><label :for="`role-${user.id}`">调整角色<select :id="`role-${user.id}`" v-model="roleDrafts[user.id]" class="input" :disabled="Number(user.id) === Number(selfId) || savingId === user.id"><option value="user">普通用户</option><option value="admin">管理员</option><option value="super_admin">超级管理员</option></select></label><button class="btn btn-primary" :disabled="Number(user.id) === Number(selfId) || savingId === user.id || roleDrafts[user.id] === user.role" @click="saveRole(user)">{{ savingId === user.id ? '保存中…' : Number(user.id) === Number(selfId) ? '不可调整本人' : '保存角色' }}</button></footer></article></div>
    </template>
  </section>
</template>

<style scoped>
.admin-page{--red:#d5261e;--ink:#171717;--paper:#f4eedf;color:var(--ink);max-width:1100px;margin:auto;padding:28px 24px 56px}.page-head{display:flex;justify-content:space-between;align-items:flex-end;gap:20px;margin-bottom:24px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.13em;color:var(--red);margin:0 0 8px}.page-title{font-size:clamp(30px,4vw,44px);line-height:1;margin:0;font-weight:900}.intro{color:#625d54;margin:12px 0 0}.panel{background:var(--paper);border:2px solid var(--ink);box-shadow:4px 4px 0 var(--red)}.toolbar{padding:14px 16px;display:flex;justify-content:space-between;align-items:end;gap:14px;margin-bottom:18px}.toolbar label,.role-control label{display:grid;gap:6px;font-weight:800}.input{min-width:210px;min-height:44px;border:2px solid var(--ink);border-radius:0;background:#fffaf0;padding:8px;font:inherit}.user-list{display:grid;gap:16px}.user-card{padding:17px}.user-head{display:flex;align-items:center;gap:13px}.user-id{padding:11px 8px;min-width:54px;text-align:center;border:2px solid var(--ink);background:var(--red);color:white;font-weight:900}.identity{flex:1;min-width:0}.identity h2{margin:0;font-size:21px;overflow-wrap:anywhere}.identity p{margin:4px 0 0;color:#625d54;overflow-wrap:anywhere}.self-tag{font-size:12px;border:1px solid var(--ink);padding:2px 5px;background:#e5ecd9;vertical-align:middle}.registration-tag{padding:5px 8px;border:2px solid var(--ink);font-size:13px;font-weight:800;background:#e5ecd9;white-space:nowrap}.registration-待审核{background:#f9d9a5}.registration-已拒绝{background:#ffe1da}.details{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin:18px 0}.details dt{font-size:12px;color:#625d54;font-weight:800}.details dd{margin:3px 0 0;overflow-wrap:anywhere}.role-control{border-top:1px solid #aaa18f;padding-top:13px;display:flex;justify-content:flex-end;align-items:end;gap:10px}.role-control .input{min-width:200px}.btn{min-height:44px;border:2px solid var(--ink);border-radius:0;background:var(--paper);font-weight:800;padding:8px 14px;cursor:pointer;color:var(--ink)}.btn-primary{background:var(--red);color:white;box-shadow:3px 3px 0 var(--ink)}.btn:focus-visible{outline:3px solid var(--ink);outline-offset:2px}.btn:disabled{opacity:.58;cursor:not-allowed}.state{text-align:center;padding:30px;display:grid;gap:8px}.notice{padding:12px 14px;border:2px solid var(--ink);margin:12px 0}.success{background:#e5ecd9}.error{background:#ffe1da}.text-button{border:0;background:none;text-decoration:underline;font-weight:800;cursor:pointer}@media(max-width:760px){.details{grid-template-columns:1fr 1fr}}@media(max-width:620px){.admin-page{padding:22px 14px 42px}.page-head{align-items:flex-start;flex-direction:column}.toolbar{align-items:stretch;flex-direction:column}.user-head{align-items:flex-start;flex-wrap:wrap}.identity{flex-basis:calc(100% - 76px)}.registration-tag{margin-left:67px}.details{grid-template-columns:1fr 1fr}.role-control{align-items:stretch;flex-direction:column}.role-control label,.role-control .input,.role-control .btn{width:100%;min-width:0}}
</style>
