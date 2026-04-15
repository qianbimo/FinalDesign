<script setup>
import { ref } from 'vue'
import { getAnnotationByStudyApi } from '@/api/annotation'

const loading = ref(false)
const studyId = ref(null)
const data = ref(null)
const riskLevelMap = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High'
}
const viewTypeMap = {
  AXIAL: 'Axial',
  CORONAL: 'Coronal',
  SAGITTAL: 'Sagittal',
  THREE_D: '3D'
}

function riskLevelText(level) {
  return riskLevelMap[level] || level
}

function viewTypeText(viewType) {
  return viewTypeMap[viewType] || viewType
}

async function query() {
  if (!studyId.value) return
  loading.value = true
  try {
    data.value = await getAnnotationByStudyApi(studyId.value)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>Annotation Viewer</template>
    <el-form inline>
      <el-form-item label="Study ID">
        <el-input-number v-model="studyId" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="query">Query</el-button>
      </el-form-item>
    </el-form>

    <div v-if="data" style="margin-top: 16px">
      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="Study ID">{{ data.studyId }}</el-descriptions-item>
        <el-descriptions-item label="Segmentation Path">{{ data.segmentationPath }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>CT Files</el-divider>
      <el-table :data="data.ctFiles || []">
        <el-table-column prop="id" label="File ID" width="120" />
        <el-table-column prop="fileName" label="File Name" min-width="220" />
        <el-table-column prop="fileType" label="Type" width="120" />
      </el-table>

      <el-divider>Nodules</el-divider>
      <el-table :data="data.nodules || []">
        <el-table-column prop="noduleNo" label="No" width="80" />
        <el-table-column prop="diameterMm" label="Diameter (mm)" width="140" />
        <el-table-column prop="malignancyProb" label="Malignancy Prob" width="140" />
        <el-table-column label="Risk Level" width="120">
          <template #default="scope">{{ riskLevelText(scope.row.riskLevel) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="220" />
      </el-table>

      <el-divider>Annotations</el-divider>
      <el-table :data="data.annotations || []">
        <el-table-column label="View" width="120">
          <template #default="scope">{{ viewTypeText(scope.row.viewType) }}</template>
        </el-table-column>
        <el-table-column prop="overlayPath" label="Overlay Path" min-width="320" />
        <el-table-column prop="color" label="Color" width="120" />
      </el-table>
    </div>

    <el-empty v-else description="Input study ID and query" />
  </el-card>
</template>
