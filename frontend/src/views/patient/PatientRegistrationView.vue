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
    ElMessage.warning('医生ID和预约时间不能为空')
    return
  }
  loading.value = true
  try {
    const data = await createRegistrationApi({
      doctorId: form.doctorId,
      appointmentTime: form.appointmentTime,
      description: form.description
    })
    ElMessage.success(`挂号申请已提交，编号：${data.id}`)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>挂号申请</template>
    <el-form label-width="160px">
      <el-form-item label="医生档案ID">
        <el-input-number v-model="form.doctorId" :min="1" />
      </el-form-item>
      <el-form-item label="预约时间">
        <el-date-picker
          v-model="form.appointmentTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="请选择预约时间"
        />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
