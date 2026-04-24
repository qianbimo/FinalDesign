<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDoctorPatientStudyDetailApi } from '@/api/doctor'
import { getCtFilesByStudyApi } from '@/api/study'
import { getAiResultByStudyApi, startAiTaskApi } from '@/api/aiTask'
import { getAnnotationByStudyApi } from '@/api/annotation'
import { auditReportApi, getReportByStudyApi, updateReportApi } from '@/api/report'
import { isImageFileType, isImagePath, toFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const studyId = route.params.studyId
const patientId = route.params.patientId

const loading = ref(false)
const aiLoading = ref(false)
const study = ref(null)
const files = ref([])
const aiResult = ref(null)
const annotationData = ref(null)
const report = ref(null)
const reportSaving = ref(false)
const reportAuditing = ref(false)
const reportForm = reactive({
  reportTitle: '',
  reportSummary: '',
  reportContent: ''
})

const studyStatusMap = {
  WAIT_UPLOAD: '待上传CT',
  UPLOADED: '已上传',
  PREPROCESSING: '预处理中',
  ANALYZING: '分析中',
  FINISHED: '已完成',
  FAILED: '失败'
}

const taskStatusMap = {
  WAITING: '等待中',
  RUNNING: '运行中',
  SUCCESS: '成功',
  FAILED: '失败'
}

const reportStatusMap = {
  DRAFT: '草稿',
  REVIEWED: '已审核',
  FINAL: '最终版'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
}

function taskStatusText(status) {
  return taskStatusMap[status] || '未知状态'
}

function reportStatusText(status) {
  return reportStatusMap[status] || '未知状态'
}

function reportStatusTagType(status) {
  switch (status) {
    case 'DRAFT':
      return 'warning'
    case 'REVIEWED':
      return 'success'
    case 'FINAL':
      return 'info'
    default:
      return 'info'
  }
}

const canEditReport = computed(() => report.value && report.value.status !== 'FINAL')
const canAuditReport = computed(() => report.value && report.value.status !== 'REVIEWED' && report.value.status !== 'FINAL')

function toUniqueFileUrls(paths) {
  const seen = new Set()
  const urls = []
  for (const path of paths) {
    const url = toFileUrl(path)
    if (!url || seen.has(url)) continue
    seen.add(url)
    urls.push(url)
  }
  return urls
}

const originalPreviewCandidates = computed(() => {
  const imageFile = files.value.find((item) => isImageFileType(item.fileType) || isImagePath(item.filePath))
  return toUniqueFileUrls([
    annotationData.value?.originalPreviewPath,
    annotationData.value?.annotatedPreviewPath,
    annotationData.value?.overlayPreviewPath,
    imageFile?.filePath,
    `result/${studyId}/pipeline/figures/pipeline_ct_slice.png`,
    `result/${studyId}/original_preview.png`,
    `result/${studyId}/pipeline_ct_slice.png`,
    `overlay/${studyId}/nodule1_axial.png`,
    `overlay/${studyId}/nodule1_coronal.png`
  ])
})

const originalPreviewIndex = ref(0)

const originalPreviewUrl = computed(() => {
  return originalPreviewCandidates.value[originalPreviewIndex.value] || ''
})

const originalPreviewList = computed(() => {
  return originalPreviewCandidates.value.slice(originalPreviewIndex.value)
})

watch(originalPreviewCandidates, () => {
  originalPreviewIndex.value = 0
})

function handleOriginalPreviewError() {
  if (originalPreviewIndex.value < originalPreviewCandidates.value.length - 1) {
    originalPreviewIndex.value += 1
  }
}

const annotatedPreviewUrl = computed(() => {
  const path =
    annotationData.value?.annotatedPreviewPath ||
    annotationData.value?.overlayPreviewPath ||
    `result/${studyId}/pipeline/figures/pipeline_annotated.png`
  return toFileUrl(path)
})

const overlayPreviewUrl = computed(() => {
  const path = annotationData.value?.overlayPreviewPath || `result/${studyId}/pipeline/figures/pipeline_overlay.png`
  return toFileUrl(path)
})

function filePreviewUrl(file) {
  return toFileUrl(file.filePath)
}

function syncReportForm(reportData) {
  reportForm.reportTitle = reportData?.reportTitle || ''
  reportForm.reportSummary = reportData?.reportSummary || ''
  reportForm.reportContent = reportData?.reportContent || ''
}

async function loadReport() {
  try {
    report.value = await getReportByStudyApi(studyId)
    syncReportForm(report.value)
  } catch (error) {
    report.value = null
    syncReportForm(null)
  }
}

async function loadData() {
  loading.value = true
  try {
    study.value = await getDoctorPatientStudyDetailApi(patientId, studyId)
    files.value = await getCtFilesByStudyApi(studyId)

    try {
      aiResult.value = await getAiResultByStudyApi(studyId)
    } catch (error) {
      aiResult.value = null
    }

    try {
      annotationData.value = await getAnnotationByStudyApi(studyId)
    } catch (error) {
      annotationData.value = null
    }

    await loadReport()
  } finally {
    loading.value = false
  }
}

async function startAi() {
  if (study.value?.status === 'WAIT_UPLOAD') {
    ElMessage.warning('该检查尚未上传CT文件，无法启动智能分析')
    return
  }

  aiLoading.value = true
  try {
    const data = await startAiTaskApi(studyId)
    ElMessage.success(`智能分析任务已启动，任务编号：${data.taskId}`)
    await loadData()
  } finally {
    aiLoading.value = false
  }
}

async function saveReport() {
  if (!report.value) {
    ElMessage.warning('当前暂无可编辑报告')
    return
  }

  if (!canEditReport.value) {
    ElMessage.warning('最终版报告不允许继续修改')
    return
  }

  reportSaving.value = true
  try {
    await updateReportApi(report.value.id, {
      reportTitle: reportForm.reportTitle,
      reportSummary: reportForm.reportSummary,
      reportContent: reportForm.reportContent
    })
    ElMessage.success('报告内容已保存')
    await loadReport()
  } finally {
    reportSaving.value = false
  }
}

async function auditReport() {
  if (!report.value) {
    ElMessage.warning('当前暂无可审核报告')
    return
  }

  if (!canAuditReport.value) {
    ElMessage.info('当前报告状态无需重复审核')
    return
  }

  await ElMessageBox.confirm('确认提交审核后，将把报告状态更新为“已审核”。是否继续？', '确认审核', {
    type: 'warning',
    confirmButtonText: '确认审核',
    cancelButtonText: '取消'
  })

  reportAuditing.value = true
  try {
    await auditReportApi(report.value.id)
    ElMessage.success('报告审核完成')
    await loadReport()
  } finally {
    reportAuditing.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="doctor-detail-page">
    <el-card class="detail-card detail-card--mb">
      <template #header>
        <div class="detail-card-head">
          <span>病例详情</span>
          <el-button class="detail-btn" type="success" :loading="aiLoading" @click="startAi">启动智能分析</el-button>
        </div>
      </template>

      <el-descriptions class="detail-descriptions" :column="2" border>
        <el-descriptions-item label="检查ID">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="患者ID">{{ study?.patientId }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ study?.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检查日期">{{ study?.studyDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ studyStatusText(study?.status) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="detail-card detail-card--mb">
      <template #header>影像文件</template>
      <el-table class="detail-table" :data="files">
        <el-table-column prop="id" label="文件编号" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column prop="fileSize" label="大小" width="140" />
        <el-table-column label="预览" width="220">
          <template #default="scope">
            <el-image
              v-if="isImageFileType(scope.row.fileType) || isImagePath(scope.row.filePath)"
              :src="filePreviewUrl(scope.row)"
              :preview-src-list="[filePreviewUrl(scope.row)]"
              fit="cover"
              class="file-preview-image"
            >
              <template #error>
                <div class="preview-error-text">图片加载失败</div>
              </template>
            </el-image>
            <span v-else class="preview-muted-text">体数据文件，暂不直接预览</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="detail-card detail-card--mb">
      <template #header>原图预览</template>
      <el-image
        :src="originalPreviewUrl"
        :preview-src-list="originalPreviewList"
        fit="contain"
        class="preview-large-image"
        @error="handleOriginalPreviewError"
      >
        <template #error>
          <el-empty description="暂无可用原图预览" />
        </template>
      </el-image>
    </el-card>

    <el-card class="detail-card detail-card--mb">
      <template #header>标注与叠加图</template>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="preview-card">
            <div class="preview-card-title">标注图</div>
            <el-image
              :src="annotatedPreviewUrl"
              :preview-src-list="[annotatedPreviewUrl]"
              fit="cover"
              class="preview-card-image"
            >
              <template #error>
                <div class="preview-card-error">标注图加载失败（pipeline_annotated.png）</div>
              </template>
            </el-image>
            <div class="preview-card-foot">pipeline_annotated.png</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="preview-card">
            <div class="preview-card-title">叠加图</div>
            <el-image
              :src="overlayPreviewUrl"
              :preview-src-list="[overlayPreviewUrl]"
              fit="cover"
              class="preview-card-image"
            >
              <template #error>
                <div class="preview-card-error">叠加图加载失败（pipeline_overlay.png）</div>
              </template>
            </el-image>
            <div class="preview-card-foot">pipeline_overlay.png</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="detail-card">
      <template #header>智能分析结果概览</template>
      <el-empty v-if="!aiResult" description="暂无智能分析结果" />
      <el-descriptions v-else class="detail-descriptions" :column="2" border>
        <el-descriptions-item label="任务编号">{{ aiResult?.task?.id }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ taskStatusText(aiResult?.task?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="结节数量">{{ aiResult?.nodules?.length || 0 }}</el-descriptions-item>
        <el-descriptions-item label="标注数量">{{ aiResult?.annotations?.length || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="detail-card detail-card--mt">
      <template #header>
        <div class="detail-card-head">
          <span>报告编辑与审核</span>
          <el-tag v-if="report" :type="reportStatusTagType(report.status)">
            {{ reportStatusText(report.status) }}
          </el-tag>
        </div>
      </template>

      <el-empty v-if="!report" description="暂无报告，请先启动智能分析生成报告" />

      <el-form v-else class="report-form" label-width="90px">
        <el-form-item label="报告标题">
          <el-input v-model="reportForm.reportTitle" :disabled="!canEditReport" />
        </el-form-item>

        <el-form-item label="报告摘要">
          <el-input v-model="reportForm.reportSummary" type="textarea" :rows="3" :disabled="!canEditReport" />
        </el-form-item>

        <el-form-item label="报告内容">
          <el-input v-model="reportForm.reportContent" type="textarea" :rows="10" :disabled="!canEditReport" />
        </el-form-item>

        <el-form-item>
          <el-button class="detail-btn" type="primary" :loading="reportSaving" :disabled="!canEditReport" @click="saveReport">
            保存修改
          </el-button>
          <el-button
            class="detail-btn"
            type="success"
            :loading="reportAuditing"
            :disabled="!canAuditReport"
            @click="auditReport"
          >
            确认审核
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.doctor-detail-page {
  --detail-card-radius: 16px;
  --detail-control-radius: 10px;
}

.detail-card {
  border-radius: var(--detail-card-radius);
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.detail-card--mb {
  margin-bottom: 16px;
}

.detail-card--mt {
  margin-top: 16px;
}

.detail-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-table {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #eef2f7;
}

.file-preview-image {
  width: 160px;
  height: 90px;
}

.preview-error-text {
  font-size: 12px;
  color: #909399;
}

.preview-muted-text {
  color: #909399;
}

.preview-large-image {
  width: 100%;
  max-width: 640px;
  height: 360px;
  background: #f5f7fa;
  border-radius: 12px;
}

.preview-card {
  border-radius: 14px;
  border: 1px solid #eef2f7;
}

.preview-card-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.preview-card-image {
  width: 100%;
  height: 220px;
  background: #f5f7fa;
  border-radius: 12px;
}

.preview-card-error {
  padding-top: 90px;
  text-align: center;
  color: #909399;
}

.preview-card-foot {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
}

:deep(.detail-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.detail-card .el-card__body) {
  padding: 18px;
}

:deep(.detail-table th.el-table__cell) {
  background: #f8fafc;
}

:deep(.detail-descriptions) {
  border-radius: 10px;
  overflow: hidden;
}

:deep(.report-form .el-input__wrapper),
:deep(.report-form .el-textarea__inner) {
  border-radius: var(--detail-control-radius);
}

:deep(.detail-btn),
:deep(.doctor-detail-page .el-tag) {
  border-radius: var(--detail-control-radius);
}
</style>
