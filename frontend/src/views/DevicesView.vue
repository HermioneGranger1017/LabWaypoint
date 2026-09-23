<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api, assetUrl } from '../lib/api'
import { statusTone } from '../lib/format'

const devices = ref([])
const categories = ref([])
const search = ref('')
const categoryId = ref('')
const status = ref('')
const loading = ref(true)
const error = ref('')
let sequence = 0

const categoryName = (id) => categories.value.find(c => Number(c.id) === Number(id))?.name || '未分类'
const filtered = computed(() => devices.value.filter(device => {
  const q = search.value.trim().toLocaleLowerCase()
  const matchesText = !q || [device.name, device.model, device.serialNo, device.location].some(value => String(value || '').toLocaleLowerCase().includes(q))
  return matchesText && (!status.value || device.status === status.value)
}))

async function loadDevices() {
  const current = ++sequence
  loading.value = true
  error.value = ''
  try {
    const path = categoryId.value ? `/device?categoryId=${encodeURIComponent(categoryId.value)}` : '/device'
    const result = await api.get(path)
    if (current === sequence) devices.value = Array.isArray(result) ? result : []
  } catch (err) {
    if (current === sequence) error.value = err.message
  } finally {
    if (current === sequence) loading.value = false
  }
}

async function loadCategories() {
  try { const result = await api.get('/category'); categories.value = Array.isArray(result) ? result : [] } catch { /* device list remains usable */ }
}
function clearFilters() { search.value = ''; categoryId.value = ''; status.value = '' }
watch(categoryId, loadDevices)
onMounted(() => { loadCategories(); loadDevices() })
</script>

<template>
  <div>
    <div class="page-head"><div><div class="eyebrow">01 / EQUIPMENT ARCHIVE · 设备中心</div><h1>每件设备，<br>都有它的坐标。</h1><p>查询档案、确认状态，并找到需要的使用说明与借用入口。</p></div><div class="page-number" aria-hidden="true">01</div></div>
    <section class="panel filter-panel" aria-label="设备筛选"><div class="filter-row"><label class="field"><span>搜索设备 / SEARCH</span><input v-model="search" type="search" placeholder="名称、型号、编号或位置" /></label><label class="field"><span>设备分类 / CATEGORY</span><select v-model="categoryId"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="String(category.id)">{{ category.name }}</option></select></label><label class="field"><span>设备状态 / STATUS</span><select v-model="status"><option value="">全部状态</option><option v-for="item in ['在库', '已借出', '维修中', '报废']" :key="item">{{ item }}</option></select></label><button class="btn btn-quiet" type="button" @click="clearFilters">清除筛选 ×</button></div></section>
    <div class="result-bar"><span><b>{{ loading ? '—' : filtered.length }}</b> / 条设备档案</span><span>LAB EQUIPMENT · INDEX</span></div>
    <div v-if="loading" class="loading-line" role="status" aria-label="正在加载设备"></div>
    <div v-if="error" class="error-note">{{ error }} <button type="button" class="retry" @click="loadDevices">重试</button></div>
    <div v-if="!loading && !error && !filtered.length" class="empty-state"><strong>{{ devices.length ? '没有匹配的设备' : '暂无设备档案' }}</strong>{{ devices.length ? '试试调整关键词或筛选条件。' : '管理员添加设备后，档案会显示在这里。' }}</div>
    <div v-if="!loading && !error && filtered.length" class="device-grid"><RouterLink v-for="device in filtered" :key="device.id" :to="`/devices/${device.id}`" class="device-card"><div class="device-photo"><img v-if="device.imageUrl" :src="assetUrl(device.imageUrl)" :alt="`${device.name}照片`" @error="$event.target.style.display = 'none'; $event.target.nextElementSibling.style.display = 'grid'" /><div class="photo-placeholder" :style="device.imageUrl ? 'display:none' : ''" aria-hidden="true"><span></span></div><span class="status-pill" :class="statusTone(device.status)">{{ device.status || '未知状态' }}</span></div><div class="device-card-body"><span class="mini-label">{{ categoryName(device.categoryId) }}</span><h3>{{ device.name }}</h3><p>{{ device.model || '型号未填写' }}</p><div class="device-meta"><div><span>存放位置</span><br><b>{{ device.location || '—' }}</b></div><div><span>设备编号</span><br><b class="mono">{{ device.serialNo || '—' }}</b></div></div></div><div class="card-foot"><span>查看设备档案</span><b aria-hidden="true">↗</b></div></RouterLink></div>
  </div>
</template>

<style scoped>
.filter-panel { margin-bottom: 18px; }
.filter-row .btn { white-space: nowrap; }
.result-bar { border-bottom: 2px solid var(--ink); padding: 10px 0 11px; margin-bottom: 18px; display: flex; justify-content: space-between; gap: 16px; color: var(--soft); font-size: 11px; font-weight: 900; letter-spacing: .12em; }
.result-bar b { color: var(--red); font-size: 17px; }
.retry { border: 0; background: none; color: var(--deep-red); font-weight: 900; text-decoration: underline; margin-left: 8px; }
.device-card .device-meta b { line-height: 1.5; }
@media (max-width: 580px) { .result-bar span:last-child { display: none; } }
</style>
