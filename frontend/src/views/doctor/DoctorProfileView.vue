<script setup>
import { onMounted, ref } from 'vue'
import { getDoctorProfileApi } from '@/api/doctor'

const loading = ref(false)
const profile = ref(null)

async function loadData() {
  loading.value = true
  try {
    profile.value = await getDoctorProfileApi()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>Doctor Profile</template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="ID">{{ profile?.id }}</el-descriptions-item>
      <el-descriptions-item label="Department">{{ profile?.department }}</el-descriptions-item>
      <el-descriptions-item label="Title">{{ profile?.title }}</el-descriptions-item>
      <el-descriptions-item label="Specialty">{{ profile?.specialty }}</el-descriptions-item>
      <el-descriptions-item label="License No">{{ profile?.licenseNo }}</el-descriptions-item>
      <el-descriptions-item label="Introduction">{{ profile?.introduction }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
