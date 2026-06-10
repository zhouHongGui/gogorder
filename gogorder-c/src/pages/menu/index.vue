<template>
  <view class="safe-page menu-page">
    <view class="menu-header">
      <view class="back" @click="goBack">‹</view>
      <view class="shop-copy" @click="switchShop">
        <view class="shop-name-row">
          <text class="shop-name">{{ shop.name }}</text>
          <text class="shop-arrow">›</text>
        </view>
        <text class="shop-status">{{ shop.statusName }} · {{ businessHours(shop) }}</text>
      </view>
      <view class="mode" :class="{ preorder: orderType === 'PREORDER' }">
        {{ orderType === 'PREORDER' ? '预约单' : '即时单' }}
      </view>
    </view>

    <picker
      v-if="orderType === 'PREORDER'"
      mode="selector"
      :range="slotLabels"
      :value="selectedSlotIndex"
      @change="changeSlot"
    >
      <view class="preorder-bar">
        <view>
          <text class="preorder-label">预约取餐时间</text>
          <text class="preorder-value">{{ selectedSlotLabel }}</text>
        </view>
        <text class="preorder-arrow">›</text>
      </view>
    </picker>

    <view class="search-box">
      <view class="search-icon" />
      <input v-model="keyword" confirm-type="search" placeholder="搜索想喝的饮品" @confirm="loadProducts" />
      <text v-if="keyword" class="clear" @click="keyword = ''">×</text>
    </view>

    <view class="menu-body" :class="{ 'has-preorder': orderType === 'PREORDER' }">
      <scroll-view class="category-rail" scroll-y>
        <view
          v-for="category in categories"
          :key="category.id || 'all'"
          class="category-item"
          :class="{ active: selectedCategoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          <text>{{ category.name }}</text>
        </view>
      </scroll-view>

      <scroll-view class="product-panel" scroll-y>
        <view class="panel-heading">
          <text class="panel-title">{{ selectedCategoryName }}</text>
          <text class="panel-count">{{ products.length }} 款</text>
        </view>

        <view v-if="loading" class="loading">菜单加载中...</view>
        <view v-else-if="!products.length" class="empty">
          <view class="empty-mark">GO</view>
          <text>这个分类暂时没有商品</text>
        </view>
        <view v-else class="product-list">
          <view
            v-for="product in products"
            :key="product.productId"
            class="product-item"
            :class="{ soldout: product.soldOut }"
            @click="openProduct(product)"
          >
            <view class="product-image" :style="product.image ? { backgroundImage: `url(${product.image})` } : {}">
              <view v-if="!product.image" class="drink-placeholder">
                <view class="drink-lid" />
                <view class="drink-body">GO</view>
              </view>
              <text v-if="product.soldOut" class="soldout-mark">售罄</text>
            </view>
            <view class="product-copy">
              <text class="product-name">{{ product.name }}</text>
              <text class="product-description">{{ product.description || '现点现做，新鲜好喝' }}</text>
              <text class="sales">近30天售 {{ product.monthlySales || 0 }}</text>
              <view class="product-bottom">
                <text class="price"><text>¥</text>{{ money(product.displayPrice) }}<text class="from">起</text></text>
                <view class="add" :class="{ disabled: product.soldOut }">+</view>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <view v-if="detail" class="detail-mask" @click="closeDetail">
      <view class="detail-sheet" @click.stop>
        <view class="sheet-handle" />
        <view class="detail-top">
          <view class="detail-image" :style="detail.image ? { backgroundImage: `url(${detail.image})` } : {}">
            <view v-if="!detail.image" class="large-drink">
              <view class="large-lid" />
              <view class="large-body">GO</view>
            </view>
          </view>
          <view class="detail-copy">
            <text class="detail-name">{{ detail.name }}</text>
            <text class="detail-description">{{ detail.description }}</text>
            <view class="tag-row">
              <text v-for="tag in detail.tags" :key="tag" class="tag">{{ tag }}</text>
            </view>
          </view>
          <text class="close" @click="closeDetail">×</text>
        </view>

        <scroll-view class="spec-scroll" scroll-y>
          <view v-for="spec in detail.specs" :key="spec.templateId" class="spec-group">
            <view class="spec-heading">
              <text class="spec-name">{{ spec.name }}</text>
              <text class="spec-rule">
                {{ spec.required ? '必选' : '可选' }} · {{ spec.type === 1 ? '单选' : `最多选${spec.maxSelect}项` }}
              </text>
            </view>
            <view class="option-list">
              <view
                v-for="option in spec.options"
                :key="option.optionId"
                class="option"
                :class="{ selected: isSelected(spec, option) }"
                @click="selectOption(spec, option)"
              >
                <text>{{ option.label }}</text>
                <text v-if="option.priceAdd">+¥{{ money(option.priceAdd) }}</text>
              </view>
            </view>
          </view>
        </scroll-view>

        <view class="detail-footer">
          <view>
            <text class="footer-label">已选规格价格</text>
            <text class="footer-price"><text>¥</text>{{ money(selectedPrice) }}</text>
          </view>
          <button class="confirm-button" :disabled="detail.soldOut" @click="confirmSpecs">
            {{ detail.soldOut ? '商品已售罄' : '选好了' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { getCategories, getProductDetail, getProducts } from '../../api/product'
import { getPreorderSlots } from '../../api/shop'
import { businessHours, getCurrentShop } from '../../utils/shop'

const shop = ref(getCurrentShop() || {})
const orderType = ref('NORMAL')
const categories = ref([])
const selectedCategoryId = ref(null)
const products = ref([])
const keyword = ref('')
const loading = ref(false)
const detail = ref(null)
const selections = reactive({})
const slots = ref([])
const selectedSlotIndex = ref(0)
let searchTimer

const selectedCategoryName = computed(() => categories.value.find(item => item.id === selectedCategoryId.value)?.name || '全部商品')
const slotLabels = computed(() => slots.value.map(slot => `${slot.dateLabel} ${slot.timeLabel}`))
const selectedSlotLabel = computed(() => slotLabels.value[selectedSlotIndex.value] || '暂无可预约时间')
const selectedPrice = computed(() => {
  if (!detail.value) return 0
  return detail.value.price + detail.value.specs.reduce((total, spec) => {
    const selected = selections[spec.templateId]
    const ids = Array.isArray(selected) ? selected : selected ? [selected] : []
    return total + spec.options
      .filter(option => ids.includes(option.optionId))
      .reduce((sum, option) => sum + (option.priceAdd || 0), 0)
  }, 0)
})

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadProducts, 350)
})

onLoad(async options => {
  if (!shop.value.id) {
    uni.redirectTo({ url: '/pages/shop/select' })
    return
  }
  orderType.value = options.orderType === 'PREORDER' || !shop.value.instantAvailable ? 'PREORDER' : 'NORMAL'
  await Promise.all([loadCategories(), loadProducts()])
  if (orderType.value === 'PREORDER') loadSlots()
})

onShow(() => {
  const selected = getCurrentShop()
  if (!shop.value.id || !selected?.id || selected.id === shop.value.id) return
  shop.value = selected
  selectedCategoryId.value = null
  if (!selected.instantAvailable) orderType.value = 'PREORDER'
  loadCategories()
  loadProducts()
  if (orderType.value === 'PREORDER') loadSlots()
})

async function loadCategories() {
  const list = await getCategories(shop.value.id)
  categories.value = [{ id: null, name: '全部' }, ...list]
}

async function loadProducts() {
  loading.value = true
  try {
    products.value = await getProducts({
      shopId: shop.value.id,
      categoryId: selectedCategoryId.value,
      keyword: keyword.value.trim()
    })
  } catch (error) {
    products.value = []
  } finally {
    loading.value = false
  }
}

async function loadSlots() {
  try {
    slots.value = await getPreorderSlots(shop.value.id)
  } catch (error) {
    slots.value = []
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  loadProducts()
}

async function openProduct(product) {
  if (product.soldOut) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  detail.value = await getProductDetail(shop.value.id, product.productId)
  initializeSelections(detail.value.specs)
}

function initializeSelections(specs) {
  Object.keys(selections).forEach(key => delete selections[key])
  specs.forEach(spec => {
    const defaults = spec.options.filter(option => option.isDefault === 1).map(option => option.optionId)
    if (spec.type === 1) {
      selections[spec.templateId] = defaults[0] || (spec.required ? spec.options[0]?.optionId : '')
    } else {
      const requiredCount = spec.required ? Math.max(1, spec.minSelect || 1) : 0
      selections[spec.templateId] = defaults.length ? defaults.slice(0, spec.maxSelect) : spec.options.slice(0, requiredCount).map(option => option.optionId)
    }
  })
}

function isSelected(spec, option) {
  const selected = selections[spec.templateId]
  return Array.isArray(selected) ? selected.includes(option.optionId) : selected === option.optionId
}

function selectOption(spec, option) {
  if (spec.type === 1) {
    if (!spec.required && selections[spec.templateId] === option.optionId) selections[spec.templateId] = ''
    else selections[spec.templateId] = option.optionId
    return
  }
  const selected = [...(selections[spec.templateId] || [])]
  const index = selected.indexOf(option.optionId)
  if (index >= 0) {
    selected.splice(index, 1)
  } else if (selected.length < spec.maxSelect) {
    selected.push(option.optionId)
  } else {
    uni.showToast({ title: `最多选择${spec.maxSelect}项`, icon: 'none' })
  }
  selections[spec.templateId] = selected
}

function confirmSpecs() {
  const invalid = detail.value.specs.find(spec => {
    const selected = selections[spec.templateId]
    const count = Array.isArray(selected) ? selected.length : selected ? 1 : 0
    return spec.required && count < Math.max(1, spec.minSelect || 1)
  })
  if (invalid) {
    uni.showToast({ title: `请选择${invalid.name}`, icon: 'none' })
    return
  }
  uni.showToast({ title: '规格已选择，购物车即将开放', icon: 'none' })
  closeDetail()
}

function changeSlot(event) {
  selectedSlotIndex.value = Number(event.detail.value)
}

function closeDetail() {
  detail.value = null
}

function switchShop() {
  uni.navigateTo({ url: '/pages/shop/select' })
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: '/pages/index/index' })
  })
}

function money(cents) {
  const value = (Number(cents || 0) / 100).toFixed(2)
  return value.endsWith('.00') ? value.slice(0, -3) : value
}
</script>

<style lang="scss" scoped>
.menu-page {
  height: 100vh;
  overflow: hidden;
  background: #f5f3ee;
}

.menu-header {
  display: flex;
  height: calc(var(--status-bar-height) + 106rpx);
  align-items: flex-end;
  padding: var(--status-bar-height) 26rpx 20rpx;
  background: #fff;
  box-sizing: border-box;
}

.back {
  width: 54rpx;
  height: 54rpx;
  border-radius: 18rpx;
  background: #f4f1eb;
  color: #3b352e;
  font-size: 44rpx;
  line-height: 48rpx;
  text-align: center;
}

.shop-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}

.shop-name-row {
  display: flex;
  align-items: center;
}

.shop-name {
  overflow: hidden;
  max-width: 370rpx;
  color: #24211c;
  font-size: 28rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-arrow {
  margin-left: 7rpx;
  color: #8d857b;
  font-size: 31rpx;
}

.shop-status {
  display: block;
  margin-top: 5rpx;
  color: #948c81;
  font-size: 19rpx;
}

.mode {
  padding: 9rpx 14rpx;
  border-radius: 18rpx;
  background: #e4f1e8;
  color: #367c50;
  font-size: 18rpx;
}

.mode.preorder {
  background: #f3e7d5;
  color: #9d6a31;
}

.preorder-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 19rpx 28rpx;
  background: #2a261f;
  color: #fff;
}

.preorder-label,
.preorder-value {
  display: block;
}

.preorder-label {
  color: #c7bfb2;
  font-size: 18rpx;
}

.preorder-value {
  margin-top: 5rpx;
  font-size: 24rpx;
  font-weight: 700;
}

.preorder-arrow {
  color: #d5b27f;
  font-size: 35rpx;
}

.search-box {
  display: flex;
  align-items: center;
  margin: 18rpx 24rpx;
  padding: 0 20rpx;
  border-radius: 25rpx;
  background: #fff;
}

.search-icon {
  width: 17rpx;
  height: 17rpx;
  margin-right: 14rpx;
  border: 4rpx solid #928a80;
  border-radius: 50%;
}

.search-box input {
  height: 70rpx;
  flex: 1;
  font-size: 23rpx;
}

.clear {
  padding: 10rpx;
  color: #a49c92;
  font-size: 31rpx;
}

.menu-body {
  display: flex;
  height: calc(100vh - var(--status-bar-height) - 212rpx);
  min-height: 0;
}

.menu-body.has-preorder {
  height: calc(100vh - var(--status-bar-height) - 300rpx);
}

.category-rail {
  width: 156rpx;
  flex-shrink: 0;
  background: #ebe7df;
}

.category-item {
  display: flex;
  position: relative;
  min-height: 94rpx;
  align-items: center;
  justify-content: center;
  padding: 12rpx 16rpx;
  box-sizing: border-box;
  color: #756e64;
  font-size: 22rpx;
  text-align: center;
}

.category-item.active {
  background: #fff;
  color: #25221d;
  font-weight: 800;
}

.category-item.active::before {
  position: absolute;
  left: 0;
  width: 7rpx;
  height: 38rpx;
  border-radius: 0 6rpx 6rpx 0;
  background: #b87939;
  content: "";
}

.product-panel {
  min-width: 0;
  flex: 1;
  padding: 0 20rpx 40rpx;
  background: #fff;
  box-sizing: border-box;
}

.panel-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 19rpx 0;
}

.panel-title {
  color: #302c26;
  font-size: 26rpx;
  font-weight: 800;
}

.panel-count {
  color: #a29a90;
  font-size: 18rpx;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 27rpx;
}

.product-item {
  display: flex;
  min-height: 174rpx;
}

.product-item.soldout {
  opacity: 0.55;
}

.product-image {
  display: flex;
  position: relative;
  width: 170rpx;
  height: 170rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #f3e7d3, #e3c69f);
  background-position: center;
  background-size: cover;
}

.drink-placeholder {
  position: relative;
  width: 88rpx;
  height: 125rpx;
  transform: rotate(4deg);
}

.drink-lid {
  position: absolute;
  z-index: 2;
  width: 88rpx;
  height: 15rpx;
  border-radius: 10rpx;
  background: #302a23;
}

.drink-body {
  display: flex;
  position: absolute;
  top: 10rpx;
  left: 7rpx;
  width: 74rpx;
  height: 110rpx;
  align-items: center;
  justify-content: center;
  border-radius: 7rpx 7rpx 25rpx 25rpx;
  background: #c7833f;
  color: #fff;
  font-size: 18rpx;
  font-weight: 900;
}

.soldout-mark {
  position: absolute;
  padding: 7rpx 14rpx;
  border-radius: 17rpx;
  background: rgba(38, 34, 29, 0.8);
  color: #fff;
  font-size: 18rpx;
}

.product-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}

.product-name,
.product-description,
.sales {
  display: block;
}

.product-name {
  color: #29251f;
  font-size: 26rpx;
  font-weight: 800;
}

.product-description {
  overflow: hidden;
  margin-top: 8rpx;
  color: #918a80;
  font-size: 18rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sales {
  margin-top: 10rpx;
  color: #aaa399;
  font-size: 17rpx;
}

.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
}

.price {
  color: #29251f;
  font-size: 29rpx;
  font-weight: 850;
}

.price > text:first-child {
  font-size: 17rpx;
}

.price .from {
  margin-left: 4rpx;
  color: #8e877d;
  font-size: 16rpx;
  font-weight: 400;
}

.add {
  width: 43rpx;
  height: 43rpx;
  border-radius: 50%;
  background: #1c1b17;
  color: #fff;
  font-size: 29rpx;
  line-height: 40rpx;
  text-align: center;
}

.add.disabled {
  background: #aaa49b;
}

.loading,
.empty {
  display: flex;
  min-height: 430rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #9b948a;
  font-size: 20rpx;
}

.empty-mark {
  width: 78rpx;
  height: 78rpx;
  margin-bottom: 20rpx;
  border-radius: 26rpx;
  background: #1c1b17;
  color: #e6b879;
  font-weight: 850;
  line-height: 78rpx;
  text-align: center;
}

.detail-mask {
  display: flex;
  position: fixed;
  z-index: 1000;
  inset: 0;
  align-items: flex-end;
  background: rgba(27, 24, 20, 0.48);
}

.detail-sheet {
  width: 100%;
  max-height: 88vh;
  overflow: hidden;
  border-radius: 38rpx 38rpx 0 0;
  background: #f8f6f1;
}

.sheet-handle {
  width: 70rpx;
  height: 7rpx;
  margin: 15rpx auto 9rpx;
  border-radius: 6rpx;
  background: #d4cec5;
}

.detail-top {
  display: flex;
  position: relative;
  padding: 17rpx 28rpx 25rpx;
}

.detail-image {
  display: flex;
  width: 220rpx;
  height: 220rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 29rpx;
  background: linear-gradient(145deg, #f1dfc5, #d9b680);
  background-position: center;
  background-size: cover;
}

.large-drink {
  position: relative;
  width: 108rpx;
  height: 155rpx;
  transform: rotate(5deg);
}

.large-lid {
  position: absolute;
  z-index: 2;
  width: 108rpx;
  height: 18rpx;
  border-radius: 11rpx;
  background: #2c2822;
}

.large-body {
  display: flex;
  position: absolute;
  top: 12rpx;
  left: 8rpx;
  width: 92rpx;
  height: 137rpx;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx 8rpx 31rpx 31rpx;
  background: #c17b39;
  color: #fff;
  font-size: 23rpx;
  font-weight: 900;
}

.detail-copy {
  min-width: 0;
  flex: 1;
  margin-left: 22rpx;
  padding-top: 9rpx;
}

.detail-name,
.detail-description {
  display: block;
}

.detail-name {
  color: #29251f;
  font-size: 34rpx;
  font-weight: 850;
}

.detail-description {
  margin-top: 13rpx;
  color: #8e867b;
  font-size: 20rpx;
  line-height: 1.6;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
  margin-top: 15rpx;
}

.tag {
  padding: 5rpx 9rpx;
  border-radius: 9rpx;
  background: #eee5d8;
  color: #8e683d;
  font-size: 16rpx;
}

.close {
  position: absolute;
  top: 7rpx;
  right: 23rpx;
  padding: 10rpx;
  color: #80786e;
  font-size: 37rpx;
}

.spec-scroll {
  max-height: 44vh;
  padding: 0 28rpx;
  box-sizing: border-box;
}

.spec-group {
  padding: 24rpx 0;
  border-top: 1rpx solid #e9e4dc;
}

.spec-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spec-name {
  color: #302c26;
  font-size: 25rpx;
  font-weight: 800;
}

.spec-rule {
  color: #9b9388;
  font-size: 17rpx;
}

.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 13rpx;
  margin-top: 18rpx;
}

.option {
  display: flex;
  min-width: 138rpx;
  justify-content: space-between;
  padding: 14rpx 18rpx;
  border: 2rpx solid transparent;
  border-radius: 18rpx;
  background: #eeeae3;
  box-sizing: border-box;
  color: #655e54;
  font-size: 20rpx;
}

.option.selected {
  border-color: #a97740;
  background: #f3e4d0;
  color: #75491f;
  font-weight: 700;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 28rpx calc(22rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #e8e3db;
  background: #fff;
}

.footer-label,
.footer-price {
  display: block;
}

.footer-label {
  color: #999186;
  font-size: 17rpx;
}

.footer-price {
  margin-top: 3rpx;
  color: #27231e;
  font-size: 35rpx;
  font-weight: 850;
}

.footer-price text {
  font-size: 19rpx;
}

.confirm-button {
  width: 260rpx;
  height: 78rpx;
  margin: 0;
  border-radius: 39rpx;
  background: #1c1b17;
  color: #fff;
  font-size: 25rpx;
  font-weight: 750;
  line-height: 78rpx;
}
</style>
