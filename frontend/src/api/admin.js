import request from '@/utils/request'

export function getAdminUsersApi(params) {
  return request({ url: '/api/admin/users', method: 'get', params })
}

export function createAdminUserApi(payload) {
  return request({ url: '/api/admin/users', method: 'post', data: payload })
}

export function updateAdminUserStatusApi(id, status) {
  return request({
    url: `/api/admin/users/${id}/status`,
    method: 'put',
    data: { status: String(status) }
  })
}

export function resetAdminUserPasswordApi(id, newPassword) {
  return request({
    url: `/api/admin/users/${id}/reset-password`,
    method: 'put',
    data: { newPassword }
  })
}

export function deleteAdminUserApi(id) {
  return request({
    url: `/api/admin/users/${id}`,
    method: 'delete'
  })
}

export function getAdminDashboardApi() {
  return request({ url: '/api/admin/dashboard', method: 'get' })
}

export function getAdminReportsApi(params) {
  return request({ url: '/api/admin/reports', method: 'get', params })
}
