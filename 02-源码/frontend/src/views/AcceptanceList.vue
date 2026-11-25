<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>验收记录</h1>
        <p>完工工单的现场验收，四项检查项全部合格方可关闭问题</p>
      </div>
      <div class="right">
        <el-checkbox v-model="query.pending" @change="search">仅看待验收</el-checkbox>
        <el-input v-model="query.keyword" placeholder="工单号 / 井盖号" clearable style="width: 190px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
      </div>
    </div>

    <div class="metrics four">
      <div class="metric"><div class="k">待验收</div><div class="v">{{ stat.pending }}<small>单</small></div><div class="d">超过 24 小时未验收将提醒</div></div>
      <div class="metric"><div class="k">本月验收</div><div class="v">{{ stat.month }}<small>单</small></div><div class="d">含返工后复验</div></div>
      <div class="metric warn"><div class="k">一次合格率</div><div class="v">{{ stat.passRate }}<small>%</small></div><div class="d">按首次验收结果统计</div></div>
      <div class="metric danger"><div class="k">返工工单</div><div class="v">{{ stat.rework }}<small>单</small></div><div class="d">需复验并追责</div></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="orderNo" label="工单号" width="158" />
        <el-table-column label="井盖 / 道路" min-width="160">
          <template #default="{ row }">
            <div class="mono">{{ row.coverNo }}</div>
            <div class="muted">{{ row.roadName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="teamName" label="维修班组" width="120" />
        <el-table-column label="风险 / 缺损" width="120">
          <template #default="{ row }">
            <span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span>
            <span class="muted" style="margin-left: 5px">{{ row.damageType }}</span>
          </template>
        </el-table-column>
        <el-table-column label="维修前后" width="150">
          <template #default="{ row }">
            <div class="pair">
              <img v-if="row.beforeImage" :src="row.beforeImage" class="thumb" alt="维修前" />
              <span v-else class="thumb empty">无</span>
              <img v-if="row.afterImage" :src="row.afterImage" class="thumb" alt="维修后" />
              <span v-else class="thumb empty">无</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="完工时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.finishTime) || '未完工' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.orderStatus)}`">{{ row.orderStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.orderStatus === '待验收'" link type="primary" @click="openVerify(row)">验收</el-button>
            <el-button v-else link @click="openRecords(row)">验收记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="verifyVisible" title="现场验收" width="560px">
      <div v-if="current" class="verify-head">
        <img v-if="current.afterImage" :src="current.afterImage" class="big" alt="完工照片" />
        <div>
          <div class="mono">{{ current.orderNo }}</div>
          <div class="muted">{{ current.coverNo }} · {{ current.roadName }}</div>
          <div class="muted">班组：{{ current.teamName }}</div>
          <div class="muted">完工：{{ formatDateTime(current.finishTime) }}</div>
        </div>
      </div>
      <el-form label-width="92px" style="margin-top: 12px">
        <el-form-item label="检查项">
          <el-checkbox v-model="checks.surfaceOk">路面平整无破损</el-checkbox>
          <el-checkbox v-model="checks.signOk">标识标线清晰</el-checkbox>
          <el-checkbox v-model="checks.barrierOk">围挡移除干净</el-checkbox>
          <el-checkbox v-model="checks.cleanOk">现场无建筑垃圾</el-checkbox>
        </el-form-item>
        <el-form-item label="验收结论">
          <el-radio-group v-model="checks.pass">
            <el-radio :value="true">合格，关闭问题</el-radio>
            <el-radio :value="false">不合格，退回返工</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="验收评分"><el-rate v-model="checks.score" :max="5" show-score /></el-form-item>
        <el-form-item label="验收意见"><el-input v-model="checks.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitVerify">提交验收</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="recordsVisible" title="验收记录" size="520px">
      <el-timeline>
        <el-timeline-item v-for="r in records" :key="r.id" :timestamp="formatDateTime(r.acceptTime)" placement="top">
          <div>
            <span class="tag" :class="r.pass ? 'tag-green' : 'tag-red'">{{ r.pass ? '合格' : '不合格' }}</span>
            <span style="margin-left: 6px">评分 {{ r.score }} 分 · {{ r.acceptorName || '验收员' }}</span>
            <div class="muted" style="margin-top: 4px">{{ r.remark || '无意见' }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!records.length" description="暂无验收记录" />
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAcceptances, submitAcceptance } from '@/api'
import { formatDateTime, tagType } from '@/utils/format'
import request from '@/api/request'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', pending: false })
const verifyVisible = ref(false)
const recordsVisible = ref(false)
const records = ref([])
const current = ref(null)
const checks = reactive({ pass: true, surfaceOk: true, signOk: true, barrierOk: true, cleanOk: true, score: 5, remark: '' })
const stat = reactive({ pending: 0, month: 0, passRate: 0, rework: 0 })

async function load() {
  loading.value = true
  try {
    const res = await pageAcceptances(query)
    rows.value = res.data.records
    total.value = res.data.total
    stat.pending = res.data.records.filter((r) => r.orderStatus === '待验收').length
    stat.month = res.data.total
    stat.rework = res.data.records.filter((r) => (r.reworkCount || 0) > 0).length
    const done = res.data.records.filter((r) => r.orderStatus === '已完成').length
    stat.passRate = res.data.records.length ? Math.round((done / res.data.records.length) * 100) : 0
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }

function openVerify(row) {
  current.value = row
  Object.assign(checks, { pass: true, surfaceOk: true, signOk: true, barrierOk: true, cleanOk: true, score: 5, remark: '' })
  verifyVisible.value = true
}
async function submitVerify() {
  if (!checks.pass && !checks.remark) return ElMessage.warning('不合格请填写退回原因')
  await submitAcceptance(current.value.orderId, { ...checks })
  ElMessage.success(checks.pass ? '验收通过，问题已关闭' : '已退回返工')
  verifyVisible.value = false
  load()
}
async function openRecords(row) {
  recordsVisible.value = true
  records.value = []
  const res = await request.get(`/acceptances/${row.orderId}/records`)
  records.value = res.data || []
}
onMounted(load)
</script>

<style scoped>
.metrics.four { grid-template-columns: repeat(4, 1fr); }
.pair { display: flex; gap: 4px; }
.thumb.empty { display: grid; place-items: center; font-size: 10px; color: var(--text-3); }
.verify-head { display: flex; gap: 12px; align-items: flex-start; }
.verify-head .big { width: 180px; height: 120px; object-fit: cover; border-radius: 6px; border: 1px solid var(--line); }
</style>
