<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getRegistrationListApi } from '@/api/registration'

const router = useRouter()
const authStore = useAuthStore()

const roleViews = {
  PATIENT: {
    eyebrow: '患者工作台',
    title: '把挂号、检查进度和报告结果放在同一张清晰的健康面板里',
    desc: '患者端突出流程清楚、结果直观，便于完成挂号申请并随时查看自己的 CT 检查记录和智能分析结果。',
    heroClass: 'hero-patient',
    metrics: [
      { label: '当前角色', value: '患者', note: '重点关注挂号进度、检查状态和报告结果' },
      { label: '推荐入口', value: '挂号申请', note: '先提交预约，再由医生确认挂号并安排检查' },
      { label: '结果查看', value: '检查记录', note: '集中查看 AI 结果、医生审核和最终报告' }
    ],
    modules: [
      { tag: '预约', title: '挂号申请', desc: '选择医生与预约时间，并填写本次检查说明。', path: '/app/patient/registration' },
      { tag: '结果', title: '检查记录', desc: '查看自己的 CT 检查记录、AI 结果和最终报告。', path: '/app/patient/studies' },
      { tag: '资料', title: '个人资料', desc: '完善患者基础信息，便于挂号和检查流程准确关联。', path: '/app/patient/profile' }
    ]
  },
  DOCTOR: {
    eyebrow: '医生工作台',
    title: '围绕临床流程组织页面，让挂号处理、上传检查与报告管理更顺手',
    desc: '医生端按“确认挂号、上传 CT、启动分析、查看报告”的诊疗顺序组织主要入口，减少来回切换。',
    heroClass: 'hero-doctor',
    metrics: [
      { label: '当前角色', value: '医生', note: '核心工作是挂号处理、检查上传和报告确认' },
      { label: '关键流程', value: '先确认挂号', note: '确认挂号后才能创建检查记录并上传 CT' },
      { label: '主要入口', value: '报告中心', note: '集中查看病例、启动分析并进入具体报告详情' }
    ],
    modules: [
      { tag: '流程', title: '挂号处理', desc: '确认挂号单、创建检查记录并上传 CT 影像。', path: '/app/doctor/upload' },
      { tag: '报告', title: '报告中心', desc: '集中查看病例列表、分析状态和报告处理入口。', path: '/app/doctor/studies' },
      { tag: '患者', title: '患者列表', desc: '查看患者基础信息，便于后续病例关联与处理。', path: '/app/doctor/patients' },
      { tag: '资料', title: '医生资料', desc: '维护科室、职称和执业信息，便于挂号展示。', path: '/app/doctor/profile' }
    ]
  },
  ADMIN: {
    eyebrow: '管理员工作台',
    title: '把系统概览、用户管理和报告总览整合到统一的管理视角中',
    desc: '管理员端以系统治理为核心，帮助统一查看平台运行状态，处理账号、权限与报告级事务。',
    heroClass: 'hero-admin',
    metrics: [
      { label: '当前角色', value: '管理员', note: '负责系统概览、用户治理和报告总览' },
      { label: '优先查看', value: '系统概览', note: '建议先查看全局状态，再进入具体管理页面' },
      { label: '治理重点', value: '用户与报告', note: '用户状态与报告质量是当前的主要关注点' }
    ],
    modules: [
      { tag: '概览', title: '系统概览', desc: '查看用户、检查、AI 任务和报告的整体情况。', path: '/app/admin/dashboard' },
      { tag: '用户', title: '用户管理', desc: '集中维护患者、医生和管理员账户。', path: '/app/admin/users' },
      { tag: '报告', title: '报告总览', desc: '统一查看所有报告状态并定位异常记录。', path: '/app/admin/reports' }
    ]
  }
}

const isDoctor = computed(() => authStore.role === 'DOCTOR')
const currentView = computed(() => roleViews[authStore.role] || roleViews.PATIENT)
const pendingRegistrationCount = ref(0)
const pendingCountLoading = ref(false)
const pendingCountLoadError = ref(false)

const pendingRegistrationCountText = computed(() => {
  if (pendingCountLoading.value) return '...'
  return String(pendingRegistrationCount.value)
})

const pendingRegistrationStatusText = computed(() => {
  if (!isDoctor.value) return ''
  if (pendingCountLoading.value) return '正在统计未处理挂号'
  if (pendingCountLoadError.value) return '统计失败，请稍后重试'
  if (pendingRegistrationCount.value === 0) return '当前没有待处理挂号'
  return '请优先处理待确认挂号'
})

async function loadDoctorPendingRegistrationCount() {
  if (!isDoctor.value) return

  pendingCountLoading.value = true
  pendingCountLoadError.value = false

  try {
    const size = 200
    let current = 1
    let count = 0
    let scannedPages = 0

    while (scannedPages < 1000) {
      const data = await getRegistrationListApi({ current, size })
      const records = data?.records || []
      const total = Number(data?.total || records.length)

      records.forEach((item) => {
        if (item?.status === 'PENDING') {
          count += 1
        }
      })

      scannedPages += 1
      if (records.length === 0 || current * size >= total) break
      current += 1
    }

    pendingRegistrationCount.value = count
  } catch (error) {
    pendingCountLoadError.value = true
    pendingRegistrationCount.value = 0
  } finally {
    pendingCountLoading.value = false
  }
}

function to(path) {
  router.push(path)
}

onMounted(() => {
  loadDoctorPendingRegistrationCount()
})
</script>

<template>
  <div class="page-shell">
    <section class="page-hero" :class="currentView.heroClass">
      <span class="page-hero__eyebrow">{{ currentView.eyebrow }}</span>
      <h1 class="page-hero__title">{{ currentView.title }}</h1>
      <p class="page-hero__desc">{{ currentView.desc }}</p>
    </section>

    <section class="stat-grid">
      <article v-for="item in currentView.metrics" :key="item.label" class="stat-card">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__note">{{ item.note }}</div>
      </article>
    </section>

    <section v-if="isDoctor" class="doctor-pending-panel">
      <article
        class="doctor-pending-card"
        :class="{ 'is-loading': pendingCountLoading, 'has-error': pendingCountLoadError }"
      >
        <div class="doctor-pending-card__icon">待</div>
        <div class="doctor-pending-card__body">
          <div class="doctor-pending-card__label">未处理挂号</div>
          <div class="doctor-pending-card__value">{{ pendingRegistrationCountText }}</div>
          <div class="doctor-pending-card__hint">{{ pendingRegistrationStatusText }}</div>
        </div>
        <button class="doctor-pending-card__action" type="button" @click="loadDoctorPendingRegistrationCount">
          刷新
        </button>
      </article>
    </section>

    <section class="surface-card section-card">
      <div class="section-head">
        <div>
          <h2 class="section-title">常用模块</h2>
          <p class="section-subtitle">这里展示当前角色最常使用的功能入口，便于快速进入下一步操作。</p>
        </div>
      </div>

      <div class="feature-grid">
        <article
          v-for="item in currentView.modules"
          :key="item.path"
          class="feature-card"
          role="button"
          tabindex="0"
          @click="to(item.path)"
        >
          <div>
            <span class="feature-card__badge">{{ item.tag }}</span>
            <h3 class="feature-card__title">{{ item.title }}</h3>
            <p class="feature-card__desc">{{ item.desc }}</p>
          </div>
          <div class="feature-card__action">进入模块</div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.doctor-pending-panel {
  margin-bottom: 16px;
}

.doctor-pending-card {
  display: flex;
  align-items: center;
  gap: 14px;
  border-radius: 14px;
  padding: 16px 18px;
  border: 1px solid #dbeafe;
  background: linear-gradient(120deg, #eff6ff 0%, #f8fafc 100%);
  box-shadow: 0 8px 18px rgba(30, 64, 175, 0.08);
}

.doctor-pending-card__icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.doctor-pending-card__body {
  min-width: 0;
  flex: 1;
}

.doctor-pending-card__label {
  font-size: 13px;
  color: #2563eb;
  margin-bottom: 4px;
}

.doctor-pending-card__value {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.doctor-pending-card__hint {
  font-size: 13px;
  color: #475569;
}

.doctor-pending-card__action {
  border: 0;
  border-radius: 10px;
  padding: 8px 14px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease, background-color 160ms ease;
}

.doctor-pending-card__action:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.24);
  background: #1d4ed8;
}

.doctor-pending-card.is-loading .doctor-pending-card__value {
  animation: pulse 1.2s infinite ease-in-out;
}

.doctor-pending-card.has-error {
  border-color: #fecaca;
  background: linear-gradient(120deg, #fff1f2 0%, #fff7ed 100%);
}

.doctor-pending-card.has-error .doctor-pending-card__icon {
  background: #dc2626;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

@media (max-width: 768px) {
  .doctor-pending-card {
    flex-wrap: wrap;
  }

  .doctor-pending-card__action {
    width: 100%;
  }
}
</style>
