<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPatientStudiesApi } from '@/api/patient'
import { uploadCtFileApi } from '@/api/upload'
import { createStudyApi } from '@/api/study'

const studies = ref([])
const selectedStudyId = ref(null)
const file = ref(null)
const loading = ref(false)
const createLoading = ref(false)
const uploadResult = ref(null)
const createForm = reactive({
  doctorId: null,
  studyDate: '',
  studyDesc: '',
  deviceInfo: ''
})

const studyStatusMap = {
  UPLOADED: 'Uploaded',
  PREPROCESSING: 'Preprocessing',
  ANALYZING: 'Analyzing',
  FINISHED: 'Finished',
  FAILED: 'Failed'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status
}

async function loadStudies() {
  const data = await getPatientStudiesApi({ current: 1, size: 100 })
  studies.value = data.records || []
  if (!selectedStudyId.value && studies.value.length > 0) {
    selectedStudyId.value = studies.value[0].id
  }
}

async function createStudy() {
  if (!createForm.doctorId) {
    ElMessage.warning('Doctor profile ID is required')
    return
  }
  createLoading.value = true
  try {
    const data = await createStudyApi({
      doctorId: createForm.doctorId,
      studyDate: createForm.studyDate || undefined,
      studyDesc: createForm.studyDesc || undefined,
      deviceInfo: createForm.deviceInfo || undefined
    })
    selectedStudyId.value = data.studyId
    ElMessage.success(`Study created: ${data.studyId}`)
    await loadStudies()
  } finally {
    createLoading.value = false
  }
}

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
}

async function submitUpload() {
  if (!selectedStudyId.value) {
    ElMessage.warning('Please select a study')
    return
  }
  if (!file.value) {
    ElMessage.warning('Please select file')
    return
  }

  const name = file.value.name.toLowerCase()
  if (!(name.endsWith('.dcm') || name.endsWith('.nii') || name.endsWith('.nii.gz'))) {
    ElMessage.error('Only .dcm, .nii, .nii.gz are allowed')
    return
  }

  loading.value = true
  try {
    uploadResult.value = await uploadCtFileApi(selectedStudyId.value, file.value)
    ElMessage.success('Upload success')
    await loadStudies()
  } finally {
    loading.value = false
  }
}

onMounted(loadStudies)
</script>

<template>
  <el-card style="margin-bottom: 16px">
    <template #header>Create Study</template>
    <el-form label-width="160px">
      <el-form-item label="Doctor Profile ID">
        <el-input-number v-model="createForm.doctorId" :min="1" />
      </el-form-item>
      <el-form-item label="Study Date">
        <el-date-picker v-model="createForm.studyDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="Study Description">
        <el-input v-model="createForm.studyDesc" />
      </el-form-item>
      <el-form-item label="Device Info">
        <el-input v-model="createForm.deviceInfo" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="createLoading" @click="createStudy">Create Study</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card>
    <template #header>Upload CT File</template>

    <el-form label-width="160px">
      <el-form-item label="Select Study">
        <el-select v-model="selectedStudyId" placeholder="Please select a study" style="width: 360px">
          <el-option
            v-for="s in studies"
            :key="s.id"
            :label="`${s.studyNo} (${studyStatusText(s.status)})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="CT File">
        <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange">
          <template #trigger>
            <el-button>Select File</el-button>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submitUpload">Upload</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="uploadResult" type="success" :closable="false" show-icon>
      <template #title>
        Upload success: fileId={{ uploadResult.fileId }}, type={{ uploadResult.fileType }}
      </template>
    </el-alert>
  </el-card>
</template>
