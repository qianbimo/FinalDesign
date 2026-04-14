import request from '@/utils/request'

export function createStudyApi(payload) {
  return request({ url: '/api/study/create', method: 'post', data: payload })
}

export function getStudyListApi(params) {
  return request({ url: '/api/study/list', method: 'get', params })
}

export function getStudyDetailApi(id) {
  return request({ url: `/api/study/${id}`, method: 'get' })
}

export function getCtFilesByStudyApi(studyId) {
  return request({ url: `/api/ct-file/study/${studyId}`, method: 'get' })
}