<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getProductCategories, getProductList, setProductSoldOut, updateProductStatus } from '../../api/product'
import type { StaffCategory, StaffProduct } from '../../types/product'
import { getCurrentShop } from '../../utils/session'

type StatusFilter = { label: string; value?: number }
type SoldOutFilter = { label: string; value?: boolean }

const statusFilters: StatusFilter[] = [
  { label: '全部' },
  { label: '销售中', value: 1 },
  { label: '已停售', value: 0 }
]
const soldOutFilters: SoldOutFilter[] = [
  { label: '全部' },
  { label: '已售空', value: true }
]

const currentShop = ref(getCurrentShop())
const categories = ref<StaffCategory[]>([])
const products = ref<StaffProduct[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const activeCategoryId = ref<number | undefined>(undefined)
const activeStatus = ref<number | undefined>(undefined)
const activeSoldOut = ref<boolean | undefined>(undefined)
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)

const hasMore = computed(() => products.value.length < total.value)
const totalText = computed(() => total.value ? `共 ${total.value} 个商品` : '暂无商品')

onShow(() => {
  currentShop.value = getCurrentShop()
  void loadAll(true)
})

onPullDownRefresh(async () => {
  try {
    await loadAll(true)
  } finally {
    uni.stopPullDownRefresh()
  }
})

onReachBottom(() => {
  if (hasMore.value && !loadingMore.value) void loadMore()
})

async function loadAll(reset = false) {
  await Promise.all([loadCategories(), loadProducts(reset)])
}

async function loadCategories() {
  try {
    categories.value = await getProductCategories()
  } catch {
    // request 已统一提示。
  }
}

async function loadProducts(reset = false) {
  if (loading.value) return
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const result = await getProductList({
      keyword: keyword.value.trim(),
      categoryId: activeCategoryId.value,
      status: activeStatus.value,
      stockState: activeSoldOut.value ? 'ZERO' : undefined,
      pageNum: pageNum.value,
      pageSize
    })
    total.value = result.total
    products.value = result.rows
  } catch {
    // request 已统一提示。
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  pageNum.value += 1
  try {
    const result = await getProductList({
      keyword: keyword.value.trim(),
      categoryId: activeCategoryId.value,
      status: activeStatus.value,
      stockState: activeSoldOut.value ? 'ZERO' : undefined,
      pageNum: pageNum.value,
      pageSize
    })
    total.value = result.total
    products.value = products.value.concat(result.rows)
  } catch {
    pageNum.value -= 1
  } finally {
    loadingMore.value = false
  }
}

function search() {
  void loadProducts(true)
}

function resetSearch() {
  keyword.value = ''
  activeCategoryId.value = undefined
  activeStatus.value = undefined
  activeSoldOut.value = undefined
  void loadProducts(true)
}

function switchCategory(categoryId?: number) {
  activeCategoryId.value = categoryId
  void loadProducts(true)
}

function switchStatus(status?: number) {
  activeStatus.value = status
  void loadProducts(true)
}

function switchSoldOut(soldOut?: boolean) {
  activeSoldOut.value = soldOut
  void loadProducts(true)
}

function toggleStatus(product: StaffProduct) {
  const nextStatus = product.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '恢复销售' : '停售'
  uni.showModal({
    title: action,
    content: `确认将「${product.productName}」${action}？`,
    success: ({ confirm }) => {
      if (!confirm) return
      void updateProductStatus(product.shopProductId, nextStatus).then(async () => {
        uni.showToast({ title: nextStatus === 1 ? '已恢复销售' : '已停售', icon: 'success' })
        await loadProducts(true)
      })
    }
  })
}

function toggleSoldOut(product: StaffProduct, soldOut: boolean) {
  const action = soldOut ? '设为售空' : '恢复有货'
  uni.showModal({
    title: action,
    content: soldOut
      ? `确认将「${product.productName}」设为售空？售空后 C 端不可下单。`
      : `确认将「${product.productName}」恢复为有货？恢复后 C 端可下单。`,
    success: ({ confirm }) => {
      if (!confirm) return
      void setProductSoldOut(product.shopProductId, soldOut, `员工端${action}：${product.productName}`).then(async () => {
        uni.showToast({ title: soldOut ? '已设为售空' : '已恢复有货', icon: 'success' })
        await loadProducts(true)
      })
    }
  })
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/workbench/index' })
  })
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

function imageStyle(product: StaffProduct) {
  return product.productImage ? { backgroundImage: `url(${product.productImage})` } : {}
}
</script>

<template>
  <view class="safe-page product-page">
    <view class="hero">
      <view class="hero-top">
        <text class="back" @click="goBack">‹ 返回</text>
        <text class="eyebrow">PRODUCTS</text>
      </view>
      <text class="title">商品管理</text>
      <text class="subtitle">{{ currentShop?.shopName || '当前门店' }} · {{ totalText }}</text>
    </view>

    <view class="filter-card">
      <view class="search-row">
        <input v-model="keyword" class="search-input" placeholder="搜索商品名称" confirm-type="search" @confirm="search" />
        <text class="search-btn" @click="search">搜索</text>
        <text class="reset-btn" @click="resetSearch">重置</text>
      </view>

      <scroll-view class="chip-scroll" scroll-x>
        <view class="chip-row">
          <text class="filter-chip" :class="{ active: activeCategoryId === undefined }" @click="switchCategory(undefined)">全部分类</text>
          <text
            v-for="category in categories"
            :key="category.id"
            class="filter-chip"
            :class="{ active: activeCategoryId === category.id }"
            @click="switchCategory(category.id)"
          >
            {{ category.name }}
          </text>
        </view>
      </scroll-view>

      <view class="filter-row">
        <text
          v-for="item in statusFilters"
          :key="item.label"
          class="small-chip"
          :class="{ active: activeStatus === item.value }"
          @click="switchStatus(item.value)"
        >
          {{ item.label }}
        </text>
      </view>

      <scroll-view class="chip-scroll soldout-scroll" scroll-x>
        <view class="chip-row">
          <text
            v-for="item in soldOutFilters"
            :key="item.label"
            class="small-chip"
            :class="{ active: activeSoldOut === item.value }"
            @click="switchSoldOut(item.value)"
          >
            {{ item.label }}
          </text>
        </view>
      </scroll-view>
    </view>

    <view v-if="!products.length && !loading" class="empty-card">
      <text class="empty-title">没有找到商品</text>
      <text class="empty-desc">换个分类、状态或关键词试试</text>
    </view>

    <view v-for="product in products" :key="product.shopProductId" class="product-card" :class="{ off: product.status === 0 }">
      <view class="product-main">
        <view class="product-image" :style="imageStyle(product)">
          <text v-if="!product.productImage">GO</text>
        </view>
        <view class="product-copy">
          <view class="product-title-row">
            <text class="product-name">{{ product.productName }}</text>
            <text class="status-pill" :class="{ off: product.status === 0 }">{{ product.statusDesc }}</text>
          </view>
          <text class="category">{{ product.categoryNames || '未分类' }}</text>
          <view class="price-row">
            <text class="price">{{ formatMoney(product.effectivePrice) }}</text>
            <text class="stock">{{ product.stockDesc }}</text>
          </view>
        </view>
      </view>

      <view class="sales-row">
        <view class="sales-item">
          <text class="sales-value">{{ product.todaySales || 0 }}</text>
          <text class="sales-label">今日销量</text>
        </view>
        <view class="sales-item">
          <text class="sales-value">{{ product.monthlySales || 0 }}</text>
          <text class="sales-label">近30天</text>
        </view>
      </view>

      <view class="card-actions">
        <text v-if="product.status === 1 && !product.soldOut" class="action warning" @click="toggleSoldOut(product, true)">设为售空</text>
        <text v-else-if="product.status === 1" class="action ghost" @click="toggleSoldOut(product, false)">恢复有货</text>
        <text class="action primary" :class="{ restore: product.status === 0 }" @click="toggleStatus(product)">
          {{ product.status === 1 ? '停售' : '恢复销售' }}
        </text>
      </view>
    </view>

    <view v-if="loading || loadingMore" class="loading-tip">加载中...</view>
    <view v-else-if="products.length && !hasMore" class="loading-tip">已经到底了</view>
  </view>
</template>

<style scoped lang="scss">
.product-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 22rpx) 24rpx calc(env(safe-area-inset-bottom) + 42rpx);
  background:
    radial-gradient(circle at 88% 4%, rgba(231, 182, 117, .28), transparent 26%),
    linear-gradient(180deg, #fbf7ee 0%, #f5f0e7 48%, #efe8dd 100%);
}

.hero {
  padding: 26rpx 28rpx 30rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, #181711 0%, #2b251d 62%, #4a3420 100%);
  box-shadow: 0 20rpx 54rpx rgba(44, 34, 22, .14);
  color: #fff;
}

.hero-top,
.product-main,
.product-title-row,
.price-row,
.sales-row,
.card-actions,
.search-row,
.filter-row {
  display: flex;
  align-items: center;
}

.hero-top,
.product-title-row,
.price-row,
.card-actions {
  justify-content: space-between;
}

.back {
  color: rgba(255, 255, 255, .78);
  font-size: 24rpx;
}

.eyebrow {
  color: #e8ba7c;
  font-size: 18rpx;
  font-weight: 850;
  letter-spacing: 4rpx;
}

.title {
  display: block;
  margin-top: 30rpx;
  font-size: 46rpx;
  font-weight: 880;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, .68);
  font-size: 23rpx;
}

.filter-card,
.product-card,
.empty-card {
  margin-top: 22rpx;
  border: 1rpx solid rgba(255, 255, 255, .76);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .88);
  box-shadow: 0 18rpx 46rpx rgba(70, 56, 38, .09);
}

.filter-card {
  padding: 22rpx;
}

.search-row {
  gap: 12rpx;
}

.search-input {
  height: 76rpx;
  flex: 1;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: #fffaf3;
  color: #292620;
  font-size: 25rpx;
}

.search-btn,
.reset-btn {
  width: 106rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #181711;
  color: #fff7ea;
  font-size: 24rpx;
  font-weight: 780;
  line-height: 76rpx;
  text-align: center;
}

.reset-btn {
  background: #efe4d4;
  color: #9a6a36;
}

.chip-scroll {
  width: 100%;
  margin-top: 20rpx;
  overflow: hidden;
  white-space: nowrap;
}

.soldout-scroll {
  margin-top: 14rpx;
}

.chip-row {
  display: inline-block;
  min-width: 100%;
  padding-right: 12rpx;
  box-sizing: border-box;
  font-size: 0;
  white-space: nowrap;
}

.filter-row {
  gap: 12rpx;
  margin-top: 18rpx;
}

.filter-chip,
.small-chip {
  display: inline-block;
  height: 58rpx;
  margin-right: 12rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f2eadf;
  box-sizing: border-box;
  color: #8f8275;
  font-size: 23rpx;
  font-weight: 760;
  line-height: 58rpx;
  vertical-align: top;
  white-space: nowrap;
}

.small-chip {
  height: 54rpx;
  font-size: 22rpx;
  line-height: 54rpx;
}

.filter-row .small-chip {
  margin-right: 0;
}

.chip-row .filter-chip:last-child,
.chip-row .small-chip:last-child {
  margin-right: 0;
}

.filter-chip.active,
.small-chip.active {
  background: #181711;
  color: #fff7ea;
}

.product-card {
  padding: 24rpx;
}

.product-card.off {
  opacity: .78;
}

.product-image {
  display: flex;
  width: 112rpx;
  height: 112rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 28rpx;
  background:
    linear-gradient(145deg, rgba(225, 180, 116, .24), rgba(180, 104, 42, .12)),
    #f6eadb;
  background-position: center;
  background-size: cover;
  color: #9a6a36;
  font-size: 28rpx;
  font-weight: 880;
}

.product-copy {
  min-width: 0;
  flex: 1;
  margin-left: 20rpx;
}

.product-name {
  min-width: 0;
  overflow: hidden;
  color: #181711;
  font-size: 28rpx;
  font-weight: 860;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill {
  flex-shrink: 0;
  margin-left: 14rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #e7f5ee;
  color: #39745a;
  font-size: 20rpx;
  font-weight: 800;
}

.status-pill.off {
  background: #f8e8e5;
  color: #bd5144;
}

.category {
  display: block;
  overflow: hidden;
  margin-top: 10rpx;
  color: #9d9387;
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-row {
  margin-top: 14rpx;
}

.price {
  color: #bd651c;
  font-size: 27rpx;
  font-weight: 880;
}

.stock {
  color: #6f675d;
  font-size: 22rpx;
  font-weight: 760;
}

.sales-row {
  gap: 14rpx;
  margin-top: 22rpx;
}

.sales-item {
  flex: 1;
  padding: 16rpx 10rpx;
  border-radius: 22rpx;
  background: #faf4ec;
  text-align: center;
}

.sales-value,
.sales-label {
  display: block;
}

.sales-value {
  color: #181711;
  font-size: 28rpx;
  font-weight: 880;
}

.sales-label {
  margin-top: 6rpx;
  color: #9d9387;
  font-size: 20rpx;
}

.card-actions {
  gap: 14rpx;
  margin-top: 22rpx;
}

.action {
  flex: 1;
  height: 64rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 840;
  line-height: 64rpx;
  text-align: center;
}

.action.primary {
  background: #f2e4d1;
  color: #9c6534;
}

.action.primary.restore {
  background: #181711;
  color: #fff7ea;
}

.action.warning {
  background: #f7e5df;
  color: #bd5144;
}

.action.ghost {
  background: #f1e8db;
  color: #8f6b44;
}

.empty-card {
  padding: 56rpx 24rpx;
  text-align: center;
}

.empty-title,
.empty-desc,
.loading-tip {
  display: block;
}

.empty-title {
  color: #292620;
  font-size: 30rpx;
  font-weight: 850;
}

.empty-desc,
.loading-tip {
  color: #9f9588;
  font-size: 22rpx;
}

.empty-desc {
  margin-top: 12rpx;
}

.loading-tip {
  padding: 28rpx 0 10rpx;
  text-align: center;
}

</style>
