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
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
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
  <el-card>
    <template #header>病例列表</template>
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="检查编号" width="120" />
      <el-table-column prop="studyNo" label="检查编号" min-width="180" />
      <el-table-column prop="patientId" label="患者编号" width="120" />
      <el-table-column prop="studyDate" label="检查日期" width="130" />
      <el-table-column label="状态" width="140">
        <template #default="scope">{{ studyStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="240">
        <template #default="scope">
          <el-button link type="primary" @click="toDetail(scope.row)">详情</el-button>
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
</template>
