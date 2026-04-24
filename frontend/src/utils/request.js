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
  'registrationId is required': '请先选择挂号单',
  'appointmentTime is required': '预约时间不能为空',
  'Study not found': '检查记录不存在',
  'Patient profile not found': '患者档案不存在',
  'Doctor profile not found': '医生档案不存在',
  'AI task not found': '智能分析任务不存在',
  'No AI task found for this study': '该检查暂无智能分析任务',
  'Please upload CT file first': '请先上传CT影像文件',
  'Registration is cancelled': '挂号单已取消，无法创建检查',
  'Registration is finished': '挂号单已完成，不能重复创建检查',
  'Study already created for this registration': '该挂号单已创建检查记录',
  'Study must be created from registration first': '请先基于挂号单创建检查记录',
  'File size exceeds limit': '文件大小超出限制',
  'No upload files found': '请先选择上传文件',
  'Only one file or one .mhd + .raw pair is allowed per study': '每个挂号单仅支持 1 个文件，或 1 组同名 .mhd + .raw',
  'A single .mhd or .raw file is not allowed, upload both together': '上传 .mhd 或 .raw 时，必须同时上传同名配对文件',
  'Only .dcm/.nii/.nii.gz or matched .mhd+.raw is supported': '仅支持 .dcm/.nii/.nii.gz，或同名 .mhd + .raw',
  'Two-file upload must be exactly one .mhd and one .raw': '双文件上传必须是 1 个 .mhd + 1 个 .raw',
  '.mhd and .raw must have the same base filename': '.mhd 与 .raw 文件名（不含扩展名）必须一致',
  'Unsupported file extension': '文件扩展名不受支持',
  'Unsupported CT file type': 'CT 文件类型不受支持',
  'Invalid file name': '文件名不合法',
  'Invalid filename extension': '文件扩展名不合法',
  'Illegal file path': '文件路径不合法',
  'Report not found': '报告不存在',
  'Registration record not found': '挂号记录不存在',
  'Cannot delete current user': '不能删除当前登录账号',
  'Cannot delete last admin': '不能删除最后一个管理员账号',
  'User has related records and cannot be deleted': '该用户已有业务关联数据，暂不允许删除',
  'Old password is incorrect': '原密码错误',
  'New password cannot be the same as old password': '新密码不能与原密码相同',
  'Old password cannot be empty': '原密码不能为空',
  'New password cannot be empty': '新密码不能为空'
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
