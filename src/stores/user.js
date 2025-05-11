// stores/user.js
import { defineStore } from 'pinia' // 确保这里正确导入

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: null
  }),
  actions: {
    logout() {
      this.userInfo = null
      this.token = null
    }
  }
})