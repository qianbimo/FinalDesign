<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const roleConfigMap = {
  PATIENT: {
    label: '患者端',
    themeClass: 'role-patient',
    brandTitle: '肺结节患者服务中心',
    brandSubtitle: '挂号申请、检查进度与报告查看',
    headerNote: '患者端强调流程清晰和结果易读，方便快速查看自己的检查状态与最终报告。'
  },
  DOCTOR: {
    label: '医生端',
    themeClass: 'role-doctor',
    brandTitle: '肺结节诊疗工作站',
    brandSubtitle: '挂号处理、检查上传、智能分析与报告管理',
    headerNote: '医生端围绕临床工作流设计，便于从挂号处理一路进入检查、分析和报告审核。'
  },
  ADMIN: {
    label: '管理员端',
    themeClass: 'role-admin',
    brandTitle: '肺结节系统管理中心',
    brandSubtitle: '系统概览、用户管理与报告总览',
    headerNote: '管理员端提供全局视角，便于统一查看平台状态并处理系统级事务。'
  }
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
    { title: '报告中心', path: '/app/doctor/studies' },
    { title: '挂号处理', path: '/app/doctor/upload' }
  ],
  ADMIN: [
    { title: '工作台', path: '/app/workspace' },
    { title: '系统概览', path: '/app/admin/dashboard' },
    { title: '用户管理', path: '/app/admin/users' },
    { title: '报告总览', path: '/app/admin/reports' }
  ]
}

const roleConfig = computed(() => roleConfigMap[authStore.role] || roleConfigMap.PATIENT)
const menus = computed(() => menuMap[authStore.role] || [])
const activePath = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '工作台')

function onSelect(path) {
  router.push(path)
}

function logout() {
  authStore.clearSession()
  router.push('/login')
}
</script>

<template>
  <div class="layout-shell" :class="roleConfig.themeClass">
    <header class="topbar surface-card">
      <div class="topbar-main">
        <div class="brand-group">
          <div class="brand-mark">{{ roleConfig.label.slice(0, 2) }}</div>
          <div class="brand-copy">
            <div class="role-chip">{{ roleConfig.label }}</div>
            <h1>{{ roleConfig.brandTitle }}</h1>
            <p>{{ roleConfig.brandSubtitle }}</p>
          </div>
        </div>

        <div class="user-tools">
          <div class="user-box">
            <span class="user-box__label">当前用户</span>
            <strong>{{ authStore.realName || authStore.userId }}</strong>
          </div>
          <el-button type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </div>

      <div class="topbar-nav">
        <button
          v-for="item in menus"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ active: item.path === activePath }"
          @click="onSelect(item.path)"
        >
          {{ item.title }}
        </button>
      </div>
    </header>

    <div class="layout-main-wrap">
      <section class="page-header surface-card">
        <div class="page-header__crumb">{{ roleConfig.label }} / {{ pageTitle }}</div>
        <h2>{{ pageTitle }}</h2>
        <p>{{ roleConfig.headerNote }}</p>
      </section>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout-shell {
  min-height: 100vh;
  padding: 18px;
}

.topbar {
  padding: 18px 20px 16px;
}

.topbar-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.brand-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.08em;
  background: #e9eef6;
  color: #27476f;
}

.role-patient .brand-mark {
  background: #e8f1ee;
  color: #2d6257;
}

.role-admin .brand-mark {
  background: #ece9f3;
  color: #4e5772;
}

.brand-copy h1 {
  margin: 6px 0 4px;
  font-size: 24px;
  line-height: 1.15;
}

.brand-copy p {
  margin: 0;
  color: #66758f;
  line-height: 1.6;
}

.role-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #4f607c;
  background: #eef2f8;
}

.role-patient .role-chip {
  color: #3f645c;
  background: #edf3f1;
}

.role-admin .role-chip {
  color: #545c72;
  background: #efedf5;
}

.user-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-box {
  min-width: 180px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(116, 138, 183, 0.12);
  background: #f7f9fc;
}

.user-box__label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #7a88a3;
  letter-spacing: 0.08em;
}

.user-box strong {
  display: block;
  margin-top: 6px;
  font-size: 16px;
}

.topbar-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(116, 138, 183, 0.12);
}

.nav-item {
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid rgba(116, 138, 183, 0.14);
  border-radius: 10px;
  background: #ffffff;
  color: #45546d;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.nav-item:hover {
  background: #f4f7fb;
}

.nav-item.active {
  color: #ffffff;
  border-color: transparent;
  background: #335c8d;
}

.role-patient .nav-item.active {
  background: #3c7267;
}

.role-admin .nav-item.active {
  background: #5a6279;
}

.layout-main-wrap {
  padding-top: 18px;
}

.page-header {
  padding: 18px 20px;
}

.page-header__crumb {
  color: #7a88a3;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-header h2 {
  margin: 10px 0 8px;
  font-size: 28px;
  line-height: 1.12;
}

.page-header p {
  margin: 0;
  color: #66758f;
  line-height: 1.7;
}

.layout-main {
  padding-top: 18px;
}

@media (max-width: 900px) {
  .topbar-main {
    flex-direction: column;
  }

  .user-tools {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 640px) {
  .layout-shell {
    padding: 12px;
  }

  .topbar,
  .page-header {
    padding: 16px;
  }

  .brand-group {
    align-items: flex-start;
  }

  .brand-copy h1 {
    font-size: 22px;
  }

  .user-tools {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
