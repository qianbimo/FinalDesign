<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getPatientProfileApi, updatePatientProfileApi } from '@/api/patient'

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  gender: '',
  age: null,
  birthday: '',
  idCard: '',
  medicalRecordNo: '',
  address: '',
  remark: ''
})

function calculateAgeByBirthday(birthday) {
  if (!birthday) return null

  const birth = new Date(`${birthday}T00:00:00`)
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

onMounted(loadProfile)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>患者资料</span>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </div>
    </template>

    <el-form label-width="160px">
      <el-form-item label="性别">
        <el-select v-model="form.gender" style="width: 220px">
          <el-option label="男" value="MALE" />
          <el-option label="女" value="FEMALE" />
        </el-select>
      </el-form-item>

      <el-form-item label="出生日期">
        <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>

      <el-form-item label="年龄">
        <el-input :model-value="ageText" disabled style="width: 220px" />
      </el-form-item>

      <el-form-item label="身份证号">
        <el-input v-model="form.idCard" />
      </el-form-item>

      <el-form-item label="病历号">
        <el-input v-model="form.medicalRecordNo" disabled />
        <div style="font-size: 12px; color: #909399; margin-top: 4px">病历号由系统自动生成，不支持手动修改。</div>
      </el-form-item>

      <el-form-item label="地址">
        <el-input v-model="form.address" />
      </el-form-item>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" />
      </el-form-item>
    </el-form>
  </el-card>
</template>
