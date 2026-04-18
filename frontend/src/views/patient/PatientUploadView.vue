<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function toRegistration() {
  router.push('/app/patient/registration')
}

function toStudies() {
  router.push('/app/patient/studies')
}
</script>

<template>
  <el-card>
    <template #header>CT上传权限说明</template>
    <el-alert
      title="患者端不支持CT上传。请先提交挂号申请，由医生确认后在医生端上传CT影像。"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-space>
      <el-button type="primary" @click="toRegistration">前往挂号申请</el-button>
      <el-button @click="toStudies">查看我的检查记录</el-button>
    </el-space>
  </el-card>
</template>
