<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getPatientProfileApi, updatePatientPasswordApi, updatePatientProfileApi } from '@/api/patient'

const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)

const form = reactive({
  gender: '',
  age: null,
  birthday: '',
  idCard: '',
  medicalRecordNo: '',
  address: '',
  remark: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

function calculateAgeByBirthday(birthday) {
  if (!birthday) return null

  const birth = new Date(`${birthday}T00:00:00`)
  if (Number.isNaN(birth.getTime())) return null

  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  const monthDiff = now.getMonth() - birth.getMonth()
  const dayDiff = now.getDate() - birth.getDate()
  if (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0)) {
    age -= 1
  }
  return age < 0 ? 0 : age
}

const ageText = computed(() => {
  if (!form.birthday) return '-'
  const age = calculateAgeByBirthday(form.birthday)
  return age === null ? '-' : `${age}`
})

watch(
  () => form.birthday,
  (birthday) => {
    form.age = calculateAgeByBirthday(birthday)
  }
)

async function loadProfile() {
  loading.value = true
  try {
    const data = await getPatientProfileApi()
    Object.assign(form, data)
    form.age = calculateAgeByBirthday(form.birthday)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updatePatientProfileApi({
      gender: form.gender,
      birthday: form.birthday,
      idCard: form.idCard,
      address: form.address,
      remark: form.remark
    })
    ElMessage.success('资料更新成功')
    await loadProfile()
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
    await updatePatientPasswordApi({
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

onMounted(loadProfile)
</script>

<template>
  <div class="patient-profile-page" v-loading="loading">
    <el-card class="patient-panel-card patient-panel-card--mb">
      <template #header>
        <div class="panel-head">
          <span>个人资料</span>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </div>
      </template>

      <el-form label-width="150px">
        <el-form-item label="性别">
          <el-select v-model="form.gender" class="profile-short-input" placeholder="请选择性别">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
          </el-select>
        </el-form-item>

        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>

        <el-form-item label="年龄">
          <el-input :model-value="ageText" disabled class="profile-short-input" />
        </el-form-item>

        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" />
        </el-form-item>

        <el-form-item label="病历号">
          <el-input v-model="form.medicalRecordNo" disabled />
          <div class="muted-tip">病历号由系统自动生成，不支持手动修改。</div>
        </el-form-item>

        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="patient-panel-card">
      <template #header>
        <div class="panel-head">
          <span>修改密码</span>
          <el-button type="warning" :loading="passwordSaving" @click="savePassword">确认修改</el-button>
        </div>
      </template>

      <el-form label-width="150px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.patient-profile-page {
  display: block;
}

.patient-panel-card {
  border-radius: 16px;
  border: 1px solid #e6edf7;
  overflow: hidden;
}

.patient-panel-card--mb {
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-short-input {
  width: 220px;
}

.muted-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

:deep(.patient-panel-card .el-card__header) {
  padding: 14px 18px;
  background: #f8fafc;
}

:deep(.patient-panel-card .el-card__body) {
  padding: 18px;
}
</style>
