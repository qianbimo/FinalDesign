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
      <el-form-item label="年龄">
        <el-input-number v-model="form.age" :min="0" :max="130" />
      </el-form-item>
      <el-form-item label="出生日期">
        <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item>
      <el-form-item label="病历号"><el-input v-model="form.medicalRecordNo" /></el-form-item>
      <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
  </el-card>
</template>
