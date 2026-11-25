<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>菜单管理</h1>
        <p>菜单树与前端路由一一对应，隐藏项不会下发给前端</p>
      </div>
      <div class="right">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openEdit()">新增菜单</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由地址" min-width="150" />
        <el-table-column prop="component" label="组件" min-width="130" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.menuType === 'DIR' ? 'tag-gray' : 'tag-blue'">{{ row.menuType === 'DIR' ? '目录' : '菜单' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="perm" label="权限标识" min-width="140" />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="84">
          <template #default="{ row }">
            <span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '正常' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" title="菜单信息" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <!-- 菜单管理自身不提供作为父级，避免权限自举 -->
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="顶级菜单" :value="0" />
            <el-option v-for="m in parentOptions" :key="m.id" :label="m.menuName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="菜单名称"><el-input v-model="form.menuName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型">
            <el-select v-model="form.menuType" style="width: 100%">
              <el-option label="目录" value="DIR" />
              <el-option label="菜单" value="MENU" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="路由地址"><el-input v-model="form.path" placeholder="/covers" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="组件名"><el-input v-model="form.component" placeholder="CoverList" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="图标"><el-input v-model="form.icon" placeholder="cover" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="权限标识"><el-input v-model="form.perm" placeholder="cover:list" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { menuTree } from '@/api'

const rows = ref([])
const loading = ref(false)
const editVisible = ref(false)
const form = ref({})

// 可作为父级的只有目录与顶级，并且排除「菜单管理」自身
const parentOptions = computed(() => {
  const out = []
  const walk = (nodes) => nodes.forEach((n) => {
    if (n.menuName !== '菜单管理' && n.menuType === 'DIR') out.push(n)
    if (n.children) walk(n.children)
  })
  walk(rows.value || [])
  return out
})

async function load() {
  loading.value = true
  try {
    const res = await menuTree(false)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}
function openEdit(row) {
  form.value = row ? { ...row } : { parentId: 0, menuType: 'MENU', sort: 10, status: 1 }
  editVisible.value = true
}
async function save() {
  if (!form.value.menuName) return ElMessage.warning('请填写菜单名称')
  ElMessage.info('菜单新增/修改请通过初始化脚本维护，本页用于查看与微调展示顺序')
  editVisible.value = false
}
onMounted(load)
</script>
