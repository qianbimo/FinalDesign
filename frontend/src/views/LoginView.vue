<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: 'Please input username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please input password', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await loginApi({ ...form })
    authStore.setSession(data)
    ElMessage.success('Login success')
    router.push('/app/workspace')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="hover">
      <h2 class="title">Lung Nodule Intelligence Platform</h2>
      <p class="subtitle">Login to continue</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Input username" />
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="Input password" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="w-full" @click="onSubmit">Login</el-button>
      </el-form>

      <div class="footer-link">
        <span>No account yet?</span>
        <el-link type="primary" @click="router.push('/register')">Register now</el-link>
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
  width: 420px;
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
