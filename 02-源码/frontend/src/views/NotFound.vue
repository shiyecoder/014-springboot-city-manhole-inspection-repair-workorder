<template>
  <div class="nf">
    <div class="box">
      <h2>页面不存在或尚未开通</h2>
      <p>当前地址：<span class="mono">{{ $route.fullPath }}</span></p>
      <p class="muted">该页面可能不在当前角色权限范围内，或路由尚未配置。</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回工作台</el-button>
    </div>
  </div>
</template>

<script setup>
// 兜底页：避免未配置路由时整站白屏
</script>

<style scoped>
.nf { height: 100vh; display: grid; place-items: center; background: #F4F8F5; }
.box { background: #fff; border: 1px solid #E3EBE6; border-radius: 8px; padding: 32px 40px; text-align: center; }
h2 { margin: 0 0 8px; font-size: 18px; }
p { margin: 4px 0; font-size: 13px; color: #5C6B63; }
</style>
