<script setup>
import { onMounted, reactive, ref } from 'vue'
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
  allergyHistory: '',
  pastHistory: '',
  familyHistory: '',
  remark: ''
})

async function loadProfile() {
  loading.value = true
  try {
    const data = await getPatientProfileApi()
    Object.assign(form, data)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    await updatePatientProfileApi({ ...form })
    ElMessage.success('Profile updated')
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
        <span>Patient Profile</span>
        <el-button type="primary" :loading="saving" @click="saveProfile">Save</el-button>
      </div>
    </template>

    <el-form label-width="160px">
      <el-form-item label="Gender">
        <el-select v-model="form.gender" style="width: 220px">
          <el-option label="Male" value="MALE" />
          <el-option label="Female" value="FEMALE" />
        </el-select>
      </el-form-item>
      <el-form-item label="Age">
        <el-input-number v-model="form.age" :min="0" :max="130" />
      </el-form-item>
      <el-form-item label="Birthday">
        <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="ID Card"><el-input v-model="form.idCard" /></el-form-item>
      <el-form-item label="Medical Record No"><el-input v-model="form.medicalRecordNo" /></el-form-item>
      <el-form-item label="Address"><el-input v-model="form.address" /></el-form-item>
      <el-form-item label="Allergy History"><el-input v-model="form.allergyHistory" type="textarea" /></el-form-item>
      <el-form-item label="Past History"><el-input v-model="form.pastHistory" type="textarea" /></el-form-item>
      <el-form-item label="Family History"><el-input v-model="form.familyHistory" type="textarea" /></el-form-item>
      <el-form-item label="Remark"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
  </el-card>
</template>
