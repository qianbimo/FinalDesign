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
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}
const taskStatusMap = {
  WAITING: '等待中',
  RUNNING: '运行中',
  SUCCESS: '成功',
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
    aiTask.value = await getPatientAiResultApi(studyId)
    report.value = await getPatientReportApi(studyId)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>检查详情</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编号">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="检查日期">{{ study?.studyDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ studyStatusText(study?.status) }}</el-descriptions-item>
        <el-descriptions-item label="设备信息">{{ study?.deviceInfo }}</el-descriptions-item>
        <el-descriptions-item label="检查描述">{{ study?.studyDesc }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>AI 结果</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ aiTask?.id }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ taskStatusText(aiTask?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="模型版本">{{ aiTask?.modelVersion }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ aiTask?.finishedAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>报告单</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ report?.reportTitle }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ report?.reportSummary }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ reportStatusText(report?.status) }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <pre style="white-space: pre-wrap; margin: 0">{{ report?.reportContent }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
