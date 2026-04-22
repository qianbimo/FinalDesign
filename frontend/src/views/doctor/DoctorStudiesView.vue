<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDoctorStudiesApi } from '@/api/doctor'
import { startAiTaskApi } from '@/api/aiTask'

const router = useRouter()
const loading = ref(false)
const aiLoadingId = ref(null)
const tableData = ref([])
const pager = reactive({ current: 1, size: 10, total: 0 })

const studyStatusMap = {
  WAIT_UPLOAD: '待上传CT',
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
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

async function loadData() {
  loading.value = true
  try {
    const data = await getDoctorStudiesApi({ current: pager.current, size: pager.size })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function toDetail(row) {
  router.push(`/app/doctor/studies/${row.patientId}/${row.id}`)
}

async function startAi(row) {
  if (row.status === 'WAIT_UPLOAD') {
    ElMessage.warning('该检查尚未上传 CT 文件，暂时无法启动智能分析')
    return
  }

  aiLoadingId.value = row.id
  try {
    const data = await startAiTaskApi(row.id)
    ElMessage.success(`智能分析任务已启动，任务编号：${data.taskId}`)
    await loadData()
  } finally {
    aiLoadingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="page-hero hero-doctor">
      <span class="page-hero__eyebrow">报告中心</span>
      <h1 class="page-hero__title">集中查看病例状态，并进入报告与分析详情</h1>
      <p class="page-hero__desc">
        这里整合了医生当前负责的检查记录。你可以查看患者、检查编号、状态，并继续启动智能分析或进入详情页处理报告。
      </p>
    </section>

    <el-card class="fd-panel-card">
      <template #header>
        <div class="section-head" style="margin-bottom: 0">
          <div>
            <h2 class="section-title">报告中心列表</h2>
            <p class="section-subtitle">病例列表已并入报告中心，后续报告编辑和审核都从详情页进入。</p>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="patientName" label="患者姓名" width="140" />
        <el-table-column prop="patientId" label="患者编号" width="120" />
        <el-table-column prop="studyNo" label="检查编号" min-width="180" />
        <el-table-column prop="studyDate" label="检查日期" width="130" />
        <el-table-column label="当前状态" width="140">
          <template #default="scope">
            <el-tag :type="studyStatusTagType(scope.row.status)">
              {{ studyStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240">
          <template #default="scope">
            <el-button link type="primary" @click="toDetail(scope.row)">查看详情</el-button>
            <el-button link type="success" :loading="aiLoadingId === scope.row.id" @click="startAi(scope.row)">
              启动智能分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px"
        background
        layout="total, prev, pager, next"
        :total="pager.total"
        :current-page="pager.current"
        :page-size="pager.size"
        @current-change="(p) => { pager.current = p; loadData() }"
      />
    </el-card>
  </div>
</template>
