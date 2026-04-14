import request from '@/utils/request'

export function startAiTaskApi(studyId) {
  return request({ url: `/api/ai-task/start/${studyId}`, method: 'post' })
}

export function getAiTaskApi(taskId) {
  return request({ url: `/api/ai-task/${taskId}`, method: 'get' })
}

export function getAiResultByStudyApi(studyId) {
  return request({ url: `/api/ai-task/study/${studyId}/result`, method: 'get' })
}