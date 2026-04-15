<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPatientStudiesApi } from '@/api/patient'

const router = useRouter()
const loading = ref(false)
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
    const data = await getPatientStudiesApi({ current: pager.current, size: pager.size })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function toDetail(id) {
  router.push(`/app/patient/studies/${id}`)
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>检查记录</template>
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="编号" width="110" />
      <el-table-column prop="studyNo" label="检查编号" min-width="180" />
      <el-table-column prop="studyDate" label="检查日期" width="130" />
      <el-table-column label="状态" width="140">
        <template #default="scope">{{ studyStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="studyDesc" label="检查描述" min-width="200" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button type="primary" link @click="toDetail(scope.row.id)">详情</el-button>
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
