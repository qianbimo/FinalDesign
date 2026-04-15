<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAdminUserApi,
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
  PATIENT: 'Patient',
  DOCTOR: 'Doctor',
  ADMIN: 'Admin'
}

function roleText(role) {
  return roleMap[role] || role
}

function statusText(status) {
  return status === 1 ? 'Enabled' : 'Disabled'
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
    ElMessage.warning('Username, password and real name are required')
    return
  }
  createLoading.value = true
  try {
    await createAdminUserApi({ ...createForm })
    ElMessage.success('User created')
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
  ElMessage.success('Status updated')
  await loadData()
}

async function resetPassword(row) {
  await resetAdminUserPasswordApi(row.id, '123456')
  ElMessage.success(`Password reset to 123456: ${row.username}`)
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>User Management</span>
        <el-button type="primary" @click="createDialogVisible = true">Create User</el-button>
      </div>
    </template>

    <el-form inline>
      <el-form-item label="Role">
        <el-select v-model="filters.role" clearable style="width: 160px">
          <el-option label="Patient" value="PATIENT" />
          <el-option label="Doctor" value="DOCTOR" />
          <el-option label="Admin" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="Status">
        <el-select v-model="filters.status" clearable style="width: 140px">
          <el-option label="Enabled (1)" :value="1" />
          <el-option label="Disabled (0)" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="Keyword">
        <el-input v-model="filters.keyword" placeholder="username/realName/phone/email" style="width: 260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="() => { pager.current = 1; loadData() }">Search</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading">
      <el-table-column prop="id" label="ID" width="120" />
      <el-table-column prop="username" label="Username" min-width="140" />
      <el-table-column prop="realName" label="Real Name" min-width="140" />
      <el-table-column label="Role" width="120">
        <template #default="scope">{{ roleText(scope.row.role) }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="Phone" min-width="140" />
      <el-table-column prop="email" label="Email" min-width="180" />
      <el-table-column label="Status" width="100">
        <template #default="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column prop="profileId" label="Profile ID" width="120" />
      <el-table-column label="Action" min-width="220">
        <template #default="scope">
          <el-button link type="primary" @click="toggleStatus(scope.row)">
            {{ scope.row.status === 1 ? 'Disable' : 'Enable' }}
          </el-button>
          <el-button link type="warning" @click="resetPassword(scope.row)">Reset Password</el-button>
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

  <el-dialog v-model="createDialogVisible" title="Create User" width="520px">
    <el-form label-width="130px">
      <el-form-item label="Username"><el-input v-model="createForm.username" /></el-form-item>
      <el-form-item label="Password"><el-input v-model="createForm.password" type="password" show-password /></el-form-item>
      <el-form-item label="Role">
        <el-select v-model="createForm.role" style="width: 100%">
          <el-option label="Patient" value="PATIENT" />
          <el-option label="Doctor" value="DOCTOR" />
          <el-option label="Admin" value="ADMIN" />
        </el-select>
      </el-form-item>
      <el-form-item label="Real Name"><el-input v-model="createForm.realName" /></el-form-item>
      <el-form-item label="Phone"><el-input v-model="createForm.phone" /></el-form-item>
      <el-form-item label="Email"><el-input v-model="createForm.email" /></el-form-item>
      <el-form-item label="Status">
        <el-select v-model="createForm.status" style="width: 100%">
          <el-option label="Enabled (1)" :value="1" />
          <el-option label="Disabled (0)" :value="0" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialogVisible = false">Cancel</el-button>
      <el-button type="primary" :loading="createLoading" @click="createUser">Create</el-button>
    </template>
  </el-dialog>
</template>
