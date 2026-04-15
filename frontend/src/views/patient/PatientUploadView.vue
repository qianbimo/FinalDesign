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
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
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
    ElMessage.warning('医生档案编号 不能为空')
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
    ElMessage.success(`检查记录创建成功，编号：${data.studyId}`)
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
    ElMessage.warning('请先选择检查记录')
    return
  }
  if (!file.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  const name = file.value.name.toLowerCase()
  if (!(name.endsWith('.dcm') || name.endsWith('.nii') || name.endsWith('.nii.gz'))) {
    ElMessage.error('仅支持 .dcm、.nii、.nii.gz 格式')
    return
  }

  loading.value = true
  try {
    uploadResult.value = await uploadCtFileApi(selectedStudyId.value, file.value)
    ElMessage.success('上传成功')
    await loadStudies()
  } finally {
    loading.value = false
  }
}

onMounted(loadStudies)
</script>

<template>
  <el-card style="margin-bottom: 16px">
    <template #header>创建检查记录</template>
    <el-form label-width="160px">
      <el-form-item label="医生档案编号">
        <el-input-number v-model="createForm.doctorId" :min="1" />
      </el-form-item>
      <el-form-item label="检查日期">
        <el-date-picker v-model="createForm.studyDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="检查描述">
        <el-input v-model="createForm.studyDesc" />
      </el-form-item>
      <el-form-item label="设备信息">
        <el-input v-model="createForm.deviceInfo" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="createLoading" @click="createStudy">创建记录</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-card>
    <template #header>上传影像文件</template>

    <el-form label-width="160px">
      <el-form-item label="选择检查记录">
        <el-select v-model="selectedStudyId" placeholder="请选择检查记录" style="width: 360px">
          <el-option
            v-for="s in studies"
            :key="s.id"
            :label="`${s.studyNo} (${studyStatusText(s.status)})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="影像文件">
        <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange">
          <template #trigger>
            <el-button>选择文件</el-button>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submitUpload">开始上传</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="uploadResult" type="success" :closable="false" show-icon>
      <template #title>
        上传成功：文件编号={{ uploadResult.fileId }}，类型={{ uploadResult.fileType }}
      </template>
    </el-alert>
  </el-card>
</template>
