<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="34"><Ship /></el-icon>
        </div>
        <h2>集装箱码头闸口预约与办单管理系统</h2>
        <p>集装箱码头闸口预约与办单管理平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="quick-accounts">
        <el-tag
          v-for="acc in accounts"
          :key="acc.username"
          class="acc-tag"
          @click="fillAccount(acc)"
        >
          {{ acc.label }}：{{ acc.username }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authApi } from '@/api'
import { useUserStore } from '@/store/user'
import { firstAccessiblePath } from '@/router'

const router = useRouter()
const userStore = useUserStore()

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


const accounts = [
  { label: '管理员', username: 'admin' },
  { label: '办单员', username: 'gate' },
  { label: '公司一', username: 'company1' },
  { label: '司机', username: '13800000001' }
]

function fillAccount(acc) {
  form.username = acc.username
  form.password = '123456'
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const data = await authApi.login(form)
    userStore.setLogin(data.token, data.user)
    ElMessage.success(`欢迎，${data.user.name}`)
    // 按角色跳转：管理员/闸口进看板，公司/司机进入各自第一个有权限的菜单
    router.push(firstAccessiblePath(data.user.role))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d4e89 0%, #2b6cb0 50%, #3182ce 100%);
}

.login-box {
  width: 440px;
  padding: 40px 38px 28px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 26px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #1d4e89;
  margin-bottom: 12px;
}

.login-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
  color: #1f2d3d;
}

.login-header p {
  margin: 0;
  font-size: 12px;
  color: #909399;
  letter-spacing: 0.5px;
}

.quick-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.acc-tag {
  cursor: pointer;
}
</style>
