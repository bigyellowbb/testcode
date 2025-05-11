import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import router from './router'  // 确保导入路由
import { createPinia } from 'pinia'


const app = createApp(App)
const pinia = createPinia()

app.use(router)  // 挂载路由
app.use(pinia)

app.mount('#app')

axios.defaults.baseURL = 'http://localhost:8080' // 后端地址
axios.defaults.headers.post['Content-Type'] = 'application/json'

console.log('环境变量:', import.meta.env)
