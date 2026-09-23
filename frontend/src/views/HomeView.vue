<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, assetUrl } from '../lib/api'
import { formatDate, statusTone } from '../lib/format'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const devices = ref([])
const borrows = ref([])
const loading = ref(true)
const error = ref('')
const greeting = computed(() => { const hour = new Date().getHours(); return hour < 11 ? '早上好' : hour < 18 ? '下午好' : '晚上好' })
const stats = computed(() => [
  { label: '设备总数', value: devices.value.length, english: 'TOTAL', featured: true },
  { label: '在库设备', value: devices.value.filter(d => d.status === '在库').length, english: 'AVAILABLE' },
  { label: '借出设备', value: devices.value.filter(d => d.status === '已借出').length, english: 'BORROWED' },
  { label: '维修中', value: devices.value.filter(d => d.status === '维修中').length, english: 'MAINTENANCE' },
  { label: '已报废', value: devices.value.filter(d => d.status === '报废').length, english: 'RETIRED' },
])
const latestDevices = computed(() => [...devices.value].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, 4))
const latestBorrows = computed(() => [...borrows.value].sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, 4))

async function load() {
  loading.value = true
  error.value = ''
  const [deviceResult, borrowResult] = await Promise.allSettled([api.get('/device'), api.get('/borrow/my')])
  if (deviceResult.status === 'fulfilled') devices.value = Array.isArray(deviceResult.value) ? deviceResult.value : []
  if (borrowResult.status === 'fulfilled') borrows.value = Array.isArray(borrowResult.value) ? borrowResult.value : []
  if (deviceResult.status === 'rejected' || borrowResult.status === 'rejected') error.value = '部分数据暂时无法载入，请稍后重试。'
  loading.value = false
}
onMounted(load)
</script>

<template>
  <div class="home-page">
    <div class="page-head"><div><div class="eyebrow">00 / COMMAND CENTER · 控制中心</div><h1>{{ greeting }}，{{ auth.user?.realName || auth.user?.username || '实验室成员' }}。</h1><p>今天，从这里查看设备状态、你的借还进度和实验室知识。</p></div><div class="page-number" aria-hidden="true">00</div></div>
    <section class="poster-hero">
      <div class="poster-hero-content"><div class="eyebrow" style="color:#ffe0cf">LABWAYPOINT / EXPLORE TOGETHER</div><h1>资源就位，<br>探索开始。</h1><p>让设备、教程与协作流程井然有序。你要找的下一件工具，从这里出发。</p><div class="hero-actions"><RouterLink class="btn btn-dark" to="/devices">进入设备中心 <span aria-hidden="true">↗</span></RouterLink><RouterLink class="btn" to="/wiki">打开实验室 Wiki <span aria-hidden="true">↗</span></RouterLink></div></div><span class="hero-number" aria-hidden="true">01</span>
    </section>
    <div v-if="loading" class="loading-line" role="status" aria-label="正在加载首页数据"></div>
    <div v-if="error" class="error-note home-error" role="alert">{{ error }} <button type="button" @click="load">重新加载</button></div>

    <div class="section-label">设备状态 <span>/ EQUIPMENT STATUS</span></div>
    <div class="stats-grid"><div v-for="item in stats" :key="item.english" class="stat-card" :class="{ featured: item.featured }"><small>{{ item.english }} / {{ item.label }}</small><strong>{{ loading ? '—' : String(item.value).padStart(2, '0') }}</strong></div></div>

    <div class="home-columns">
      <section class="panel home-section"><div class="section-heading"><div><span class="mini-label">RECENT EQUIPMENT</span><h2>最近设备</h2></div><RouterLink to="/devices">查看全部 ↗</RouterLink></div><div v-if="!loading && !latestDevices.length" class="empty-state"><strong>暂无设备档案</strong>设备添加后会显示在这里。</div><RouterLink v-for="device in latestDevices" :key="device.id" class="recent-row" :to="`/devices/${device.id}`"><div class="recent-thumb"><img v-if="device.imageUrl" :src="assetUrl(device.imageUrl)" alt="" @error="$event.target.style.display='none'" /><span v-else aria-hidden="true">◩</span></div><div class="recent-main"><b>{{ device.name }}</b><small>{{ device.model || device.serialNo || '设备档案' }}</small></div><span class="status-pill" :class="statusTone(device.status)">{{ device.status || '未知' }}</span><span aria-hidden="true">↗</span></RouterLink></section>
      <section class="panel home-section"><div class="section-heading"><div><span class="mini-label">BORROWING RECORDS</span><h2>我的借还动态</h2></div><RouterLink to="/borrow/my">查看全部 ↗</RouterLink></div><div v-if="!loading && !latestBorrows.length" class="empty-state"><strong>暂无借还记录</strong>你提交的借用申请会显示在这里。</div><RouterLink v-for="record in latestBorrows" :key="record.id" class="recent-row borrow-row" to="/borrow/my"><div class="recent-index">#{{ String(record.id).padStart(2, '0') }}</div><div class="recent-main"><b>{{ record.deviceName || `设备 #${record.deviceId}` }}</b><small>{{ formatDate(record.createdAt) }}</small></div><span class="status-pill" :class="statusTone(record.status)">{{ record.status || '—' }}</span><span aria-hidden="true">↗</span></RouterLink></section>
    </div>

    <div class="section-label">快速入口 <span>/ DIRECT ACCESS</span></div>
    <div class="grid grid-3"><RouterLink to="/devices" class="quick-card"><span class="quick-top">01 / 设备档案 <b>↗</b></span><strong>查找一件设备</strong><p>浏览状态、教程与存放位置。</p></RouterLink><RouterLink to="/scan" class="quick-card"><span class="quick-top">02 / 快速定位 <b>↗</b></span><strong>扫码查询</strong><p>用二维码快速打开设备档案。</p></RouterLink><RouterLink to="/borrow/my" class="quick-card"><span class="quick-top">03 / 我的事项 <b>↗</b></span><strong>查看借还</strong><p>跟进申请、借用和归还状态。</p></RouterLink></div>

    <div class="wiki-callout"><div><span class="mini-label">KNOWLEDGE ARCHIVE / 知识档案</span><h2>经验需要被记录，<br>也需要被找到。</h2><p>实验室 Wiki 正在整理设备使用说明、实验室规范与调试经验。</p></div><RouterLink to="/wiki" class="btn btn-primary">进入 Wiki ↗</RouterLink><span class="wiki-shape" aria-hidden="true"></span></div>
  </div>
</template>

<style scoped>
.home-error { margin-top: 15px; display: flex; justify-content: space-between; gap: 12px; }
.home-error button { border: 0; background: none; color: var(--deep-red); text-decoration: underline; font-weight: 900; }
.stats-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; }
.home-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 17px; margin-top: 30px; }
.home-section { min-width: 0; }
.section-heading { display: flex; justify-content: space-between; align-items: end; gap: 15px; padding-bottom: 15px; border-bottom: 2px solid var(--ink); margin-bottom: 4px; }
.section-heading h2 { margin: 4px 0 0; font-size: 22px; }
.section-heading a { font-size: 12px; font-weight: 900; color: var(--deep-red); white-space: nowrap; }
.home-section .empty-state { margin-top: 16px; }
.recent-row { min-width: 0; display: flex; align-items: center; gap: 12px; border-bottom: 1px solid var(--line); padding: 10px 0; font-size: 18px; }
.recent-row:last-child { border-bottom: 0; }
.recent-row:hover .recent-main b { color: var(--red); }
.recent-thumb { width: 42px; height: 42px; flex: 0 0 42px; display: grid; place-items: center; background: var(--muted); overflow: hidden; font-size: 22px; }
.recent-thumb img { width: 100%; height: 100%; object-fit: cover; }
.recent-main { min-width: 0; display: grid; gap: 3px; flex: 1; }
.recent-main b { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recent-main small { font-size: 11px; color: var(--soft); }
.recent-index { font: 900 15px "Arial Black", Impact, sans-serif; color: var(--red); }
.borrow-row { min-height: 62px; }
.wiki-callout { overflow: hidden; position: relative; display: flex; align-items: end; justify-content: space-between; gap: 30px; margin-top: 34px; padding: 25px 29px; border: 2px solid var(--ink); background: var(--muted); }
.wiki-callout > div, .wiki-callout > a { z-index: 1; }
.wiki-callout h2 { font: 900 25px/1.25 "Arial Black", Impact, "Microsoft YaHei", sans-serif; margin: 9px 0 7px; }
.wiki-callout p { margin: 0; color: var(--soft); font-size: 13px; }
.wiki-shape { position: absolute; width: 180px; height: 320px; right: 16%; top: -75px; background: var(--red); transform: rotate(37deg); opacity: .14; }
@media (max-width: 1250px) { .stats-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 980px) { .home-columns { grid-template-columns: 1fr; } }
@media (max-width: 580px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } .stats-grid .stat-card:first-child { grid-column: 1 / -1; } .wiki-callout { flex-direction: column; align-items: start; padding: 21px; } .wiki-callout .btn { width: 100%; } .recent-row .status-pill { font-size: 10px; } }
</style>
