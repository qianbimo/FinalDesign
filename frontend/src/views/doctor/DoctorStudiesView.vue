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
  UPLOADED: 'Uploaded',
  PREPROCESSING: 'Preprocessing',
  ANALYZING: 'Analyzing',
  FINISHED: 'Finished',
  FAILED: 'Failed'
}

function studyStatusText(status) {
  return studyStatusMap[status] || status
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
    ElMessage.success(`AI task started: ${data.taskId}`)
    await loadData()
  } finally {
    aiLoadingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>Studies</template>
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="Study ID" width="120" />
      <el-table-column prop="studyNo" label="Study No" min-width="180" />
      <el-table-column prop="patientId" label="Patient ID" width="120" />
      <el-table-column prop="studyDate" label="Study Date" width="130" />
      <el-table-column label="Status" width="140">
        <template #default="scope">{{ studyStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="Action" min-width="240">
        <template #default="scope">
          <el-button link type="primary" @click="toDetail(scope.row)">Detail</el-button>
          <el-button link type="success" :loading="aiLoadingId === scope.row.id" @click="startAi(scope.row)">
            Start AI
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
