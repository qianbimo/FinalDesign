<script setup>
import { ref } from 'vue'
import { getAnnotationByStudyApi } from '@/api/annotation'
import { toFileUrl } from '@/utils/fileUrl'

const loading = ref(false)
const studyId = ref(null)
const data = ref(null)
const riskLevelMap = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}
const viewTypeMap = {
  AXIAL: '轴位',
  CORONAL: '冠状位',
  SAGITTAL: '矢状位',
  THREE_D: '三维'
}

function riskLevelText(level) {
  return riskLevelMap[level] || '未知风险'
}

function viewTypeText(viewType) {
  return viewTypeMap[viewType] || '未知视图'
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
    <template #header>标注查看</template>
    <el-form inline>
      <el-form-item label="检查编号">
        <el-input-number v-model="studyId" :min="1" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="query">查询</el-button>
      </el-form-item>
    </el-form>

    <div v-if="data" style="margin-top: 16px">
      <el-descriptions :column="2" border style="margin-bottom: 16px">
        <el-descriptions-item label="检查编号">{{ data.studyId }}</el-descriptions-item>
        <el-descriptions-item label="分割结果路径">{{ data.segmentationPath }}</el-descriptions-item>
      </el-descriptions>

      <el-divider>影像文件</el-divider>
      <el-table :data="data.ctFiles || []">
        <el-table-column prop="id" label="文件编号" width="120" />
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="120" />
      </el-table>

      <el-divider>结节列表</el-divider>
      <el-table :data="data.nodules || []">
        <el-table-column prop="noduleNo" label="编号" width="80" />
        <el-table-column prop="diameterMm" label="直径（毫米）" width="140" />
        <el-table-column prop="malignancyProb" label="恶性概率" width="140" />
        <el-table-column label="风险等级" width="120">
          <template #default="scope">{{ riskLevelText(scope.row.riskLevel) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="220" />
      </el-table>

      <el-divider>标注信息</el-divider>
      <el-table :data="data.annotations || []">
        <el-table-column label="视图" width="120">
          <template #default="scope">{{ viewTypeText(scope.row.viewType) }}</template>
        </el-table-column>
        <el-table-column prop="overlayPath" label="叠加图路径" min-width="320" />
        <el-table-column prop="color" label="颜色" width="120" />
        <el-table-column label="预览" width="220">
          <template #default="scope">
            <el-image
              :src="toFileUrl(scope.row.overlayPath)"
              :preview-src-list="[toFileUrl(scope.row.overlayPath)]"
              fit="cover"
              style="width: 160px; height: 90px"
            >
              <template #error>
                <div style="font-size: 12px; color: #909399">图片加载失败</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-empty v-else description="请输入检查编号 后查询" />
  </el-card>
</template>
