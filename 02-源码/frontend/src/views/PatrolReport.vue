<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>巡检上报</h1>
        <p>发现井盖缺损可在此直接上报，提交后自动计算风险等级并进入派单流程</p>
      </div>
      <div class="right"><el-button @click="$router.push('/my/tasks')">返回我的任务</el-button></div>
    </div>

    <div class="report-wrap">
      <div class="card">
        <div class="card-h"><h3>问题信息</h3><span class="muted">带 * 为必填</span></div>
        <el-form :model="form" label-width="94px">
          <el-form-item label="井盖" required>
            <el-select v-model="form.coverId" filterable remote :remote-method="searchCover" placeholder="输入井盖编号或道路搜索" style="width: 100%">
              <el-option v-for="c in coverOpts" :key="c.id" :label="`${c.coverNo} · ${c.roadName} · ${c.locationDesc}`" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-row :gutter="14">
            <el-col :span="12"><el-form-item label="缺损类型" required>
              <el-select v-model="form.damageType" style="width: 100%">
                <el-option v-for="v in ['破损', '松动', '沉降', '丢失', '异响']" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item></el-col>
            <el-col :span="12"><el-form-item label="严重度" required>
              <el-select v-model="form.severity" style="width: 100%">
                <el-option v-for="v in ['轻微', '一般', '严重']" :key="v" :label="v" :value="v" />
              </el-select>
            </el-form-item></el-col>
          </el-row>
          <el-form-item label="来源">
            <el-radio-group v-model="form.sourceType">
              <el-radio value="巡检发现">巡检发现</el-radio>
              <el-radio value="市民举报">市民举报</el-radio>
              <el-radio value="日常巡查">日常巡查</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="问题描述"><el-input v-model="form.description" type="textarea" :rows="4" placeholder="如：井盖与井圈间隙约 2cm，车辆碾压时明显跳动" /></el-form-item>
          <el-form-item label="现场照片" required><ImageUpload v-model="form.imageUrl" biz-type="ISSUE" tip="必填，用于风险评分与后续验收比对" /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submit">提交上报</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card side">
        <div class="card-h"><h3>上报须知</h3></div>
        <ol class="tips">
          <li>现场照片需能看清井盖整体与缺损部位，避免逆光。</li>
          <li>严重度按实际影响填写：影响车辆通行选「严重」。</li>
          <li>提交后系统按道路等级、人流、历史维修、重复上报等 6 个因素计算风险分。</li>
          <li>风险分达到重大阈值的工单要求 4 小时内到场，请如实上报。</li>
          <li>同一井盖 7 天内重复上报会自动累计重复次数并抬高权重。</li>
        </ol>
        <div class="note" style="margin-top: 14px">今日你已上报 <b>{{ todayCount }}</b> 条问题。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { coverOptions, createIssue, pageIssues } from '@/api'
import ImageUpload from '@/components/ImageUpload.vue'

const form = ref({ damageType: '破损', severity: '一般', sourceType: '巡检发现' })
const coverOpts = ref([])
const todayCount = ref(0)

async function searchCover(keyword) {
  const res = await coverOptions(keyword)
  coverOpts.value = res.data || []
}
function reset() {
  form.value = { damageType: '破损', severity: '一般', sourceType: '巡检发现' }
}
async function submit() {
  if (!form.value.coverId) return ElMessage.warning('请选择井盖')
  if (!form.value.damageType || !form.value.severity) return ElMessage.warning('请选择缺损类型与严重度')
  if (!form.value.imageUrl) return ElMessage.warning('请上传现场照片')
  await createIssue(form.value)
  ElMessage.success('上报成功，已进入风险评分与派单流程')
  reset()
  await loadToday()
}
async function loadToday() {
  const res = await pageIssues({ page: 1, size: 1, mine: true })
  todayCount.value = res.data.total
}
onMounted(() => { searchCover(''); loadToday() })
</script>

<style scoped>
.report-wrap { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 14px; align-items: start; }
.tips { margin: 0; padding-left: 18px; font-size: 12.5px; color: var(--text-2); line-height: 1.9; }
</style>
