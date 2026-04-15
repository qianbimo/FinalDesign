import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/',
  timeout: 20000
})

const errorMessageMap = {
  'Request failed': '请求失败',
  'Network error': '网络异常，请稍后重试',
  'Access denied': '无权限访问',
  'Username already exists': '用户名已存在',
  'Username or password is incorrect': '用户名或密码错误',
  'Only PATIENT can register via public signup': '公开注册仅允许患者角色',
  'patientId is required': '缺少 patientId 参数'
}

function toReadableMessage(message, fallback) {
  if (!message) return fallback
  return errorMessageMap[message] || message
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('lung_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    const message = toReadableMessage(res.message, '请求失败')
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error?.response?.status

    if (status === 401) {
      localStorage.removeItem('lung_token')
      localStorage.removeItem('lung_role')
      localStorage.removeItem('lung_user_id')
      localStorage.removeItem('lung_real_name')
      if (window.location.pathname !== '/login') {
        ElMessage.warning('登录已失效，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.error('无权限访问该资源')
      return Promise.reject(error)
    }

    const message = toReadableMessage(
      error?.response?.data?.message || error.message,
      '网络异常，请稍后重试'
    )
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
