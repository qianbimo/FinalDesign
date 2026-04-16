import request from '@/utils/request'

export function getRegistrationDoctorsApi() {
  return request({ url: '/api/registration/doctors', method: 'get' })
}

export function createRegistrationApi(payload) {
  return request({ url: '/api/registration', method: 'post', data: payload })
}

export function getRegistrationListApi(params) {
  return request({ url: '/api/registration/list', method: 'get', params })
}

export function updateRegistrationStatusApi(id, status) {
  return request({
    url: `/api/registration/${id}/status`,
    method: 'put',
    data: { status }
  })
}
