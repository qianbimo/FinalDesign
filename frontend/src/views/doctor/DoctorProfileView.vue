<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDoctorProfileApi, updateDoctorProfileApi } from '@/api/doctor'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  department: '',
  title: '',
  licenseNo: '',
  introduction: ''
})

function fillForm(profile) {
  form.department = profile?.department || ''
  form.title = profile?.title || ''
  form.licenseNo = profile?.licenseNo || ''
  form.introduction = profile?.introduction || ''
}

async function loadData() {
  loading.value = true
  try {
    const profile = await getDoctorProfileApi()
    fillForm(profile)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  saving.value = true
  try {
    await updateDoctorProfileApi({ ...form })
    ElMessage.success('医生资料已更新')
    await loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="page-hero hero-doctor">
      <span class="page-hero__eyebrow">医生资料</span>
      <h1 class="page-hero__title">维护当前医生账户的科室与执业信息</h1>
      <p class="page-hero__desc">
        这里用于维护医生展示资料。更新后，挂号选择、病例关联和医生端资料展示都会同步使用最新信息。
      </p>
    </section>

    <el-card class="fd-form-card" v-loading="loading">
      <div class="section-head">
        <div>
          <h2 class="section-title">资料编辑</h2>
          <p class="section-subtitle">请填写医生所属科室、职称、执业证号与个人简介。</p>
        </div>
      </div>

      <el-form label-width="110px" @submit.prevent="handleSubmit">
        <el-row :gutter="18">
          <el-col :xs="24" :md="12">
            <el-form-item label="所属科室">
              <el-input v-model="form.department" placeholder="例如：胸外科" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="医生职称">
              <el-input v-model="form.title" placeholder="例如：副主任医师" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="执业证号">
              <el-input v-model="form.licenseNo" placeholder="请输入执业证号" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="24">
            <el-form-item label="个人简介">
              <el-input
                v-model="form.introduction"
                type="textarea"
                :rows="5"
                maxlength="500"
                show-word-limit
                placeholder="请输入医生简介，例如从业年限、临床方向、门诊安排等"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button class="soft-action" @click="loadData">重新加载</el-button>
          <el-button type="primary" :loading="saving" @click="handleSubmit">保存资料</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>
