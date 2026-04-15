import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/',
  timeout: 20000
})

const errorMessageMap = {
  'Request failed': '请求失败',
  'Network error': '网络异常，请稍后重试',
  'Network Error': '网络异常，请稍后重试',
  'Access denied': '无权限访问',
  'Username already exists': '用户名已存在',
  'Username or password is incorrect': '用户名或密码错误',
  'Only PATIENT can register via public signup': '公开注册仅允许患者角色',
  'patientId is required': '缺少患者编号参数',
  'doctorId is required': '医生编号不能为空',
  'appointmentTime is required': '预约时间不能为空',
  'Study not found': '检查记录不存在',
  'Patient profile not found': '患者档案不存在',
  'Doctor profile not found': '医生档案不存在',
  'AI task not found': '智能分析任务不存在',
  'No AI task found for this study': '该检查暂无智能分析任务',
  'Please upload CT file first': '请先上传影像文件',
  'File size exceeds limit': '文件大小超出限制',
  'Report not found': '报告不存在',
  'Registration record not found': '挂号记录不存在'
}

function toReadableMessage(message, fallback) {
  if (!message) return fallback
  if (errorMessageMap[message]) return errorMessageMap[message]
  if (/[A-Za-z]/.test(message)) return fallback
  return message
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
