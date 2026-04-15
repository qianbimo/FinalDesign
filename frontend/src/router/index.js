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
        meta: { title: 'Workspace', roles: ['PATIENT', 'DOCTOR', 'ADMIN'], menu: true }
      },
      {
        path: 'patient/profile',
        name: 'PatientProfile',
        component: () => import('@/views/patient/PatientProfileView.vue'),
        meta: { title: 'Patient Profile', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'patient/studies',
        name: 'PatientStudies',
        component: () => import('@/views/patient/PatientStudiesView.vue'),
        meta: { title: 'My Studies', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'patient/studies/:studyId',
        name: 'PatientStudyDetail',
        component: () => import('@/views/patient/PatientStudyDetailView.vue'),
        meta: { title: 'Study Detail', roles: ['PATIENT'] }
      },
      {
        path: 'patient/registration',
        name: 'PatientRegistration',
        component: () => import('@/views/patient/PatientRegistrationView.vue'),
        meta: { title: 'Appointment', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'patient/upload',
        name: 'PatientUpload',
        component: () => import('@/views/patient/PatientUploadView.vue'),
        meta: { title: 'Upload CT', roles: ['PATIENT'], menu: true }
      },
      {
        path: 'doctor/profile',
        name: 'DoctorProfile',
        component: () => import('@/views/doctor/DoctorProfileView.vue'),
        meta: { title: 'Doctor Profile', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/patients',
        name: 'DoctorPatients',
        component: () => import('@/views/doctor/DoctorPatientsView.vue'),
        meta: { title: 'Patients', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/studies',
        name: 'DoctorStudies',
        component: () => import('@/views/doctor/DoctorStudiesView.vue'),
        meta: { title: 'Studies', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/studies/:patientId/:studyId',
        name: 'DoctorStudyDetail',
        component: () => import('@/views/doctor/DoctorStudyDetailView.vue'),
        meta: { title: 'Case Detail', roles: ['DOCTOR'] }
      },
      {
        path: 'doctor/reports',
        name: 'DoctorReports',
        component: () => import('@/views/doctor/DoctorReportsView.vue'),
        meta: { title: 'Reports', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'doctor/annotations',
        name: 'DoctorAnnotations',
        component: () => import('@/views/doctor/DoctorAnnotationsView.vue'),
        meta: { title: 'Annotations', roles: ['DOCTOR'], menu: true }
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: { title: 'Dashboard', roles: ['ADMIN'], menu: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsersView.vue'),
        meta: { title: 'User Management', roles: ['ADMIN'], menu: true }
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
