import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  // 优先使用 localStorage 中的 token，确保登录后立即获取用户信息时能拿到最新 token
  const token = localStorage.getItem('token') || userStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // Attach current workspace ID for data isolation
  const workspaceId = localStorage.getItem('currentWorkspaceId')
  if (workspaceId) {
    config.headers['X-Workspace-Id'] = workspaceId
  }
  return config
})

request.interceptors.response.use(
  response => {
    // 后端返回格式: { code: 200, message: "Success", data: {...} }
    // 需要返回实际的 data 字段
    const res = response.data as any
    if (res.code === 200) {
      return res.data
    }
    // 如果 code 不是 200，抛出错误
    ElMessage.error(res.message || 'Request failed')
    return Promise.reject(new Error(res.message || 'Request failed'))
  },
  error => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      return Promise.reject(error)
    }
    // 优先展示后端返回的具体错误信息（含业务校验的中文提示）。
    // 仅当后端没有返回 message（如服务真正崩溃）时才退回通用文案。
    const backendMessage = error.response?.data?.message
    if (backendMessage) {
      ElMessage.error(backendMessage)
    } else if (error.response?.status !== 500) {
      ElMessage.error('请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
