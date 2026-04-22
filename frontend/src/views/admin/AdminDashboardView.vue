<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getAdminDashboardApi, getAdminReportsApi } from '@/api/admin'
import { getRegistrationListApi } from '@/api/registration'

const loading = ref(false)
const dashboard = ref({})
const chartsVisible = ref(false)

const userPieRef = ref(null)
const registrationPieRef = ref(null)
const reportPieRef = ref(null)

let userChart = null
let registrationChart = null
let reportChart = null

const FALLBACK_COLORS = ['#5c6bc0', '#26a69a', '#ffca28', '#ef5350', '#8d6e63', '#42a5f5']

const REGISTRATION_STATUS_META = {
  PENDING: { label: '待确认', color: '#90a4ae' },
  CONFIRMED: { label: '已确认', color: '#42a5f5' },
  CANCELLED: { label: '已取消', color: '#ef5350' },
  FINISHED: { label: '已完成', color: '#66bb6a' },
  UNKNOWN: { label: '未知', color: '#b0bec5' }
}

const REPORT_STATUS_META = {
  DRAFT: { label: '草稿', color: '#42a5f5' },
  REVIEWED: { label: '已审核', color: '#26a69a' },
  FINAL: { label: '最终版', color: '#66bb6a' },
  UNKNOWN: { label: '未知', color: '#b0bec5' }
}

function userRolePieData() {
  const raw = [
    { label: '患者', value: Number(dashboard.value.patientUsers || 0), color: '#4caf50' },
    { label: '医生', value: Number(dashboard.value.doctorUsers || 0), color: '#42a5f5' },
    { label: '管理员', value: Number(dashboard.value.adminUsers || 0), color: '#ffb74d' }
  ]
  const data = raw
    .filter((item) => item.value > 0)
    .map((item) => ({
      name: item.label,
      value: item.value,
      itemStyle: { color: item.color }
    }))

  if (data.length > 0) return data
  return [{ name: '暂无数据', value: 1, itemStyle: { color: '#d0d7de' } }]
}

function statusPieData(rawStats, statusMeta) {
  const stats = rawStats || {}
  const used = new Set()
  const data = []

  Object.entries(statusMeta).forEach(([key, meta]) => {
    const value = Number(stats[key] || 0)
    used.add(key)
    if (value > 0) {
      data.push({
        name: meta.label,
        value,
        itemStyle: { color: meta.color }
      })
    }
  })

  let extraColorIdx = 0
  Object.entries(stats).forEach(([key, valueRaw]) => {
    if (used.has(key)) return
    const value = Number(valueRaw || 0)
    if (value <= 0) return

    data.push({
      name: key,
      value,
      itemStyle: { color: FALLBACK_COLORS[extraColorIdx % FALLBACK_COLORS.length] }
    })
    extraColorIdx += 1
  })

  if (data.length > 0) return data
  return [{ name: '暂无数据', value: 1, itemStyle: { color: '#d0d7de' } }]
}

function buildPieOption(title, seriesData) {
  return {
    animation: true,
    animationDuration: 900,
    animationEasing: 'cubicOut',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: 0,
      left: 'center',
      type: 'scroll'
    },
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{c}'
        },
        labelLine: { show: true },
        data: seriesData
      }
    ]
  }
}

function initOrUpdateChart(elRef, currentChart, option) {
  if (!elRef.value) return currentChart
  const chart = currentChart || echarts.init(elRef.value)
  chart.setOption(option, true)
  return chart
}

function renderCharts() {
  userChart = initOrUpdateChart(
    userPieRef,
    userChart,
    buildPieOption('用户构成', userRolePieData())
  )

  registrationChart = initOrUpdateChart(
    registrationPieRef,
    registrationChart,
    buildPieOption(
      '挂号状态分布',
      statusPieData(dashboard.value.registrationStatusStats, REGISTRATION_STATUS_META)
    )
  )

  reportChart = initOrUpdateChart(
    reportPieRef,
    reportChart,
    buildPieOption('报告状态分布', statusPieData(dashboard.value.reportStatusStats, REPORT_STATUS_META))
  )
}

function handleResize() {
  userChart?.resize()
  registrationChart?.resize()
  reportChart?.resize()
}

function hasValidStatusStats(stats) {
  if (!stats || typeof stats !== 'object') return false
  return Object.values(stats).some((value) => Number(value || 0) > 0)
}

async function aggregateStatusByPagedApi(fetchPage, getStatus) {
  const size = 200
  let current = 1
  let scannedPages = 0
  const result = {}

  while (scannedPages < 1000) {
    const pageData = await fetchPage(current, size)
    const records = pageData?.records || []
    const total = Number(pageData?.total || records.length)

    records.forEach((item) => {
      const status = getStatus(item) || 'UNKNOWN'
      result[status] = Number(result[status] || 0) + 1
    })

    scannedPages += 1
    if (records.length === 0 || current * size >= total) break
    current += 1
  }

  return result
}

async function enrichStatusStats(raw) {
  const next = { ...(raw || {}) }

  if (!hasValidStatusStats(next.registrationStatusStats)) {
    try {
      next.registrationStatusStats = await aggregateStatusByPagedApi(
        (current, size) => getRegistrationListApi({ current, size }),
        (item) => item?.status
      )
    } catch (error) {
      console.warn('fallback registration status stats failed', error)
    }
  }

  if (!hasValidStatusStats(next.reportStatusStats)) {
    try {
      next.reportStatusStats = await aggregateStatusByPagedApi(
        (current, size) => getAdminReportsApi({ current, size }),
        (item) => item?.status
      )
    } catch (error) {
      console.warn('fallback report status stats failed', error)
    }
  }

  return next
}

async function loadData() {
  loading.value = true
  chartsVisible.value = false
  try {
    const raw = await getAdminDashboardApi()
    dashboard.value = await enrichStatusStats(raw)
    await nextTick()
    renderCharts()
    requestAnimationFrame(() => {
      chartsVisible.value = true
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  userChart?.dispose()
  registrationChart?.dispose()
  reportChart?.dispose()
})
</script>

<template>
  <div v-loading="loading">
    <el-card class="overview-chart-panel">
      <template #header>
        <div class="overview-chart-header">
          <div class="overview-chart-title">系统概览统计图</div>
          <div class="overview-chart-desc">点击进入系统概览页面后，统计图将按顺序呈现。</div>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col
          :xs="24"
          :lg="8"
          class="chart-col"
          :class="{ 'is-visible': chartsVisible }"
          style="transition-delay: 0ms"
        >
          <el-card shadow="never" class="chart-card">
            <div class="chart-card__title">1. 用户总数构成</div>
            <div ref="userPieRef" class="pie-canvas" />
          </el-card>
        </el-col>

        <el-col
          :xs="24"
          :lg="8"
          class="chart-col"
          :class="{ 'is-visible': chartsVisible }"
          style="transition-delay: 120ms"
        >
          <el-card shadow="never" class="chart-card">
            <div class="chart-card__title">2. 挂号状态分布</div>
            <div ref="registrationPieRef" class="pie-canvas" />
          </el-card>
        </el-col>

        <el-col
          :xs="24"
          :lg="8"
          class="chart-col"
          :class="{ 'is-visible': chartsVisible }"
          style="transition-delay: 240ms"
        >
          <el-card shadow="never" class="chart-card">
            <div class="chart-card__title">3. 报告状态分布</div>
            <div ref="reportPieRef" class="pie-canvas" />
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.overview-chart-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-chart-title {
  font-size: 16px;
  font-weight: 600;
}

.overview-chart-desc {
  font-size: 13px;
  color: #667085;
}

.chart-col {
  margin-bottom: 16px;
  opacity: 0;
  transform: translateY(12px);
  transition: opacity 420ms ease, transform 420ms ease;
}

.chart-col.is-visible {
  opacity: 1;
  transform: translateY(0);
}

.chart-card {
  border: 1px solid #eef2f7;
}

.chart-card__title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.pie-canvas {
  width: 100%;
  height: 320px;
}
</style>
