<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createRegistrationApi } from '@/api/registration'

const loading = ref(false)
const form = reactive({
  doctorId: null,
  appointmentTime: '',
  description: ''
})

async function submit() {
  if (!form.doctorId || !form.appointmentTime) {
    ElMessage.warning('Doctor ID and appointment time are required')
    return
  }
  loading.value = true
  try {
    const data = await createRegistrationApi({
      doctorId: form.doctorId,
      appointmentTime: form.appointmentTime,
      description: form.description
    })
    ElMessage.success(`Appointment submitted, ID: ${data.id}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>Appointment Request</template>
    <el-form label-width="180px">
      <el-form-item label="Doctor Profile ID">
        <el-input-number v-model="form.doctorId" :min="1" />
      </el-form-item>
      <el-form-item label="Appointment Time">
        <el-date-picker
          v-model="form.appointmentTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="Select appointment time"
        />
      </el-form-item>
      <el-form-item label="Description">
        <el-input v-model="form.description" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">Submit</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
