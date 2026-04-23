<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getDoctorPatientsApi } from '@/api/doctor'

const loading = ref(false)
const tableData = ref([])
const pager = reactive({ current: 1, size: 10, total: 0 })

const genderMap = {
  MALE: '男',
  FEMALE: '女'
}

function genderText(gender) {
  return genderMap[gender] || '未知'
}

function resolveAge(row) {
  if (Number.isInteger(row?.age)) {
    return `${row.age}`
  }

  if (!row?.birthday) {
    return '-'
  }

  const birth = new Date(`${row.birthday}T00:00:00`)
  if (Number.isNaN(birth.getTime())) {
    return '-'
  }

  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const monthDiff = now.getMonth() - birth.getMonth()
  const dayDiff = now.getDate() - birth.getDate()

  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age -= 1
  }

  return age < 0 ? '-' : `${age}`
}

async function loadData() {
  loading.value = true
  try {
    const data = await getDoctorPatientsApi({ current: pager.current, size: pager.size })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card class="doctor-panel-card">
    <template #header>患者列表</template>

    <el-table class="doctor-panel-table" :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="患者编号" width="120" />
      <el-table-column prop="patientName" label="患者姓名" width="140" />
      <el-table-column prop="medicalRecordNo" label="病历号" min-width="180" />
      <el-table-column label="性别" width="100">
        <template #default="scope">{{ genderText(scope.row.gender) }}</template>
      </el-table-column>
      <el-table-column label="年龄" width="90">
        <template #default="scope">{{ resolveAge(scope.row) }}</template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="220" />
    </el-table>

    <el-pagination
      class="doctor-panel-pagination"
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
.doctor-panel-card {
  border-radius: 16px;
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.doctor-panel-pagination {
  margin-top: 16px;
}

:deep(.doctor-panel-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.doctor-panel-card .el-card__body) {
  padding: 18px;
}

:deep(.doctor-panel-table) {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef2f7;
}

:deep(.doctor-panel-table th.el-table__cell) {
  background: #f8fafc;
}

:deep(.doctor-panel-pagination .btn-prev),
:deep(.doctor-panel-pagination .btn-next),
:deep(.doctor-panel-pagination .el-pager li) {
  border-radius: 10px;
}
</style>
