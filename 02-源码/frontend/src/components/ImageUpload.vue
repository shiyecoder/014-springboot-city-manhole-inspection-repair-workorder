<template>
  <div class="iu">
    <el-upload :show-file-list="false" :http-request="doUpload" accept="image/png,image/jpeg,image/webp">
      <div v-if="modelValue" class="preview">
        <img :src="modelValue" alt="已上传图片" />
        <span class="mask">点击更换</span>
      </div>
      <div v-else class="picker">
        <el-icon><Plus /></el-icon>
        <span>上传图片</span>
      </div>
    </el-upload>
    <div class="tip">{{ tip }}</div>
  </div>
</template>

<script setup>
// 统一图片上传：走 /api/files/upload，落盘到项目根 uploads/，返回相对路径
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const props = defineProps({
  modelValue: { type: String, default: '' },
  bizType: { type: String, default: 'COVER' },
  tip: { type: String, default: '支持 jpg / png，单张不超过 10MB' }
})
const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

async function doUpload(options) {
  const fd = new FormData()
  fd.append('file', options.file)
  fd.append('bizType', props.bizType)
  uploading.value = true
  try {
    const res = await request.post('/files/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    emit('update:modelValue', res.data.url)
    ElMessage.success('图片上传成功')
  } catch (e) {
    // 错误提示已由拦截器统一处理
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.iu { display: flex; flex-direction: column; gap: 6px; }
.preview { position: relative; width: 148px; height: 100px; border-radius: 6px; overflow: hidden; border: 1px solid var(--line); cursor: pointer; }
.preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.preview .mask { position: absolute; left: 0; right: 0; bottom: 0; height: 24px; background: rgba(0, 0, 0, .55); color: #fff; font-size: 11.5px; display: grid; place-items: center; }
.picker { width: 148px; height: 100px; border: 1px dashed #C6D6CC; border-radius: 6px; display: grid; place-items: center; gap: 4px; color: var(--text-3); font-size: 12px; background: #FAFCFB; }
.picker:hover { border-color: var(--brand); color: var(--brand); }
.tip { font-size: 11.5px; color: var(--text-3); }
</style>
