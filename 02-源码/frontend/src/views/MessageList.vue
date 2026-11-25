<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>消息中心</h1>
        <p>派单、催办、验收与超时提醒都在这里，未读消息会在顶栏铃铛提示</p>
      </div>
      <div class="right">
        <el-radio-group v-model="query.unread" @change="load">
          <el-radio-button :value="false">全部</el-radio-button>
          <el-radio-button :value="true">未读</el-radio-button>
        </el-radio-group>
        <el-button @click="readAll">全部已读</el-button>
      </div>
    </div>

    <div class="card">
      <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: !m.read_flag }" @click="read(m)">
        <div class="dot" />
        <div class="body">
          <div class="title">
            <span class="tag" :class="`tag-${msgTag(m.msgType)}`">{{ m.msg_type }}</span>
            <b>{{ m.title }}</b>
          </div>
          <div class="content">{{ m.content }}</div>
          <div class="muted">{{ formatDateTime(m.createTime) }}</div>
        </div>
        <el-button v-if="!m.read_flag" link type="primary" size="small">标记已读</el-button>
      </div>
      <el-empty v-if="!rows.length" description="暂无消息" />
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
          layout="prev, pager, next" @change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageMessages, readAllMessages, readMessage } from '@/api'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, unread: false })

const msgTag = (t) => ({ 派单: 'blue', 催办: 'amber', 验收: 'green', 超时: 'red', 系统: 'gray' }[t] || 'gray')

async function load() {
  const res = await pageMessages(query)
  rows.value = res.data.records
  total.value = res.data.total
}
async function read(m) {
  if (m.read_flag) return
  await readMessage(m.id)
  m.read_flag = 1
}
async function readAll() {
  await readAllMessages()
  ElMessage.success('已全部标记为已读')
  load()
}
onMounted(load)
</script>

<style scoped>
.msg { display: flex; align-items: flex-start; gap: 10px; padding: 12px 4px; border-bottom: 1px solid var(--line); cursor: pointer; }
.msg:last-of-type { border-bottom: none; }
.msg .dot { width: 7px; height: 7px; border-radius: 50%; background: transparent; margin-top: 8px; flex: 0 0 7px; }
.msg.unread .dot { background: var(--danger); }
.msg.unread b { font-weight: 600; }
.msg .body { flex: 1; min-width: 0; }
.msg .title { display: flex; align-items: center; gap: 8px; font-size: 13.5px; }
.msg .content { font-size: 12.5px; color: var(--text-2); margin: 4px 0 3px; }
.msg:hover { background: #F8FBF9; }
</style>
