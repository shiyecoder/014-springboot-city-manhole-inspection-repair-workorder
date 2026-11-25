<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>风险分级规则</h1>
        <p>创新点：规则可配置 + 计算留痕 + 结果解释 + 人工修正。权重调整即生成新版本，历史版本不追溯改写</p>
      </div>
      <div class="right"><el-button type="primary" @click="openEdit(active)">新增规则版本</el-button></div>
    </div>

    <div class="rule-wrap">
      <div class="card">
        <div class="card-h">
          <h3>当前生效规则</h3>
          <span class="tag tag-green" v-if="active">v{{ active.ruleVersion?.replace('v', '') }} · 生效中</span>
          <span class="right muted" v-if="active">生效时间 {{ formatDateTime(active.effectTime) }}</span>
        </div>
        <div class="weights">
          <div v-for="w in weightList" :key="w.key" class="w">
            <div class="wk">{{ w.label }}</div>
            <div class="wv">{{ active?.[w.key] ?? 0 }}<small>分</small></div>
            <div class="bar"><i :style="{ width: (active?.[w.key] || 0) * 3 + '%' }" /></div>
          </div>
        </div>
        <div class="threshold">
          <div class="t red"><span>重大阈值</span><b>≥ {{ active?.redThreshold ?? '-' }} 分</b><em>4 小时内到场</em></div>
          <div class="t amber"><span>紧急阈值</span><b>≥ {{ active?.yellowThreshold ?? '-' }} 分</b><em>8 小时内到场</em></div>
          <div class="t blue"><span>一般</span><b>&lt; {{ active?.yellowThreshold ?? '-' }} 分</b><em>24 小时内处置</em></div>
          <div class="t sum"><span>权重合计</span><b>{{ weightSum }} 分</b><em>应对齐 100 分</em></div>
        </div>
        <div class="note" style="margin-top: 14px">
          {{ active?.ruleName }}：规则每次发布都会记录版本号、发布人与生效时间；评分记录会保存当时的规则版本与输入快照，调整权重不会改变已生成的分数。
        </div>
      </div>

      <div class="card">
        <div class="card-h"><h3>版本历史</h3><span class="muted">共 {{ rules.length }} 个版本</span></div>
        <el-table :data="rules" size="small">
          <el-table-column prop="ruleVersion" label="版本" width="76" />
          <el-table-column prop="ruleName" label="说明" min-width="180" />
          <el-table-column label="阈值" width="110">
            <template #default="{ row }">{{ row.redThreshold }} / {{ row.yellowThreshold }}</template>
          </el-table-column>
          <el-table-column label="生效时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.effectTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <span class="tag" :class="row.enabled ? 'tag-green' : 'tag-gray'">{{ row.enabled ? '生效' : '历史' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">以此为基准</el-button></template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="editVisible" title="发布新规则版本" width="640px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="版本号"><el-input v-model="form.ruleVersion" placeholder="v1.4" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="重大阈值"><el-input-number v-model="form.redThreshold" :min="50" :max="100" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="紧急阈值"><el-input-number v-model="form.yellowThreshold" :min="20" :max="80" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生效时间">
            <el-date-picker v-model="form.effectTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item label="规则说明"><el-input v-model="form.ruleName" placeholder="说明本次调整原因，如：主干道权重提高" /></el-form-item></el-col>
        </el-row>
        <el-divider content-position="left">因素权重（合计应为 100）</el-divider>
        <el-row :gutter="12">
          <el-col v-for="w in weightList" :key="w.key" :span="8">
            <el-form-item :label="w.label" label-width="96px">
              <el-input-number v-model="form[w.key]" :min="0" :max="50" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="note" :class="{ warn: formWeightSum !== 100 }">当前权重合计 <b>{{ formWeightSum }}</b> 分{{ formWeightSum === 100 ? '，符合要求' : '，建议调整到 100 分以保证分档一致' }}</div>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="publish">发布并生效</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getActiveRule, listRules, publishRule } from '@/api'
import { formatDateTime } from '@/utils/format'

const active = ref(null)
const rules = ref([])
const editVisible = ref(false)
const form = ref({})
const weightList = [
  { key: 'wseverity', label: '缺损严重度' },
  { key: 'wduration', label: '未处理时长' },
  { key: 'wroad', label: '道路等级' },
  { key: 'wrepair', label: '历史维修次数' },
  { key: 'wcrowd', label: '人流等级' },
  { key: 'wrepeat', label: '重复报修次数' }
]

const weightSum = computed(() => weightList.reduce((s, w) => s + Number(active.value?.[w.key] || 0), 0))
const formWeightSum = computed(() => weightList.reduce((s, w) => s + Number(form.value?.[w.key] || 0), 0))

async function load() {
  const [a, r] = await Promise.all([getActiveRule(), listRules()])
  active.value = a.data
  rules.value = r.data || []
}
function openEdit(base) {
  const src = base || active.value || {}
  form.value = {
    ruleVersion: nextVersion(src.ruleVersion),
    ruleName: src.ruleName || '',
    redThreshold: src.redThreshold || 70,
    yellowThreshold: src.yellowThreshold || 45,
    effectTime: '',
    ...weightList.reduce((o, w) => ({ ...o, [w.key]: src[w.key] || 0 }), {}),
    enabled: 1
  }
  editVisible.value = true
}
function nextVersion(v) {
  const n = Number(String(v || 'v1.0').replace('v', '')) || 1
  return 'v' + (n + 0.1).toFixed(1)
}
async function publish() {
  if (formWeightSum.value !== 100) return ElMessage.warning('权重合计必须为 100 分')
  if (!form.value.ruleName) return ElMessage.warning('请填写规则说明')
  await publishRule(form.value)
  ElMessage.success('新规则版本已发布并生效')
  editVisible.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.rule-wrap { display: grid; grid-template-columns: minmax(0, 1fr) 460px; gap: 14px; align-items: start; }
.weights { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.w { border: 1px solid var(--line); border-radius: 6px; padding: 10px 12px; background: #FAFCFB; }
.wk { font-size: 12.5px; color: var(--text-2); }
.wv { font-size: 20px; font-weight: 600; margin: 2px 0 6px; }
.wv small { font-size: 11px; color: var(--text-3); margin-left: 2px; font-weight: 400; }
.bar { height: 4px; background: #E6EEE9; border-radius: 3px; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--brand); }
.threshold { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 14px; }
.t { border-radius: 6px; padding: 10px 12px; border-left: 3px solid var(--line); background: #F7FAF8; }
.t span { font-size: 12px; color: var(--text-2); }
.t b { display: block; font-size: 16px; margin: 2px 0; }
.t em { font-style: normal; font-size: 11.5px; color: var(--text-3); }
.t.red { border-left-color: var(--danger); background: #FCF1EF; }
.t.amber { border-left-color: var(--warn); background: #FDF8EE; }
.t.blue { border-left-color: var(--info); background: #EDF4F8; }
.t.sum { border-left-color: var(--brand); background: #EEF5F1; }
</style>
