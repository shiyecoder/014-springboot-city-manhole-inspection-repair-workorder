<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>风险计算记录</h1>
        <p>每条问题都保留规则版本、输入快照、分项得分与解释文本，支持人工修正且不覆盖原始分</p>
      </div>
      <div class="right">
        <el-select v-model="query.level" clearable placeholder="全部等级" style="width: 130px" @change="search">
          <el-option v-for="v in ['重大', '紧急', '一般']" :key="v" :label="v" :value="v" />
        </el-select>
        <el-select v-model="query.version" clearable placeholder="全部版本" style="width: 130px" @change="search">
          <el-option v-for="v in versions" :key="v" :label="v" :value="v" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="问题号 / 井盖号" clearable style="width: 180px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
      </div>
    </div>

    <div class="metrics four">
      <div class="metric"><div class="k">计算总数</div><div class="v">{{ stats.total }}<small>条</small></div><div class="d">全部问题均已评分</div></div>
      <div class="metric"><div class="k">规则采纳率</div><div class="v">{{ stats.adoptRate }}<small>%</small></div><div class="d">未人工修正的比例</div></div>
      <div class="metric warn"><div class="k">人工修正</div><div class="v">{{ stats.adjusted }}<small>条</small></div><div class="d">修正率 {{ stats.adjustRate }}%</div></div>
      <div class="metric danger"><div class="k">重大 / 紧急</div><div class="v">{{ (stats.levelCount?.重大 || 0) }}<small>/ {{ stats.levelCount?.紧急 || 0 }}</small></div><div class="d">需重点跟进</div></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="评分" width="106">
          <template #default="{ row }">
            <div class="score" :class="`s-${tagType(row.riskLevel)}`">{{ row.totalScore }}</div>
            <div class="muted" style="font-size: 11px">{{ row.riskLevel }}<span v-if="row.adjustLevel"> → {{ row.adjustLevel }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="问题 / 井盖" min-width="200">
          <template #default="{ row }">
            <div>{{ row.issueNo }}</div>
            <div class="muted">{{ row.coverNo }} · {{ row.roadName }} · {{ row.damageType }}</div>
          </template>
        </el-table-column>
        <el-table-column label="规则版本" width="96">
          <template #default="{ row }"><span class="tag tag-gray">{{ row.ruleVersion }}</span></template>
        </el-table-column>
        <el-table-column prop="explainText" label="结果解释" min-width="300" show-overflow-tooltip />
        <el-table-column label="计算时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button link type="warning" @click="openAdjust(row)">修正</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="风险计算明细" size="600px">
      <div v-if="detail">
        <div class="detail-head">
          <div class="l1"><h1>{{ detail.totalScore }} 分</h1>
            <span class="tag" :class="`tag-${tagType(detail.riskLevel)}`">{{ detail.riskLevel }}</span>
            <span class="tag tag-gray">{{ detail.ruleVersion }}</span>
          </div>
          <div class="l2"><span>问题<b>{{ detail.issueNo || detail.issue?.issueNo || '—' }}</b></span>
            <span>井盖<b>{{ detail.coverNo || detail.inputSnapshot?.coverNo }}</b></span>
            <span>道路<b>{{ detail.roadName || detail.issue?.roadName || '—' }}</b></span></div>
        </div>
        <div class="note">{{ detail.explainText }}</div>
        <div class="card-h" style="margin-top: 16px"><h3>分项得分</h3></div>
        <div class="factor" v-for="f in detail.factors || []" :key="f.name">
          <span class="fn">{{ f.name }}</span>
          <div class="fbar"><i :style="{ width: Math.min(100, f.score * 3) + '%' }" /></div>
          <b>{{ f.score }}</b>
        </div>
        <div class="card-h" style="margin-top: 16px"><h3>输入快照</h3></div>
        <dl class="kv">
          <template v-for="(v, k) in detail.inputSnapshot || {}" :key="k">
            <dt>{{ snapLabel(k) }}</dt><dd>{{ v }}</dd>
          </template>
        </dl>
        <template v-if="(detail.improve || []).length">
          <div class="card-h" style="margin-top: 16px"><h3>可改进项</h3></div>
          <ul class="imp"><li v-for="i in detail.improve" :key="i">{{ i }}</li></ul>
        </template>
        <template v-if="detail.adjustLevel">
          <div class="card-h" style="margin-top: 16px"><h3>人工修正</h3></div>
          <div class="note warn">修正为「{{ detail.adjustLevel }}」，原因：{{ detail.adjustReason }}（{{ formatDateTime(detail.adjustTime) }}）。原始分数 {{ detail.totalScore }} 分保留不变。</div>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="adjustVisible" title="人工修正风险等级" width="480px">
      <div class="note warn">修正只影响后续处置时限，不会覆盖算法算出的原始分数，修正记录会写入留痕。</div>
      <el-form label-width="86px" style="margin-top: 12px">
        <el-form-item label="原始等级"><b>{{ current?.riskLevel }}（{{ current?.totalScore }} 分）</b></el-form-item>
        <el-form-item label="修正为">
          <el-select v-model="adjustForm.level" style="width: 160px">
            <el-option v-for="v in ['重大', '紧急', '一般']" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="修正原因"><el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="如：该路段临时封闭，实际人流下降" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确认修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adjustScore, getScore, listRules, pageScores, riskStats } from '@/api'
import { formatDateTime, tagType } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', level: '', version: '' })
const stats = reactive({ total: 0, adjusted: 0, adoptRate: 0, adjustRate: 0, levelCount: {} })
const versions = ref([])
const drawer = ref(false)
const detail = ref(null)
const adjustVisible = ref(false)
const current = ref(null)
const adjustForm = ref({ level: '紧急', reason: '' })

const SNAP = { coverNo: '井盖编号', roadLevel: '道路等级', damageType: '缺损类型', severity: '严重度', repairCount: '历史维修', crowdLevel: '人流等级', waitHours: '等待时长(h)', repeatCount: '重复次数' }
const snapLabel = (k) => SNAP[k] || k

async function load() {
  loading.value = true
  try {
    const res = await pageScores(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }

async function openDetail(row) {
  drawer.value = true
  detail.value = null
  const res = await getScore(row.id)
  detail.value = res.data
}
function openAdjust(row) {
  current.value = row
  adjustForm.value = { level: row.riskLevel === '重大' ? '紧急' : '重大', reason: '' }
  adjustVisible.value = true
}
async function submitAdjust() {
  if (!adjustForm.value.reason) return ElMessage.warning('请填写修正原因')
  await adjustScore(current.value.id, adjustForm.value)
  ElMessage.success('已修正，原始分数保留')
  adjustVisible.value = false
  load()
  loadStats()
}
async function loadStats() {
  const res = await riskStats()
  Object.assign(stats, res.data)
}
onMounted(async () => {
  load()
  loadStats()
  const r = await listRules()
  versions.value = (r.data || []).map((x) => x.ruleVersion)
})
</script>

<style scoped>
.metrics.four { grid-template-columns: repeat(4, 1fr); }
.score { font-size: 20px; font-weight: 600; line-height: 24px; }
.s-red { color: var(--danger); }
.s-amber { color: var(--warn); }
.s-blue { color: var(--info); }
.factor { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; font-size: 12.5px; }
.fn { width: 100px; color: var(--text-2); }
.fbar { flex: 1; height: 8px; background: #EDF3EF; border-radius: 4px; overflow: hidden; }
.fbar i { display: block; height: 100%; background: linear-gradient(90deg, #3A7D5A, #234E3B); }
.imp { margin: 0; padding-left: 18px; font-size: 12.5px; color: var(--text-2); line-height: 1.8; }
</style>
