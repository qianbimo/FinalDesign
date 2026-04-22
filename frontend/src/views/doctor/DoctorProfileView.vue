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
    <template #header>医生资料</template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="编号">{{ profile?.id }}</el-descriptions-item>
      <el-descriptions-item label="科室">{{ profile?.department }}</el-descriptions-item>
      <el-descriptions-item label="职称">{{ profile?.title }}</el-descriptions-item>
      <el-descriptions-item label="执业证号">{{ profile?.licenseNo }}</el-descriptions-item>
      <el-descriptions-item label="个人简介">{{ profile?.introduction }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
