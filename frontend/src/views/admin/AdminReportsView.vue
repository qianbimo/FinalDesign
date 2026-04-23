<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAdminReportsApi } from '@/api/admin'
import { getReportByIdApi } from '@/api/report'

const loading = ref(false)
const detailLoading = ref(false)
const tableData = ref([])
const pager = reactive({ current: 1, size: 10, total: 0 })
const filters = reactive({ status: '', keyword: '' })

const detailVisible = ref(false)
const detailReport = ref(null)

const reportStatusMap = {
  DRAFT: '草稿',
  REVIEWED: '已审核',
  FINAL: '最终版'
}

const generatedByMap = {
  SYSTEM: '系统',
  DOCTOR: '医生'
}

function reportStatusText(status) {
  return reportStatusMap[status] || '未知状态'
}

function generatedByText(value) {
  return generatedByMap[value] || '未知'
}

async function loadData() {
  loading.value = true
  try {
    const data = await getAdminReportsApi({
      current: pager.current,
      size: pager.size,
      status: filters.status || undefined,
      keyword: filters.keyword || undefined
    })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detailReport.value = null
  try {
    detailReport.value = await getReportByIdApi(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card class="admin-panel-card">
    <template #header>报告总览</template>

    <el-form inline class="admin-filter-form">
      <el-form-item label="报告状态">
        <el-select v-model="filters.status" clearable style="width: 160px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已审核" value="REVIEWED" />
          <el-option label="最终版" value="FINAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="filters.keyword" placeholder="标题/摘要" style="width: 260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="() => { pager.current = 1; loadData() }">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table class="admin-panel-table" :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="报告编号" width="110" />
      <el-table-column prop="studyNo" label="检查编号" min-width="180" />
      <el-table-column prop="patientName" label="患者姓名" min-width="120" />
      <el-table-column prop="doctorName" label="医生姓名" min-width="120" />
      <el-table-column prop="reportTitle" label="标题" min-width="220" />
      <el-table-column label="状态" width="110">
        <template #default="scope">{{ reportStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="生成方式" width="110">
        <template #default="scope">{{ generatedByText(scope.row.generatedBy) }}</template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="80" />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button size="small" class="action-btn" type="primary" @click="openDetail(scope.row)">
            查看
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="admin-panel-pagination"
      background
      layout="total, prev, pager, next"
      :total="pager.total"
      :current-page="pager.current"
      :page-size="pager.size"
      @current-change="(p) => { pager.current = p; loadData() }"
    />
  </el-card>

  <el-dialog v-model="detailVisible" class="admin-panel-dialog" title="报告详情" width="760px">
    <div v-loading="detailLoading">
      <el-empty v-if="!detailReport && !detailLoading" description="未查询到报告详情" />
      <el-descriptions v-else-if="detailReport" :column="2" border>
        <el-descriptions-item label="报告编号">{{ detailReport.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ detailReport.studyId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ reportStatusText(detailReport.status) }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detailReport.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="标题" :span="2">{{ detailReport.reportTitle }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ detailReport.reportSummary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <pre class="report-content-pre">{{ detailReport.reportContent }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </el-dialog>
</template>

<style scoped>
.admin-panel-card {
  --admin-card-radius: 16px;
  --admin-control-radius: 10px;
  border-radius: var(--admin-card-radius);
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.admin-filter-form {
  margin-bottom: 8px;
}

.admin-panel-pagination {
  margin-top: 16px;
}

.report-content-pre {
  white-space: pre-wrap;
  margin: 0;
}

:deep(.admin-panel-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.admin-panel-card .el-card__body) {
  padding: 18px;
}

:deep(.admin-filter-form .el-input__wrapper),
:deep(.admin-filter-form .el-select__wrapper) {
  border-radius: var(--admin-control-radius);
}

:deep(.admin-filter-form .el-button),
:deep(.admin-panel-table .action-btn) {
  border-radius: var(--admin-control-radius);
}

:deep(.admin-panel-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef2f7;
}

:deep(.admin-panel-table th.el-table__cell) {
  background: #f8fafc;
}

:deep(.admin-panel-pagination .btn-prev),
:deep(.admin-panel-pagination .btn-next),
:deep(.admin-panel-pagination .el-pager li) {
  border-radius: 10px;
}

:deep(.admin-panel-dialog .el-dialog) {
  border-radius: var(--admin-card-radius);
  overflow: hidden;
}

:deep(.admin-panel-dialog .el-dialog__header) {
  border-bottom: 1px solid #eef2f7;
  margin-right: 0;
  padding: 16px 20px;
}

:deep(.admin-panel-dialog .el-dialog__body) {
  padding: 16px 20px;
}

:deep(.admin-panel-dialog .el-descriptions) {
  border-radius: 10px;
  overflow: hidden;
}
</style>
