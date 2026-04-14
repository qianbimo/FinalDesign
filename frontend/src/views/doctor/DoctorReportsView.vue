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
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}
const reportStatusMap = {
  DRAFT: '草稿',
  REVIEWED: '已审核',
  FINAL: '最终版'
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
    ElMessage.success('报告已更新')
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
    ElMessage.success('报告已审核')
    await loadReport()
  } finally {
    auditLoading.value = false
  }
}

onMounted(loadStudies)
</script>

<template>
  <el-card>
    <template #header>报告中心</template>

    <el-form inline>
      <el-form-item label="检查记录">
        <el-select v-model="selectedStudyId" placeholder="请选择检查记录" style="width: 320px" @change="loadReport">
          <el-option
            v-for="s in studies"
            :key="s.id"
            :label="`${s.studyNo} (${studyStatusText(s.status)})`"
            :value="s.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="loadReport">加载</el-button>
      </el-form-item>
    </el-form>

    <div v-if="report" v-loading="loading">
      <el-form label-width="140px">
        <el-form-item label="标题"><el-input v-model="report.reportTitle" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="report.reportSummary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="report.reportContent" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="状态">
          <el-tag>{{ reportStatusText(report.status) }}</el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saveLoading" @click="saveReport">保存</el-button>
          <el-button type="success" :loading="auditLoading" @click="auditReport">审核</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-empty v-else description="请选择检查记录后加载报告" />
  </el-card>
</template>
