<template>
  <div class="source-create">
    <PageHeader :title="isEdit ? t('button.editDataSource') : t('button.createDataSource')">
      <el-button @click="router.back()">{{ t('common.cancel') }}</el-button>
    </PageHeader>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px">
        <el-form-item :label="t('table.name')" prop="name">
          <el-input v-model="form.name" :placeholder="t('form.dataSourceName')" />
        </el-form-item>
        <el-form-item :label="t('table.type')" prop="type">
          <el-select v-model="form.type" :placeholder="t('form.selectType')" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="SQL Server" value="SQLSERVER" />
            <el-option label="Redis" value="REDIS" />
            <el-option label="MongoDB" value="MONGODB" />
            <el-option label="Elasticsearch" value="ELASTICSEARCH" />
            <el-option label="Kafka" value="KAFKA" />
            <el-option label="REST API" value="API" />
            <el-option label="File" value="FILE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('form.description')">
          <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="t('form.optionalDescription')" />
        </el-form-item>

        <template v-if="isDatabase">
          <el-form-item :label="t('form.host')">
            <el-input v-model="form.config.host" placeholder="数据库实际访问地址，如 10.0.0.5 或 db.example.com" />
            <el-alert
              v-if="isLocalhostInput"
              type="warning"
              :closable="false"
              show-icon
              style="margin-top: 6px"
            >
              当前应用运行在容器中，localhost / 127.0.0.1 指向应用容器自身，而非你的数据库主机。
              如需连接本机数据库请填写宿主机 IP 或 host.docker.internal，远程数据库请填写真实 IP 或域名。
            </el-alert>
          </el-form-item>
          <el-form-item :label="t('form.port')">
            <el-input-number v-model="form.config.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item :label="t('form.database')">
            <el-input v-model="form.config.database" :placeholder="t('form.databaseName')" />
          </el-form-item>
          <el-form-item :label="t('form.username')">
            <el-input v-model="form.config.username" :placeholder="t('form.username')" />
          </el-form-item>
          <el-form-item :label="t('form.password')">
            <el-input v-model="form.config.password" type="password" show-password :placeholder="isEdit ? t('form.passwordKeepEmptyHint') : t('form.password')" />
          </el-form-item>
        </template>

        <template v-if="form.type === 'API'">
          <el-form-item :label="t('form.baseUrl')">
            <el-input v-model="form.config.baseUrl" placeholder="https://api.example.com" />
          </el-form-item>
          <el-form-item :label="t('form.authType')">
            <el-select v-model="form.config.authType">
              <el-option label="None" value="NONE" />
              <el-option label="Bearer Token" value="BEARER" />
              <el-option :label="t('form.basicAuth')" value="BASIC" />
              <el-option label="API Key" value="API_KEY" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="form.type === 'FILE'">
          <el-form-item :label="t('form.filePath')">
            <el-input v-model="form.config.filePath" placeholder="/data/file.csv" />
          </el-form-item>
          <el-form-item :label="t('form.format')">
            <el-select v-model="form.config.format">
              <el-option label="CSV" value="CSV" />
              <el-option label="JSON" value="JSON" />
              <el-option label="Parquet" value="PARQUET" />
              <el-option label="Excel" value="EXCEL" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSubmit">
            {{ isEdit ? t('common.update') : t('common.create') }}
          </el-button>
          <el-button @click="handleTestConnection" :loading="testing">{{ t('common.testConnection') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import { dataApi } from '@/api/data'
import type { FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const testing = ref(false)

const isEdit = computed(() => !!route.query.id)

const form = reactive({
  name: '',
  type: 'MYSQL',
  description: '',
  config: {} as Record<string, any>,
})

const rules: FormRules = {
  name: [{ required: true, message: t('message.pleaseEnterName'), trigger: 'blur' }],
  type: [{ required: true, message: t('message.pleaseSelectType'), trigger: 'change' }],
}

const isDatabase = computed(() => ['MYSQL', 'POSTGRESQL', 'ORACLE', 'SQLSERVER', 'MONGODB', 'ELASTICSEARCH'].includes(form.type))

const isLocalhostInput = computed(() => {
  const host = String(form.config.host || '').trim().toLowerCase()
  return ['localhost', '127.0.0.1', '::1', '0.0.0.0'].includes(host)
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      description: form.description,
      connectionConfig: form.config,
    }
    if (isEdit.value) {
      await dataApi.updateSource(route.query.id as string, payload)
      ElMessage.success(t('common.operationSuccess'))
    } else {
      await dataApi.createSource(payload)
      ElMessage.success(t('common.operationSuccess'))
    }
    router.push('/data/sources')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

async function handleTestConnection() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  testing.value = true
  try {
    let result: any
    // 编辑态且未重新输入密码时，按已保存的数据源 id 测试（使用库中已存密码）
    if (isEdit.value && !form.config.password) {
      result = await dataApi.testConnection(route.query.id as string)
    } else {
      const payload = {
        name: form.name,
        type: form.type,
        description: form.description,
        connectionConfig: form.config,
      }
      result = await dataApi.testConnectionByConfig(payload)
    }
    if (result?.success) {
      ElMessage.success(result.message || t('message.connectionTestSuccessful'))
    } else {
      ElMessage.error(result?.message || t('message.connectionTestFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('message.connectionTestFailed'))
  } finally {
    testing.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await dataApi.getSource(route.query.id as string) as any
      form.name = res.name ?? ''
      form.type = res.type ?? 'MYSQL'
      form.description = res.description ?? ''
      form.config = {
        host: res.host,
        port: res.port,
        database: res.database,
        username: res.username,
        // 不回显后端返回的脱敏密码（******），留空表示沿用原密码
        password: '',
      }
    } catch { /* handled */ }
  }
})
</script>
