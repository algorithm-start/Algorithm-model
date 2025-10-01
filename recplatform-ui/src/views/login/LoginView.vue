<template>
  <div class="login-view">
    <!-- Language Switcher in top right corner -->

    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">
          <div class="login-brand">
            <span class="login-mark">Re</span>
            <h1 class="login-title">Recplatform</h1>
          </div>
          <p class="login-subtitle">{{ t('login.subtitle') }}</p>
        </div>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item :label="t('login.username')" prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('login.username')"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item :label="t('login.password')" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.password')"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ t('login.loginButton') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: () => t('login.username'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('login.password'), trigger: 'blur' }],
}



async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success(t('common.operationSuccess'))
    router.push('/home')
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f7fa;
}
.lang-switcher {
  position: absolute;
  top: 20px;
  right: 20px;
}
.lang-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  cursor: pointer;
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.3s;
}
.lang-button:hover {
  background: rgba(255, 255, 255, 0.2);
}
.login-card {
  width: 420px;
  border-radius: 12px;
}
.login-header {
  text-align: center;
}
.login-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.login-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8f1f 100%);
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}
.login-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.login-subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}
.login-btn {
  width: 100%;
}
.login-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--el-border-color-light);
}
.login-card :deep(.el-form-item__label) {
  color: #606266;
}
.login-card :deep(.el-input__wrapper) {
  background: #fff;
}
.login-card :deep(.el-input__inner) {
  color: #303133;
}
.login-card :deep(.el-input__inner::placeholder) {
  color: #c0c4cc;
}
.login-card :deep(.el-input__prefix .el-icon) {
  color: #909399;
}
</style>
