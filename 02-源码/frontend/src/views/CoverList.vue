<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>井盖设施台账</h1>
        <p>全市井盖基础档案与风险状态，共 {{ total }} 座 · 支持按道路、等级、状态筛选</p>
      </div>
      <div class="right">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openEdit()">新增井盖</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <div class="item"><label>关键字</label>
          <el-input v-model="query.keyword" placeholder="编号 / 道路 / 位置" clearable @keyup.enter="search" />
        </div>
        <div class="item"><label>道路等级</label>
          <el-select v-model="query.roadLevel" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['主干道', '次干道', '支路']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <div class="item"><label>风险等级</label>
          <el-select v-model="query.riskLevel" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['重大', '紧急', '一般']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <div class="item"><label>设施状态</label>
          <el-select v-model="query.coverStatus" clearable placeholder="全部" @change="search">
            <el-option v-for="v in ['正常', '维修中', '停用']" :key="v" :label="v" :value="v" />
          </el-select>
        </div>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" style="margin-top: 14px">
        <el-table-column label="现场图" width="76">
          <template #default="{ row }"><img class="thumb" :src="row.imageUrl" alt="井盖现场图" /></template>
        </el-table-column>
        <el-table-column prop="coverNo" label="井盖编号" width="112" />
        <el-table-column prop="coverType" label="类型" width="100" />
        <el-table-column label="所在道路 / 定位" min-width="220">
          <template #default="{ row }">
            <div>{{ row.roadName }}</div>
            <div class="muted">{{ row.locationDesc }}</div>
          </template>
        </el-table-column>
        <el-table-column label="道路 / 人流" width="130">
          <template #default="{ row }">
            <span class="tag tag-gray">{{ row.roadLevel }}</span>
            <span class="tag tag-gray" style="margin-left: 4px">{{ row.crowdLevel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="风险等级" width="96">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.riskLevel)}`">{{ row.riskLevel }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }"><span class="tag" :class="`tag-${tagType(row.coverStatus)}`">{{ row.coverStatus }}</span></template>
        </el-table-column>
        <el-table-column prop="dutyUserName" label="责任人" width="88" />
        <el-table-column prop="ownerUnit" label="权属单位" width="110" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <!-- 详情抽屉：档案 + 历史问题 + 维修工单 + 变更留痕 -->
    <el-drawer v-model="drawer" title="井盖详情" size="560px">
      <div v-if="detail">
        <img :src="detail.imageUrl" style="width: 100%; height: 200px; object-fit: cover; border-radius: 8px" alt="井盖现场图" />
        <div class="detail-head" style="margin-top: 12px">
          <div class="l1"><h1>{{ detail.coverNo }}</h1>
            <span class="tag" :class="`tag-${tagType(detail.riskLevel)}`">{{ detail.riskLevel }}</span>
            <span class="tag" :class="`tag-${tagType(detail.coverStatus)}`">{{ detail.coverStatus }}</span>
          </div>
          <div class="l2"><span>道路<b>{{ detail.roadName }}</b></span><span>责任人<b>{{ detail.dutyUserName }}</b></span>
            <span>累计维修<b>{{ detail.repairCount }}</b> 次</span></div>
        </div>
        <dl class="kv">
          <dt>类型规格</dt><dd>{{ detail.coverType }} · {{ detail.spec }}</dd>
          <dt>材质</dt><dd>{{ detail.material }}</dd>
          <dt>安装日期</dt><dd>{{ detail.installDate }}</dd>
          <dt>道路等级</dt><dd>{{ detail.roadLevel }} / 人流 {{ detail.crowdLevel }}</dd>
          <dt>权属单位</dt><dd>{{ detail.ownerUnit }}</dd>
          <dt>定位坐标</dt><dd class="mono">{{ detail.longitude }}, {{ detail.latitude }}</dd>
          <dt>位置描述</dt><dd>{{ detail.locationDesc }}</dd>
        </dl>
        <div class="card-h" style="margin-top: 18px"><h3>历史问题（{{ (detail.issues || []).length }}）</h3></div>
        <el-table :data="detail.issues || []" size="small">
          <el-table-column prop="issueNo" label="问题编号" width="150" />
          <el-table-column prop="damageType" label="类型" width="70" />
          <el-table-column prop="riskLevel" label="风险" width="70" />
          <el-table-column prop="issueStatus" label="状态" width="80" />
          <el-table-column label="上报时间" min-width="140">
            <template #default="{ row }">{{ formatDateTime(row.reportTime) }}</template>
          </el-table-column>
        </el-table>
        <div class="card-h" style="margin-top: 16px"><h3>状态变更留痕</h3></div>
        <el-timeline>
          <el-timeline-item v-for="t in detail.timeline || []" :key="t.id" :timestamp="formatDateTime(t.createTime)" placement="top">
            {{ t.action }} · {{ t.operatorName || '系统' }}<span class="muted" v-if="t.remark">（{{ t.remark }}）</span>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑井盖' : '新增井盖'" width="620px">
      <el-form :model="form" label-width="88px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="井盖编号"><el-input v-model="form.coverNo" placeholder="如 JHD-0301" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型">
            <el-select v-model="form.coverType" style="width: 100%">
              <el-option v-for="v in ['雨水井盖', '污水井盖', '通信井盖', '燃气井盖', '电力井盖']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="道路名称"><el-input v-model="form.roadName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="道路等级">
            <el-select v-model="form.roadLevel" style="width: 100%">
              <el-option v-for="v in ['主干道', '次干道', '支路']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="人流等级">
            <el-select v-model="form.crowdLevel" style="width: 100%">
              <el-option v-for="v in ['密集', '一般', '稀少']" :key="v" :label="v" :value="v" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格"><el-input v-model="form.spec" placeholder="Φ700 重型" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="材质"><el-input v-model="form.material" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="权属单位"><el-input v-model="form.ownerUnit" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="位置描述"><el-input v-model="form.locationDesc" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="经度"><el-input v-model.number="form.longitude" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="纬度"><el-input v-model.number="form.latitude" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="现场图片">
            <ImageUpload v-model="form.imageUrl" biz-type="COVER" tip="台账列表缩略图，建议 4:3" />
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCover, getCover, pageCovers, saveCover } from '@/api'
import { formatDateTime, tagType } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', roadLevel: '', riskLevel: '', coverStatus: '' })

const drawer = ref(false)
const detail = ref(null)
const editVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageCovers(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }
const reset = () => { Object.assign(query, { page: 1, keyword: '', roadLevel: '', riskLevel: '', coverStatus: '' }); load() }

async function openDetail(row) {
  drawer.value = true
  detail.value = null
  const res = await getCover(row.id)
  detail.value = res.data
}
function openEdit(row) {
  form.value = row ? { ...row } : { coverType: '雨水井盖', roadLevel: '主干道', crowdLevel: '一般', coverStatus: '正常', riskLevel: '一般' }
  editVisible.value = true
}
async function save() {
  if (!form.value.coverNo || !form.value.roadName) return ElMessage.warning('请填写编号与道路名称')
  await saveCover(form.value, form.value.id)
  ElMessage.success('保存成功')
  editVisible.value = false
  load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除井盖 ${row.coverNo}？删除后历史记录仍保留。`, '删除确认', { type: 'warning' })
  await deleteCover(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
