<template>
  <view class="safe-page profile-page">
    <view class="profile-hero">
      <view class="top-bar">
        <view>
          <text class="top-kicker">GOGORDER MEMBER</text>
          <text class="top-title">我的</text>
        </view>
        <view class="settings-button" @click="openEditor">
          <view class="gear-ring"><view class="gear-core" /></view>
        </view>
      </view>

      <view class="member-card">
        <view class="member-main">
          <view class="avatar-shell" @click="openEditor">
            <image v-if="user?.avatar" class="avatar-image" :src="user.avatar" mode="aspectFill" />
            <text v-else class="avatar-text">{{ avatarText }}</text>
            <view class="avatar-edit">+</view>
          </view>
          <view class="member-copy">
            <text class="member-name">{{ displayName }}</text>
            <text class="member-phone">{{ maskedPhone }}</text>
            <view class="member-level">
              <view class="level-mark">GO</view>
              <text>鲜享会员</text>
            </view>
          </view>
          <text class="edit-link" @click="openEditor">编辑资料</text>
        </view>

        <view class="member-progress">
          <view class="progress-copy">
            <text>欢迎回来，今天也要喝点好的</text>
            <text class="progress-value">MEMBER</text>
          </view>
          <view class="progress-track"><view class="progress-active" /></view>
        </view>
      </view>
    </view>

    <view class="profile-content">
      <view class="balance-card">
        <view class="balance-shine" />
        <view class="balance-heading">
          <view>
            <text class="balance-kicker">MY BALANCE</text>
            <text class="balance-label">账户余额</text>
          </view>
          <view class="balance-detail" @click="showBalanceNotice">余额明细 ›</view>
        </view>
        <view class="balance-value-row">
          <text class="currency">¥</text>
          <text class="balance-value">{{ balanceYuan }}</text>
        </view>
        <view class="balance-footer">
          <text>余额用于订单支付</text>
          <text>充值请联系门店工作人员</text>
        </view>
      </view>

      <view class="section-card order-card">
        <view class="section-heading">
          <text class="section-title">我的订单</text>
          <text class="section-more" @click="openOrders">查看全部 ›</text>
        </view>
        <view class="order-actions">
          <view class="order-action" @click="openOrders">
            <view class="action-icon pending-icon">
              <view class="receipt-paper"><view class="receipt-line" /><view class="receipt-line short" /></view>
            </view>
            <text>待支付</text>
          </view>
          <view class="order-action" @click="openOrders">
            <view class="action-icon making-icon">
              <view class="cup-lid" /><view class="cup-body">GO</view>
            </view>
            <text>制作中</text>
          </view>
          <view class="order-action" @click="openOrders">
            <view class="action-icon pickup-icon">
              <view class="bag-handle" /><view class="bag-body" />
            </view>
            <text>待取餐</text>
          </view>
          <view class="order-action" @click="openOrders">
            <view class="action-icon complete-icon"><view class="check-mark" /></view>
            <text>已完成</text>
          </view>
        </view>
      </view>

      <view class="section-card service-card">
        <text class="section-title service-title">更多服务</text>
        <view class="service-row" @click="openShop">
          <view class="service-icon location-icon"><view class="location-dot" /></view>
          <view class="service-copy">
            <text class="service-name">我的门店</text>
            <text class="service-description">{{ currentShopName }}</text>
          </view>
          <text class="service-arrow">›</text>
        </view>
        <view class="service-row" @click="showDeveloping('优惠券')">
          <view class="service-icon coupon-icon"><view class="coupon-cut cut-one" /><view class="coupon-cut cut-two" /></view>
          <view class="service-copy">
            <text class="service-name">优惠券</text>
            <text class="service-description">查看可用优惠与活动</text>
          </view>
          <text class="service-arrow">›</text>
        </view>
        <view class="service-row" @click="showDeveloping('帮助与客服')">
          <view class="service-icon help-icon">?</view>
          <view class="service-copy">
            <text class="service-name">帮助与客服</text>
            <text class="service-description">常见问题与门店联系</text>
          </view>
          <text class="service-arrow">›</text>
        </view>
      </view>

      <button class="logout-button" @click="logout">退出当前账号</button>
      <text class="version">GOGORDER · FRESHLY MADE</text>
    </view>

    <view v-if="editorVisible" class="editor-mask" @click="closeEditor">
      <view class="editor-sheet" @click.stop>
        <view class="sheet-handle" />
        <view class="editor-heading">
          <view>
            <text class="editor-title">编辑个人资料</text>
            <text class="editor-description">完善资料，让每一次点单更有归属感</text>
          </view>
          <view class="editor-close" @click="closeEditor">×</view>
        </view>
        <view class="editor-avatar">
          <image v-if="form.avatar" :src="form.avatar" mode="aspectFill" />
          <text v-else>{{ formAvatarText }}</text>
        </view>
        <view class="field">
          <text class="field-label">昵称</text>
          <input v-model="form.nickname" maxlength="50" placeholder="设置一个喜欢的昵称" />
        </view>
        <view class="field">
          <text class="field-label">头像图片地址</text>
          <input v-model="form.avatar" maxlength="255" placeholder="请输入头像图片地址" />
        </view>
        <button class="save-button" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存资料' }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserBalance, getUserInfo, updateUserInfo } from '../../api/auth'
import { clearSession, getToken, getUser, saveUser } from '../../utils/auth'
import { getCurrentShop } from '../../utils/shop'
import type { UserInfo, UserUpdateRequest } from '../../types/auth'

const user = ref<UserInfo | null>(getUser())
const balance = ref(0)
const editorVisible = ref(false)
const saving = ref(false)
const form = reactive<UserUpdateRequest>({ nickname: '', avatar: '' })

const displayName = computed(() => user.value?.nickname?.trim() || 'GOGORDER 用户')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const formAvatarText = computed(() => (form.nickname.trim() || 'G').slice(0, 1).toUpperCase())
const maskedPhone = computed(() => {
  const phone = user.value?.phone || ''
  return phone.length === 11 ? `${phone.slice(0, 3)} ${phone.slice(3, 7).replace(/\d/g, '•')} ${phone.slice(-4)}` : phone
})
const balanceYuan = computed(() => (balance.value / 100).toFixed(2))
const currentShopName = computed(() => getCurrentShop()?.name || '尚未选择常用门店')

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    const [userInfo, balanceInfo] = await Promise.all([getUserInfo(), getUserBalance()])
    user.value = userInfo
    balance.value = Number(balanceInfo.balance || 0)
    saveUser(userInfo)
  } catch {
    user.value = getUser() || user.value
  }
})

function openEditor() {
  form.nickname = user.value?.nickname || ''
  form.avatar = user.value?.avatar || ''
  editorVisible.value = true
}

function closeEditor() {
  editorVisible.value = false
}

async function save() {
  if (saving.value) return
  saving.value = true
  try {
    const updated = await updateUserInfo({ nickname: form.nickname.trim(), avatar: form.avatar.trim() })
    user.value = updated
    saveUser(updated)
    editorVisible.value = false
    uni.showToast({ title: '资料已更新', icon: 'success' })
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存失败', icon: 'none' })
  } finally {
    saving.value = false
  }
}

function openOrders() {
  uni.switchTab({ url: '/pages/order/index' })
}

function openShop() {
  uni.navigateTo({ url: '/pages/shop/select' })
}

function showBalanceNotice() {
  uni.showModal({
    title: '余额说明',
    content: '余额可用于订单支付。V1.0 暂不支持 C 端充值，如需充值请联系门店工作人员。',
    showCancel: false
  })
}

function showDeveloping(name: string) {
  uni.showToast({ title: `${name}功能即将开放`, icon: 'none' })
}

async function logout() {
  const result = await uni.showModal({
    title: '退出登录',
    content: '确定退出当前账号吗？',
    confirmText: '退出'
  })
  if (!result.confirm) return
  clearSession()
  uni.reLaunch({ url: '/pages/login/index' })
}
</script>

<style lang="scss" scoped>
.profile-page {
  min-height: 100vh;
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
  background: #f5f1e9;
}

.profile-hero {
  padding: calc(var(--status-bar-height) + 26rpx) 28rpx 118rpx;
  background:
    radial-gradient(circle at 90% 5%, rgba(221, 168, 98, .34), transparent 28%),
    linear-gradient(150deg, #191713 0%, #393228 100%);
}

.top-bar, .member-main, .progress-copy, .balance-heading, .balance-value-row,
.section-heading, .order-actions, .service-row {
  display: flex;
  align-items: center;
}

.top-bar { justify-content: space-between; color: #fff; }
.top-kicker { display: block; color: #d5aa72; font-size: 18rpx; font-weight: 700; letter-spacing: 4rpx; }
.top-title { display: block; margin-top: 8rpx; font-size: 42rpx; font-weight: 800; }
.settings-button { width: 68rpx; height: 68rpx; border: 1rpx solid rgba(255,255,255,.2); border-radius: 24rpx; background: rgba(255,255,255,.08); display: flex; align-items: center; justify-content: center; }
.gear-ring { width: 29rpx; height: 29rpx; border: 5rpx dotted #e6d4b9; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.gear-core { width: 8rpx; height: 8rpx; border-radius: 50%; background: #e6d4b9; }

.member-card { margin-top: 32rpx; padding: 30rpx; border: 1rpx solid rgba(255,255,255,.13); border-radius: 34rpx; background: rgba(255,255,255,.08); color: #fff; backdrop-filter: blur(18rpx); }
.avatar-shell { position: relative; width: 108rpx; height: 108rpx; flex: none; border: 4rpx solid rgba(255,255,255,.35); border-radius: 36rpx; background: #f1d4aa; overflow: visible; display: flex; align-items: center; justify-content: center; }
.avatar-image { width: 100%; height: 100%; border-radius: 32rpx; }
.avatar-text { color: #2d271f; font-size: 42rpx; font-weight: 900; }
.avatar-edit { position: absolute; right: -8rpx; bottom: -8rpx; width: 34rpx; height: 34rpx; border: 4rpx solid #2d2922; border-radius: 50%; background: #fff; color: #2d2922; font-size: 25rpx; font-weight: 800; line-height: 31rpx; text-align: center; }
.member-copy { min-width: 0; flex: 1; margin-left: 22rpx; display: flex; flex-direction: column; }
.member-name { max-width: 260rpx; overflow: hidden; font-size: 31rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.member-phone { margin-top: 7rpx; color: rgba(255,255,255,.57); font-size: 21rpx; letter-spacing: 1rpx; }
.member-level { align-self: flex-start; margin-top: 13rpx; padding: 6rpx 12rpx; border-radius: 16rpx; background: rgba(215,171,113,.16); color: #e3bf8c; display: flex; align-items: center; gap: 8rpx; font-size: 18rpx; }
.level-mark { font-size: 15rpx; font-weight: 900; }
.edit-link { color: #d6b382; font-size: 21rpx; }
.member-progress { margin-top: 28rpx; padding-top: 22rpx; border-top: 1rpx solid rgba(255,255,255,.12); }
.progress-copy { justify-content: space-between; color: rgba(255,255,255,.6); font-size: 19rpx; }
.progress-value { color: #d6b382; font-weight: 700; letter-spacing: 2rpx; }
.progress-track { height: 6rpx; margin-top: 15rpx; border-radius: 6rpx; background: rgba(255,255,255,.11); overflow: hidden; }
.progress-active { width: 68%; height: 100%; border-radius: 6rpx; background: linear-gradient(90deg, #c99960, #efd1a5); }

.profile-content { margin-top: -86rpx; padding: 0 24rpx; }
.balance-card, .section-card { position: relative; overflow: hidden; border-radius: 32rpx; background: #fff; box-shadow: 0 16rpx 48rpx rgba(58,48,34,.07); }
.balance-card { padding: 30rpx; background: linear-gradient(135deg, #fffaf1 0%, #f0ddbd 100%); }
.balance-shine { position: absolute; right: -45rpx; top: -100rpx; width: 260rpx; height: 260rpx; border: 34rpx solid rgba(255,255,255,.34); border-radius: 50%; }
.balance-heading { position: relative; justify-content: space-between; }
.balance-kicker { display: block; color: #ab7841; font-size: 17rpx; font-weight: 800; letter-spacing: 4rpx; }
.balance-label { display: block; margin-top: 7rpx; color: #4a3a28; font-size: 23rpx; }
.balance-detail { padding: 10rpx 16rpx; border: 1rpx solid rgba(125,86,41,.15); border-radius: 22rpx; color: #79552e; font-size: 20rpx; }
.balance-value-row { position: relative; margin-top: 19rpx; align-items: baseline; color: #2c241b; }
.currency { margin-right: 8rpx; font-size: 31rpx; font-weight: 800; }
.balance-value { font-size: 64rpx; font-weight: 900; letter-spacing: -2rpx; }
.balance-footer { position: relative; margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid rgba(121,85,46,.13); color: #8e704d; display: flex; justify-content: space-between; font-size: 19rpx; }

.section-card { margin-top: 22rpx; padding: 28rpx; }
.section-heading { justify-content: space-between; }
.section-title { color: #242019; font-size: 29rpx; font-weight: 800; }
.section-more { color: #9c9283; font-size: 21rpx; }
.order-actions { justify-content: space-around; margin-top: 30rpx; }
.order-action { width: 23%; color: #575046; display: flex; flex-direction: column; align-items: center; gap: 13rpx; font-size: 21rpx; }
.action-icon { position: relative; width: 72rpx; height: 72rpx; border-radius: 25rpx; background: #f7f2e9; display: flex; align-items: center; justify-content: center; color: #9b7144; }
.receipt-paper { width: 31rpx; height: 39rpx; border: 3rpx solid currentColor; border-radius: 5rpx; }
.receipt-line { width: 18rpx; height: 3rpx; margin: 9rpx auto 0; border-radius: 3rpx; background: currentColor; }.receipt-line.short { width: 12rpx; margin-top: 6rpx; }
.cup-lid { position: absolute; top: 19rpx; width: 33rpx; height: 5rpx; border-radius: 5rpx; background: currentColor; }.cup-body { width: 27rpx; height: 31rpx; margin-top: 8rpx; border: 3rpx solid currentColor; border-radius: 3rpx 3rpx 9rpx 9rpx; font-size: 9rpx; font-weight: 900; display: flex; align-items: center; justify-content: center; }
.bag-handle { position: absolute; top: 19rpx; width: 22rpx; height: 13rpx; border: 3rpx solid currentColor; border-bottom: 0; border-radius: 12rpx 12rpx 0 0; }.bag-body { width: 34rpx; height: 31rpx; margin-top: 12rpx; border: 3rpx solid currentColor; border-radius: 5rpx; }
.complete-icon { background: #edf5ef; color: #527b5d; }.check-mark { width: 27rpx; height: 14rpx; border-left: 4rpx solid currentColor; border-bottom: 4rpx solid currentColor; transform: rotate(-45deg) translate(3rpx, -2rpx); }

.service-title { display: block; margin-bottom: 12rpx; }
.service-row { min-height: 92rpx; border-bottom: 1rpx solid #f0ebe3; }
.service-row:last-child { border-bottom: 0; }
.service-icon { position: relative; width: 58rpx; height: 58rpx; flex: none; border-radius: 20rpx; background: #f6f1e8; color: #9b7144; display: flex; align-items: center; justify-content: center; font-size: 27rpx; font-weight: 800; }
.location-icon::before { content: ''; width: 22rpx; height: 27rpx; border: 3rpx solid currentColor; border-radius: 50% 50% 50% 4rpx; transform: rotate(-45deg); }.location-dot { position: absolute; width: 7rpx; height: 7rpx; border-radius: 50%; background: currentColor; }
.coupon-icon { width: 31rpx; height: 23rpx; margin: 0 13rpx; border: 3rpx solid currentColor; border-radius: 5rpx; }.coupon-cut { position: absolute; top: 7rpx; width: 7rpx; height: 7rpx; border-radius: 50%; background: #f6f1e8; }.cut-one { left: -5rpx; }.cut-two { right: -5rpx; }
.service-copy { min-width: 0; flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; }
.service-name { color: #312c25; font-size: 25rpx; font-weight: 650; }
.service-description { max-width: 480rpx; margin-top: 5rpx; overflow: hidden; color: #a39a8d; font-size: 19rpx; text-overflow: ellipsis; white-space: nowrap; }
.service-arrow { color: #b4ab9e; font-size: 35rpx; }
.logout-button { height: 82rpx; margin-top: 25rpx; border-radius: 28rpx; background: #fff; color: #9b5148; font-size: 24rpx; line-height: 82rpx; }
.version { display: block; margin-top: 28rpx; color: #bbb2a5; font-size: 17rpx; letter-spacing: 3rpx; text-align: center; }

.editor-mask { position: fixed; z-index: 100; inset: 0; background: rgba(22,19,15,.48); display: flex; align-items: flex-end; }
.editor-sheet { width: 100%; padding: 16rpx 30rpx calc(34rpx + env(safe-area-inset-bottom)); border-radius: 38rpx 38rpx 0 0; background: #fff; }
.sheet-handle { width: 78rpx; height: 7rpx; margin: 0 auto 25rpx; border-radius: 7rpx; background: #ded7cc; }
.editor-heading { display: flex; align-items: flex-start; justify-content: space-between; }
.editor-title { display: block; font-size: 32rpx; font-weight: 800; }.editor-description { display: block; margin-top: 8rpx; color: #9b9286; font-size: 20rpx; }
.editor-close { width: 55rpx; height: 55rpx; border-radius: 50%; background: #f5f1ea; color: #746d62; font-size: 38rpx; line-height: 50rpx; text-align: center; }
.editor-avatar { width: 108rpx; height: 108rpx; margin: 28rpx auto 16rpx; border-radius: 36rpx; background: #181711; color: #e6c18e; overflow: hidden; display: flex; align-items: center; justify-content: center; font-size: 42rpx; font-weight: 900; }.editor-avatar image { width: 100%; height: 100%; }
.field { padding: 20rpx 0; border-bottom: 1rpx solid #eee8df; }.field-label { display: block; color: #958c80; font-size: 20rpx; }.field input { height: 58rpx; margin-top: 5rpx; font-size: 27rpx; }
.save-button { height: 88rpx; margin-top: 30rpx; border-radius: 44rpx; background: #181711; color: #fff; font-size: 27rpx; font-weight: 700; line-height: 88rpx; }
</style>
