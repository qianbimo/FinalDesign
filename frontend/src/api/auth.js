import request from '@/utils/request'

export function loginApi(payload) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data: payload
  })
}

export function registerApi(payload) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data: payload
  })
}