import request from '@/utils/request'

export function getPatientProfileApi() {
  return request({ url: '/api/patient/profile', method: 'get' })
}

export function updatePatientProfileApi(payload) {
  return request({ url: '/api/patient/profile', method: 'put', data: payload })
}

export function updatePatientPasswordApi(payload) {
  return request({ url: '/api/patient/password', method: 'put', data: payload })
}

export function getPatientStudiesApi(params) {
  return request({ url: '/api/patient/studies', method: 'get', params })
}

export function getPatientStudyDetailApi(studyId) {
  return request({ url: `/api/patient/studies/${studyId}`, method: 'get' })
}

export function getPatientAiResultApi(studyId) {
  return request({ url: `/api/patient/studies/${studyId}/ai-result`, method: 'get' })
}

export function getPatientReportApi(studyId) {
  return request({ url: `/api/patient/studies/${studyId}/report`, method: 'get' })
}
