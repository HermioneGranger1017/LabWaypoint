<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import jsQR from 'jsqr'

const router = useRouter()
const manualValue = ref('')
const message = ref('')
const error = ref('')
const cameraState = ref('idle')
const video = ref(null)
const imageInput = ref(null)
let stream = null
let frameId = 0
let detector = null
let scanning = false
let lastFrameScan = 0
let scanCanvas = null
const secure = computed(() => window.isSecureContext || ['localhost', '127.0.0.1', '::1'].includes(window.location.hostname))
const barcodeAvailable = computed(() => typeof window.BarcodeDetector === 'function')

function resolveDeviceId(value) {
  const text = String(value || '').trim()
  if (/^\d+$/.test(text)) return Number(text)
  let candidates = [text]
  try {
    const url = new URL(text, window.location.origin)
    const allowedOrigins = [window.location.origin]
    if (import.meta.env.VITE_PUBLIC_APP_URL) {
      try { allowedOrigins.push(new URL(import.meta.env.VITE_PUBLIC_APP_URL).origin) } catch { /* Ignore an invalid optional setting. */ }
    }
    if (!allowedOrigins.includes(url.origin)) return null
    candidates = [url.href, url.pathname, url.hash, url.search]
    const queryId = url.searchParams.get('deviceId') || url.searchParams.get('id')
    if (queryId && /^\d+$/.test(queryId)) return Number(queryId)
  } catch { /* Treat the input as a plain path or QR payload. */ }
  for (const candidate of candidates) {
    const match = candidate.match(/(?:\/devices\/|device(?:Id)?[=:])([0-9]+)(?:\b|\/|$)/i)
    if (match) return Number(match[1])
  }
  return null
}
function openValue(value) {
  error.value = ''; message.value = ''
  const id = resolveDeviceId(value)
  if (!id || id < 1) { error.value = '无法识别本站设备编号。请输入正整数设备 ID，或使用本站包含 /devices/{编号} 的设备链接。'; return false }
  stopCamera()
  router.push(`/devices/${id}`)
  return true
}
function unsupportedMessage() {
  if (!secure.value) return '当前页面不是安全上下文。浏览器通常只允许 HTTPS 或 localhost 使用摄像头；可改用图片解析或手动输入。'
  return ''
}
async function startCamera() {
  error.value = ''; message.value = ''
  const limitation = unsupportedMessage()
  if (limitation) { error.value = limitation; cameraState.value = 'unsupported'; return }
  if (!navigator.mediaDevices?.getUserMedia) { error.value = '浏览器未提供摄像头访问接口，请使用图片解析或手动输入。'; cameraState.value = 'unsupported'; return }
  try {
    detector = barcodeAvailable.value ? new window.BarcodeDetector({ formats: ['qr_code'] }) : null
    stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false })
    cameraState.value = 'starting'
    await new Promise((resolve) => requestAnimationFrame(resolve))
    if (!video.value) throw new Error('摄像头预览未能启动。')
    video.value.srcObject = stream
    await video.value.play()
    cameraState.value = 'scanning'; scanning = true
    scanFrame()
  } catch (err) {
    stopCamera()
    if (err?.name === 'NotAllowedError' || err?.name === 'PermissionDeniedError') error.value = '摄像头权限未获准。请在浏览器设置中允许访问，或使用图片解析/手动输入。'
    else if (err?.name === 'NotFoundError') error.value = '未找到可用摄像头。请使用图片解析或手动输入。'
    else error.value = err?.message || '无法启动摄像头，请使用图片解析或手动输入。'
    cameraState.value = 'error'
  }
}
async function scanFrame() {
  if (!scanning || !video.value || video.value.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) { if (scanning) frameId = requestAnimationFrame(scanFrame); return }
  const now = performance.now()
  if (now - lastFrameScan > 120) {
    lastFrameScan = now
    try {
      let raw = ''
      if (detector) {
        const codes = await detector.detect(video.value)
        raw = codes[0]?.rawValue || ''
      }
      if (!raw) raw = decodeWithJsQr(video.value)
      if (raw) {
        if (openValue(raw)) return
      }
    } catch { /* Continue scanning; some frames cannot be decoded while the camera adjusts. */ }
  }
  if (scanning) frameId = requestAnimationFrame(scanFrame)
}
function decodeWithJsQr(source) {
  if (!scanCanvas) scanCanvas = document.createElement('canvas')
  const sourceWidth = source.videoWidth || source.width
  const sourceHeight = source.videoHeight || source.height
  if (!sourceWidth || !sourceHeight) return ''
  const scale = Math.min(1, 900 / Math.max(sourceWidth, sourceHeight))
  scanCanvas.width = Math.max(1, Math.round(sourceWidth * scale))
  scanCanvas.height = Math.max(1, Math.round(sourceHeight * scale))
  const context = scanCanvas.getContext('2d', { willReadFrequently: true })
  if (!context) return ''
  context.drawImage(source, 0, 0, scanCanvas.width, scanCanvas.height)
  const pixels = context.getImageData(0, 0, scanCanvas.width, scanCanvas.height)
  return jsQR(pixels.data, pixels.width, pixels.height, { inversionAttempts: 'attemptBoth' })?.data || ''
}
function stopCamera() {
  scanning = false
  if (frameId) cancelAnimationFrame(frameId)
  frameId = 0
  if (stream) stream.getTracks().forEach((track) => track.stop())
  stream = null
  if (video.value) video.value.srcObject = null
  if (cameraState.value === 'scanning' || cameraState.value === 'starting') cameraState.value = 'idle'
}
async function chooseImage() { error.value = ''; imageInput.value?.click() }
async function readImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''; message.value = ''
  try {
    if (!file.type.startsWith('image/')) throw new Error('请选择二维码图片文件。')
    const bitmap = await createImageBitmap(file)
    let raw = ''
    try {
      if (barcodeAvailable.value) {
        try {
          const codes = await new window.BarcodeDetector({ formats: ['qr_code'] }).detect(bitmap)
          raw = codes[0]?.rawValue || ''
        } catch { /* Use the bundled decoder as a fallback. */ }
      }
      if (!raw) raw = decodeWithJsQr(bitmap)
    } finally { bitmap.close?.() }
    if (!raw) throw new Error('图片中没有识别到二维码。可以换一张清晰图片，或手动输入设备编号/链接。')
    openValue(raw)
  } catch (err) { error.value = err.message || '无法解析这张图片中的二维码。' }
}
onBeforeUnmount(stopCamera)
</script>

<template>
  <section class="scan-page">
    <header class="scan-head"><div><p class="eyebrow">QUICK ACCESS / 设备查询</p><h1 class="page-title">扫码查询</h1><p class="intro">扫描设备二维码，或输入设备编号/设备链接打开档案。</p></div><span class="scan-mark" aria-hidden="true">⌗</span></header>
    <div class="scan-grid">
      <section class="panel scan-panel"><h2>摄像头扫描</h2><div class="camera-frame"><video v-if="cameraState === 'scanning' || cameraState === 'starting'" ref="video" playsinline muted aria-label="二维码摄像头画面" /><div v-else class="camera-placeholder"><b>⌗</b><span>{{ cameraState === 'scanning' ? '将二维码置于画面中央' : '摄像头预览将在这里显示' }}</span></div></div><p v-if="!secure" class="hint">{{ unsupportedMessage() }}</p><p v-else class="hint">{{ barcodeAvailable ? '支持原生识别；无法解码时会使用 jsQR 回退。需要 HTTPS 或 localhost。' : '浏览器不支持 BarcodeDetector，将使用 jsQR 识别摄像头画面。摄像头需要 HTTPS 或 localhost。' }}</p><p v-if="error" class="error-note" role="alert">{{ error }}</p><p v-if="message" class="success-note" role="status">{{ message }}</p><div class="actions"><button v-if="cameraState !== 'scanning'" class="btn btn-primary" type="button" :disabled="!secure" @click="startCamera">开启摄像头</button><button v-else class="btn" type="button" @click="stopCamera">停止扫描</button><button class="btn" type="button" @click="chooseImage">选择二维码图片</button><input ref="imageInput" class="visually-hidden" type="file" accept="image/*" @change="readImage" /></div></section>
      <section class="panel manual-panel"><p class="eyebrow">ALTERNATIVE / 手动打开</p><h2>输入设备编号或链接</h2><p class="hint">支持正整数设备 ID，例如 <code>18</code>，也支持含 <code>/devices/18</code> 的链接或二维码内容。</p><form @submit.prevent="openValue(manualValue)"><label class="field"><span>设备编号 / URL</span><input v-model="manualValue" class="input" autocomplete="off" placeholder="例如 18 或 https://lab.example/#/devices/18" /></label><p v-if="error" class="error-note" role="alert">{{ error }}</p><button class="btn btn-primary" type="submit">打开设备档案 ↗</button></form></section>
    </div>
  </section>
</template>

<style scoped>
.scan-page{max-width:1080px;margin:auto;padding:12px 0 48px}.scan-head{display:flex;justify-content:space-between;align-items:end;border-bottom:3px solid var(--ink);padding-bottom:18px;margin-bottom:20px}.eyebrow{margin:0;color:var(--deep-red);font-size:12px;font-weight:900;letter-spacing:.12em}.page-title{font-size:clamp(32px,5vw,52px);margin:6px 0}.intro{margin:6px 0 0;color:var(--soft)}.scan-mark{font:900 82px/1 Impact,sans-serif;color:var(--red)}.scan-grid{display:grid;grid-template-columns:1.1fr .9fr;gap:18px}.scan-panel,.manual-panel{padding:clamp(16px,3vw,26px)}h2{font-size:22px;margin:0 0 16px}.camera-frame{aspect-ratio:4/3;display:grid;place-items:center;background:#171717;border:2px solid var(--ink);overflow:hidden}.camera-frame video{width:100%;height:100%;object-fit:cover}.camera-placeholder{height:100%;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;color:#f4eedf;text-align:center}.camera-placeholder b{font:900 72px/1 Impact,sans-serif;color:var(--red)}.camera-placeholder span{font-size:13px}.hint{color:var(--soft);font-size:13px;line-height:1.6}.actions{display:flex;flex-wrap:wrap;gap:10px;margin-top:14px}.field{display:grid;gap:7px;font-weight:800;margin:20px 0}.input{width:100%;min-height:48px;border:2px solid var(--ink);border-radius:0;background:var(--surface);padding:10px;font:inherit}.error-note{padding:10px;background:#ffe1da;color:#811b16;font-size:13px}.success-note{padding:10px;background:#e5ecd9;font-size:13px}.visually-hidden{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap}code{overflow-wrap:anywhere}@media(max-width:760px){.scan-grid{grid-template-columns:1fr}.scan-mark{font-size:60px}}@media(max-width:560px){.scan-head{align-items:flex-start}.actions{flex-direction:column}.actions .btn{width:100%}}
</style>
