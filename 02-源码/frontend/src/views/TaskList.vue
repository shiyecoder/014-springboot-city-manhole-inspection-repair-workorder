<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>巡检任务</h1>
        <p>按路线生成的巡检计划，跟踪执行进度与异常发现情况</p>
      </div>
      <div class="right">
        <el-date-picker v-model="query.planDate" type="date" value-format="YYYY-MM-DD" placeholder="计划日期" style="width: 160px" @change="search" />
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px" @change="search">
          <el-option v-for="v in ['待执行', '进行中', '已完成', '已超期']" :key="v" :label="v" :value="v" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="任务号 / 路线" clearable style="width: 190px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="routeName" label="巡检路线" min-width="150" />
        <el-table-column prop="roadName" label="道路" width="110" />
        <el-table-column label="计划时间" width="150">
          <template #default="{ row }">{{ row.planDate }} {{ row.planStart }}</template>
        </el-table-column>
        <el-table-column prop="inspectorName" label="巡检员" width="90" />
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="percent(row)" :stroke-width="8" :color="row.abnormalCount ? '#B7791F' : '#234E3B'" />
            <span class="muted">{{ row.doneCount }} / {{ row.totalCount }} 点位</span>
          </template>
        </el-table-column>
        <el-table-column label="异常" width="82">
          <template #default="{ row }">
            <span :class="row.abnormalCount ? 'tag tag-amber' : 'tag tag-green'">{{ row.abnormalCount }} 处</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.taskStatus)}`">{{ row.taskStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">执行明细</el-button>
            <el-button v-if="row.taskStatus !== '已完成'" link type="primary" @click="finish(row)">收尾</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="巡检执行明细" size="620px">
      <div v-if="detail">
        <div class="detail-head">
          <div class="l1"><h1>{{ detail.taskNo }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.taskStatus)}`">{{ detail.taskStatus }}</span></div>
          <div class="l2">
            <span>路线<b>{{ detail.routeName }}</b></span>
            <span>巡检员<b>{{ detail.inspectorName }}</b></span>
            <span>计划<b>{{ detail.planDate }} {{ detail.planStart }}</b></span>
          </div>
        </div>
        <div class="note">已完成 {{ detail.doneCount }} / {{ detail.totalCount }} 个点位，发现异常 {{ detail.abnormalCount }} 处。异常点位在下方标红。</div>
        <el-table :data="detail.records || []" size="small" style="margin-top: 12px">
          <el-table-column label="现场图" width="72">
            <template #default="{ row }"><img v-if="row.imageUrl" class="thumb" :src="row.imageUrl" alt="巡检现场图" /></template>
          </el-table-column>
          <el-table-column prop="coverNo" label="井盖" width="110" />
          <el-table-column prop="checkResult" label="检查结果" width="90" />
          <el-table-column prop="severity" label="严重度" width="80" />
          <el-table-column prop="remark" label="备注" min-width="140" />
          <el-table-column label="检查时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.checkTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { finishTask, getTask, pageTasks } from '@/api'
import { formatDateTime, tagType } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '', planDate: '' })
const drawer = ref(false)
const detail = ref(null)

const percent = (row) => (row.totalCount ? Math.min(100, Math.round((row.doneCount / row.totalCount) * 100)) : 0)

async function load() {
  loading.value = true
  try {
    const res = await pageTasks(query)
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
  const res = await getTask(row.id)
  detail.value = res.data
}
async function finish(row) {
  await ElMessageBox.confirm(`确认结束巡检任务 ${row.taskNo}？未完成点位将记录为漏检。`, '任务收尾', { type: 'warning' })
  await finishTask(row.id)
  ElMessage.success('任务已收尾')
  load()
}
onMounted(load)
</script>
