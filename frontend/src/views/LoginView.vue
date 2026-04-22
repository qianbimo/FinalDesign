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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await loginApi({ ...form })
    authStore.setSession(data)
    ElMessage.success('登录成功')
    router.push('/app/workspace')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <section class="auth-showcase">
      <div class="showcase-inner">
        <span class="showcase-chip">肺结节智能分析系统</span>
        <h1>一个同时服务患者、医生与管理员的完整影像分析平台</h1>
        <p>
          从预约到 CT 上传，从 AI 分析到报告审核，整条业务链路在同一平台内完成，并为不同角色提供清晰的工作界面。
        </p>

        <div class="showcase-grid">
          <article>
            <strong>患者端</strong>
            <span>挂号申请、进度查看、报告回看</span>
          </article>
          <article>
            <strong>医生端</strong>
            <span>确认挂号、上传 CT、启动分析、审核报告</span>
          </article>
          <article>
            <strong>管理员端</strong>
            <span>用户治理、报告总览与平台总控</span>
          </article>
        </div>
      </div>
    </section>

    <section class="auth-form-wrap">
      <div class="auth-card">
        <div class="auth-card__head">
          <h2>登录系统</h2>
          <p>登录后会自动进入当前角色对应的正式工作台</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-button type="primary" class="auth-submit" :loading="loading" @click="onSubmit">进入平台</el-button>
        </el-form>

        <div class="auth-card__foot">
          <span>没有账号？</span>
          <el-link type="primary" @click="router.push('/register')">患者注册</el-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  background:
    radial-gradient(circle at 8% 14%, rgba(255, 199, 110, 0.22), transparent 24%),
    radial-gradient(circle at 88% 18%, rgba(89, 176, 255, 0.18), transparent 22%),
    linear-gradient(140deg, #fff8ec 0%, #f2f7ff 52%, #eef9f4 100%);
}

.auth-showcase,
.auth-form-wrap {
  padding: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.showcase-inner {
  max-width: 700px;
}

.showcase-chip {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  color: #18456f;
  background: rgba(208, 233, 255, 0.84);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.showcase-inner h1 {
  margin: 20px 0 14px;
  font-size: 48px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.showcase-inner p {
  margin: 0;
  color: #61718f;
  font-size: 16px;
  line-height: 1.9;
}

.showcase-grid {
  margin-top: 30px;
  display: grid;
  gap: 14px;
}

.showcase-grid article {
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.74);
  box-shadow: 0 18px 36px rgba(21, 34, 62, 0.08);
}

.showcase-grid strong {
  display: block;
  font-size: 18px;
}

.showcase-grid span {
  display: block;
  margin-top: 8px;
  color: #61718f;
  line-height: 1.7;
}

.auth-card {
  width: min(470px, 100%);
  padding: 28px;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(18px);
  box-shadow: 0 28px 60px rgba(20, 31, 57, 0.14);
}

.auth-card__head h2 {
  margin: 0;
  font-size: 30px;
}

.auth-card__head p {
  margin: 10px 0 22px;
  color: #61718f;
}

.auth-submit {
  width: 100%;
  height: 48px;
  margin-top: 6px;
}

.auth-card__foot {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #61718f;
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .showcase-inner h1 {
    font-size: 38px;
  }
}

@media (max-width: 640px) {
  .auth-showcase,
  .auth-form-wrap {
    padding: 18px;
  }

  .showcase-inner h1 {
    font-size: 30px;
  }

  .auth-card {
    padding: 22px;
  }
}
</style>
