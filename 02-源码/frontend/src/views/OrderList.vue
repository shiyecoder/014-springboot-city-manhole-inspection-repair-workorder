<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>维修工单</h1>
        <p>派单、接单、安全措施、完工提交与验收的全流程工单台账</p>
      </div>
      <div class="right">
        <el-checkbox v-model="query.overdue" @change="search">仅看超时</el-checkbox>
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px" @change="search">
          <el-option v-for="v in ['待接单', '处理中', '待验收', '返工中', '已完成', '已作废']" :key="v" :label="v" :value="v" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="工单号 / 井盖号" clearable style="width: 190px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="orderNo" label="工单号" width="158" />
        <el-table-column label="井盖 / 道路" min-width="170">
          <template #default="{ row }">
            <div class="mono">{{ row.coverNo }}</div>
            <div class="muted">{{ row.roadName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="风险 / 缺损" width="142">
          <template #default="{ row }">
            <span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span>
            <span class="muted" style="margin-left: 5px">{{ row.damageType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="teamName" label="维修班组" width="120" />
        <el-table-column prop="repairerName" label="维修人" width="86" />
        <el-table-column label="要求到场" width="145">
          <template #default="{ row }">
            <span :class="row.overdue ? 'overdue' : ''">{{ formatDateTime(row.requireArrive) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <span class="tag" :class="`tag-${tagType(row.orderStatus)}`">{{ row.orderStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === '待派单' || row.orderStatus === '待接单'" link type="primary" @click="openDispatch(row)">派单</el-button>
            <el-button v-if="['待接单', '处理中', '返工中'].includes(row.orderStatus)" link type="warning" @click="urge(row)">催办</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="工单详情" size="620px">
      <div v-if="detail">
        <div class="detail-head">
          <div class="l1"><h1>{{ detail.orderNo }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.orderStatus)}`">{{ detail.orderStatus }}</span>
            <el-tag v-if="detail.overdue" type="danger" effect="plain" size="small">已超时</el-tag>
          </div>
          <div class="l2">
            <span>井盖<b>{{ detail.coverNo }}</b></span><span>道路<b>{{ detail.roadName }}</b></span>
            <span>班组<b>{{ detail.teamName }}</b></span><span>维修人<b>{{ detail.repairerName }}</b></span>
          </div>
        </div>
        <div class="steps-mini" style="margin-bottom: 12px">
          <span v-for="(s, i) in flowSteps" :key="s" class="s">
            <i :class="{ on: stepIndex >= i }">{{ i + 1 }}</i>{{ s }}
            <span v-if="i < flowSteps.length - 1" class="muted">→</span>
          </span>
        </div>
        <dl class="kv">
          <dt>要求到场</dt><dd>{{ formatDateTime(detail.requireArrive) }}</dd>
          <dt>到场时间</dt><dd>{{ formatDateTime(detail.arriveTime) || '未到场' }}</dd>
          <dt>完工时间</dt><dd>{{ formatDateTime(detail.finishTime) || '未完工' }}</dd>
          <dt>安全措施</dt><dd>{{ detail.safetyMeasure || '未登记' }}</dd>
          <dt>维修方案</dt><dd>{{ detail.planDesc }}</dd>
          <dt>维修结果</dt><dd>{{ detail.resultDesc || '未提交' }}</dd>
          <dt>材料费用</dt><dd>{{ money(detail.materialCost) }}</dd>
          <dt>返工/催办</dt><dd>{{ detail.reworkCount }} 次 / {{ detail.urgeCount }} 次</dd>
        </dl>
        <div class="imgs">
          <div><div class="muted">维修前</div>
            <img v-if="detail.beforeImage" :src="detail.beforeImage" alt="维修前" /><span v-else class="empty-img">未上传</span></div>
          <div><div class="muted">维修后</div>
            <img v-if="detail.afterImage" :src="detail.afterImage" alt="维修后" /><span v-else class="empty-img">未上传</span></div>
        </div>
        <div class="card-h" style="margin-top: 16px"><h3>领料记录</h3></div>
        <el-table :data="detail.materials || []" size="small">
          <el-table-column prop="partName" label="备件" min-width="130" />
          <el-table-column prop="spec" label="规格" width="110" />
          <el-table-column prop="qty" label="数量" width="70" />
          <el-table-column label="领用时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog v-model="dispatchVisible" title="派发维修工单" width="560px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="维修班组">
          <el-select v-model="form.teamName" style="width: 100%">
            <el-option v-for="v in teams" :key="v.name" :label="v.name" :value="v.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="维修人">
          <el-select v-model="form.repairerId" style="width: 100%">
            <el-option v-for="u in repairers" :key="u.id" :label="`${u.realName}（${u.teamName || '未分组'}）`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="要求到场">
          <el-date-picker v-model="form.requireArrive" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="安全措施">
          <el-checkbox-group v-model="form.safetyList">
            <el-checkbox v-for="v in safetyOpts" :key="v" :label="v" :value="v" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="维修方案"><el-input v-model="form.planDesc" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispatch">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dispatchOrder, getOrder, pageOrders, pageUsers, urgeOrder } from '@/api'
import { formatDateTime, money, tagType } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '', overdue: false })
const drawer = ref(false)
const detail = ref(null)
const dispatchVisible = ref(false)
const form = ref({ safetyList: [] })
const repairers = ref([])
const teams = [{ name: '井盖维修一队' }, { name: '井盖维修二队' }, { name: '排水抢修队' }, { name: '通信井盖组' }]
const safetyOpts = ['临时围挡', '警示锥', '反光标识', '气体检测', '交通疏导']
const flowSteps = ['派单', '接单', '安全措施', '维修', '待验收', '完成']

const stepIndex = computed(() => {
  const map = { 待接单: 0, 处理中: 2, 待验收: 4, 返工中: 3, 已完成: 5, 已作废: -1 }
  return map[detail.value?.orderStatus] ?? 0
})

async function load() {
  loading.value = true
  try {
    const res = await pageOrders(query)
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
  const res = await getOrder(row.id)
  detail.value = res.data
}
async function openDispatch(row) {
  form.value = { issueId: row.issueId, orderId: row.id, teamName: row.teamName || teams[0].name, safetyList: ['临时围挡', '警示锥'] }
  dispatchVisible.value = true
  if (!repairers.value.length) {
    const res = await pageUsers({ page: 1, size: 100, roleId: 4 })
    repairers.value = res.data.records || []
  }
}
async function submitDispatch() {
  if (!form.value.repairerId) return ElMessage.warning('请选择维修人')
  await dispatchOrder({
    issueId: form.value.issueId,
    teamName: form.value.teamName,
    repairerId: form.value.repairerId,
    requireArrive: form.value.requireArrive,
    safetyMeasure: (form.value.safetyList || []).join(','),
    planDesc: form.value.planDesc
  })
  ElMessage.success('派单成功')
  dispatchVisible.value = false
  load()
}
async function urge(row) {
  await urgeOrder(row.id)
  ElMessage.success('已向维修人发送催办提醒')
  load()
}
onMounted(load)
</script>

<style scoped>
.overdue { color: var(--danger); font-weight: 600; }
.imgs { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 14px; }
.imgs img { width: 100%; height: 130px; object-fit: cover; border-radius: 6px; border: 1px solid var(--line); }
.empty-img { display: grid; place-items: center; height: 130px; border: 1px dashed var(--line); border-radius: 6px; color: var(--text-3); font-size: 12px; }
.steps-mini .s i.on { background: var(--brand); color: #fff; }
</style>
