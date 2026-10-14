import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// Axios 实例：统一携带 JWT，统一处理业务错误
const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

// 请求拦截：自动附加 Authorization: Bearer <token>
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('gss_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：解包统一 Result
request.interceptors.response.use(
  (response) => {
    // Blob 响应（如文件下载）直接返回完整 response，便于读取 headers 和 data
    if (response.config?.responseType === 'blob') {
      return response
    }
    const res = response.data
    // 非 Result 结构（如直接返回的文件等）直接返回
    if (res == null || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 200) {
      return res.data
    }
    // 未登录 / 登录过期（后端 JwtInterceptor 以 HTTP 200 + 业务码 401 返回）
    if (res.code === 401) {
      ElMessage.error(res.msg || '登录已过期，请重新登录')
      localStorage.removeItem('gss_token')
      localStorage.removeItem('gss_user')
      router.push('/login')
      return Promise.reject(new Error(res.msg || '未登录或登录已过期'))
    }
    // 已登录但无权限：仅提示，不强制登出
    if (res.code === 403) {
      ElMessage.error(res.msg || '无权限执行该操作')
      return Promise.reject(new Error(res.msg || '无权限'))
    }
    // 其他业务错误
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  async (error) => {
    const status = error.response ? error.response.status : 0
    // Blob 请求的错误响应（后端返回 JSON 错误而非文件）：读取 Blob 内容解析错误信息
    if (error.config?.responseType === 'blob' && error.response?.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        const json = JSON.parse(text)
        if (json.code === 401 || json.code === 403) {
          ElMessage.error(json.msg || '登录已过期，请重新登录')
          localStorage.removeItem('gss_token')
          localStorage.removeItem('gss_user')
          router.push('/login')
        } else {
          ElMessage.error(json.msg || '导出失败')
        }
        return Promise.reject(new Error(json.msg || '导出失败'))
      } catch {
        // 非 JSON 响应，走通用错误处理
      }
    }
    if (status === 401 || status === 403) {
      ElMessage.error(error.response?.data?.msg || '登录已过期，请重新登录')
      localStorage.removeItem('gss_token')
      localStorage.removeItem('gss_user')
      router.push('/login')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
