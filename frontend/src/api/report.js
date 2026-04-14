import request from '@/utils/request'

export function getReportByStudyApi(studyId) {
  return request({ url: `/api/report/study/${studyId}`, method: 'get' })
}

export function updateReportApi(reportId, payload) {
  return request({ url: `/api/report/${reportId}`, method: 'put', data: payload })
}

export function auditReportApi(reportId) {
  return request({ url: `/api/report/${reportId}/audit`, method: 'post' })
}