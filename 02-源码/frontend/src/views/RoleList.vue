<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>角色管理</h1>
        <p>角色决定侧边栏菜单与接口权限，菜单树按角色动态下发</p>
      </div>
      <div class="right"><el-button @click="load">刷新</el-button></div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleCode" label="角色编码" width="140" />
        <el-table-column label="数据范围" width="130">
          <template #default="{ row }">
            <span class="tag tag-blue">{{ scopeText(row.dataScope) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="240" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tag" :class="row.status ? 'tag-green' : 'tag-gray'">{{ row.status ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAssign(row)">分配菜单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="assignVisible" :title="`为「${current?.roleName}」分配菜单`" width="520px">
      <div class="note">勾选后保存，该角色下次登录即可看到对应菜单。菜单管理项不在可分配范围内，避免权限自举。</div>
      <el-tree ref="treeRef" :data="menuTreeData" show-checkbox node-key="id" :props="{ label: 'menuName', children: 'children' }"
        default-expand-all style="margin-top: 10px; max-height: 420px; overflow: auto" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assignMenus, listRoles, menuTree, roleMenus } from '@/api'

const rows = ref([])
const loading = ref(false)
const assignVisible = ref(false)
const current = ref(null)
const menuTreeData = ref([])
const treeRef = ref(null)

const scopeText = (v) => ({ 1: '全部数据', 2: '本部门', 3: '本班组', 4: '仅本人' }[v] || '仅本人')

async function load() {
  loading.value = true
  try {
    const res = await listRoles()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}
async function openAssign(row) {
  current.value = row
  assignVisible.value = true
  const [tree, checked] = await Promise.all([menuTree(true), roleMenus(row.id)])
  menuTreeData.value = tree.data || []
  // 只设置叶子节点，父节点由 tree 自动推导半选状态
  const leafIds = (checked.data || []).filter((id) => !hasChild(id))
  setTimeout(() => treeRef.value?.setCheckedKeys(leafIds), 0)
}
function hasChild(id) {
  const walk = (nodes) => nodes.some((n) => n.id === id ? (n.children && n.children.length) : walk(n.children || []))
  return !!walk(menuTreeData.value)
}
async function saveAssign() {
  const ids = [...(treeRef.value?.getCheckedKeys() || []), ...(treeRef.value?.getHalfCheckedKeys() || [])]
  await assignMenus(current.value.id, ids)
  ElMessage.success('菜单权限已保存')
  assignVisible.value = false
}
onMounted(load)
</script>
