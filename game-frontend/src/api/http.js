import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { getAccessToken, getRefreshToken, setTokens, clearLogin } from '../utils/auth'

// 统一请求实例: baseURL 走 Vite 环境变量，便于部署时切换后端地址
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 60000
})

// 刷新登录态用的裸实例: 不带拦截器，避免 401 处理递归
const bareHttp = axios.create({
  baseURL: http.defaults.baseURL,
  timeout: 10000
})

// 刷新并发锁: 多个请求同时 401 时只发一次 refresh，其余复用同一结果
let refreshPromise = null

function refreshLogin() {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getRefreshToken()
      if (!refreshToken) throw new Error('无 refreshToken')
      const { data: result } = await bareHttp.post('/auth/refresh', { refreshToken })
      if (result.code !== 0) throw new Error(result.message)
      setTokens({ accessToken: result.data.accessToken, refreshToken: result.data.refreshToken })
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

// 请求拦截器: 统一携带 access token
http.interceptors.request.use(config => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器: 解包统一 Result 返回 data；401 时自动刷新并重放原请求
http.interceptors.response.use(
  response => {
    const result = response.data
    if (result.code === 0) return result.data
    if (result.code === 40100) return handleUnauthorized(response.config)
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(new Error(result.message))
  },
  error => {
    if (error.response && error.response.status === 401) {
      return handleUnauthorized(error.config)
    }
    ElMessage.error('网络错误，请检查连接后重试')
    return Promise.reject(error)
  }
)

async function handleUnauthorized(config) {
  try {
    await refreshLogin()
    config.headers.Authorization = `Bearer ${getAccessToken()}`
    return http.request(config)
  } catch (e) {
    clearLogin()
    ElMessage.warning('登录已过期，请重新登录')
    if (router.currentRoute.value.name !== 'login') {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(e)
  }
}

export default http
