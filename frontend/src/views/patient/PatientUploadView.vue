<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPatientStudiesApi } from '@/api/patient'
import { uploadCtFileApi } from '@/api/upload'

const studies = ref([])
const selectedStudyId = ref(null)
const file = ref(null)
const loading = ref(false)
const uploadResult = ref(null)
const studyStatusMap = {
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status
}

async function loadStudies() {
  const data = await getPatientStudiesApi({ current: 1, size: 100 })
  studies.value = data.records || []
  if (studies.value.length > 0) {
    selectedStudyId.value = studies.value[0].id
  }
}

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
}

async function submitUpload() {
  if (!selectedStudyId.value) {
    ElMessage.warning('请选择检查记录')
    return
  }
  if (!file.value) {
    ElMessage.warning('请选择要上传的文件')
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
  } finally {
    loading.value = false
  }
}

onMounted(loadStudies)
</script>

<template>
  <el-card>
    <template #header>CT 文件上传</template>

    <el-form label-width="140px">
      <el-form-item label="选择检查">
        <el-select v-model="selectedStudyId" placeholder="请选择检查记录" style="width: 320px">
          <el-option
            v-for="s in studies"
            :key="s.id"
            :label="`${s.studyNo} (${studyStatusText(s.status)})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="CT 文件">
        <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange">
          <template #trigger>
            <el-button>选择文件</el-button>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submitUpload">上传</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="uploadResult" type="success" :closable="false" show-icon>
      <template #title>
        上传成功：文件ID={{ uploadResult.fileId }}，类型={{ uploadResult.fileType }}
      </template>
    </el-alert>
  </el-card>
</template>
