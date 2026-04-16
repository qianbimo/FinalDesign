<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDoctorPatientStudyDetailApi } from '@/api/doctor'
import { getCtFilesByStudyApi } from '@/api/study'
import { getAiResultByStudyApi, startAiTaskApi } from '@/api/aiTask'
import { getAnnotationByStudyApi } from '@/api/annotation'
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
const studyStatusMap = {
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
const viewTypeMap = {
  AXIAL: '轴位',
  CORONAL: '冠状位',
  SAGITTAL: '矢状位',
  THREE_D: '三维'
}

function studyStatusText(status) {
  return studyStatusMap[status] || '未知状态'
}

function taskStatusText(status) {
  return taskStatusMap[status] || '未知状态'
}

function viewTypeText(viewType) {
  return viewTypeMap[viewType] || '未知视图'
}

const annotationImages = computed(() => annotationData.value?.annotations || [])

const originalPreviewUrl = computed(() => {
  const imageFile = files.value.find((item) => isImageFileType(item.fileType) || isImagePath(item.filePath))
  if (imageFile) return toFileUrl(imageFile.filePath)
  return toFileUrl(`result/${studyId}/original_preview.png`)
})

function filePreviewUrl(file) {
  return toFileUrl(file.filePath)
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
  } finally {
    loading.value = false
  }
}

async function startAi() {
  aiLoading.value = true
  try {
    const data = await startAiTaskApi(studyId)
    ElMessage.success(`智能分析任务已启动，任务编号：${data.taskId}`)
    await loadData()
  } finally {
    aiLoading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>病例详情</span>
          <el-button type="success" :loading="aiLoading" @click="startAi">启动智能分析</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检查编号">{{ study?.id }}</el-descriptions-item>
        <el-descriptions-item label="检查编号">{{ study?.studyNo }}</el-descriptions-item>
        <el-descriptions-item label="患者编号">{{ study?.patientId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ studyStatusText(study?.status) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>影像文件</template>
      <el-table :data="files">
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
          <el-empty description="暂无可用原图预览（若上传 NII/DCM，可由后续切片服务提供）" />
        </template>
      </el-image>
    </el-card>

    <el-card style="margin-bottom: 16px">
      <template #header>标注叠加图</template>
      <el-empty v-if="annotationImages.length === 0" description="暂无标注叠加图" />
      <el-row v-else :gutter="16">
        <el-col v-for="item in annotationImages" :key="item.id" :xs="24" :sm="12" :lg="8" style="margin-bottom: 16px">
          <el-card shadow="hover">
            <div style="font-weight: 600; margin-bottom: 8px">{{ viewTypeText(item.viewType) }}</div>
            <el-image
              :src="toFileUrl(item.overlayPath)"
              :preview-src-list="[toFileUrl(item.overlayPath)]"
              fit="cover"
              style="width: 100%; height: 180px; background: #f5f7fa"
            >
              <template #error>
                <div style="padding-top: 70px; text-align: center; color: #909399">标注图加载失败</div>
              </template>
            </el-image>
            <div style="margin-top: 8px; color: #606266; font-size: 12px">{{ item.overlayPath }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <template #header>智能分析结果概览</template>
      <el-empty v-if="!aiResult" description="暂无智能分析结果" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="任务编号">{{ aiResult?.task?.id }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">{{ taskStatusText(aiResult?.task?.taskStatus) }}</el-descriptions-item>
        <el-descriptions-item label="结节数量">{{ aiResult?.nodules?.length || 0 }}</el-descriptions-item>
        <el-descriptions-item label="标注数量">{{ aiResult?.annotations?.length || 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>
