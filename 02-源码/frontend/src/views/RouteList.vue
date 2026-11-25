<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>巡检路线</h1>
        <p>按道路分段的巡检路线与点位顺序，可一键生成本周巡检任务</p>
      </div>
      <div class="right">
        <el-select v-model="query.cycleType" clearable placeholder="全部周期" style="width: 150px" @change="search">
          <el-option label="每日巡检" value="DAY" />
          <el-option label="每周巡检" value="WEEK" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="路线 / 道路" clearable style="width: 200px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
        <el-button type="primary" @click="generate">生成巡检任务</el-button>
      </div>
    </div>

    <div class="route-grid" v-loading="loading">
      <div v-for="r in rows" :key="r.id" class="card route-card">
        <div class="rc-head">
          <div>
            <h3>{{ r.routeName }}</h3>
            <div class="muted">{{ r.routeNo }} · {{ r.roadName }} {{ r.roadRange }}</div>
          </div>
          <span class="tag tag-blue">{{ r.cycleType === 'DAY' ? '每日' : '每周' }}</span>
        </div>
        <div class="rc-meta">
          <span>点位 <b>{{ r.coverCount }}</b> 个</span>
          <span>预计 <b>{{ r.estimateMin }}</b> 分钟</span>
          <span>负责人 <b>{{ r.leaderName }}</b></span>
        </div>
        <div class="rc-cycle">{{ r.cycleDesc }}</div>
        <div class="points">
          <div v-for="p in (r.points || []).slice(0, 6)" :key="p.seq" class="pt">
            <i>{{ p.seq }}</i>
            <span class="mono">{{ p.coverNo }}</span>
            <span class="tag" :class="`tag-${tagType(p.riskLevel)}`">{{ p.riskLevel }}</span>
          </div>
          <span v-if="(r.points || []).length > 6" class="muted">等 {{ r.points.length }} 个点位</span>
        </div>
      </div>
      <div v-if="!rows.length && !loading" class="empty-box">暂无巡检路线</div>
    </div>

    <div class="pager">
      <span class="muted">共 {{ total }} 条路线</span>
      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
        :page-sizes="[6, 12, 24]" layout="sizes, prev, pager, next" @change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateTasks, pageRoutes } from '@/api'
import { tagType } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 6, keyword: '', cycleType: '' })

async function load() {
  loading.value = true
  try {
    const res = await pageRoutes(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }

// 按当前周期生成次日巡检任务：已经生成过的路线会自动跳过
async function generate() {
  await ElMessageBox.confirm('将按各路线的巡检周期生成下一批巡检任务，已生成的周期不会重复创建。', '生成巡检任务', { type: 'info' })
  const res = await generateTasks({})
  ElMessage.success(`已生成 ${res.data} 条巡检任务`)
  load()
}
onMounted(load)
</script>

<style scoped>
.route-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.route-card { display: flex; flex-direction: column; gap: 10px; }
.rc-head { display: flex; align-items: flex-start; gap: 8px; }
.rc-head h3 { margin: 0 0 2px; font-size: 15px; }
.rc-meta { display: flex; gap: 16px; font-size: 12.5px; color: var(--text-2); }
.rc-meta b { color: var(--text); font-weight: 600; }
.rc-cycle { font-size: 12.5px; color: var(--brand); background: #EEF5F1; border-radius: 4px; padding: 5px 9px; }
.points { display: flex; flex-wrap: wrap; gap: 6px; }
.pt { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; background: #F7FAF8; border: 1px solid var(--line); border-radius: 4px; padding: 3px 7px; }
.pt i { font-style: normal; width: 15px; height: 15px; border-radius: 50%; background: var(--brand); color: #fff; font-size: 10px; display: grid; place-items: center; }
</style>
