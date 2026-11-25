<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>我的巡检任务</h1>
        <p>你的巡检计划与点位清单，逐点登记检查结果，异常点位会直接进入问题池</p>
      </div>
      <div class="right">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px" @change="load">
          <el-option v-for="v in ['待执行', '进行中', '已完成']" :key="v" :label="v" :value="v" />
        </el-select>
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="$router.push('/my/report')">上报新问题</el-button>
      </div>
    </div>

    <div class="task-grid" v-loading="loading">
      <div v-for="t in rows" :key="t.id" class="card task-card">
        <div class="tc-head">
          <div>
            <h3>{{ t.routeName }}</h3>
            <div class="muted">{{ t.taskNo }} · {{ t.roadName }}</div>
          </div>
          <span class="tag" :class="`tag-${tagType(t.taskStatus)}`">{{ t.taskStatus }}</span>
        </div>
        <div class="tc-meta">
          <span>计划 {{ t.planDate }} {{ t.planStart }}</span>
          <span>点位 {{ t.doneCount }}/{{ t.totalCount }}</span>
          <span :class="t.abnormalCount ? 'warn' : ''">异常 {{ t.abnormalCount }}</span>
        </div>
        <el-progress :percentage="t.totalCount ? Math.min(100, Math.round(t.doneCount / t.totalCount * 100)) : 0" :stroke-width="7" :show-text="false" color="#3A7D5A" />
        <div class="tc-foot">
          <el-button size="small" @click="openDetail(t)">查看点位</el-button>
          <el-button v-if="t.taskStatus !== '已完成'" size="small" type="primary" @click="finish(t)">完成巡检</el-button>
        </div>
      </div>
      <div v-if="!rows.length && !loading" class="empty-box">今天没有巡检任务，休息一下</div>
    </div>

    <el-drawer v-model="drawer" title="巡检点位" size="640px">
      <div v-if="detail">
        <div class="detail-head">
          <div class="l1"><h1>{{ detail.routeName }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.taskStatus)}`">{{ detail.taskStatus }}</span></div>
          <div class="l2"><span>任务号<b>{{ detail.taskNo }}</b></span><span>计划<b>{{ detail.planDate }} {{ detail.planStart }}</b></span>
            <span>进度<b>{{ detail.doneCount }}/{{ detail.totalCount }}</b></span></div>
        </div>
        <el-table :data="detail.records || []" size="small">
          <el-table-column prop="seq" label="序号" width="60" />
          <el-table-column prop="coverNo" label="井盖" width="110" />
          <el-table-column prop="roadName" label="道路" min-width="100" />
          <el-table-column label="风险" width="80">
            <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span></template>
          </el-table-column>
          <el-table-column label="检查结果" width="100">
            <template #default="{ row }">
              <span v-if="row.checkResult" class="tag" :class="row.checkResult === '正常' ? 'tag-green' : 'tag-red'">{{ row.checkResult }}</span>
              <span v-else class="muted">未检查</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="96">
            <template #default="{ row }">
              <el-button v-if="!row.checkResult" link type="primary" @click="openRecord(row)">登记</el-button>
              <el-button v-else link @click="openRecord(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="recordVisible" title="登记检查结果" width="520px">
      <el-form :model="form" label-width="86px">
        <el-form-item label="井盖"><span class="mono">{{ form.coverNo }}</span></el-form-item>
        <el-form-item label="检查结果">
          <el-radio-group v-model="form.checkResult">
            <el-radio value="正常">正常</el-radio>
            <el-radio value="异常">发现异常</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.checkResult === '异常'" label="严重度">
          <el-select v-model="form.severity" style="width: 160px">
            <el-option v-for="v in ['轻微', '一般', '严重']" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="现场照片"><ImageUpload v-model="form.imageUrl" biz-type="PATROL" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { finishTask, getTask, pageTasks, submitRecord } from '@/api'
import { tagType } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const rows = ref([])
const loading = ref(false)
const query = reactive({ page: 1, size: 12, mine: true, status: '' })
const drawer = ref(false)
const detail = ref(null)
const currentTask = ref(null)
const recordVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageTasks(query)
    rows.value = res.data.records
  } finally {
    loading.value = false
  }
}
async function openDetail(t) {
  currentTask.value = t
  drawer.value = true
  detail.value = null
  const res = await getTask(t.id)
  detail.value = res.data
}
function openRecord(row) {
  form.value = { coverId: row.coverId || row.id, coverNo: row.coverNo, checkResult: row.checkResult || '正常', severity: '一般', imageUrl: row.imageUrl || '', remark: row.remark || '' }
  recordVisible.value = true
}
async function submit() {
  if (form.value.checkResult === '异常' && !form.value.imageUrl) return ElMessage.warning('异常点位必须上传现场照片')
  await submitRecord(currentTask.value.id, form.value)
  ElMessage.success('检查结果已登记')
  recordVisible.value = false
  openDetail(currentTask.value)
}
async function finish(t) {
  await ElMessageBox.confirm(`确认完成巡检任务 ${t.taskNo}？`, '完成巡检', { type: 'warning' })
  await finishTask(t.id)
  ElMessage.success('任务已完成')
  load()
}
onMounted(load)
</script>

<style scoped>
.task-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.task-card { display: flex; flex-direction: column; gap: 10px; }
.tc-head { display: flex; align-items: flex-start; gap: 8px; }
.tc-head h3 { margin: 0 0 2px; font-size: 15px; }
.tc-meta { display: flex; gap: 14px; font-size: 12.5px; color: var(--text-2); }
.tc-meta .warn { color: var(--warn); font-weight: 600; }
.tc-foot { display: flex; gap: 8px; }
</style>
