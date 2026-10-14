import { defineStore } from 'pinia'

/**
 * 当前登录用户状态
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('gss_token') || '',
    user: JSON.parse(localStorage.getItem('gss_user') || 'null')
  }),
  getters: {
    role: (state) => state.user?.role || '',
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('gss_token', token)
      localStorage.setItem('gss_user', JSON.stringify(user))
    },
    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem('gss_token')
      localStorage.removeItem('gss_user')
    }
  }
})
