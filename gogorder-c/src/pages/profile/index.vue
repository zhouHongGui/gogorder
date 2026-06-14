<template>
  <view class="safe-page profile-page">
    <view class="profile-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="field">
        <text>昵称</text>
        <input v-model="form.nickname" maxlength="50" placeholder="设置一个喜欢的昵称" />
      </view>
      <view class="field">
        <text>头像地址</text>
        <input v-model="form.avatar" maxlength="255" placeholder="请输入头像图片地址" />
      </view>
      <button class="primary-button" @click="save">保存资料</button>
      <button class="logout-button" @click="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { updateUserInfo } from '../../api/auth'
import { clearSession, getUser, saveUser } from '../../utils/auth'
import type { UserUpdateRequest } from '../../types/auth'

const current = getUser()
const form = reactive<UserUpdateRequest>({ nickname: current?.nickname || '', avatar: current?.avatar || '' })
const avatarText = computed(() => (form.nickname || 'G').slice(0, 1))

async function save() {
  const user = await updateUserInfo(form)
  saveUser(user)
  uni.showToast({ title: '保存成功', icon: 'success' })
}

function logout() {
  clearSession()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.profile-page { padding: 40rpx 32rpx; }
.profile-card { padding: 42rpx 34rpx; border-radius: 36rpx; background: #fff; }
.avatar { width: 120rpx; height: 120rpx; margin: 0 auto 40rpx; border-radius: 40rpx; background: #181711; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 46rpx; font-weight: 800; }
.field { padding: 24rpx 0; border-bottom: 1rpx solid #ece6dc; }
.field text { display: block; color: #8c8579; font-size: 23rpx; margin-bottom: 14rpx; }
.field input { height: 58rpx; font-size: 29rpx; }
.logout-button { margin-top: 22rpx; background: transparent; color: #a3493f; font-size: 27rpx; }
</style>
