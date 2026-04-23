<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPatientStudiesApi } from '@/api/patient'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const pager = reactive({ current: 1, size: 10, total: 0 })

const studyStatusMap = {
  WAIT_UPLOAD: '待上传CT',
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败',
  CANCELLED: '已取消挂号'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
}

async function loadData() {
  loading.value = true
  try {
    const data = await getPatientStudiesApi({ current: pager.current, size: pager.size, includeCancelled: true })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function toDetail(id) {
  if (!id || id < 0) return
  router.push(`/app/patient/studies/${id}`)
}

onMounted(loadData)
</script>

<template>
  <el-card class="patient-panel-card">
    <template #header>检查记录</template>
    <el-table class="patient-panel-table" :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="编号" width="110" />
      <el-table-column prop="studyNo" label="检查编号" min-width="180" />
      <el-table-column prop="studyDate" label="检查日期" width="130" />
      <el-table-column label="状态" width="140">
        <template #default="scope">{{ studyStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="studyDesc" label="检查描述" min-width="200" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button
            type="primary"
            link
            :disabled="scope.row.status === 'CANCELLED' || scope.row.id < 0"
            @click="toDetail(scope.row.id)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="patient-panel-pagination"
      background
      layout="total, prev, pager, next"
      :total="pager.total"
      :current-page="pager.current"
      :page-size="pager.size"
      @current-change="(p) => { pager.current = p; loadData() }"
    />
  </el-card>
</template>

<style scoped>
.patient-panel-card {
  border-radius: 16px;
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.patient-panel-pagination {
  margin-top: 16px;
}

:deep(.patient-panel-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.patient-panel-card .el-card__body) {
  padding: 18px;
}

:deep(.patient-panel-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef2f7;
}

:deep(.patient-panel-table th.el-table__cell) {
  background: #f8fafc;
}

:deep(.patient-panel-pagination .btn-prev),
:deep(.patient-panel-pagination .btn-next),
:deep(.patient-panel-pagination .el-pager li) {
  border-radius: 10px;
}
</style>
