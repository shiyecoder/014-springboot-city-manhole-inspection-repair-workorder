<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>我的工单</h1>
        <p>接单 → 上传安全措施照片 → 领料 → 提交维修结果，每一步都会留痕</p>
      </div>
      <div class="right">
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px" @change="load">
          <el-option v-for="v in ['待接单', '处理中', '返工中', '待验收', '已完成']" :key="v" :label="v" :value="v" />
        </el-select>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="orderNo" label="工单号" width="158" />
        <el-table-column label="井盖 / 道路" min-width="170">
          <template #default="{ row }">
            <div class="mono">{{ row.coverNo }}</div>
            <div class="muted">{{ row.roadName }} · {{ row.coverType }}</div>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="88">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span></template>
        </el-table-column>
        <el-table-column prop="damageType" label="缺损" width="82" />
        <el-table-column label="要求到场" width="150">
          <template #default="{ row }"><span :class="row.overdue ? 'overdue' : ''">{{ formatDateTime(row.requireArrive) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.orderStatus)}`">{{ row.orderStatus }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.orderStatus === '待接单'" link type="primary" @click="accept(row)">接单</el-button>
            <el-button v-if="['处理中', '返工中'].includes(row.orderStatus)" link type="primary" @click="openSafety(row)">安全措施</el-button>
            <el-button v-if="['处理中', '返工中'].includes(row.orderStatus)" link type="primary" @click="openApply(row)">领料</el-button>
            <el-button v-if="['处理中', '返工中'].includes(row.orderStatus)" link type="success" @click="openSubmit(row)">提交结果</el-button>
            <el-button link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="safetyVisible" title="登记安全措施" width="520px">
      <el-form label-width="94px">
        <el-form-item label="安全措施">
          <el-checkbox-group v-model="safetyList">
            <el-checkbox v-for="v in safetyOpts" :key="v" :label="v" :value="v" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="现场照片">
          <ImageUpload v-model="beforeImage" biz-type="ORDER" tip="须拍到围挡与警示设施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="safetyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSafety">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="领用备件" width="520px">
      <el-form label-width="94px">
        <el-form-item label="备件">
          <el-select v-model="applyForm.partId" filterable style="width: 100%">
            <el-option v-for="p in parts" :key="p.id" :label="`${p.partName} ${p.spec}（库存 ${p.stockQty}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="applyForm.qty" :min="1" :max="20" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="applyForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">确认领料</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="submitVisible" title="提交维修结果" width="560px">
      <el-form label-width="94px">
        <el-form-item label="维修结果"><el-input v-model="resultDesc" type="textarea" :rows="3" placeholder="如：已更换井盖并恢复路面，标识已喷涂" /></el-form-item>
        <el-form-item label="完工照片"><ImageUpload v-model="afterImage" biz-type="ORDER" tip="必填，验收将与你上传的照片比对" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResult">提交验收</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" title="工单详情" size="560px">
      <div v-if="detail">
        <div class="detail-head">
          <div class="l1"><h1>{{ detail.orderNo }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.orderStatus)}`">{{ detail.orderStatus }}</span></div>
          <div class="l2"><span>井盖<b>{{ detail.coverNo }}</b></span><span>道路<b>{{ detail.roadName }}</b></span>
            <span>要求到场<b>{{ formatDateTime(detail.requireArrive) }}</b></span></div>
        </div>
        <dl class="kv">
          <dt>安全措施</dt><dd>{{ detail.safetyMeasure || '未登记' }}</dd>
          <dt>维修方案</dt><dd>{{ detail.planDesc }}</dd>
          <dt>维修结果</dt><dd>{{ detail.resultDesc || '未提交' }}</dd>
          <dt>材料费用</dt><dd>{{ money(detail.materialCost) }}</dd>
          <dt>返工次数</dt><dd>{{ detail.reworkCount }}</dd>
        </dl>
        <div class="imgs">
          <div><div class="muted">维修前</div><img v-if="detail.beforeImage" :src="detail.beforeImage" alt="维修前" /></div>
          <div><div class="muted">维修后</div><img v-if="detail.afterImage" :src="detail.afterImage" alt="维修后" /></div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { acceptOrder, applyPart, getOrder, pageOrders, pageParts, registerSafety, submitOrder } from '@/api'
import { formatDateTime, money, tagType } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, mine: true, status: '' })
const detail = ref(null)
const drawer = ref(false)
const safetyVisible = ref(false)
const applyVisible = ref(false)
const submitVisible = ref(false)
const safetyList = ref([])
const beforeImage = ref('')
const afterImage = ref('')
const resultDesc = ref('')
const current = ref(null)
const parts = ref([])
const applyForm = ref({ qty: 1 })
const safetyOpts = ['临时围挡', '警示锥', '反光标识', '气体检测', '交通疏导']

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
async function accept(row) {
  await ElMessageBox.confirm(`确认接单 ${row.orderNo}？接单后请按时到场。`, '接单确认', { type: 'info' })
  await acceptOrder(row.id)
  ElMessage.success('已接单')
  load()
}
function openSafety(row) {
  current.value = row
  safetyList.value = (row.safetyMeasure || '').split(',').filter(Boolean)
  beforeImage.value = row.beforeImage || ''
  safetyVisible.value = true
}
async function submitSafety() {
  if (!beforeImage.value) return ElMessage.warning('请上传安全措施现场照片')
  await registerSafety(current.value.id, { safetyMeasure: safetyList.value.join(','), beforeImage: beforeImage.value })
  ElMessage.success('安全措施已登记')
  safetyVisible.value = false
  load()
}
async function openApply(row) {
  current.value = row
  applyForm.value = { orderId: row.id, qty: 1 }
  applyVisible.value = true
  const res = await pageParts({ page: 1, size: 100 })
  parts.value = res.data.records || []
}
async function submitApply() {
  if (!applyForm.value.partId) return ElMessage.warning('请选择备件')
  await applyPart(applyForm.value)
  ElMessage.success('领料成功，库存已扣减')
  applyVisible.value = false
}
function openSubmit(row) {
  current.value = row
  resultDesc.value = row.resultDesc || ''
  afterImage.value = row.afterImage || ''
  submitVisible.value = true
}
async function submitResult() {
  if (!afterImage.value) return ElMessage.warning('请上传完工照片')
  await submitOrder(current.value.id, { resultDesc: resultDesc.value, afterImage: afterImage.value })
  ElMessage.success('已提交，等待验收')
  submitVisible.value = false
  load()
}
async function openDetail(row) {
  drawer.value = true
  detail.value = null
  const res = await getOrder(row.id)
  detail.value = res.data
}
onMounted(load)
</script>

<style scoped>
.overdue { color: var(--danger); font-weight: 600; }
.imgs { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 14px; }
.imgs img { width: 100%; height: 140px; object-fit: cover; border-radius: 6px; border: 1px solid var(--line); }
</style>
