import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const getMe = () => request.get('/auth/me')
export const updatePassword = (data) => request.put('/auth/password', data)
export const updateProfile = (data) => request.put('/auth/profile', data)

// 看板
export const getDashboard = () => request.get('/dashboard')

// 井盖台账
export const pageCovers = (params) => request.get('/manhole-repair/page', { params })
export const getCover = (id) => request.get(`/manhole-repair/${id}`)
export const saveCover = (data, id) => (id ? request.put(`/manhole-repair/${id}`, data) : request.post('/manhole-repair', data))
export const deleteCover = (id) => request.delete(`/manhole-repair/${id}`)
export const coverOptions = (keyword) => request.get('/manhole-repair/options', { params: { keyword } })

// 问题上报
export const pageIssues = (params) => request.get('/issues/page', { params })
export const getIssue = (id) => request.get(`/issues/${id}`)
export const createIssue = (data) => request.post('/issues', data)
export const confirmIssue = (id) => request.post(`/issues/${id}/confirm`)
export const transitionIssue = (id, data) => request.post(`/issues/${id}/transition`, data)
export const evaluateIssue = (id) => request.post(`/issues/${id}/evaluate`)

// 维修工单
export const pageOrders = (params) => request.get('/orders/page', { params })
export const getOrder = (id) => request.get(`/orders/${id}`)
export const dispatchOrder = (data) => request.post('/orders/dispatch', data)
export const acceptOrder = (id) => request.post(`/orders/${id}/accept`)
export const registerSafety = (id, data) => request.post(`/orders/${id}/safety`, data)
export const submitOrder = (id, data) => request.post(`/orders/${id}/submit`, data)
export const cancelOrder = (id, data) => request.post(`/orders/${id}/cancel`, data)
export const urgeOrder = (id) => request.post(`/orders/${id}/urge`)

// 巡检
export const pageRoutes = (params) => request.get('/patrol/route/page', { params })
export const generateTasks = (data) => request.post('/patrol/task/generate', data)
export const pageTasks = (params) => request.get('/patrol/task/page', { params })
export const getTask = (id) => request.get(`/patrol/task/${id}`)
export const submitRecord = (id, data) => request.post(`/patrol/task/${id}/record`, data)
export const finishTask = (id) => request.post(`/patrol/task/${id}/finish`)

// 验收
export const pageAcceptances = (params) => request.get('/acceptances/page', { params })
export const submitAcceptance = (orderId, data) => request.post(`/acceptances/${orderId}/submit`, data)

// 备件
export const pageParts = (params) => request.get('/parts/page', { params })
export const savePart = (data) => request.post('/parts', data)
export const stockIn = (id, data) => request.post(`/parts/${id}/in`, data)
export const applyPart = (data) => request.post('/parts/apply', data)
export const partFlows = (partId) => request.get('/parts/flows', { params: { partId } })

// 风险（创新点）
export const getActiveRule = () => request.get('/risk/rule/active')
export const listRules = () => request.get('/risk/rules')
export const publishRule = (data) => request.post('/risk/rules', data)
export const evaluate = (issueId) => request.post(`/risk/evaluate/${issueId}`)
export const pageScores = (params) => request.get('/risk/score/page', { params })
export const getScore = (id) => request.get(`/risk/score/${id}`)
export const adjustScore = (id, data) => request.post(`/risk/score/${id}/adjust`, data)
export const riskStats = () => request.get('/risk/stats')

// 系统
export const pageUsers = (params) => request.get('/sys/user/page', { params })
export const saveUser = (data) => request.post('/sys/user', data)
export const resetPassword = (id) => request.post(`/sys/user/${id}/reset`)
export const listRoles = () => request.get('/sys/roles')
export const listDepts = () => request.get('/sys/depts')
export const menuTree = (forAssign) => request.get('/sys/menus', { params: { forAssign } })
export const roleMenus = (roleId) => request.get(`/sys/role/${roleId}/menus`)
export const assignMenus = (roleId, menuIds) => request.post(`/sys/role/${roleId}/menus`, { menuIds })
export const listDicts = (type) => request.get('/sys/dicts', { params: { type } })
export const saveDict = (data) => request.post('/sys/dict', data)
export const pageMessages = (params) => request.get('/sys/message/page', { params })
export const unreadCount = () => request.get('/sys/message/unread')
export const readMessage = (id) => request.post(`/sys/message/${id}/read`)
export const readAllMessages = () => request.post('/sys/message/read-all')
