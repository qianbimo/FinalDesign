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
  <el-card>
    <template #header>患者列表</template>
    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="患者编号" width="120" />
      <el-table-column prop="medicalRecordNo" label="病历号" min-width="180" />
      <el-table-column label="性别" width="120">
        <template #default="scope">{{ genderText(scope.row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="100" />
      <el-table-column prop="address" label="地址" min-width="220" />
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
