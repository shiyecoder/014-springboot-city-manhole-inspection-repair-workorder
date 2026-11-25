<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>个人中心</h1>
        <p>维护个人资料与登录密码，头像上传后右上角会同步更新</p>
      </div>
    </div>

    <div class="profile-wrap">
      <div class="card">
        <div class="card-h"><h3>基本资料</h3></div>
        <el-form :model="form" label-width="90px">
          <el-form-item label="头像">
            <ImageUpload v-model="form.avatar" biz-type="AVATAR" tip="建议 1:1 正方形，用于顶栏与工单签名" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="所属班组"><el-input v-model="form.teamName" disabled /></el-form-item></el-col>
          </el-row>
          <el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="card-h"><h3>账号信息</h3></div>
        <dl class="kv">
          <dt>登录账号</dt><dd class="mono">{{ user?.username }}</dd>
          <dt>角色</dt><dd><span class="tag tag-blue">{{ user?.roleName }}</span></dd>
          <dt>所属部门</dt><dd>{{ user?.deptName }}</dd>
          <dt>最近登录</dt><dd>{{ formatDateTime(user?.lastLogin) }}</dd>
        </dl>
        <div class="card-h" style="margin-top: 18px"><h3>修改密码</h3></div>
        <el-form :model="pwd" label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
          <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item><el-button type="primary" @click="savePwd">修改密码</el-button></el-form-item>
        </el-form>
        <div class="note">密码修改后当前登录状态仍然有效，下次登录请使用新密码。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updatePassword, updateProfile } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const store = useUserStore()
const user = computed(() => store.user)
const form = ref({})
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function saveProfile() {
  await updateProfile(form.value)
  await store.loadMe()
  ElMessage.success('资料已保存')
}
async function savePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写原密码与新密码')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await updatePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  pwd.oldPassword = pwd.newPassword = pwd.confirm = ''
}
onMounted(() => {
  form.value = { ...store.user }
})
</script>

<style scoped>
.profile-wrap { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 14px; align-items: start; }
</style>
