<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRegistrationListApi } from '@/api/registration'
import { createStudyApi } from '@/api/study'
import { uploadCtFileApi } from '@/api/upload'
import { getDoctorPatientsApi, getDoctorStudiesApi } from '@/api/doctor'

const loading = ref(false)
const confirmLoading = ref(false)
const uploadLoading = ref(false)

const registrations = ref([])
const studies = ref([])
const patientNameMap = ref({})
const selectedRegistrationId = ref(null)
const selectedStudyId = ref(null)
const file = ref(null)
const uploadResult = ref(null)

const form = reactive({
  studyDesc: ''
})

const registrationStatusMap = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  CANCELLED: '已取消',
  FINISHED: '已完成'
}

const studyStatusMap = {
  WAIT_UPLOAD: '待上传CT',
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

const selectedRegistration = computed(() => {
  return registrations.value.find((item) => item.id === selectedRegistrationId.value) || null
})

const selectedStudy = computed(() => {
  return studies.value.find((item) => item.id === selectedStudyId.value) || null
})

const appointmentTimeText = computed(() => {
  return selectedRegistration.value?.appointmentTime || ''
})

function registrationStatusText(status) {
  return registrationStatusMap[status] || status || '未知状态'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status || '未知状态'
}

function registrationStatusTagType(status) {
  switch (status) {
    case 'PENDING':
      return 'warning'
    case 'CONFIRMED':
      return 'success'
    case 'CANCELLED':
      return 'danger'
    case 'FINISHED':
      return 'info'
    default:
      return 'info'
  }
}

function studyStatusTagType(status) {
  switch (status) {
    case 'WAIT_UPLOAD':
      return 'info'
    case 'UPLOADED':
      return 'warning'
    case 'PREPROCESSING':
    case 'ANALYZING':
      return 'warning'
    case 'FINISHED':
      return 'success'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

function patientName(patientId) {
  return patientNameMap.value[patientId] || `患者#${patientId}`
}

function registrationOptionLabel(item) {
  return `${patientName(item.patientId)} | 预约时间：${item.appointmentTime || '-'} | 状态：${registrationStatusText(item.status)}`
}

function studyOptionLabel(item) {
  const name = item.patientName || patientName(item.patientId)
  return `${name} | 检查编号：${item.studyNo} | 状态：${studyStatusText(item.status)}`
}

function syncDescriptionFromRegistration() {
  form.studyDesc = selectedRegistration.value?.description || ''
}

function resetUploadState() {
  file.value = null
  uploadResult.value = null
}

async function loadPatients() {
  const data = await getDoctorPatientsApi({ current: 1, size: 500 })
  const map = {}
  for (const item of data.records || []) {
    map[item.id] = item.patientName || `患者#${item.id}`
  }
  patientNameMap.value = map
}

async function loadRegistrations() {
  const data = await getRegistrationListApi({ current: 1, size: 200 })
  registrations.value = (data.records || []).filter((item) => item.status !== 'CANCELLED')

  if (selectedRegistrationId.value && !registrations.value.some((item) => item.id === selectedRegistrationId.value)) {
    selectedRegistrationId.value = null
  }

  if (!selectedRegistrationId.value && registrations.value.length > 0) {
    selectedRegistrationId.value = registrations.value[0].id
    syncDescriptionFromRegistration()
  }
}

async function loadStudies() {
  const data = await getDoctorStudiesApi({ current: 1, size: 200 })
  studies.value = data.records || []

  if (selectedStudyId.value && !studies.value.some((item) => item.id === selectedStudyId.value)) {
    selectedStudyId.value = null
  }

  if (!selectedStudyId.value && studies.value.length > 0) {
    selectedStudyId.value = studies.value[0].id
  }
}

async function refreshAll() {
  loading.value = true
  try {
    await Promise.all([loadPatients(), loadRegistrations(), loadStudies()])
  } finally {
    loading.value = false
  }
}

function toStudyDate(appointmentTime) {
  if (!appointmentTime) return undefined
  const text = String(appointmentTime)
  const idx = text.indexOf('T')
  if (idx > 0) return text.substring(0, idx)
  if (text.length >= 10) return text.substring(0, 10)
  return undefined
}

async function confirmRegistration() {
  const registration = selectedRegistration.value
  if (!registration) {
    ElMessage.warning('请先选择挂号单')
    return
  }

  if (registration.status === 'CANCELLED') {
    ElMessage.error('已取消的挂号单不能创建检查')
    return
  }

  if (registration.status === 'FINISHED') {
    ElMessage.error('已完成的挂号单不能重复创建检查')
    return
  }

  if (registration.status !== 'PENDING') {
    ElMessage.info('该挂号单已确认，请直接上传CT或重新选择挂号单')
    return
  }

  confirmLoading.value = true
  try {
    const data = await createStudyApi({
      registrationId: registration.id,
      studyDate: toStudyDate(registration.appointmentTime),
      studyDesc: form.studyDesc || undefined
    })
    selectedStudyId.value = data.studyId
    ElMessage.success(`挂号单确认成功，已创建检查记录：${data.studyId}`)
    await refreshAll()
  } finally {
    confirmLoading.value = false
  }
}

function onRegistrationChange() {
  syncDescriptionFromRegistration()
  resetUploadState()
}

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
}

async function submitUpload() {
  if (!selectedStudyId.value) {
    ElMessage.warning('请先选择检查记录')
    return
  }

  if (!file.value) {
    ElMessage.warning('请先选择CT文件')
    return
  }

  const name = file.value.name.toLowerCase()
  if (!(name.endsWith('.dcm') || name.endsWith('.nii') || name.endsWith('.nii.gz'))) {
    ElMessage.error('仅支持 .dcm、.nii、.nii.gz 格式')
    return
  }

  uploadLoading.value = true
  try {
    uploadResult.value = await uploadCtFileApi(selectedStudyId.value, file.value)
    ElMessage.success('CT上传成功')
    await loadStudies()
  } finally {
    uploadLoading.value = false
  }
}

onMounted(refreshAll)
</script>

<template>
  <div v-loading="loading" class="doctor-upload-page">
    <el-card class="doctor-panel-card doctor-panel-card--spaced">
      <template #header>挂号单处理</template>

      <el-form label-width="160px" class="doctor-form">
        <el-form-item label="选择挂号单">
          <el-select
            v-model="selectedRegistrationId"
            style="width: 680px"
            placeholder="请选择挂号单"
            @change="onRegistrationChange"
          >
            <el-option
              v-for="item in registrations"
              :key="item.id"
              :label="registrationOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="当前状态">
          <el-tag :type="registrationStatusTagType(selectedRegistration?.status)">
            {{ registrationStatusText(selectedRegistration?.status) }}
          </el-tag>
        </el-form-item>

        <el-form-item label="预约时间（患者选择）">
          <el-input :model-value="appointmentTimeText" disabled style="width: 320px" />
        </el-form-item>

        <el-form-item label="检查描述（患者自述）">
          <el-input
            v-model="form.studyDesc"
            type="textarea"
            :rows="3"
            placeholder="默认已带入患者挂号自述，医生可按实际情况修改"
            style="width: 680px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="confirmLoading" @click="confirmRegistration">确认挂号单</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="doctor-panel-card">
      <template #header>医生上传CT影像</template>

      <el-alert
        title="流程说明：患者提交挂号后，医生确认挂号单并自动生成检查记录，再上传CT并启动智能分析。"
        type="info"
        :closable="false"
        show-icon
        class="doctor-flow-alert"
      />

      <el-form label-width="160px" class="doctor-form">
        <el-form-item label="选择检查记录">
          <el-select v-model="selectedStudyId" style="width: 680px" placeholder="请选择检查记录">
            <el-option
              v-for="item in studies"
              :key="item.id"
              :label="studyOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="当前状态">
          <el-tag :type="studyStatusTagType(selectedStudy?.status)">
            {{ studyStatusText(selectedStudy?.status) }}
          </el-tag>
        </el-form-item>

        <el-form-item label="CT文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange">
            <template #trigger>
              <el-button>选择文件</el-button>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="uploadLoading" @click="submitUpload">开始上传</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="uploadResult" type="success" :closable="false" show-icon>
        <template #title>
          上传成功：文件编号 {{ uploadResult.fileId }}，类型 {{ uploadResult.fileType }}
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<style scoped>
.doctor-upload-page {
  --doctor-card-radius: 16px;
  --doctor-control-radius: 10px;
}

.doctor-panel-card {
  border-radius: var(--doctor-card-radius);
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.doctor-panel-card--spaced {
  margin-bottom: 16px;
}

.doctor-flow-alert {
  margin-bottom: 16px;
}

:deep(.doctor-panel-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.doctor-panel-card .el-card__body) {
  padding: 18px;
}

:deep(.doctor-form .el-input__wrapper),
:deep(.doctor-form .el-textarea__inner),
:deep(.doctor-form .el-select__wrapper) {
  border-radius: var(--doctor-control-radius);
}

:deep(.doctor-form .el-button),
:deep(.doctor-upload-page .el-upload-list__item),
:deep(.doctor-upload-page .el-alert) {
  border-radius: var(--doctor-control-radius);
}

:deep(.doctor-upload-page .el-tag) {
  border-radius: 8px;
}

@media (max-width: 768px) {
  :deep(.doctor-form .el-form-item__content > .el-select),
  :deep(.doctor-form .el-form-item__content > .el-input),
  :deep(.doctor-form .el-form-item__content > .el-textarea) {
    width: 100% !important;
  }
}
</style>
