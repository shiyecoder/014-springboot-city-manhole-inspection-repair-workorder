<template>
  <div class="login-page">
    <div class="left">
      <div class="brand">
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#fff" stroke-width="1.6">
          <circle cx="12" cy="12" r="8.5" /><circle cx="12" cy="12" r="4" stroke-dasharray="2 2" />
        </svg>
        <span>城市井盖巡检与维修工单管理系统</span>
      </div>
      <div class="slogan">
        <h1>一盖一档<em>巡 · 派 · 修 · 验 全流程闭环</em></h1>
        <p>覆盖井盖台账、巡检路线、问题上报、维修派单、现场验收与备件领用。风险分级巡检支持规则配置、计算留痕与人工修正。</p>
        <div class="tags">
          <span>巡检路线</span><span>风险分级</span><span>维修工单</span><span>备件管控</span><span>验收归档</span>
        </div>
      </div>
      <div class="foot">市政设施管理科 · 养护调度中心<br />内部系统，请勿外传</div>
    </div>

    <div class="right">
      <div class="form-card">
        <h2>账号登录</h2>
        <p class="sub">请使用单位分配的账号登录，连续 5 次失败将锁定 15 分钟</p>

        <div class="roles">
          <button v-for="r in roleTabs" :key="r.code" :class="{ on: form.role === r.code }" @click="pickRole(r)">
            {{ r.name }}
          </button>
        </div>

        <el-form :model="form" @submit.prevent="doLogin">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="doLogin" />
          </el-form-item>
          <div class="row">
            <el-checkbox v-model="remember">记住用户名</el-checkbox>
            <span class="muted">忘记密码请联系管理员</span>
          </div>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="doLogin">登 录</el-button>
        </el-form>

        <div class="tip">默认管理员：admin / admin123<br />巡检员 patrol01 · 维修 repair01 · 验收 accept01 · 调度 dispatch01（密码同上）</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const loading = ref(false)
const remember = ref(true)

const roleTabs = [
  { code: 'ADMIN', name: '管理员', username: 'admin' },
  { code: 'DISPATCHER', name: '调度员', username: 'dispatch01' },
  { code: 'INSPECTOR', name: '巡检员', username: 'patrol01' },
  { code: 'REPAIRER', name: '维修人员', username: 'repair01' },
  { code: 'ACCEPTOR', name: '验收人员', username: 'accept01' }
]

const form = reactive({ username: 'admin', password: 'admin123', role: 'ADMIN' })

function pickRole(r) {
  form.role = r.code
  form.username = r.username
  form.password = 'admin123'
}

onMounted(() => {
  const saved = localStorage.getItem('mh_last_user')
  if (saved) form.username = saved
})

async function doLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const user = await store.login({ username: form.username.trim(), password: form.password })
    if (remember.value) localStorage.setItem('mh_last_user', form.username.trim())
    ElMessage.success(`欢迎回来，${user.realName}`)
    const target = user.roleCode === 'INSPECTOR' ? '/my/tasks' : user.roleCode === 'REPAIRER' ? '/my/orders' : '/dashboard'
    router.push(route.query.redirect || target)
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: grid; grid-template-columns: 480px 1fr; min-height: 100vh; }
.left {
  position: relative; overflow: hidden; display: flex; flex-direction: column;
  padding: 48px 44px; color: #fff;
  /* 主题背景图：黄昏城市路面井盖实拍，压在品牌绿渐变之下，保证文字可读 */
  background-image:
    linear-gradient(160deg, rgba(23, 54, 42, .92) 0%, rgba(35, 78, 59, .82) 48%, rgba(17, 41, 32, .94) 100%),
    url('/images/login-bg.jpg');
  background-size: cover, cover;
  background-position: center, center;
}
.left::before {
  content: ''; position: absolute; inset: 0;
  background-image: radial-gradient(circle at 1px 1px, rgba(255,255,255,.14) 1px, transparent 0);
  background-size: 26px 26px; opacity: .35;
}
.left::after {
  content: ''; position: absolute; right: -120px; bottom: -140px; width: 420px; height: 420px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,.14);
  box-shadow: 0 0 0 40px rgba(255,255,255,.04), 0 0 0 90px rgba(255,255,255,.03);
}
.brand { position: relative; display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; letter-spacing: .4px; }
.slogan { position: relative; margin: auto 0; }
.slogan h1 { font-size: 27px; line-height: 42px; font-weight: 600; margin: 0 0 16px; letter-spacing: 1px; }
.slogan h1 em { display: block; font-style: normal; color: #9FD3B4; white-space: nowrap; letter-spacing: .5px; }
.slogan p { font-size: 13px; line-height: 25px; color: rgba(255,255,255,.72); max-width: 340px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 22px; }
.tags span { font-size: 12px; padding: 4px 10px; border: 1px solid rgba(255,255,255,.22); border-radius: 3px; color: rgba(255,255,255,.85); }
.foot { position: relative; font-size: 11.5px; line-height: 20px; color: rgba(255,255,255,.42); }
.right { display: flex; align-items: center; justify-content: center; padding: 40px; background: #F4F8F5; }
.form-card { width: 380px; background: #fff; border: 1px solid #E3EBE6; border-radius: 10px; padding: 34px 32px 26px; }
.form-card h2 { margin: 0; font-size: 21px; font-weight: 600; letter-spacing: .5px; }
.sub { margin: 6px 0 22px; font-size: 12.5px; color: #8A9990; line-height: 19px; }
.roles { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 18px; }
.roles button {
  flex: 1 1 30%; height: 32px; font-size: 12.5px; border: 1px solid #E3EBE6; background: #fff;
  border-radius: 5px; color: #5C6B63; cursor: pointer; font-family: inherit;
}
.roles button.on { border-color: #234E3B; background: #EDF4EF; color: #234E3B; font-weight: 500; }
.row { display: flex; align-items: center; justify-content: space-between; margin: 4px 0 18px; font-size: 12.5px; }
.submit { width: 100%; height: 42px; font-size: 14.5px; letter-spacing: 3px; }
.tip { margin-top: 16px; font-size: 11.5px; color: #8A9990; text-align: center; line-height: 19px; }
:deep(.el-form-item) { margin-bottom: 14px; }
:deep(.el-input__wrapper) { border-radius: 6px; }
@media (max-width: 860px) {
  .login-page { grid-template-columns: 1fr; }
  .left { display: none; }
}
</style>
