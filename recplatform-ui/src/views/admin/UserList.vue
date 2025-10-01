<template>
  <div class="user-list">
    <PageHeader :title="t('menu.users')" :subtitle="t('admin.manageUsers')">
      <el-button type="primary" @click="showDialog()">
        <el-icon><Plus /></el-icon>
        {{ t('button.addUser') }}
      </el-button>
    </PageHeader>

    <el-card>
      <el-table :data="users" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="username" :label="t('form.username')" min-width="140" />
        <el-table-column prop="displayName" :label="t('form.displayName')" min-width="160" />
        <el-table-column prop="email" :label="t('form.email')" min-width="200" />
        <el-table-column prop="roles" :label="t('table.roles')" width="200">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 4px;">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="t('table.status')" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.enabled ? 'ACTIVE' : 'STOPPED'" :label="row.enabled ? t('status.active') : t('status.disabled')" />
          </template>
        </el-table-column>
        <el-table-column :label="t('table.actions')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDialog(row)">{{ t('common.edit') }}</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingUser ? t('button.editUser') : t('button.addUser')" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('form.username')" prop="username">
          <el-input v-model="form.username" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item :label="t('form.displayName')" prop="displayName">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item :label="t('form.email')" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item :label="t('form.password')" prop="password" v-if="!editingUser">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('table.roles')">
          <el-select v-model="form.roles" multiple style="width: 100%">
            <el-option v-for="role in allRoles" :key="role.code" :label="role.name" :value="role.code" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('status.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import request from '@/api/index'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const loading = ref(false)
const { t } = useI18n()
const users = ref<any[]>([])
const dialogVisible = ref(false)
const editingUser = ref<any>(null)
const formRef = ref<FormInstance>()

const allRoles = ref<any[]>([])

const form = reactive({
  username: '',
  displayName: '',
  email: '',
  password: '',
  roles: [] as string[],
  enabled: true,
})

const rules: FormRules = {
  username: [{ required: true, message: 'Required', trigger: 'blur' }],
  displayName: [{ required: true, message: 'Required', trigger: 'blur' }],
  email: [{ required: true, message: 'Required', trigger: 'blur' }, { type: 'email', message: 'Invalid email', trigger: 'blur' }],
  password: [{ required: true, message: 'Required', trigger: 'blur' }],
}

async function loadRoles() {
  try {
    const res = await request.get('/iam/roles')
    const records = (res as any).records || (res as any).items || (res as any).list || []
    allRoles.value = records
  } catch {
    allRoles.value = [
      { code: 'ADMIN', name: 'Administrator' },
      { code: 'USER', name: 'User' },
      { code: 'VIEWER', name: 'Viewer' },
    ]
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/iam/users')
    const records = (res as any).records || (res as any).items || (res as any).list || []
    // Map backend UserVO to list format
    users.value = records.map((u: any) => ({
      id: u.id,
      username: u.username,
      displayName: u.nickname || u.displayName || u.username,
      email: u.email || '',
      roles: (u.roles || []).map((r: any) => r.code || r),
      enabled: u.status === 'ACTIVE',
    }))
  } catch {
    users.value = []
  } finally { loading.value = false }
}

function showDialog(user?: any) {
  editingUser.value = user || null
  if (user) {
    Object.assign(form, { ...user, password: '' })
  } else {
    Object.assign(form, { username: '', displayName: '', email: '', password: '', roles: [], enabled: true })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingUser.value) {
      await request.put(`/iam/users/${editingUser.value.id}`, {
        nickname: form.displayName,
        email: form.email,
        roleIds: allRoles.value.filter(r => form.roles.includes(r.code)).map(r => r.id),
      })
      ElMessage.success(t('message.userUpdated'))
    } else {
      await request.post('/iam/users', {
        username: form.username,
        password: form.password,
        nickname: form.displayName,
        email: form.email,
        roleIds: allRoles.value.filter(r => form.roles.includes(r.code)).map(r => r.id),
      })
      ElMessage.success(t('message.userCreated'))
    }
    dialogVisible.value = false
    loadData()
  } catch { /* handled */ }
}

async function handleDelete(id: string) {
  try {
    await ElMessageBox.confirm(t('message.deleteUserConfirm'), t('message.confirmTitle'), { type: 'warning' })
    await request.delete(`/iam/users/${id}`)
    ElMessage.success(t('message.userDeleted'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => {
  loadRoles()
  loadData()
})
</script>
