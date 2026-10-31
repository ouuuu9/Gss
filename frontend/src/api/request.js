import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 统一请求实例：自动携带 JWT，统一解包后端 Result 结构
const request = axios.create({
  baseURL: '/',
  timeout: 15000
})

// 登录失效时清理本地凭证并回到登录页（响应拦截器中多处复用）
function redirectToLogin(message) {
  ElMessage.error(message)
  localStorage.removeItem('gss_token')
  localStorage.removeItem('gss_user')
  router.push('/login')
}

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

request.interceptors.response.use(
  (response) => {
    // 文件下载需要读取 headers 和二进制 data，返回完整 response，不解包
    if (response.config?.responseType === 'blob') {
      return response
    }

    const res = response.data
    // 不是统一 Result 结构（例如接口直接返回普通数据）时原样返回
    if (res == null || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 200) {
      return res.data
    }

    // 后端 JwtInterceptor 以 HTTP 200 + 业务码 401 表示未登录或登录过期
    if (res.code === 401) {
      redirectToLogin(res.msg || '登录已过期，请重新登录')
      return Promise.reject(new Error(res.msg || '未登录或登录已过期'))
    }
    // 已登录但没有权限：只提示，不强制退出登录
    if (res.code === 403) {
      ElMessage.error(res.msg || '无权限执行该操作')
      return Promise.reject(new Error(res.msg || '无权限'))
    }

    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  async (error) => {
    // 下载接口出错时后端返回的是 JSON Blob，必须先读出内容才能拿到错误信息
    if (error.config?.responseType === 'blob' && error.response?.data instanceof Blob) {
      try {
        const json = JSON.parse(await error.response.data.text())
        if (json.code === 401 || json.code === 403) {
          redirectToLogin(json.msg || '登录已过期，请重新登录')
        } else {
          ElMessage.error(json.msg || '导出失败')
        }
        return Promise.reject(new Error(json.msg || '导出失败'))
      } catch {
        // Blob 内容不是 JSON，交给下面的通用错误处理
      }
    }

    const status = error.response ? error.response.status : 0
    if (status === 401 || status === 403) {
      redirectToLogin(error.response?.data?.msg || '登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
