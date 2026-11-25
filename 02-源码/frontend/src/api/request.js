import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({ baseURL: '/api', timeout: 20000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('mh_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && body.code === 200) return body
    if (body && body.code === 401) {
      localStorage.removeItem('mh_token')
      localStorage.removeItem('mh_user')
      router.push('/login')
      return Promise.reject(new Error(body.message || '登录已失效'))
    }
    const msg = (body && body.message) || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(new Error(msg))
  },
  (err) => {
    const msg = err?.response?.data?.message || err.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
