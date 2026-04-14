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
      <el-col :xs="24" :sm="12" :lg="6"><el-card>用户总数：{{ dashboard.totalUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>患者数：{{ dashboard.patientUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>医生数：{{ dashboard.doctorUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>管理员数：{{ dashboard.adminUsers || 0 }}</el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :sm="12" :lg="6"><el-card>启用用户：{{ dashboard.activeUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>检查总数：{{ dashboard.totalStudies || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>AI任务总数：{{ dashboard.totalAiTasks || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="12" :lg="6"><el-card>报告总数：{{ dashboard.totalReports || 0 }}</el-card></el-col>
    </el-row>
  </div>
</template>
