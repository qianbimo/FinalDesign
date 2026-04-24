import request from '@/utils/request'

export function uploadCtFileApi(studyId, files) {
  const normalized = Array.isArray(files) ? files : [files]
  const formData = new FormData()
  for (const file of normalized) {
    if (file) {
      formData.append('files', file)
    }
  }

  return request({
    url: `/api/upload/ct?studyId=${studyId}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
