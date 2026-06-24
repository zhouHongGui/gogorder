<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginByPassword, loginBySms, sendSms } from '../../api/auth'
import { getToken, saveSession } from '../../utils/session'

const mode = ref<'sms' | 'password'>('sms')
const phone = ref('')
const code = ref('')
const account = ref('')
const password = ref('')
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))
const canSubmit = computed(() => mode.value === 'sms'
  ? phoneValid.value && /^\d{6}$/.test(code.value)
  : account.value.trim().length >= 2 && password.value.length >= 5)

onLoad(() => {
  if (getToken()) uni.reLaunch({ url: '/pages/workbench/index' })
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

function changeMode(nextMode: 'sms' | 'password') {
  mode.value = nextMode
}

async function handleSendSms() {
  if (!phoneValid.value || sending.value || countdown.value > 0) return
  sending.value = true
  try {
    const result = await sendSms(phone.value)
    countdown.value = result.retryAfter || 60
    if (result.mockCode) {
      code.value = result.mockCode
      uni.showToast({ title: `开发验证码 ${result.mockCode}`, icon: 'none', duration: 2500 })
    } else {
      uni.showToast({ title: '验证码已发送', icon: 'success' })
    }
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // request 已统一展示错误提示。
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const result = mode.value === 'sms'
      ? await loginBySms(phone.value, code.value)
      : await loginByPassword(account.value.trim(), password.value)
    saveSession(result)
    uni.reLaunch({ url: '/pages/workbench/index' })
  } catch {
    // request 已统一展示错误提示。
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="safe-page login-page">
    <view class="brand-block">
      <view class="brand-mark">GO</view>
      <text class="brand-title">门店助手</text>
      <text class="brand-subtitle">订单、制作与门店运营</text>
    </view>

    <view class="login-card">
      <view class="mode-tabs">
        <view class="mode-tab" :class="{ active: mode === 'sms' }" @click="changeMode('sms')">短信登录</view>
        <view class="mode-tab" :class="{ active: mode === 'password' }" @click="changeMode('password')">账号登录</view>
      </view>

      <view v-if="mode === 'sms'" class="form-body">
        <view class="field">
          <text class="field-label">手机号</text>
          <input v-model="phone" class="field-input" type="number" maxlength="11" placeholder="请输入员工登录手机号" />
        </view>
        <view class="field">
          <text class="field-label">验证码</text>
          <view class="code-row">
            <input v-model="code" class="field-input code-input" type="number" maxlength="6" placeholder="6位验证码" />
            <button class="code-button" :disabled="!phoneValid || sending || countdown > 0" @click="handleSendSms">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>
      </view>

      <view v-else class="form-body">
        <view class="field">
          <text class="field-label">员工账号</text>
          <input v-model="account" class="field-input" maxlength="30" placeholder="请输入门店员工账号" />
        </view>
        <view class="field">
          <text class="field-label">密码</text>
          <input v-model="password" class="field-input" password maxlength="20" placeholder="请输入密码" />
        </view>
      </view>

      <button class="primary-button login-button" :disabled="!canSubmit || submitting" @click="handleLogin">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
      <text class="login-tip">账号由管理员创建，不支持员工自行注册</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.login-page {
  padding: calc(env(safe-area-inset-top) + 72rpx) 36rpx 48rpx;
  background:
    radial-gradient(circle at 88% 8%, rgba(230, 178, 105, 0.38), transparent 28%),
    radial-gradient(circle at 8% 72%, rgba(225, 125, 34, 0.1), transparent 32%),
    linear-gradient(180deg, #fbf7ee 0%, #f3eee4 100%);
}

.brand-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 62rpx;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 108rpx;
  height: 108rpx;
  margin-bottom: 36rpx;
  border-radius: 34rpx;
  background: #181711;
  color: #fff;
  font-size: 38rpx;
  font-weight: 800;
  box-shadow: 0 22rpx 50rpx rgba(42, 34, 24, .18);
  transform: rotate(-5deg);
}

.brand-title {
  font-size: 54rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
  line-height: 1.16;
}

.brand-subtitle {
  margin-top: 18rpx;
  color: #817b70;
  font-size: 26rpx;
}

.login-card {
  padding: 42rpx 34rpx 36rpx;
  border: 1rpx solid rgba(255, 255, 255, .78);
  border-radius: 36rpx;
  background: rgba(255, 255, 255, .86);
  box-shadow: 0 24rpx 70rpx rgba(77, 63, 42, .10);
  backdrop-filter: blur(20rpx);
}

.mode-tabs {
  display: flex;
  padding: 7rpx;
  border-radius: 24rpx;
  background: #f1e8db;
}

.mode-tab {
  flex: 1;
  height: 74rpx;
  border-radius: 20rpx;
  color: #8f887c;
  font-size: 27rpx;
  font-weight: 600;
  line-height: 74rpx;
  text-align: center;
}

.mode-tab.active {
  background: #181711;
  color: #fff;
  box-shadow: 0 12rpx 28rpx rgba(42, 34, 24, .14);
}

.form-body {
  margin-top: 36rpx;
}

.field {
  margin-bottom: 28rpx;
}

.field-label {
  display: block;
  margin-bottom: 14rpx;
  color: #7d7469;
  font-size: 23rpx;
  font-weight: 700;
}

.field-input {
  box-sizing: border-box;
  width: 100%;
  height: 94rpx;
  padding: 0 26rpx;
  border: 2rpx solid #eee4d6;
  border-radius: 24rpx;
  background: #fffaf3;
  color: #292620;
  font-size: 29rpx;
}

.code-row {
  display: flex;
  gap: 16rpx;
}

.code-input {
  flex: 1;
  width: auto;
}

.code-button {
  width: 212rpx;
  height: 94rpx;
  margin: 0;
  padding: 0;
  border-radius: 24rpx;
  background: #f2e4d1;
  color: #9c6534;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 94rpx;
}

.code-button[disabled] {
  background: #eee8de;
  color: #aaa197;
}

.login-button {
  margin-top: 14rpx;
  box-shadow: 0 18rpx 42rpx rgba(31, 28, 22, .16);
}

.login-tip {
  display: block;
  margin-top: 26rpx;
  color: #a8a196;
  font-size: 22rpx;
  line-height: 1.6;
  text-align: center;
}
</style>
