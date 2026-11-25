// 全局时间格式化：全系统统一调用，禁止各页面自行拼接
export function formatDateTime(value) {
  if (!value) return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) return value
  const d = value instanceof Date ? value : new Date(String(value).replace(/-/g, '/').replace('T', ' '))
  if (Number.isNaN(d.getTime())) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function formatDate(value) {
  if (!value) return ''
  return formatDateTime(value).slice(0, 10)
}

// 相对时间：用于"等待 3 小时"等场景
export function fromNow(value) {
  if (!value) return ''
  const d = new Date(String(value).replace(/-/g, '/').replace('T', ' '))
  if (Number.isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  const m = Math.floor(diff / 60000)
  if (m < 60) return `${Math.max(m, 0)} 分钟`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时`
  return `${Math.floor(h / 24)} 天`
}

export function money(value) {
  const n = Number(value || 0)
  return '¥ ' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 状态 → 标签样式，保证不只靠颜色表达，同时输出中文
export const STATUS_TAG = {
  待确认: 'amber', 待派单: 'amber', 处理中: 'blue', 待验收: 'amber', 返工中: 'red', 已关闭: 'green', 已取消: 'gray',
  待接单: 'amber', 已完成: 'green', 已作废: 'gray',
  待执行: 'blue', 进行中: 'amber', 正常: 'green', 维修中: 'blue',
  重大: 'red', 紧急: 'amber', 一般: 'blue', 合格: 'green', 不合格: 'red',
  待本人处理: 'amber'
}

export function tagType(status) {
  return STATUS_TAG[status] || 'gray'
}

export const RISK_COLOR = { 重大: '#A93226', 紧急: '#B7791F', 一般: '#2C6E8F' }
export const CHART_COLORS = ['#234E3B', '#3A7D5A', '#315F4C', '#7FA08F', '#B7791F', '#2C6E8F', '#A93226']
