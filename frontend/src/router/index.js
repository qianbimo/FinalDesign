import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/app/workspace'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/app',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'workspace',
        name: 'Workspace',
        component: () => import('@/views/WorkspaceView.vue'),
        meta: { title: '工作台', roles: ['PATIENT', 'DOCTOR', 'ADMIN'], menu: true }
      },
      {
        path: 'patient/profile',
        name: 'PatientProfile',
        component: () => import('@/views/patient/PatientProfileView.vue'),
        meta: { title: '患者资料', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'patient/studies',
        name: 'PatientStudies',
        component: () => import('@/views/patient/PatientStudiesView.vue'),
        meta: { title: '检查记录', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'patient/studies/:studyId',
        name: 'PatientStudyDetail',
        component: () => import('@/views/patient/PatientStudyDetailView.vue'),
        meta: { title: '检查详情', roles: ['PATIENT'] }
      },
      {
        path: 'patient/registration',
        name: 'PatientRegistration',
        component: () => import('@/views/patient/PatientRegistrationView.vue'),
        meta: { title: '挂号申请', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'doctor/profile',
        name: 'DoctorProfile',
        component: () => import('@/views/doctor/DoctorProfileView.vue'),
        meta: { title: '医生资料', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/patients',
        name: 'DoctorPatients',
        component: () => import('@/views/doctor/DoctorPatientsView.vue'),
        meta: { title: '患者列表', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/studies',
        name: 'DoctorStudies',
        component: () => import('@/views/doctor/DoctorStudiesView.vue'),
        meta: { title: '报告中心', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/upload',
        name: 'DoctorUpload',
        component: () => import('@/views/doctor/DoctorUploadView.vue'),
        meta: { title: '挂号处理', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/studies/:patientId/:studyId',
        name: 'DoctorStudyDetail',
        component: () => import('@/views/doctor/DoctorStudyDetailView.vue'),
        meta: { title: '病例详情', roles: ['DOCTOR'] }
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: '系统概览', roles: ['ADMIN'], menu: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'], menu: true }
      },
      {
        path: 'admin/reports',
        name: 'AdminReports',
        component: () => import('@/views/admin/AdminReportsView.vue'),
        meta: { title: '报告总览', roles: ['ADMIN'], menu: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isLoggedIn) {
      next('/app/workspace')
      return
    }
    next()
    return
  }

  if (!authStore.isLoggedIn) {
    next('/login')
    return
  }

  const matchedWithRoles = to.matched.find((record) => Array.isArray(record.meta?.roles))
  if (matchedWithRoles) {
    const roles = matchedWithRoles.meta.roles
    if (!roles.includes(authStore.role)) {
      next('/app/workspace')
      return
    }
  }

  next()
})

export default router
