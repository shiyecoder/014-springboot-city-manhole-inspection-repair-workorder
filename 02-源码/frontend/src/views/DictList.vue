<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>字典管理</h1>
        <p>井盖类型、缺损类型、道路等级等枚举值集中维护，前端下拉框统一读取</p>
      </div>
      <div class="right">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openEdit()">新增字典项</el-button>
      </div>
    </div>

    <div class="card">
      <el-tabs v-model="activeType" @tab-change="load">
        <el-tab-pane v-for="t in types" :key="t.value" :label="t.label" :name="t.value" />
      </el-tabs>
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="dictLabel" label="显示名称" min-width="180" />
        <el-table-column prop="dictValue" label="字典值" min-width="140" />
        <el-table-column prop="dictType" label="字典类型" width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" title="字典项" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="字典类型"><el-input v-model="form.dictType" disabled /></el-form-item>
        <el-form-item label="显示名称"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典值"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listDicts, saveDict } from '@/api'

const types = [
  { label: '井盖类型', value: 'cover_type' },
  { label: '缺损类型', value: 'damage_type' },
  { label: '道路等级', value: 'road_level' },
  { label: '人流等级', value: 'crowd_level' },
  { label: '安全措施', value: 'safety_measure' }
]
const activeType = ref('cover_type')
const rows = ref([])
const loading = ref(false)
const editVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await listDicts(activeType.value)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}
function openEdit(row) {
  form.value = row ? { ...row } : { dictType: activeType.value, sort: 10, status: 1 }
  editVisible.value = true
}
async function save() {
  if (!form.value.dictLabel || !form.value.dictValue) return ElMessage.warning('请填写显示名称与字典值')
  await saveDict(form.value)
  ElMessage.success('保存成功')
  editVisible.value = false
  load()
}
onMounted(load)
</script>
