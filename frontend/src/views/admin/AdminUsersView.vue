<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminUserApi,
  deleteAdminUserApi,
  getAdminUsersApi,
  resetAdminUserPasswordApi,
  updateAdminUserStatusApi
} from '@/api/admin'

const loading = ref(false)
const tableData = ref([])
const pager = reactive({ current: 1, size: 10, total: 0 })
const filters = reactive({ role: '', status: '', keyword: '' })

const createDialogVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  role: 'PATIENT',
  realName: '',
  phone: '',
  email: '',
  status: 1
})

const roleMap = {
  PATIENT: '患者',
  DOCTOR: '医生',
  ADMIN: '管理员'
}

function roleText(role) {
  return roleMap[role] || '未知角色'
}

function statusText(status) {
  return status === 1 ? '启用' : '禁用'
}

async function loadData() {
  loading.value = true
  try {
    const data = await getAdminUsersApi({
      current: pager.current,
      size: pager.size,
      role: filters.role || undefined,
      status: filters.status === '' ? undefined : filters.status,
      keyword: filters.keyword || undefined
    })
    tableData.value = data.records || []
    pager.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetCreateForm() {
  createForm.username = ''
  createForm.password = ''
  createForm.role = 'PATIENT'
  createForm.realName = ''
  createForm.phone = ''
  createForm.email = ''
  createForm.status = 1
}

async function createUser() {
  if (!createForm.username || !createForm.password || !createForm.realName) {
    ElMessage.warning('用户名、密码和真实姓名不能为空')
    return
  }
  createLoading.value = true
  try {
    await createAdminUserApi({ ...createForm })
    ElMessage.success('用户创建成功')
    createDialogVisible.value = false
    resetCreateForm()
    await loadData()
  } finally {
    createLoading.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  await updateAdminUserStatusApi(row.id, nextStatus)
  ElMessage.success('用户状态已更新')
  await loadData()
}

async function resetPassword(row) {
  await resetAdminUserPasswordApi(row.id, '123456')
  ElMessage.success(`已重置密码为 123456：${row.username}`)
}

async function deleteUser(row) {
  await ElMessageBox.confirm(
    `确认删除用户 ${row.username} 吗？该操作不可恢复。`,
    '删除确认',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    }
  )

  await deleteAdminUserApi(row.id)
  ElMessage.success('用户已删除')

  if (tableData.value.length === 1 && pager.current > 1) {
    pager.current -= 1
  }
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>用户管理</span>
        <el-button type="primary" @click="createDialogVisible = true">新建用户</el-button>
      </div>
    </template>

    <el-form inline>
      <el-form-item label="角色">
        <el-select v-model="filters.role" clearable style="width: 160px">
          <el-option label="患者" value="PATIENT" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable style="width: 140px">
          <el-option label="启用(1)" :value="1" />
          <el-option label="禁用(0)" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="filters.keyword" placeholder="用户名/姓名/手机号/邮箱" style="width: 260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="() => { pager.current = 1; loadData() }">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="编号" width="120" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="realName" label="真实姓名" min-width="140" />
      <el-table-column label="角色" width="120">
        <template #default="scope">{{ roleText(scope.row.role) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" min-width="260">
        <template #default="scope">
          <el-button
            link
            :type="scope.row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(scope.row)"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="resetPassword(scope.row)">重置密码</el-button>
          <el-button link type="danger" @click="deleteUser(scope.row)">删除用户</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px"
      background
      layout="total, prev, pager, next"
      :total="pager.total"
      :current-page="pager.current"
      :page-size="pager.size"
      @current-change="(p) => { pager.current = p; loadData() }"
    />
  </el-card>

  <el-dialog v-model="createDialogVisible" title="新建用户" width="520px">
    <el-form label-width="130px">
      <el-form-item label="用户名"><el-input v-model="createForm.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="createForm.password" type="password" show-password /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="createForm.role" style="width: 100%">
          <el-option label="患者" value="PATIENT" />
          <el-option label="医生" value="DOCTOR" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="真实姓名"><el-input v-model="createForm.realName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="createForm.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="createForm.email" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="createForm.status" style="width: 100%">
          <el-option label="启用(1)" :value="1" />
          <el-option label="禁用(0)" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="createLoading" @click="createUser">创建</el-button>
    </template>
  </el-dialog>
</template>
