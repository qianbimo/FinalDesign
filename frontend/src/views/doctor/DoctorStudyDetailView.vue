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
    aiResult.value = await getAiResultByStudyApi(studyId)
  } finally {
    loading.value = false
  }
}

async function startAi() {
  aiLoading.value = true
  try {
    const data = await startAiTaskApi(studyId)
    ElMessage.success(`AI任务已启动：${data.taskId}`)
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
          <span>病例详情</span>
          <el-button type="success" :loading="aiLoading" @click="startAi">启动AI</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检查ID">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="患者ID">{{ study?.patientId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ studyStatusText(study?.status) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>CT 文件</template>
      <el-table :data="files">
        <el-table-column prop="id" label="文件ID" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column prop="fileSize" label="大小" width="140" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>AI 结果概览</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ aiResult?.task?.id }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ taskStatusText(aiResult?.task?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="结节数量">{{ aiResult?.nodules?.length || 0 }}</el-descriptions-item>
        <el-descriptions-item label="标注数量">{{ aiResult?.annotations?.length || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
