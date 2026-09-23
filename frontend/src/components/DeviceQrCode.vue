<script setup>
import { computed, ref, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({ deviceId: { type: [Number, String], required: true } })
const image = ref('')
const error = ref('')
const targetUrl = computed(() => {
  const publicBase = (import.meta.env.VITE_PUBLIC_APP_URL || `${window.location.origin}${window.location.pathname}`).replace(/\/$/, '')
  return `${publicBase}/#/devices/${encodeURIComponent(props.deviceId)}`
})
const isLocal = computed(() => /localhost|127\.0\.0\.1/.test(targetUrl.value))

watch(targetUrl, async (url) => {
  image.value = ''
  error.value = ''
  try {
    image.value = await QRCode.toDataURL(url, { width: 240, margin: 1, errorCorrectionLevel: 'M', color: { dark: '#171717', light: '#fffaf0' } })
  } catch {
    error.value = '暂时无法生成二维码，请使用下方链接打开设备。'
  }
}, { immediate: true })
</script>

<template>
  <div class="device-qr">
    <div v-if="image" class="qr-paper"><img :src="image" :alt="`设备 ${deviceId} 的访问二维码`" /></div>
    <p v-else class="hint" role="status">{{ error || '正在生成二维码…' }}</p>
    <a class="qr-url mono" :href="targetUrl">{{ targetUrl }}</a>
    <p v-if="isLocal" class="qr-caution">当前链接是本机开发地址。用手机扫描前，请配置可从手机访问的 VITE_PUBLIC_APP_URL。</p>
  </div>
</template>

<style scoped>
.device-qr { width: 100%; display: grid; gap: 9px; }
.qr-paper { width: 185px; height: 185px; background: #fffaf0; border: 2px solid var(--ink); padding: 4px; }
.qr-paper img { display: block; width: 100%; height: 100%; object-fit: contain; }
.qr-url { font-size: 11px; line-height: 1.5; overflow-wrap: anywhere; color: var(--deep-red); text-decoration: underline; }
.qr-caution { color: var(--soft); font-size: 11px; line-height: 1.5; margin: 0; }
</style>
