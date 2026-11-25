<template>
  <div ref="el" class="chart-box" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

// 通用图表容器：统一主题、自动 resize，避免每个页面重复初始化
const props = defineProps({
  option: { type: Object, required: true },
  height: { type: Number, default: 220 }
})

const el = ref(null)
let chart = null

function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value, null, { renderer: 'canvas' })
  chart.setOption(props.option, true)
}

const resize = () => chart && chart.resize()

onMounted(async () => {
  await nextTick()
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart && chart.dispose()
  chart = null
})
</script>

<style scoped>
.chart-box { width: 100%; }
</style>
