<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>备件库存</h1>
        <p>井盖、井圈等维修备件的出入库与领用台账，低于预警线自动标红</p>
      </div>
      <div class="right">
        <el-checkbox v-model="query.lowStock" @change="search">仅看低库存</el-checkbox>
        <el-input v-model="query.keyword" placeholder="备件号 / 名称" clearable style="width: 190px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
        <el-button type="primary" @click="openCreate">新增备件</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="partNo" label="备件编号" width="110" />
        <el-table-column prop="partName" label="备件名称" min-width="150" />
        <el-table-column prop="category" label="分类" width="90" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="库存 / 预警" width="150">
          <template #default="{ row }">
            <span :class="row.stockQty <= row.warnQty ? 'low' : ''">{{ row.stockQty }}</span>
            <span class="muted"> / {{ row.warnQty }} {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100">
          <template #default="{ row }">{{ money(row.unitPrice) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="库位" width="100" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openIn(row)">入库</el-button>
            <el-button link type="primary" @click="openFlows(row)">流水</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="inVisible" title="备件入库" width="460px">
      <el-form label-width="80px">
        <el-form-item label="备件"><b>{{ current?.partName }} {{ current?.spec }}</b></el-form-item>
        <el-form-item label="入库数量"><el-input-number v-model="inForm.qty" :min="1" :max="500" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="inForm.remark" placeholder="如：市政仓库调拨" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIn">确认入库</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="flowsVisible" :title="`出入库流水 · ${current?.partName || ''}`" size="560px">
      <el-table :data="flows" size="small">
        <el-table-column prop="flowNo" label="流水号" width="150" />
        <el-table-column label="类型" width="76">
          <template #default="{ row }">
            <span class="tag" :class="row.flowType === '入库' ? 'tag-green' : 'tag-amber'">{{ row.flowType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="operatorName" label="经办人" width="88" />
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column label="时间" width="145">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!flows.length" description="暂无流水" />
    </el-drawer>

    <el-dialog v-model="createVisible" title="新增备件" width="540px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="备件编号"><el-input v-model="form.partNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.partName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="分类">
            <el-select v-model="form.category" style="width: 100%">
              <el-option v-for="v in ['井盖', '井圈', '螺栓', '辅材']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单位"><el-input v-model="form.unit" placeholder="套 / 个 / kg" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存"><el-input-number v-model="form.stockQty" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="预警线"><el-input-number v-model="form.warnQty" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="库位"><el-input v-model="form.location" placeholder="A-01-01" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { partFlows, pageParts, savePart, stockIn } from '@/api'
import { formatDateTime, money } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', lowStock: false })
const inVisible = ref(false)
const flowsVisible = ref(false)
const createVisible = ref(false)
const current = ref(null)
const inForm = ref({ qty: 10 })
const flows = ref([])
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageParts(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }

function openIn(row) {
  current.value = row
  inForm.value = { qty: 10 }
  inVisible.value = true
}
async function submitIn() {
  await stockIn(current.value.id, inForm.value)
  ElMessage.success('入库成功')
  inVisible.value = false
  load()
}
async function openFlows(row) {
  current.value = row
  flowsVisible.value = true
  flows.value = []
  const res = await partFlows(row.id)
  flows.value = res.data || []
}
function openCreate() {
  form.value = { category: '井盖', unit: '套', stockQty: 0, warnQty: 10, unitPrice: 0 }
  createVisible.value = true
}
async function save() {
  if (!form.value.partNo || !form.value.partName) return ElMessage.warning('请填写备件编号与名称')
  await savePart(form.value)
  ElMessage.success('保存成功')
  createVisible.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.low { color: var(--danger); font-weight: 600; }
</style>
