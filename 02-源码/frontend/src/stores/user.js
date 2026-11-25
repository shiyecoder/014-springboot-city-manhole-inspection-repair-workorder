import { defineStore } from 'pinia'
import { getMe, login as loginApi } from '@/api'

// 登录态：token + 用户信息 + 后端下发的菜单树
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mh_token') || '',
    user: JSON.parse(localStorage.getItem('mh_user') || 'null'),
    menus: []
  }),
  getters: {
    isLogin: (s) => !!s.token,
    roleCode: (s) => s.user?.roleCode || '',
    isPortal: (s) => ['INSPECTOR', 'REPAIRER'].includes(s.user?.roleCode)
  },
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.user = res.data.user
      this.menus = res.data.menus
      localStorage.setItem('mh_token', this.token)
      localStorage.setItem('mh_user', JSON.stringify(this.user))
      return res.data.user
    },
    async loadMe() {
      const res = await getMe()
      this.user = res.data.user
      this.menus = res.data.menus
      localStorage.setItem('mh_user', JSON.stringify(this.user))
      return res.data
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('mh_token')
      localStorage.removeItem('mh_user')
    }
  }
})
