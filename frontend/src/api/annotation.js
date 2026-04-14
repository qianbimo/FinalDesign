import request from '@/utils/request'

export function getAnnotationByStudyApi(studyId) {
  return request({ url: `/api/annotation/study/${studyId}`, method: 'get' })
}