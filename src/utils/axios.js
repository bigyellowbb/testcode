// src/utils/axios.js
import axios from 'axios'

// ███ 1. 创建唯一实例（删除冗余实例）
const service = axios.create({
  baseURL: import.meta.env.VITE_BASE_API || 'http://localhost:8080', // 优先使用环境变量
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  // ███ 2. 添加跨域凭证配置（关键修复）
  withCredentials: true 
})

// ███ 3. 统一请求拦截器（删除重复配置）
service.interceptors.request.use(config => {
  // ███ 4. 使用标准 token 命名（保持前后端一致）
  const token = localStorage.getItem('jwtToken') || localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ███ 5. 增强响应拦截器
service.interceptors.response.use(
  response => {
    // ███ 6. 自动刷新 Token 机制
    const newToken = response.headers['x-refresh-token']
    if (newToken) {
      localStorage.setItem('jwtToken', newToken)
    }
    
    // ███ 7. 统一响应数据结构处理
    return response.data?.content ?? response.data
  },
  error => {
    // ███ 8. 增强错误处理
    const status = error.response?.status
    const message = error.response?.data?.message 
      || error.message 
      || '未知错误'

    // ███ 9. 自动处理认证失败
    if (status === 401) {
      localStorage.removeItem('jwtToken')
      window.location.reload()
    }

    // ███ 10. 规范化错误对象
    const err = new Error(message)
    err.code = status || 'NETWORK_ERROR'
    
    console.error(`[API Error] ${err.code}: ${message}`)
    return Promise.reject(err)
  }
)

// ███ 11. 删除全局拦截器（避免重复）
// axios.interceptors.request.use(...) 

export default service