<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const roleLabelMap = {
  PATIENT: 'Patient',
  DOCTOR: 'Doctor',
  ADMIN: 'Admin'
}

const modules = {
  PATIENT: [
    { title: 'Patient Profile', path: '/app/patient/profile' },
    { title: 'My Studies', path: '/app/patient/studies' },
    { title: 'Appointment', path: '/app/patient/registration' },
    { title: 'Upload CT', path: '/app/patient/upload' }
  ],
  DOCTOR: [
    { title: 'Doctor Profile', path: '/app/doctor/profile' },
    { title: 'Patients', path: '/app/doctor/patients' },
    { title: 'Studies', path: '/app/doctor/studies' },
    { title: 'Reports', path: '/app/doctor/reports' },
    { title: 'Annotations', path: '/app/doctor/annotations' }
  ],
  ADMIN: [
    { title: 'Dashboard', path: '/app/admin/dashboard' },
    { title: 'User Management', path: '/app/admin/users' }
  ]
}

const cards = computed(() => modules[authStore.role] || [])
const roleLabel = computed(() => roleLabelMap[authStore.role] || authStore.role)

function to(path) {
  router.push(path)
}
</script>

<template>
  <div>
    <el-card>
      <h2>Workspace</h2>
      <p>Only modules available for current role are shown.</p>
      <el-tag type="primary">{{ roleLabel }}</el-tag>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col v-for="item in cards" :key="item.path" :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" style="margin-bottom: 16px; cursor: pointer" @click="to(item.path)">
          <h3>{{ item.title }}</h3>
          <p>Click to enter</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
