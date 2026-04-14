import { defineStore } from 'pinia'

const TOKEN_KEY = 'lung_token'
const ROLE_KEY = 'lung_role'
const USER_ID_KEY = 'lung_user_id'
const REAL_NAME_KEY = 'lung_real_name'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: localStorage.getItem(ROLE_KEY) || '',
    userId: localStorage.getItem(USER_ID_KEY) || '',
    realName: localStorage.getItem(REAL_NAME_KEY) || ''
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setSession(data) {
      this.token = data.token
      this.role = data.role
      this.userId = String(data.userId)
      this.realName = data.realName || ''

      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(ROLE_KEY, this.role)
      localStorage.setItem(USER_ID_KEY, this.userId)
      localStorage.setItem(REAL_NAME_KEY, this.realName)
    },
    clearSession() {
      this.token = ''
      this.role = ''
      this.userId = ''
      this.realName = ''

      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ROLE_KEY)
      localStorage.removeItem(USER_ID_KEY)
      localStorage.removeItem(REAL_NAME_KEY)
    }
  }
})