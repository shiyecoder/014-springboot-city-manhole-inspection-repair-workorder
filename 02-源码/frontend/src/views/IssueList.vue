<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>问题上报</h1>
        <p>井盖缺损问题全流程台账，含风险评分与派单流转</p>
      </div>
      <div class="right">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">登记问题</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <div class="item"><label>关键字</label>
          <el-input v-model="query.keyword" placeholder="问题号 / 井盖号 / 道路" clearable @keyup.enter="search" />
        </div>
        <div class="item"><label>缺损类型</label>
          <el-select v-model="query.damageType" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['破损', '松动', '沉降', '丢失', '异响']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <div class="item"><label>风险等级</label>
          <el-select v-model="query.riskLevel" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['重大', '紧急', '一般']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <div class="item"><label>处理状态</label>
          <el-select v-model="query.status" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['待确认', '待派单', '处理中', '待验收', '返工中', '已关闭']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="margin-top: 14px">
        <el-table-column label="现场图" width="76">
          <template #default="{ row }"><img v-if="row.imageUrl" class="thumb" :src="row.imageUrl" alt="问题现场图" /></template>
        </el-table-column>
        <el-table-column prop="issueNo" label="问题编号" width="160" />
        <el-table-column label="井盖 / 道路" min-width="180">
          <template #default="{ row }">
            <div class="mono">{{ row.coverNo }}</div>
            <div class="muted">{{ row.roadName }} · {{ row.coverType }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="damageType" label="缺损" width="72" />
        <el-table-column prop="severity" label="严重度" width="78" />
        <el-table-column label="风险" width="116">
          <template #default="{ row }">
            <span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span>
            <b class="num" style="margin-left: 5px">{{ row.riskScore }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <span class="tag" :class="`tag-${tagType(row.issueStatus)}`">{{ row.issueStatus }}</span>
            <el-tag v-if="row.overdue" size="small" type="danger" effect="plain" style="margin-left: 4px">超时</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="86" />
        <el-table-column label="上报时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.reportTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.issueStatus === '待确认'" link type="primary" @click="confirm(row)">确认</el-button>
            <el-button v-if="row.issueStatus === '待确认'" link type="danger" @click="closeIssue(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="问题详情" size="600px">
      <div v-if="detail">
        <img v-if="detail.imageUrl" :src="detail.imageUrl" style="width: 100%; height: 210px; object-fit: cover; border-radius: 8px" alt="问题现场图" />
        <div class="detail-head" style="margin-top: 12px">
          <div class="l1"><h1>{{ detail.issueNo }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.riskLevel)}`">{{ detail.riskLevel }} {{ detail.riskScore }} 分</span>
            <span class="tag" :class="`tag-${tagType(detail.issueStatus)}`">{{ detail.issueStatus }}</span>
          </div>
          <div class="l2">
            <span>井盖<b>{{ detail.coverNo }}</b></span><span>道路<b>{{ detail.roadName }}</b></span>
            <span>上报人<b>{{ detail.reporterName }}</b></span>
          </div>
        </div>
        <dl class="kv">
          <dt>缺损类型</dt><dd>{{ detail.damageType }} · 严重度 {{ detail.severity }}</dd>
          <dt>上报来源</dt><dd>{{ detail.sourceType }}</dd>
          <dt>上报时间</dt><dd>{{ formatDateTime(detail.reportTime) }}</dd>
          <dt>处置时限</dt><dd>{{ formatDateTime(detail.handleDeadline) }}</dd>
          <dt>重复报修</dt><dd>{{ detail.repeatFlag ? `是（累计 ${detail.repeatCount} 次）` : '否' }}</dd>
          <dt>关联工单</dt><dd>{{ detail.orderNo || '尚未派单' }} {{ detail.orderStatus ? `· ${detail.orderStatus}` : '' }}</dd>
          <dt>问题描述</dt><dd>{{ detail.description }}</dd>
        </dl>
        <div class="note" style="margin-top: 12px">风险评分由规则引擎按道路等级、严重度、历史维修、人流、等待时长、重复次数加权计算，规则版本与输入快照可在「计算记录」中追溯。</div>
        <div class="card-h" style="margin-top: 16px"><h3>处置时间线</h3></div>
        <el-timeline>
          <el-timeline-item v-for="t in detail.timeline || []" :key="t.id" :timestamp="formatDateTime(t.createTime)" placement="top">
            {{ t.action }} · {{ t.operatorName || '系统' }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <el-dialog v-model="createVisible" title="登记井盖问题" width="620px">
      <el-form :model="form" label-width="88px">
        <el-form-item label="井盖">
          <el-select v-model="form.coverId" filterable remote :remote-method="searchCover" placeholder="输入编号或道路搜索" style="width: 100%">
            <el-option v-for="c in coverOpts" :key="c.id" :label="`${c.coverNo} · ${c.roadName} · ${c.locationDesc}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="缺损类型">
            <el-select v-model="form.damageType" style="width: 100%">
              <el-option v-for="v in ['破损', '松动', '沉降', '丢失', '异响']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="严重度">
            <el-select v-model="form.severity" style="width: 100%">
              <el-option v-for="v in ['轻微', '一般', '严重']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源">
            <el-select v-model="form.sourceType" style="width: 100%">
              <el-option v-for="v in ['巡检发现', '市民举报', '智能监测', '日常巡查']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="问题描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述缺损情况与现场环境" /></el-form-item>
        <el-form-item label="现场图片"><ImageUpload v-model="form.imageUrl" biz-type="ISSUE" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">提交并评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { confirmIssue, coverOptions, createIssue, getIssue, pageIssues, transitionIssue } from '@/api'
import { formatDateTime, tagType } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', damageType: '', riskLevel: '', status: '' })
const drawer = ref(false)
const detail = ref(null)
const createVisible = ref(false)
const form = ref({})
const coverOpts = ref([])

async function load() {
  loading.value = true
  try {
    const res = await pageIssues(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, keyword: '', damageType: '', riskLevel: '', status: '' }); load() }

async function openDetail(row) {
  drawer.value = true
  detail.value = null
  const res = await getIssue(row.id)
  detail.value = res.data
}
async function confirm(row) {
  await confirmIssue(row.id)
  ElMessage.success('问题已确认，可进入派单')
  load()
}
async function closeIssue(row) {
  const { value } = await ElMessageBox.prompt('请填写作废原因', '作废问题', { inputPlaceholder: '如：重复上报、非真实缺损' })
  await transitionIssue(row.id, { target: '已取消', reason: value })
  ElMessage.success('已作废')
  load()
}
function openCreate() {
  form.value = { damageType: '破损', severity: '一般', sourceType: '巡检发现' }
  coverOpts.value = []
  createVisible.value = true
  searchCover('')
}
async function searchCover(keyword) {
  const res = await coverOptions(keyword)
  coverOpts.value = res.data || []
}
async function create() {
  if (!form.value.coverId) return ElMessage.warning('请选择井盖')
  if (!form.value.imageUrl) return ElMessage.warning('请上传现场图片')
  await createIssue(form.value)
  ElMessage.success('问题已登记并完成自动评分')
  createVisible.value = false
  load()
}
onMounted(load)
</script>
