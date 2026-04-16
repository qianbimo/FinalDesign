<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createRegistrationApi, getRegistrationDoctorsApi } from '@/api/registration'

const loading = ref(false)
const doctorsLoading = ref(false)
const doctorOptions = ref([])
const form = reactive({
  doctorId: '',
  appointmentTime: '',
  description: ''
})

function doctorLabel(doctor) {
  const name = doctor.realName || '未命名医生'
  const title = doctor.title || '未填写职称'
  const department = doctor.department || '未填写科室'
  return `${name}｜${title}｜${department}`
}

async function loadDoctors() {
  doctorsLoading.value = true
  try {
    const data = await getRegistrationDoctorsApi()
    doctorOptions.value = data || []
    if (!form.doctorId && doctorOptions.value.length > 0) {
      form.doctorId = doctorOptions.value[0].doctorId
    }
  } finally {
    doctorsLoading.value = false
  }
}

async function submit() {
  if (!form.doctorId || !form.appointmentTime) {
    ElMessage.warning('请选择医生并填写预约时间')
    return
  }
  loading.value = true
  try {
    const data = await createRegistrationApi({
      doctorId: form.doctorId,
      appointmentTime: form.appointmentTime,
      description: form.description
    })
    ElMessage.success(`挂号申请已提交，记录 编号：${data.id}`)
  } finally {
    loading.value = false
  }
}

onMounted(loadDoctors)
</script>

<template>
  <el-card>
    <template #header>挂号申请</template>
    <el-form label-width="180px">
      <el-form-item label="选择医生">
        <el-select
          v-model="form.doctorId"
          filterable
          clearable
          :loading="doctorsLoading"
          placeholder="请选择医生（姓名｜职称｜科室）"
          style="width: 420px"
        >
          <el-option
            v-for="doctor in doctorOptions"
            :key="doctor.doctorId"
            :label="doctorLabel(doctor)"
            :value="doctor.doctorId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="预约时间">
        <el-date-picker
          v-model="form.appointmentTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="请选择预约时间"
        />
      </el-form-item>
      <el-form-item label="病情描述">
        <el-input v-model="form.description" type="textarea" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交申请</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
