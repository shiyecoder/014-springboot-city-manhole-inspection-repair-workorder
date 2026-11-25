<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>系统账号与角色分配，密码重置后默认 123456，首次登录建议修改</p>
      </div>
      <div class="right">
        <el-select v-model="query.roleId" clearable placeholder="全部角色" style="width: 140px" @change="search">
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="账号 / 姓名 / 手机号" clearable style="width: 200px" @keyup.enter="search" />
        <el-button @click="search">查询</el-button>
        <el-button type="primary" @click="openEdit()">新增用户</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="u">
              <img class="ava" :src="row.avatar || '/avatar-default.svg'" :alt="row.realName" />
              <div><div>{{ row.realName }}</div><div class="muted mono">{{ row.username }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" width="110" />
        <el-table-column prop="deptName" label="部门" width="130" />
        <el-table-column prop="teamName" label="班组" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="150">
          <template #default="{ row }">{{ formatDateTime(row.lastLogin) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="620px">
      <el-form :model="form" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="登录账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="角色">
            <el-select v-model="form.roleId" style="width: 100%">
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="部门">
            <el-select v-model="form.deptId" style="width: 100%">
              <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="班组"><el-input v-model="form.teamName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
          </el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="头像"><ImageUpload v-model="form.avatar" biz-type="AVATAR" /></el-form-item>
          </el-col>
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
import { listDepts, listRoles, pageUsers, resetPassword, saveUser } from '@/api'
import { formatDateTime } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', roleId: null })
const roles = ref([])
const depts = ref([])
const editVisible = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await pageUsers(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
const search = () => { query.page = 1; load() }

function openEdit(row) {
  form.value = row ? { ...row } : { status: 1, roleId: roles.value[0]?.id }
  editVisible.value = true
}
async function save() {
  if (!form.value.username || !form.value.realName) return ElMessage.warning('请填写账号与姓名')
  await saveUser(form.value)
  ElMessage.success('保存成功')
  editVisible.value = false
  load()
}
async function reset(row) {
  await ElMessageBox.confirm(`将 ${row.realName} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await resetPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}
onMounted(async () => {
  load()
  const [r, d] = await Promise.all([listRoles(), listDepts()])
  roles.value = r.data || []
  depts.value = d.data || []
})
</script>

<style scoped>
.u { display: flex; align-items: center; gap: 8px; }
.ava { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); background: #E8F0EB; }
</style>
