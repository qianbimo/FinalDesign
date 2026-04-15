<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDoctorStudiesApi } from '@/api/doctor'
import { auditReportApi, getReportByStudyApi, updateReportApi } from '@/api/report'

const loading = ref(false)
const saveLoading = ref(false)
const auditLoading = ref(false)
const studies = ref([])
const selectedStudyId = ref(null)
const report = ref(null)
const studyStatusMap = {
  UPLOADED: 'Uploaded',
  PREPROCESSING: 'Preprocessing',
  ANALYZING: 'Analyzing',
  FINISHED: 'Finished',
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

function reportStatusText(status) {
  return reportStatusMap[status] || status
}

async function loadStudies() {
  const data = await getDoctorStudiesApi({ current: 1, size: 200 })
  studies.value = data.records || []
}

async function loadReport() {
  if (!selectedStudyId.value) return
  loading.value = true
  try {
    report.value = await getReportByStudyApi(selectedStudyId.value)
  } catch (error) {
    report.value = null
  } finally {
    loading.value = false
  }
}

async function saveReport() {
  if (!report.value?.id) return
  saveLoading.value = true
  try {
    await updateReportApi(report.value.id, {
      reportTitle: report.value.reportTitle,
      reportContent: report.value.reportContent,
      reportSummary: report.value.reportSummary
    })
    ElMessage.success('Report updated')
    await loadReport()
  } finally {
    saveLoading.value = false
  }
}

async function auditReport() {
  if (!report.value?.id) return
  auditLoading.value = true
  try {
    await auditReportApi(report.value.id)
    ElMessage.success('Report reviewed')
    await loadReport()
  } finally {
    auditLoading.value = false
  }
}

onMounted(loadStudies)
</script>

<template>
  <el-card>
    <template #header>Report Center</template>

    <el-form inline>
      <el-form-item label="Study">
        <el-select v-model="selectedStudyId" placeholder="Select a study" style="width: 320px" @change="loadReport">
          <el-option
            v-for="s in studies"
            :key="s.id"
            :label="`${s.studyNo} (${studyStatusText(s.status)})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="loadReport">Load</el-button>
      </el-form-item>
    </el-form>

    <div v-if="report" v-loading="loading">
      <el-form label-width="140px">
        <el-form-item label="Title"><el-input v-model="report.reportTitle" /></el-form-item>
        <el-form-item label="Summary"><el-input v-model="report.reportSummary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="Content"><el-input v-model="report.reportContent" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="Status">
          <el-tag>{{ reportStatusText(report.status) }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saveLoading" @click="saveReport">Save</el-button>
          <el-button type="success" :loading="auditLoading" @click="auditReport">Review</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-empty v-else description="Select a study and load report" />
  </el-card>
</template>
