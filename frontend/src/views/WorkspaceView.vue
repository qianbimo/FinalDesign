<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const roleLabelMap = {
  PATIENT: '患者',
  DOCTOR: '医生',
  ADMIN: '管理员'
}

const modules = {
  PATIENT: [
    { title: '患者资料', path: '/app/patient/profile' },
    { title: '检查记录', path: '/app/patient/studies' },
    { title: '挂号申请', path: '/app/patient/registration' }
  ],
  DOCTOR: [
    { title: '医生资料', path: '/app/doctor/profile' },
    { title: '患者列表', path: '/app/doctor/patients' },
    { title: '病例列表', path: '/app/doctor/studies' },
    { title: 'CT上传', path: '/app/doctor/upload' },
    { title: '报告中心', path: '/app/doctor/reports' },
    { title: '标注查看', path: '/app/doctor/annotations' }
  ],
  ADMIN: [
    { title: '系统概览', path: '/app/admin/dashboard' },
    { title: '用户管理', path: '/app/admin/users' },
    { title: '报告总览', path: '/app/admin/reports' }
  ]
}

const cards = computed(() => modules[authStore.role] || [])
const roleLabel = computed(() => roleLabelMap[authStore.role] || '未知角色')

function to(path) {
  router.push(path)
}
</script>

<template>
  <div>
    <el-card>
      <h2>工作台</h2>
      <p>仅展示当前角色可访问的功能模块。</p>
      <el-tag type="primary">{{ roleLabel }}</el-tag>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col v-for="item in cards" :key="item.path" :xs="24" :sm="12" :lg="8">
        <el-card shadow="hover" style="margin-bottom: 16px; cursor: pointer" @click="to(item.path)">
          <h3>{{ item.title }}</h3>
          <p>点击进入模块</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
