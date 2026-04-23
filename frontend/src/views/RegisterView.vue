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
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
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
    callback(new Error('手机号格式不正确，应为 11 位数字'))
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
    callback(new Error('邮箱格式不正确'))
    return
  }
  callback()
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
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
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-shell">
    <section class="register-info">
      <div class="register-info__card">
        <span class="register-chip">患者注册入口</span>
        <h1>先建立患者账号，再开始你的挂号和检查流程</h1>
        <p>
          公开注册仅面向患者。
        </p>

        <div class="register-steps">
          <article>
            <strong>01</strong>
            <span>创建账号并填写姓名、联系方式等基础信息</span>
          </article>
          <article>
            <strong>02</strong>
            <span>进入患者端提交挂号申请，选择医生和预约时间</span>
          </article>
          <article>
            <strong>03</strong>
            <span>在检查记录中持续查看 AI 结果和最终报告</span>
          </article>
        </div>
      </div>
    </section>

    <section class="register-form-wrap">
      <div class="register-card">
        <div class="register-card__head">
          <h2>创建患者账号</h2>
          <p>完成注册后即可进入患者角色页面</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="register-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入密码"
              />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="选填" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填" />
            </el-form-item>
          </div>

          <el-button type="primary" class="register-submit" :loading="loading" @click="onSubmit">完成注册</el-button>
        </el-form>

        <div class="register-card__foot">
          <span>已有账号？</span>
          <el-link type="primary" @click="router.push('/login')">返回登录</el-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.register-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background:
    radial-gradient(circle at 10% 18%, rgba(123, 221, 188, 0.2), transparent 24%),
    radial-gradient(circle at 84% 22%, rgba(103, 170, 255, 0.16), transparent 22%),
    linear-gradient(145deg, #f7fcfb 0%, #eef5ff 56%, #fffaf0 100%);
}

.register-info,
.register-form-wrap {
  padding: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.register-info__card {
  max-width: 620px;
}

.register-chip {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  color: #12584a;
  background: rgba(200, 241, 229, 0.92);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.register-info__card h1 {
  margin: 20px 0 14px;
  font-size: 46px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.register-info__card p {
  margin: 0;
  color: #61718f;
  line-height: 1.9;
}

.register-steps {
  margin-top: 28px;
  display: grid;
  gap: 14px;
}

.register-steps article {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 14px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 36px rgba(21, 34, 62, 0.08);
}

.register-steps strong {
  width: 72px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  color: #17475f;
  background: linear-gradient(135deg, #ffd79d, #cdefff 72%);
  font-size: 18px;
  letter-spacing: 0.08em;
}

.register-steps span {
  line-height: 1.75;
  color: #36445d;
}

.register-card {
  width: min(620px, 100%);
  padding: 28px;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(18px);
  box-shadow: 0 28px 60px rgba(20, 31, 57, 0.14);
}

.register-card__head h2 {
  margin: 0;
  font-size: 30px;
}

.register-card__head p {
  margin: 10px 0 20px;
  color: #61718f;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.register-submit {
  width: 100%;
  height: 48px;
  margin-top: 8px;
}

.register-card__foot {
  margin-top: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: #61718f;
}

@media (max-width: 980px) {
  .register-shell {
    grid-template-columns: 1fr;
  }

  .register-info__card h1 {
    font-size: 36px;
  }
}

@media (max-width: 700px) {
  .register-info,
  .register-form-wrap {
    padding: 18px;
  }

  .register-grid {
    grid-template-columns: 1fr;
  }

  .register-info__card h1 {
    font-size: 30px;
  }

  .register-card {
    padding: 22px;
  }
}
</style>
