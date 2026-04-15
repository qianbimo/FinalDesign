<script setup>
import { onMounted, ref } from 'vue'
import { getAdminDashboardApi } from '@/api/admin'

const loading = ref(false)
const dashboard = ref({})

async function loadData() {
  loading.value = true
  try {
    dashboard.value = await getAdminDashboardApi()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Total Users: {{ dashboard.totalUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Patients: {{ dashboard.patientUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Doctors: {{ dashboard.doctorUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Admins: {{ dashboard.adminUsers || 0 }}</el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Active Users: {{ dashboard.activeUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Total Studies: {{ dashboard.totalStudies || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Total AI Tasks: {{ dashboard.totalAiTasks || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>Total Reports: {{ dashboard.totalReports || 0 }}</el-card></el-col>
    </el-row>
  </div>
</template>
