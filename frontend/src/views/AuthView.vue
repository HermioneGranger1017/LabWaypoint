<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { api } from '../lib/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isRegister = computed(() => route.name === 'register')
const form = reactive({ username: '', password: '', confirm: '', studentNo: '', major: '' })
const busy = ref(false)
const error = ref('')
const notice = computed(() => route.query.registered ? '申请已提交。请等待管理员审核通过后再登录。' : '')

async function submit() {
  error.value = ''
  const username = form.username.trim()
  if (!/^\S{5,16}$/.test(username) || !/^\S{5,16}$/.test(form.password)) {
    error.value = '用户名和密码都需要 5–16 位，且不能包含空格。'
    return
  }
  if (isRegister.value) {
    if (form.password !== form.confirm) { error.value = '两次输入的密码不一致。'; return }
    if (!/^[A-Za-z0-9-]{1,32}$/.test(form.studentNo.trim())) { error.value = '学号需为 1–32 位字母、数字或连字符。'; return }
    if (form.major.trim().length < 2 || form.major.trim().length > 100) { error.value = '专业需要填写 2–100 个字符。'; return }
  }
  busy.value = true
  try {
    if (isRegister.value) {
      await api.post('/user/register', { username, password: form.password, studentNo: form.studentNo.trim(), major: form.major.trim() })
      router.replace({ name: 'login', query: { registered: '1' } })
      form.password = ''
      form.confirm = ''
    } else {
      await auth.login(username, form.password)
      router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/home')
    }
  } catch (err) {
    error.value = err.message || '操作失败，请稍后重试。'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-poster">
      <div class="poster-header"><span class="poster-mark" aria-hidden="true">✦</span><b>LAB<br>WAYPOINT</b><small>实验室工作平台 / SYSTEM 01</small></div>
      <div class="poster-art" aria-hidden="true"><span class="art-ring"></span><span class="art-line"></span><span class="art-square"></span><span class="art-stripes"></span></div>
      <div class="poster-message">
        <div class="poster-caption">EQUIPMENT · KNOWLEDGE · ACTION</div>
        <h1>让每一次<br><em>探索</em>都有坐标。</h1>
        <p>从设备档案到借还协作，把实验室的资源与知识组织在同一张工作台上。</p>
      </div>
      <div class="poster-footer"><span>LW / 01</span><span>面向实验室的共同工作</span><span>2026 ↗</span></div>
    </div>

    <main class="auth-main">
      <div class="auth-top"><span>WORKSPACE ACCESS / 访问入口</span><span class="auth-top-rule"></span><span>01 — 02</span></div>
      <div class="auth-card">
        <div class="auth-kicker"><span>{{ isRegister ? '02 / 申请加入' : '01 / 进入工作台' }}</span><span class="auth-star" aria-hidden="true">✳</span></div>
        <h2>{{ isRegister ? '建立你的档案' : '欢迎回来' }}<span class="title-stop">.</span></h2>
        <p class="auth-description">{{ isRegister ? '填写申请资料。管理员审核通过后即可登录。' : '使用你的实验室账号，继续今天的工作。' }}</p>
        <div v-if="notice && !isRegister" class="success-note" role="status">{{ notice }}</div>
        <div v-if="error" class="error-note" role="alert">{{ error }}</div>
        <form class="auth-form" @submit.prevent="submit">
          <label class="field"><span>用户名 / USERNAME</span><input v-model="form.username" autocomplete="username" maxlength="16" placeholder="输入 5–16 位用户名" required /></label>
          <template v-if="isRegister">
            <label class="field"><span>学号 / STUDENT ID</span><input v-model="form.studentNo" autocomplete="off" maxlength="32" placeholder="填写学号" required /></label>
            <label class="field"><span>专业 / MAJOR</span><input v-model="form.major" autocomplete="organization-title" maxlength="100" placeholder="填写专业" required /></label>
          </template>
          <label class="field"><span>密码 / PASSWORD</span><input v-model="form.password" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" minlength="5" maxlength="16" placeholder="输入 5–16 位密码" required /></label>
          <label v-if="isRegister" class="field"><span>确认密码 / CONFIRM</span><input v-model="form.confirm" type="password" autocomplete="new-password" minlength="5" maxlength="16" placeholder="再次输入密码" required /></label>
          <button class="btn btn-primary auth-submit" type="submit" :disabled="busy">{{ busy ? '处理中…' : isRegister ? '提交注册申请' : '进入工作台' }} <span aria-hidden="true">↗</span></button>
        </form>
        <div class="auth-switch"><span>{{ isRegister ? '已有账号？' : '还没有账号？' }}</span><RouterLink :to="isRegister ? '/login' : '/register'">{{ isRegister ? '返回登录' : '申请加入' }} ↗</RouterLink></div>
      </div>
      <div class="auth-bottom"><span>LABWAYPOINT</span><span>设备 / 知识 / 协作</span><span>© 2026</span></div>
    </main>
  </div>
</template>

<style scoped>
.auth-shell { display: grid; grid-template-columns: minmax(370px, 48%) 1fr; min-height: 100dvh; }
.auth-poster { min-height: 100dvh; overflow: hidden; position: relative; display: flex; flex-direction: column; justify-content: space-between; background: var(--red); color: var(--surface); border-right: 3px solid var(--ink); padding: 29px 34px 25px; }
.poster-header { display: flex; align-items: center; gap: 14px; z-index: 1; }
.poster-mark { display: grid; place-items: center; width: 48px; height: 48px; background: var(--surface); color: var(--red); font-size: 35px; line-height: 1; }
.poster-header b { font: 900 21px/.92 "Arial Black", Impact, sans-serif; letter-spacing: -.06em; }
.poster-header small { align-self: end; margin-left: auto; font-size: 10px; font-weight: 900; letter-spacing: .09em; }
.poster-art { position: absolute; inset: 0; overflow: hidden; }
.art-ring { position: absolute; width: 380px; height: 380px; border: 75px solid #f4dec0; border-radius: 50%; top: 10%; right: -115px; }
.art-ring::after { content: ""; position: absolute; inset: -76px; border: 2px solid var(--ink); border-radius: 50%; }
.art-line { position: absolute; width: 800px; height: 86px; top: 30%; left: -30%; background: var(--ink); transform: rotate(-39deg); }
.art-square { position: absolute; height: 200px; width: 200px; border: 30px solid #f4dec0; transform: rotate(30deg); left: 4%; top: 19%; opacity: .75; }
.art-stripes { position: absolute; width: 170px; height: 430px; right: -10px; bottom: 3%; transform: skew(-25deg); background: repeating-linear-gradient(90deg, transparent 0 15px, rgba(23,23,23,.32) 15px 20px); }
.poster-message { position: relative; z-index: 1; max-width: 580px; margin: auto 0 9%; }
.poster-caption { display: inline-block; font-size: 10px; font-weight: 900; letter-spacing: .2em; border-bottom: 2px solid var(--surface); padding-bottom: 10px; }
.poster-message h1 { font: 900 clamp(52px, 5vw, 86px)/1.08 "Arial Black", Impact, "Microsoft YaHei", sans-serif; letter-spacing: -.08em; margin: 19px 0 18px; text-shadow: 4px 4px 0 rgba(23,23,23,.75); }
.poster-message em { font-style: normal; color: var(--ink); text-shadow: 2px 2px 0 var(--surface); }
.poster-message p { max-width: 400px; font-size: 14px; font-weight: 700; line-height: 1.8; }
.poster-footer { position: relative; z-index: 1; display: flex; justify-content: space-between; gap: 12px; border-top: 2px solid var(--surface); padding-top: 14px; font-size: 10px; letter-spacing: .12em; font-weight: 900; }
.auth-main { min-width: 0; display: flex; flex-direction: column; justify-content: space-between; padding: 30px clamp(24px, 6vw, 100px) 24px; }
.auth-top, .auth-bottom { display: flex; align-items: center; gap: 16px; font-size: 10px; font-weight: 900; letter-spacing: .13em; }
.auth-top-rule { flex: 1; height: 1px; background: var(--line); }
.auth-bottom { justify-content: space-between; color: var(--soft); }
.auth-card { width: min(100%, 510px); margin: 60px auto; background: var(--surface); border: 2px solid var(--ink); box-shadow: 8px 8px 0 var(--ink); padding: clamp(22px, 3vw, 38px); }
.auth-kicker { display: flex; justify-content: space-between; color: var(--deep-red); font-size: 11px; font-weight: 900; letter-spacing: .13em; }
.auth-star { font-size: 30px; line-height: .6; }
.auth-card h2 { font: 900 clamp(32px, 3.6vw, 51px)/1.15 "Arial Black", Impact, "Microsoft YaHei", sans-serif; letter-spacing: -.06em; margin: 16px 0 8px; }
.title-stop { color: var(--red); }
.auth-description { font-size: 13px; color: var(--soft); margin: 0 0 26px; line-height: 1.6; }
.auth-card .success-note, .auth-card .error-note { margin-bottom: 18px; }
.auth-form { display: grid; gap: 16px; }
.auth-submit { margin-top: 8px; width: 100%; justify-content: space-between; padding: 12px 16px; font-size: 14px; }
.auth-submit span { font-size: 20px; }
.auth-switch { border-top: 1px solid var(--line); padding-top: 18px; margin-top: 25px; display: flex; justify-content: space-between; gap: 12px; font-size: 13px; }
.auth-switch a { color: var(--deep-red); font-weight: 900; }
@media (max-width: 930px) { .auth-shell { grid-template-columns: 1fr; } .auth-poster { min-height: 150px; padding: 20px 24px; border-right: 0; border-bottom: 3px solid var(--ink); } .poster-art { opacity: .25; } .poster-message, .poster-footer { display: none; } .auth-main { min-height: calc(100dvh - 150px); } .auth-card { margin: 35px auto; } }
@media (max-width: 580px) { .auth-poster { min-height: 110px; padding: 17px; } .poster-header small { display: none; } .auth-main { min-height: calc(100dvh - 110px); padding: 22px 17px 28px; } .auth-card { margin: 26px auto 35px; box-shadow: 5px 5px 0 var(--ink); } .auth-bottom { font-size: 9px; } }
</style>
