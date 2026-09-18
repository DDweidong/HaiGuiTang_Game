<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">海龟汤游戏</h1>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" @submit.prevent>
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button
              type="success"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" @submit.prevent>
            <el-form-item>
              <el-input v-model="registerForm.username" placeholder="用户名（3~20位，中文/字母/数字/下划线）" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码（6~32位）"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>
            <el-button
              type="success"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { setTokens, setUser } from '../utils/auth'

const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '' })

// 登录/注册成功后统一处理: 存登录态 → 跳回原目标页
function afterLogin(data) {
  setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken })
  setUser({ userId: data.userId, username: data.username })
  ElMessage.success(`欢迎，${data.username}`)
  router.push(route.query.redirect || '/home')
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await http.post('/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    })
    afterLogin(data)
  } catch (e) {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const data = await http.post('/auth/register', {
      username: registerForm.username,
      password: registerForm.password
    })
    afterLogin(data)
  } catch (e) {
    // 错误提示已由拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
}

.auth-card {
  width: 420px;
  background: white;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.auth-title {
  text-align: center;
  color: #333;
  margin: 0 0 24px;
}

.submit-btn {
  width: 100%;
  background-color: #42b983;
  border-color: #42b983;
}
</style>
