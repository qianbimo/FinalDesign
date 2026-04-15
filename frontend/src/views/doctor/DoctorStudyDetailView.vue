<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDoctorPatientStudyDetailApi } from '@/api/doctor'
import { getCtFilesByStudyApi } from '@/api/study'
import { getAiResultByStudyApi, startAiTaskApi } from '@/api/aiTask'

const route = useRoute()
const studyId = route.params.studyId
const patientId = route.params.patientId

const loading = ref(false)
const aiLoading = ref(false)
const study = ref(null)
const files = ref([])
const aiResult = ref(null)
const studyStatusMap = {
  UPLOADED: 'Uploaded',
  PREPROCESSING: 'Preprocessing',
  ANALYZING: 'Analyzing',
  FINISHED: 'Finished',
  FAILED: 'Failed'
}
const taskStatusMap = {
  WAITING: 'Waiting',
  RUNNING: 'Running',
  SUCCESS: 'Success',
  FAILED: 'Failed'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status
}

function taskStatusText(status) {
  return taskStatusMap[status] || status
}

async function loadData() {
  loading.value = true
  try {
    study.value = await getDoctorPatientStudyDetailApi(patientId, studyId)
    files.value = await getCtFilesByStudyApi(studyId)
    try {
      aiResult.value = await getAiResultByStudyApi(studyId)
    } catch (error) {
      aiResult.value = null
    }
  } finally {
    loading.value = false
  }
}

async function startAi() {
  aiLoading.value = true
  try {
    const data = await startAiTaskApi(studyId)
    ElMessage.success(`AI task started: ${data.taskId}`)
    await loadData()
  } finally {
    aiLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>Case Detail</span>
          <el-button type="success" :loading="aiLoading" @click="startAi">Start AI</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Study ID">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="Study No">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="Patient ID">{{ study?.patientId }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ studyStatusText(study?.status) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>CT Files</template>
      <el-table :data="files">
        <el-table-column prop="id" label="File ID" width="120" />
        <el-table-column prop="fileName" label="File Name" min-width="200" />
        <el-table-column prop="fileType" label="Type" width="120" />
        <el-table-column prop="fileSize" label="Size" width="140" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>AI Summary</template>
      <el-empty v-if="!aiResult" description="No AI result yet" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="Task ID">{{ aiResult?.task?.id }}</el-descriptions-item>
        <el-descriptions-item label="Task Status">{{ taskStatusText(aiResult?.task?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="Nodules">{{ aiResult?.nodules?.length || 0 }}</el-descriptions-item>
        <el-descriptions-item label="Annotations">{{ aiResult?.annotations?.length || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
