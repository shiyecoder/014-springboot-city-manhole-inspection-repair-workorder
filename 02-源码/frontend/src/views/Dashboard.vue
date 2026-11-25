<template>
  <div class="page dash">
    <div class="page-head">
      <div>
        <h1>{{ greeting }}，{{ user?.realName }}</h1>
        <p>数据截止 {{ now }} · 统计范围：{{ scopeText }} · 最近 30 天</p>
      </div>
      <div class="right">
        <el-select v-model="days" style="width: 140px" @change="load">
          <el-option :value="30" label="最近 30 天" />
          <el-option :value="90" label="本季度" />
          <el-option :value="365" label="本年" />
        </el-select>
        <el-button @click="$router.push('/analysis')">统计分析</el-button>
        <el-button type="primary" @click="$router.push('/issues')">快速上报</el-button>
      </div>
    </div>

    <div class="metrics">
      <div v-for="m in metrics" :key="m.name" class="metric" :class="m.tone">
        <div class="k">{{ m.name }}</div>
        <div class="v">{{ m.value }}<small>{{ m.unit }}</small></div>
        <div class="d">{{ m.desc }}</div>
      </div>
    </div>

    <div class="row-a">
      <div class="card">
        <div class="card-h"><h3>问题上报与关闭趋势</h3><span class="muted">按 3 天汇总</span></div>
        <ChartBox :option="trendOption" :height="196" />
      </div>
      <div class="card">
        <div class="card-h"><h3>工单状态分布</h3><span class="muted">在办 124</span></div>
        <ChartBox :option="statusOption" :height="196" />
      </div>
      <div class="card">
        <div class="card-h"><h3>缺损类型构成</h3></div>
        <ChartBox :option="damageOption" :height="196" />
      </div>
    </div>

    <div class="row-b">
      <div class="card">
        <div class="card-h"><h3>责任队伍处置量</h3><span class="muted">近 30 天</span></div>
        <ChartBox :option="teamOption" :height="170" />
      </div>
      <div class="card">
        <div class="card-h"><h3>风险分级结果分布</h3><span class="muted">创新模块</span></div>
        <ChartBox :option="riskOption" :height="170" />
      </div>
      <div class="card">
        <div class="card-h"><h3>道路问题热力</h3><span class="muted">未关闭数</span></div>
        <ChartBox :option="roadOption" :height="170" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getDashboard } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDateTime, RISK_COLOR, CHART_COLORS } from '@/utils/format'
import ChartBox from '@/components/ChartBox.vue'

const store = useUserStore()
const user = computed(() => store.user)
const days = ref(30)
const now = ref(formatDateTime(new Date()))
const data = ref({ metrics: [], trend: [], statusDist: [], damageDist: [], teamRank: [], riskDist: [], roadHeat: [] })

const scopeText = computed(() => (user.value?.dataScope === 'ALL' ? '全市 6 个养护片区' : '本人负责范围'))
const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 6 ? '凌晨好' : h < 12 ? '上午好' : h < 18 ? '下午好' : '晚上好'
})
const metrics = computed(() => data.value.metrics || [])

const axis = { axisLine: { lineStyle: { color: '#E3EBE6' } }, axisLabel: { color: '#8A9990', fontSize: 11 }, splitLine: { lineStyle: { color: '#EDF2EF' } } }

const trendOption = computed(() => ({
  grid: { left: 34, right: 12, top: 22, bottom: 24 },
  tooltip: { trigger: 'axis' },
  legend: { right: 0, top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#5C6B63' } },
  xAxis: { type: 'category', data: (data.value.trend || []).map((t) => t.date), ...axis, splitLine: { show: false } },
  yAxis: { type: 'value', ...axis },
  series: [
    { name: '上报', type: 'line', smooth: true, symbolSize: 5, data: (data.value.trend || []).map((t) => t.report), itemStyle: { color: '#234E3B' }, areaStyle: { color: 'rgba(35,78,59,.06)' } },
    { name: '关闭', type: 'line', smooth: true, symbolSize: 5, data: (data.value.trend || []).map((t) => t.close), itemStyle: { color: '#7FA08F' }, lineStyle: { type: 'dashed' } }
  ]
}))

const statusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11, color: '#5C6B63' } },
  color: CHART_COLORS,
  series: [{
    type: 'pie', radius: ['52%', '74%'], center: ['50%', '44%'],
    label: { show: true, position: 'center', formatter: () => '{a|在办}\n{b|' + (data.value.statusDist || []).reduce((s, x) => s + x.value, 0) + '}', rich: { a: { fontSize: 11, color: '#8A9990' }, b: { fontSize: 22, fontWeight: 600, color: '#1F2A25' } } },
    labelLine: { show: false },
    data: (data.value.statusDist || []).map((x) => ({ name: x.name, value: x.value }))
  }]
}))

const damageOption = computed(() => ({
  grid: { left: 64, right: 28, top: 8, bottom: 8 },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'value', ...axis, splitLine: { show: false }, axisLabel: { show: false } },
  yAxis: { type: 'category', data: (data.value.damageDist || []).map((x) => x.name).reverse(), ...axis, splitLine: { show: false } },
  series: [{
    type: 'bar', barWidth: 14,
    data: (data.value.damageDist || []).map((x) => x.value).reverse(),
    itemStyle: { borderRadius: [3, 3, 3, 3], color: (p) => CHART_COLORS[p.dataIndex % CHART_COLORS.length] },
    label: { show: true, position: 'right', fontSize: 11, color: '#5C6B63' }
  }]
}))

const teamOption = computed(() => ({
  grid: { left: 78, right: 26, top: 8, bottom: 8 },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'value', ...axis, axisLabel: { show: false }, splitLine: { show: false } },
  yAxis: { type: 'category', data: (data.value.teamRank || []).map((x) => x.name).reverse(), ...axis, splitLine: { show: false } },
  series: [{
    type: 'bar', barWidth: 13,
    data: (data.value.teamRank || []).map((x) => x.value).reverse(),
    itemStyle: { borderRadius: [3, 3, 3, 3], color: (p) => CHART_COLORS[p.dataIndex % 3] },
    label: { show: true, position: 'right', fontSize: 11, color: '#5C6B63' }
  }]
}))

const riskOption = computed(() => ({
  grid: { left: 34, right: 14, top: 20, bottom: 24 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: (data.value.riskDist || []).map((x) => x.name), ...axis, splitLine: { show: false } },
  yAxis: { type: 'value', ...axis },
  series: [{
    type: 'bar', barWidth: 34,
    data: (data.value.riskDist || []).map((x) => ({ value: x.value, itemStyle: { color: RISK_COLOR[x.name] || '#7FA08F', borderRadius: [3, 3, 0, 0] } })),
    label: { show: true, position: 'top', fontSize: 11, color: '#5C6B63' }
  }]
}))

const roadOption = computed(() => ({
  grid: { left: 78, right: 22, top: 6, bottom: 6 },
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  xAxis: { type: 'value', ...axis, axisLabel: { show: false }, splitLine: { show: false } },
  yAxis: { type: 'category', data: (data.value.roadHeat || []).slice(0, 5).map((x) => x.name).reverse(), ...axis, splitLine: { show: false } },
  series: [{
    type: 'bar', barWidth: 12,
    data: (data.value.roadHeat || []).slice(0, 5).map((x) => x.value).reverse(),
    itemStyle: { borderRadius: 3, color: '#3A7D5A' },
    label: { show: true, position: 'right', fontSize: 11, color: '#5C6B63' }
  }]
}))

async function load() {
  const res = await getDashboard()
  data.value = res.data
}

onMounted(load)
</script>

<style scoped>
.dash { padding: 18px 24px; }
.metrics { margin-bottom: 12px; }
.metric .v { font-size: 23px; line-height: 31px; }
.row-a { display: grid; grid-template-columns: 1.35fr 1fr 1fr; gap: 12px; }
.row-b { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.card { margin-bottom: 12px; }
</style>
