import request from '@/utils/request'

export function uploadCtFileApi(studyId, file) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url: `/api/upload/ct?studyId=${studyId}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}