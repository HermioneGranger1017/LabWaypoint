<script setup>
const topics = [
  { number: '01', title: '设备使用说明', detail: '按照设备档案整理操作、维护和收纳流程。', mark: '操' },
  { number: '02', title: '实验室规范', detail: '归纳借还、安全、清洁、协作与责任边界。', mark: '规' },
  { number: '03', title: '常见问题', detail: '集中记录高频故障、环境配置与解决思路。', mark: '问' },
  { number: '04', title: '调试经验', detail: '为团队留存可复用的排错路径与验证方法。', mark: '调' },
  { number: '05', title: '比赛复盘', detail: '为未来复盘预留归档主题与索引位置。', mark: '记' },
]
</script>

<template>
  <main class="wiki-page">
    <header class="page-title"><div><p class="eyebrow">LABORATORY KNOWLEDGE INDEX</p><h1>实验室 Wiki</h1><p>整理设备说明、规范、问题与团队经验的知识索引。</p></div><span class="index-stamp">档案<br>02</span></header>
    <section class="intro panel"><span class="intro-mark" aria-hidden="true">W</span><div><p class="eyebrow">SHARED REFERENCE</p><h2>把经验留在实验室</h2><p>这里规划实验室自己的知识分类与阅读入口，内容正在编制中。</p></div></section>
    <section class="topics" aria-label="Wiki 知识分类">
      <article v-for="topic in topics" :key="topic.number" class="topic panel">
        <span class="topic-number">{{ topic.number }}</span><span class="topic-mark" aria-hidden="true">{{ topic.mark }}</span><div class="topic-copy"><h2>{{ topic.title }}</h2><p>{{ topic.detail }}</p></div><span class="topic-status">内容编制中</span>
      </article>
    </section>
    <aside class="note"><strong>知识索引</strong><span>当前仅展示分类方向，文章内容与发布状态将在 Wiki 内容服务启用后呈现。</span></aside>
  </main>
</template>

<style scoped>
.wiki-page{display:grid;gap:22px;color:var(--color-text-primary,#171717)}.page-title{display:flex;justify-content:space-between;align-items:center;gap:16px}.page-title h1,.intro h2,.topic h2{margin:0;font-family:var(--font-display,inherit)}.page-title p:not(.eyebrow){margin:7px 0 0;color:var(--color-text-secondary,#3a3630)}.eyebrow{margin:0 0 6px;font-size:11px;font-weight:800;letter-spacing:.11em;color:var(--color-primary,#c82b27)}.panel{background:var(--color-surface,#f7f0e2);border:1px solid var(--color-ink,#171717);box-shadow:var(--shadow-panel,4px 4px 0 #171717)}.index-stamp{display:grid;place-items:center;width:54px;height:54px;background:var(--color-primary,#c82b27);color:white;text-align:center;font:800 14px/1.05 var(--font-display,inherit);transform:rotate(3deg)}.intro{position:relative;display:flex;align-items:center;gap:20px;padding:23px;overflow:hidden}.intro:after{content:"";position:absolute;right:-20px;bottom:-56px;width:180px;height:180px;border:18px solid #c82b2720;transform:rotate(45deg)}.intro-mark{display:grid;place-items:center;flex:none;width:64px;height:64px;background:var(--color-ink,#171717);color:var(--color-paper,#f7f0e2);font:900 35px var(--font-display,inherit)}.intro h2{font-size:22px}.intro p:last-child{margin:7px 0 0;color:var(--color-text-secondary,#3a3630)}.topics{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.topic{position:relative;display:grid;grid-template-columns:36px 52px 1fr;align-items:center;gap:13px;min-height:112px;padding:17px}.topic-number{align-self:start;color:var(--color-primary,#c82b27);font:800 12px var(--font-mono,monospace)}.topic-mark{display:grid;place-items:center;width:48px;height:48px;border:2px solid var(--color-ink,#171717);background:var(--color-surface-muted,#e9d8b9);font:900 24px var(--font-display,inherit)}.topic h2{font-size:17px}.topic p{margin:5px 0 0;color:var(--color-text-secondary,#3a3630);font-size:13px;line-height:1.55}.topic-status{grid-column:3;justify-self:start;margin-top:2px;padding:3px 7px;background:#ead9c0;font-size:11px;font-weight:700}.note{display:flex;gap:12px;align-items:flex-start;padding:14px 16px;border-left:5px solid var(--color-primary,#c82b27);background:var(--color-surface-muted,#e9d8b9);font-size:13px;line-height:1.6}.note strong{flex:none}@media(max-width:650px){.topics{grid-template-columns:1fr}.intro{align-items:flex-start;padding:18px}.intro-mark{width:52px;height:52px}.topic{grid-template-columns:28px 45px 1fr;gap:10px;padding:14px}.topic-mark{width:42px;height:42px}.page-title{align-items:flex-start}}
</style>
