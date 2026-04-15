<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPatientAiResultApi, getPatientReportApi, getPatientStudyDetailApi } from '@/api/patient'

const route = useRoute()
const studyId = route.params.studyId

const loading = ref(false)
const study = ref(null)
const aiTask = ref(null)
const report = ref(null)
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
const reportStatusMap = {
  DRAFT: 'Draft',
  REVIEWED: 'Reviewed',
  FINAL: 'Final'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status
}

function taskStatusText(status) {
  return taskStatusMap[status] || status
}

function reportStatusText(status) {
  return reportStatusMap[status] || status
}

async function loadData() {
  loading.value = true
  try {
    study.value = await getPatientStudyDetailApi(studyId)
    try {
      aiTask.value = await getPatientAiResultApi(studyId)
    } catch (error) {
      aiTask.value = null
    }
    try {
      report.value = await getPatientReportApi(studyId)
    } catch (error) {
      report.value = null
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>Study Detail</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="ID">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="Study No">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="Study Date">{{ study?.studyDate }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ studyStatusText(study?.status) }}</el-descriptions-item>
        <el-descriptions-item label="Device">{{ study?.deviceInfo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Description">{{ study?.studyDesc || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>AI Result</template>
      <el-empty v-if="!aiTask" description="No AI task yet" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="Task ID">{{ aiTask?.id }}</el-descriptions-item>
        <el-descriptions-item label="Task Status">{{ taskStatusText(aiTask?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="Model Version">{{ aiTask?.modelVersion }}</el-descriptions-item>
        <el-descriptions-item label="Finished At">{{ aiTask?.finishedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>Report</template>
      <el-empty v-if="!report" description="No report yet" />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="Title">{{ report?.reportTitle }}</el-descriptions-item>
        <el-descriptions-item label="Summary">{{ report?.reportSummary }}</el-descriptions-item>
        <el-descriptions-item label="Status">{{ reportStatusText(report?.status) }}</el-descriptions-item>
        <el-descriptions-item label="Content">
          <pre style="white-space: pre-wrap; margin: 0">{{ report?.reportContent }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
