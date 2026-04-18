<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const roleLabelMap = {
  PATIENT: '患者',
  DOCTOR: '医生',
  ADMIN: '管理员'
}

const menuMap = {
  PATIENT: [
    { title: '工作台', path: '/app/workspace' },
    { title: '个人资料', path: '/app/patient/profile' },
    { title: '检查记录', path: '/app/patient/studies' },
    { title: '挂号申请', path: '/app/patient/registration' }
  ],
  DOCTOR: [
    { title: '工作台', path: '/app/workspace' },
    { title: '医生资料', path: '/app/doctor/profile' },
    { title: '患者列表', path: '/app/doctor/patients' },
    { title: '病例列表', path: '/app/doctor/studies' },
    { title: 'CT上传', path: '/app/doctor/upload' },
    { title: '报告中心', path: '/app/doctor/reports' },
    { title: '标注查看', path: '/app/doctor/annotations' }
  ],
  ADMIN: [
    { title: '工作台', path: '/app/workspace' },
    { title: '管理员概览', path: '/app/admin/dashboard' },
    { title: '用户管理', path: '/app/admin/users' },
    { title: '报告总览', path: '/app/admin/reports' }
  ]
}

const menus = computed(() => menuMap[authStore.role] || [])
const activePath = computed(() => route.path)
const roleLabel = computed(() => roleLabelMap[authStore.role] || '未知角色')

function onSelect(path) {
  router.push(path)
}

function logout() {
  authStore.clearSession()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-root">
    <el-aside width="230px" class="layout-aside">
      <div class="brand">肺结节智能分析系统</div>
      <el-menu :default-active="activePath" @select="onSelect">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">{{ item.title }}</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">当前角色：<strong>{{ roleLabel }}</strong></div>
        <div class="header-right">
          <span>{{ authStore.realName || authStore.userId }}</span>
          <el-button type="danger" plain size="small" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-root {
  min-height: 100vh;
}

.layout-aside {
  background: #ffffff;
  border-right: 1px solid #e5e7eb;
}

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #1d4ed8;
  border-bottom: 1px solid #e5e7eb;
}

.layout-header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-main {
  padding: 16px;
}
</style>
