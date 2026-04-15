<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPatientStudiesApi } from '@/api/patient'

const router = useRouter()
const loading = ref(false)
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
    <template #header>My Studies</template>
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="ID" width="110" />
      <el-table-column prop="studyNo" label="Study No" min-width="180" />
      <el-table-column prop="studyDate" label="Study Date" width="130" />
      <el-table-column label="Status" width="140">
        <template #default="scope">{{ studyStatusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="studyDesc" label="Description" min-width="200" />
      <el-table-column label="Action" width="120">
        <template #default="scope">
          <el-button type="primary" link @click="toDetail(scope.row.id)">Detail</el-button>
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
