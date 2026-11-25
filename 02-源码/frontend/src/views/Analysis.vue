<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>统计分析</h1>
        <p>按道路、缺损类型、处置时效三个维度分析井盖风险与维修效率</p>
      </div>
      <div class="right">
        <el-radio-group v-model="range" @change="load">
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
          <el-radio-button :value="365">近一年</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-h"><h3>风险等级分布</h3><span class="muted">共 {{ risk.total }} 条评分</span></div>
        <ChartBox :option="levelOption" :height="240" />
      </div>
      <div class="card">
        <div class="card-h"><h3>规则采纳与修正</h3><span class="muted">修正率 {{ risk.adjustRate }}%</span></div>
        <ChartBox :option="adoptOption" :height="240" />
      </div>
      <div class="card">
        <div class="card-h"><h3>缺损类型构成</h3></div>
        <ChartBox :option="damageOption" :height="240" />
      </div>
      <div class="card">
        <div class="card-h"><h3>上报与关闭趋势</h3></div>
        <ChartBox :option="trendOption" :height="240" />
      </div>
    </div>

    <div class="card" style="margin-top: 14px">
      <div class="card-h"><h3>道路风险排行</h3><span class="muted">按高风险问题数量排序</span></div>
      <el-table :data="roadRows" size="small">
        <el-table-column type="index" label="#" width="54" />
        <el-table-column prop="road" label="道路" min-width="160" />
        <el-table-column label="问题数" width="110">
          <template #default="{ row }">{{ row.count }} 条</template>
        </el-table-column>
        <el-table-column label="重大/紧急" width="140">
          <template #default="{ row }">
            <span class="tag tag-red">{{ row.red }} 重大</span>
            <span class="tag tag-amber" style="margin-left: 4px">{{ row.amber }} 紧急</span>
          </template>
        </el-table-column>
        <el-table-column label="占比" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :stroke-width="8" color="#234E3B" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getDashboard, pageIssues, riskStats } from '@/api'
import ChartBox from '@/components/ChartBox.vue'
import { CHART_COLORS, RISK_COLOR } from '@/utils/format'

const range = ref(30)
const risk = reactive({ total: 0, adjusted: 0, adoptRate: 0, adjustRate: 0, levelCount: {} })
const dash = ref({})
const issues = ref([])

const levelOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['46%', '68%'], center: ['50%', '44%'],
    itemStyle: { borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{c} 条', fontSize: 11 },
    data: Object.entries(risk.levelCount || {}).map(([k, v]) => ({ name: k, value: v, itemStyle: { color: RISK_COLOR[k] } }))
  }]
}))

const adoptOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: ['算法评分', '规则采纳', '人工修正'] },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar', barWidth: 34, itemStyle: { color: '#234E3B', borderRadius: [4, 4, 0, 0] },
    label: { show: true, position: 'top', fontSize: 11 },
    data: [risk.total, Math.round(risk.total * risk.adoptRate / 100), risk.adjusted]
  }]
}))

const damageOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: (dash.value.damageDist || []).map((d) => d.name) },
  yAxis: { type: 'value' },
  series: [{
    type: 'bar', barWidth: 26,
    itemStyle: { color: (p) => CHART_COLORS[p.dataIndex % CHART_COLORS.length], borderRadius: [4, 4, 0, 0] },
    data: (dash.value.damageDist || []).map((d) => d.value)
  }]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 4 },
  grid: { left: 40, right: 20, top: 20, bottom: 40 },
  xAxis: { type: 'category', data: (dash.value.trend || []).map((t) => t.date) },
  yAxis: { type: 'value' },
  series: [
    { name: '上报', type: 'line', smooth: true, areaStyle: { opacity: .12 }, itemStyle: { color: '#234E3B' }, data: (dash.value.trend || []).map((t) => t.report) },
    { name: '关闭', type: 'line', smooth: true, areaStyle: { opacity: .12 }, itemStyle: { color: '#B7791F' }, data: (dash.value.trend || []).map((t) => t.close) }
  ]
}))

// 道路排行：前端按道路聚合问题，避免额外接口
const roadRows = computed(() => {
  const map = new Map()
  issues.value.forEach((i) => {
    const k = i.roadName || '未知道路'
    const cur = map.get(k) || { road: k, count: 0, red: 0, amber: 0 }
    cur.count++
    if (i.riskLevel === '重大') cur.red++
    if (i.riskLevel === '紧急') cur.amber++
    map.set(k, cur)
  })
  const arr = [...map.values()].sort((a, b) => b.red - a.red || b.count - a.count)
  const max = arr[0]?.count || 1
  return arr.map((r) => ({ ...r, percent: Math.round((r.count / max) * 100) }))
})

async function load() {
  const [s, d, i] = await Promise.all([riskStats(), getDashboard(range.value), pageIssues({ page: 1, size: 50 })])
  Object.assign(risk, s.data)
  dash.value = d.data
  issues.value = i.data.records || []
}
onMounted(load)
</script>

<style scoped>
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
</style>
