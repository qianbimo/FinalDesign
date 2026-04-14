import request from '@/utils/request'

export function getDoctorProfileApi() {
  return request({ url: '/api/doctor/profile', method: 'get' })
}

export function getDoctorPatientsApi(params) {
  return request({ url: '/api/doctor/patients', method: 'get', params })
}

export function getDoctorStudiesApi(params) {
  return request({ url: '/api/doctor/studies', method: 'get', params })
}

export function getDoctorPatientStudyDetailApi(patientId, studyId) {
  return request({ url: `/api/doctor/patient/${patientId}/studies/${studyId}`, method: 'get' })
}