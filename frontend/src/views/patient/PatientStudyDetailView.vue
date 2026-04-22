<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPatientAiResultApi, getPatientReportApi, getPatientStudyDetailApi } from '@/api/patient'
import { getCtFilesByStudyApi } from '@/api/study'
import { getAnnotationByStudyApi } from '@/api/annotation'
import { isImageFileType, isImagePath, toFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const studyId = route.params.studyId

const loading = ref(false)
const study = ref(null)
const aiTask = ref(null)
const report = ref(null)
const ctFiles = ref([])
const annotationData = ref(null)

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

const originalPreviewUrl = computed(() => {
  const aiPreviewPath = annotationData.value?.originalPreviewPath
  if (aiPreviewPath) return toFileUrl(aiPreviewPath)

  const imageFile = ctFiles.value.find((item) => isImageFileType(item.fileType) || isImagePath(item.filePath))
  if (imageFile) return toFileUrl(imageFile.filePath)

  return toFileUrl(`result/${studyId}/original_preview.png`)
})

const annotatedPreviewUrl = computed(() => {
  const path = annotationData.value?.annotatedPreviewPath || `result/${studyId}/pipeline_annotated.png`
  return toFileUrl(path)
})

const overlayPreviewUrl = computed(() => {
  const path = annotationData.value?.overlayPreviewPath || `result/${studyId}/pipeline_overlay.png`
  return toFileUrl(path)
})

function filePreviewUrl(file) {
  return toFileUrl(file.filePath)
}

async function loadData() {
  loading.value = true
  try {
    study.value = await getPatientStudyDetailApi(studyId)

    try {
      ctFiles.value = await getCtFilesByStudyApi(studyId)
    } catch (error) {
      ctFiles.value = []
    }

    try {
      aiTask.value = await getPatientAiResultApi(studyId)
    } catch (error) {
      aiTask.value = null
    }

    try {
      annotationData.value = await getAnnotationByStudyApi(studyId)
    } catch (error) {
      annotationData.value = null
    }

    try {
      report.value = await getPatientReportApi(studyId)
    } catch (error) {
      report.value = null
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>检查详情</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编号">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="检查日期">{{ study?.studyDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ studyStatusText(study?.status) }}</el-descriptions-item>
        <el-descriptions-item label="设备信息">{{ study?.deviceInfo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="检查描述">{{ study?.studyDesc || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>影像文件</template>
      <el-table :data="ctFiles">
        <el-table-column prop="id" label="文件编号" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="120" />
        <el-table-column label="预览" width="220">
          <template #default="scope">
            <el-image
              v-if="isImageFileType(scope.row.fileType) || isImagePath(scope.row.filePath)"
              :src="filePreviewUrl(scope.row)"
              :preview-src-list="[filePreviewUrl(scope.row)]"
              fit="cover"
              style="width: 160px; height: 90px"
            >
              <template #error>
                <div style="font-size: 12px; color: #909399">图片加载失败</div>
              </template>
            </el-image>
            <span v-else style="color: #909399">体数据文件，暂不直接预览</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>原图预览</template>
      <el-image
        :src="originalPreviewUrl"
        :preview-src-list="[originalPreviewUrl]"
        fit="contain"
        style="width: 100%; max-width: 640px; height: 360px; background: #f5f7fa"
      >
        <template #error>
          <el-empty description="暂无可用原图预览" />
        </template>
      </el-image>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>标注与叠加图</template>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <div style="font-weight: 600; margin-bottom: 8px">标注图</div>
            <el-image
              :src="annotatedPreviewUrl"
              :preview-src-list="[annotatedPreviewUrl]"
              fit="cover"
              style="width: 100%; height: 220px; background: #f5f7fa"
            >
              <template #error>
                <div style="padding-top: 90px; text-align: center; color: #909399">标注图加载失败（pipeline_annotated.png）</div>
              </template>
            </el-image>
            <div style="margin-top: 8px; color: #606266; font-size: 12px">pipeline_annotated.png</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="hover">
            <div style="font-weight: 600; margin-bottom: 8px">叠加图</div>
            <el-image
              :src="overlayPreviewUrl"
              :preview-src-list="[overlayPreviewUrl]"
              fit="cover"
              style="width: 100%; height: 220px; background: #f5f7fa"
            >
              <template #error>
                <div style="padding-top: 90px; text-align: center; color: #909399">叠加图加载失败（pipeline_overlay.png）</div>
              </template>
            </el-image>
            <div style="margin-top: 8px; color: #606266; font-size: 12px">pipeline_overlay.png</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>智能分析结果</template>
      <el-empty v-if="!aiTask" description="暂无智能分析任务" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="任务编号">{{ aiTask?.id }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ taskStatusText(aiTask?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="模型版本">{{ aiTask?.modelVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ aiTask?.finishedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card>
      <template #header>报告单</template>
      <el-empty v-if="!report" description="暂无报告" />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="标题">{{ report?.reportTitle }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ report?.reportSummary }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ reportStatusText(report?.status) }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <pre style="white-space: pre-wrap; margin: 0">{{ report?.reportContent }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
