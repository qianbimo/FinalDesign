<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Please confirm password'))
    return
  }
  if (value !== form.password) {
    callback(new Error('Passwords are not the same'))
    return
  }
  callback()
}

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('Phone should be 11 digits and start with 1'))
    return
  }
  callback()
}

const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('Invalid email format'))
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: 'Please input username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please input password', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [{ required: true, message: 'Please input real name', trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await registerApi({
      username: form.username,
      password: form.password,
      role: 'PATIENT',
      realName: form.realName,
      phone: form.phone,
      email: form.email
    })
    ElMessage.success('Register success, please login')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2 class="title">Patient Register</h2>
      <p class="subtitle">Public registration is only for PATIENT role</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Input username" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="Input password" />
        </el-form-item>
        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="Input password again"
          />
        </el-form-item>
        <el-form-item label="Real Name" prop="realName">
          <el-input v-model="form.realName" placeholder="Input real name" />
        </el-form-item>
        <el-form-item label="Phone" prop="phone">
          <el-input v-model="form.phone" placeholder="Optional" />
        </el-form-item>
        <el-form-item label="Email" prop="email">
          <el-input v-model="form.email" placeholder="Optional" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="w-full" @click="onSubmit">Register</el-button>
      </el-form>

      <div class="footer-link">
        <span>Already have an account?</span>
        <el-link type="primary" @click="router.push('/login')">Back to Login</el-link>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.auth-card {
  width: 460px;
  border-radius: 14px;
}

.title {
  margin: 0;
  font-size: 24px;
}

.subtitle {
  margin: 8px 0 20px;
  color: #6b7280;
}

.w-full {
  width: 100%;
}

.footer-link {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #6b7280;
}
</style>
