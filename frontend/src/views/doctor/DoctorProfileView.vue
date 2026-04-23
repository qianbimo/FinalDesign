<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDoctorProfileApi, updateDoctorPasswordApi, updateDoctorProfileApi } from '@/api/doctor'

const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)

const form = reactive({
  department: '',
  title: '',
  licenseNo: '',
  introduction: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
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

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写密码信息')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  passwordSaving.value = true
  try {
    await updateDoctorPasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码修改成功，请使用新密码登录')
  } finally {
    passwordSaving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <el-card class="fd-form-card doctor-profile-card" v-loading="loading">
      <div class="section-head">
        <div>
          <h2 class="section-title">资料编辑</h2>
          <p class="section-subtitle">请完善科室、职称、执业证号和个人简介。</p>
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

    <el-card class="fd-form-card doctor-profile-card doctor-profile-card--mt">
      <div class="section-head">
        <div>
          <h2 class="section-title">修改密码</h2>
          <p class="section-subtitle">请输入原密码，并设置新的登录密码。</p>
        </div>
      </div>

      <el-form label-width="110px">
        <el-row :gutter="18">
          <el-col :xs="24" :md="12">
            <el-form-item label="原密码">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button type="warning" :loading="passwordSaving" @click="savePassword">确认修改</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.doctor-profile-card {
  border-radius: 16px;
  border: 1px solid #e6edf7;
}

.doctor-profile-card--mt {
  margin-top: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>
