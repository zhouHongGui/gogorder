<template>
  <view class="safe-page menu-page">
    <view class="fixed-header">
      <view class="brand-hero">
        <view class="menu-header">
          <view class="back home-button" aria-label="返回首页" @click="goBack">
            <view class="home-icon">
              <view class="home-roof" />
              <view class="home-body">
                <view class="home-door" />
              </view>
            </view>
          </view>
          <image class="header-logo" src="/static/brand/store-logo.png" mode="aspectFit" />
          <view class="header-spacer" />
          <view class="shop-detail-link" @click="openShopDetail">门店详情</view>
        </view>

        <view class="service-card">
          <view class="service-copy" @click="switchShop">
            <view class="shop-name-row">
              <text class="shop-name">{{ shop.name }}</text>
              <text class="shop-arrow">›</text>
            </view>
            <view class="distance-row">
              <text class="distance-label">距你</text>
              <text class="distance-value">{{ formatDistance(shop.distance) }}</text>
            </view>
            <view class="shop-meta">
              <view class="status-dot" :class="{ unavailable: !shop.instantAvailable }" />
              <text>{{ shop.statusName }}</text>
              <text class="meta-divider">·</text>
              <text>{{ businessHours(shop) }}</text>
            </view>
          </view>
          <view class="mode-switch">
            <view
              class="mode-option"
              :class="{ active: serviceMode === 'PICKUP' }"
              @click="switchServiceMode('PICKUP')"
            >
              自取
            </view>
            <view
              class="mode-option"
              :class="{ active: serviceMode === 'DELIVERY' }"
              @click="switchServiceMode('DELIVERY')"
            >
              外卖
            </view>
          </view>
        </view>
      </view>

      <view v-if="orderType === 'PREORDER'" class="preorder-bar" @click="showPreorderTiming">
        <view class="preorder-icon">
          <view class="clock-hand clock-hour" />
          <view class="clock-hand clock-minute" />
        </view>
        <view class="preorder-copy">
          <text class="preorder-label">预约自取单</text>
          <text class="preorder-value">提交订单时选择取餐时间</text>
        </view>
        <text class="preorder-action">下单时选择</text>
      </view>

      <view class="search-box">
        <view class="search-icon" />
        <input v-model="keyword" confirm-type="search" placeholder="搜索饮品、咖啡或小食" @confirm="loadProducts" />
        <text v-if="keyword" class="clear" @click="keyword = ''">×</text>
      </view>
    </view>

    <view class="menu-body">
      <scroll-view class="category-rail" scroll-y :show-scrollbar="false">
        <view
          v-for="category in categories"
          :key="category.id || 'all'"
          class="category-item"
          :class="{ active: selectedCategoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          <view class="category-active-mark" />
          <text>{{ category.name }}</text>
        </view>
      </scroll-view>

      <scroll-view class="product-panel" scroll-y :show-scrollbar="false">
        <view class="panel-heading">
          <text class="panel-title">{{ selectedCategoryName }}</text>
          <text class="panel-count">{{ products.length }} 款</text>
        </view>

        <view v-if="loading" class="loading">
          <view class="loading-ring" />
          <text>正在准备菜单...</text>
        </view>
        <view v-else-if="productsLoadError" class="empty">
          <view class="empty-mark">!</view>
          <text>{{ productsLoadError }}</text>
          <text class="empty-copy retry-copy" @click="loadProducts">点击重新加载</text>
        </view>
        <view v-else-if="!products.length" class="empty">
          <view class="empty-mark">GO</view>
          <text>这个分类暂时没有商品</text>
          <text class="empty-copy">换个分类或搜索关键词试试</text>
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
              <text v-if="product.tags?.[0] && !product.soldOut" class="image-tag">{{ product.tags[0] }}</text>
              <text v-if="product.soldOut" class="soldout-mark">售罄</text>
            </view>
            <view class="product-copy">
              <view class="product-name-row">
                <text class="product-name">{{ product.name }}</text>
                <text v-if="product.hasSpecs" class="spec-tip">可选规格</text>
              </view>
              <text class="product-description">{{ product.description || '现点现做，新鲜好喝' }}</text>
              <view class="product-meta">
                <text>近30天售 {{ product.monthlySales || 0 }}</text>
                <text v-if="product.stock !== -1">剩余 {{ product.stock }}</text>
              </view>
              <view class="product-bottom">
                <text class="price"><text>¥</text>{{ money(product.displayPrice) }}<text class="from">起</text></text>
                <view
                  :id="`product-add-${product.productId}`"
                  class="add"
                  :class="{ disabled: product.soldOut }"
                  @click.stop="openProduct(product)"
                >
                  +
                </view>
              </view>
            </view>
          </view>
        </view>
        <view class="product-bottom-space" />
      </scroll-view>
    </view>

    <view v-if="cart.totalCount" class="cart-bar" :class="{ bump: cartBump }">
      <view class="cart-summary" @click="openCart">
        <view id="cart-target" class="cart-icon">
          <view class="cart-basket" />
          <text v-if="cart.totalCount" class="cart-badge">{{ cart.totalCount > 99 ? '99+' : cart.totalCount }}</text>
        </view>
        <view class="cart-total-copy">
          <text class="cart-total"><text>¥</text>{{ money(cart.totalAmount) }}</text>
          <text class="cart-hint">{{ cartHint }}</text>
        </view>
      </view>
      <button class="checkout-button" :disabled="!cart.totalCount" @click="checkout">
        去结算
      </button>
    </view>

    <view v-if="flyCartVisible" class="cart-fly-x" :style="flyCartPosition" :animation="flyXAnimation">
      <view class="cart-fly-y" :animation="flyYAnimation">
        <image v-if="flyCartImage" class="cart-fly-image" :src="flyCartImage" mode="aspectFill" />
        <view v-else class="cart-fly-placeholder">+</view>
      </view>
    </view>

    <view v-if="cartVisible" class="cart-mask" @click="cartVisible = false">
      <view class="cart-sheet" @click.stop>
        <view class="cart-sheet-handle" />
        <view class="cart-sheet-heading">
          <view>
            <view class="cart-sheet-title-row">
              <text class="cart-sheet-title">购物车</text>
              <text v-if="cart.totalCount" class="cart-sheet-count">共 {{ cart.totalCount }} 件</text>
            </view>
            <text class="cart-sheet-shop">{{ cart.shopName || shop.name }}</text>
          </view>
          <text v-if="cart.items.length" class="clear-cart" @click="confirmClearCart">清空</text>
        </view>

        <scroll-view class="cart-items" scroll-y :show-scrollbar="false">
          <view v-if="!cart.items.length" class="cart-empty">
            <view class="cart-empty-icon">GO</view>
            <text>购物车还是空的</text>
          </view>
          <view v-for="item in cart.items" :key="item.cartItemId" class="cart-item">
            <view class="cart-item-image" :style="item.image ? { backgroundImage: `url(${item.image})` } : {}">
              <text v-if="!item.image">GO</text>
            </view>
            <view class="cart-item-copy">
              <text class="cart-item-name">{{ item.productName }}</text>
              <text class="cart-item-spec">{{ item.specText || '默认规格' }}</text>
              <text class="cart-item-price">¥{{ money(item.unitPrice) }}</text>
            </view>
            <view class="quantity-control">
              <view class="quantity-button minus" @click="changeCartQuantity(item, item.quantity - 1)">−</view>
              <text class="quantity-value">{{ item.quantity }}</text>
              <view class="quantity-button" @click="changeCartQuantity(item, item.quantity + 1)">+</view>
            </view>
          </view>
          <view class="cart-list-space" />
        </scroll-view>

        <view class="cart-sheet-footer">
          <view>
            <text class="cart-footer-label">合计</text>
            <text class="cart-footer-total">¥{{ money(cart.totalAmount) }}</text>
          </view>
          <button class="cart-checkout" :disabled="!cart.totalCount" @click="checkout">
            去结算
          </button>
        </view>
      </view>
    </view>

    <view v-if="detail" class="detail-mask" @click="closeDetail">
      <view class="detail-sheet" @click.stop>
        <view class="sheet-handle" />
        <view class="detail-hero">
          <view class="detail-image" :style="detail.image ? { backgroundImage: `url(${detail.image})` } : {}">
            <view v-if="!detail.image" class="large-drink">
              <view class="large-lid" />
              <view class="large-body">GO</view>
            </view>
          </view>
          <view class="detail-image-shade" />
          <view class="detail-copy">
            <text class="detail-kicker">FRESHLY MADE</text>
            <text class="detail-name">{{ detail.name }}</text>
            <text class="detail-description">{{ detail.description || '现点现做，新鲜好喝' }}</text>
            <view class="tag-row">
              <text v-for="tag in detail.tags" :key="tag" class="tag">{{ tag }}</text>
            </view>
          </view>
          <view class="close" @click="closeDetail">×</view>
        </view>

        <scroll-view class="spec-scroll" scroll-y>
          <view v-for="spec in detail.specs" :key="spec.templateId" class="spec-group">
            <view class="spec-heading">
              <text class="spec-name">{{ spec.name }}</text>
              <text class="spec-rule" :class="{ required: spec.required }">
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
            <text class="footer-label">{{ selectedSpecSummary }}</text>
            <text class="footer-price"><text>¥</text>{{ money(selectedPrice) }}</text>
          </view>
          <button class="confirm-button" :disabled="detail.soldOut" @click="confirmSpecs">
            {{ detail.soldOut ? '商品已售罄' : '加入购物车' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { addCartItem, clearCart, getCart, removeCartItem, updateCartItem } from '../../api/cart'
import { getCategories, getProductDetail, getProducts } from '../../api/product'
import { businessHours, formatDistance, getCurrentShop } from '../../utils/shop'
import type { AddCartItemRequest, Cart, CartItem } from '../../types/cart'
import type { MenuCategory, Product, ProductSpec, SpecOption, SpecSelections } from '../../types/product'
import type { OrderType, Shop } from '../../types/shop'

type ServiceMode = 'PICKUP' | 'DELIVERY'

const emptyShop: Shop = {
  id: 0,
  name: '',
  address: '',
  status: 0,
  statusName: '',
  distance: null,
  isOpen: false,
  instantAvailable: false,
  preorderAvailable: true
}
const createEmptyCart = (currentShop?: Shop): Cart => ({
  shopId: currentShop?.id || null,
  shopName: currentShop?.name || '',
  items: [],
  totalAmount: 0,
  totalCount: 0
})
const shop = ref<Shop>(getCurrentShop() || emptyShop)
const orderType = ref<OrderType>('NORMAL')
const serviceMode = ref<ServiceMode>('PICKUP')
const categories = ref<MenuCategory[]>([])
const selectedCategoryId = ref<number | null>(null)
const products = ref<Product[]>([])
const keyword = ref('')
const loading = ref(false)
const productsLoadError = ref('')
const detail = ref<Product | null>(null)
const selections = reactive<SpecSelections>({})
const cart = ref<Cart>(createEmptyCart(shop.value))
const cartVisible = ref(false)
const cartBusy = ref(false)
const flyCartVisible = ref(false)
const flyCartImage = ref('')
const cartBump = ref(false)
const flyCartPosition = reactive({ left: '0px', top: '0px' })
const flyXAnimation = ref<Record<string, unknown>>({})
const flyYAnimation = ref<Record<string, unknown>>({})
const productFlyOrigin = ref<{ x: number; y: number } | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | undefined
let flyCartTimer: ReturnType<typeof setTimeout> | undefined
let cartBumpTimer: ReturnType<typeof setTimeout> | undefined
let categoriesRequestId = 0
let productsRequestId = 0
let cartRequestId = 0
let cartMutationShopId: number | null = null

const selectedCategoryName = computed(() => categories.value.find(item => item.id === selectedCategoryId.value)?.name || '全部商品')
const cartHint = computed(() => {
  if (!cart.value.totalCount) return '购物车是空的'
  return `已选 ${cart.value.totalCount} 件`
})
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
const selectedSpecSummary = computed(() => {
  if (!detail.value?.specs.length) return '默认规格'
  const labels = detail.value.specs.flatMap(spec => {
    const selected = selections[spec.templateId]
    const ids = Array.isArray(selected) ? selected : selected ? [selected] : []
    return spec.options.filter(option => ids.includes(option.optionId)).map(option => option.label)
  })
  return labels.length ? labels.join('、') : '请选择规格'
})

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadProducts, 350)
})

onLoad(async (options?: Record<string, unknown>) => {
  if (!shop.value.id) {
    uni.redirectTo({ url: '/pages/shop/select' })
    return
  }
  orderType.value = options?.orderType === 'PREORDER' || !shop.value.instantAvailable ? 'PREORDER' : 'NORMAL'
  await Promise.all([loadCategories(), loadProducts(), loadCart()])
})

onShow(() => {
  const selected = getCurrentShop()
  if (shop.value.id && selected?.id && selected.id !== shop.value.id) {
    shop.value = selected
    selectedCategoryId.value = null
    categories.value = []
    products.value = []
    productsLoadError.value = ''
    cart.value = createEmptyCart(selected)
    cartVisible.value = false
    if (!selected.instantAvailable) orderType.value = 'PREORDER'
    loadCategories()
    loadProducts()
  }
  loadCart()
})

async function loadCategories() {
  const shopId = shop.value?.id
  if (!shopId) return
  const requestId = ++categoriesRequestId
  try {
    const list = await getCategories(shopId)
    if (requestId !== categoriesRequestId || shop.value.id !== shopId) return
    categories.value = [{ id: null, name: '全部' }, ...list]
  } catch (error) {
    if (requestId !== categoriesRequestId || shop.value.id !== shopId) return
    showLoadError('分类加载失败，请重试')
  }
}

async function loadProducts() {
  const shopId = shop.value?.id
  if (!shopId) return
  const requestId = ++productsRequestId
  const categoryId = selectedCategoryId.value
  const searchKeyword = keyword.value.trim()
  loading.value = true
  productsLoadError.value = ''
  try {
    const list = await getProducts({
      shopId,
      categoryId,
      keyword: searchKeyword
    })
    if (requestId !== productsRequestId || shop.value.id !== shopId) return
    products.value = list
  } catch (error) {
    if (requestId !== productsRequestId || shop.value.id !== shopId) return
    productsLoadError.value = '菜单加载失败，请稍后重试'
    showLoadError('菜单加载失败，请重试')
  } finally {
    if (requestId === productsRequestId && shop.value.id === shopId) loading.value = false
  }
}

function selectCategory(id: number | null) {
  selectedCategoryId.value = id
  loadProducts()
}

async function openProduct(product: Product) {
  if (product.soldOut) {
    uni.showToast({ title: '商品已售罄', icon: 'none' })
    return
  }
  const shopId = shop.value?.id
  if (!shopId) return
  const originPromise = captureProductFlyOrigin(product.productId)
  detail.value = await getProductDetail(shopId, product.productId)
  await originPromise
  initializeSelections(detail.value.specs)
}

function initializeSelections(specs: ProductSpec[]) {
  Object.keys(selections).forEach(key => delete selections[Number(key)])
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

function isSelected(spec: ProductSpec, option: SpecOption): boolean {
  const selected = selections[spec.templateId]
  return Array.isArray(selected) ? selected.includes(option.optionId) : selected === option.optionId
}

function selectOption(spec: ProductSpec, option: SpecOption) {
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

async function confirmSpecs() {
  if (!detail.value) return
  const invalid = detail.value.specs.find(spec => {
    const selected = selections[spec.templateId]
    const count = Array.isArray(selected) ? selected.length : selected ? 1 : 0
    return spec.required && count < Math.max(1, spec.minSelect || 1)
  })
  if (invalid) {
    uni.showToast({ title: `请选择${invalid.name}`, icon: 'none' })
    return
  }
  await addSelectedProduct()
}

async function loadCart() {
  const shopId = shop.value?.id
  if (!shopId) return
  if (cartBusy.value && cartMutationShopId === shopId) return
  const requestId = ++cartRequestId
  try {
    const loadedCart = await getCart(shopId)
    if (requestId !== cartRequestId || shop.value.id !== shopId) return
    cart.value = loadedCart
  } catch (error) {
    if (requestId !== cartRequestId || shop.value.id !== shopId) return
    showLoadError('购物车加载失败，请重试')
  }
}

function showLoadError(title: string) {
  uni.showToast({ title, icon: 'none' })
}

function selectedSpecs(): Record<string, string | string[]> {
  return Object.fromEntries(
    Object.entries(selections)
      .filter(([, value]) => Array.isArray(value) ? value.length : Boolean(value))
      .map(([templateId, value]) => [templateId, value])
  )
}

async function addSelectedProduct() {
  if (!detail.value || cartBusy.value) return
  const shopId = shop.value.id
  const image = detail.value.image
  const request: AddCartItemRequest = {
    shopId,
    productId: detail.value.productId,
    specs: selectedSpecs(),
    quantity: 1
  }
  cartBusy.value = true
  cartMutationShopId = shopId
  ++cartRequestId
  try {
    const updatedCart = await addCartItem(request)
    if (shop.value.id !== shopId) return
    cart.value = updatedCart
    closeDetail()
    await nextTick()
    await playAddToCartAnimation(image)
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加入购物车失败', icon: 'none' })
  } finally {
    cartMutationShopId = null
    cartBusy.value = false
  }
}

function openCart() {
  cartVisible.value = true
}

async function captureProductFlyOrigin(productId: number) {
  const rect = await queryRect(`#product-add-${productId}`)
  if (rect) productFlyOrigin.value = rectCenter(rect)
}

async function playAddToCartAnimation(image?: string) {
  clearTimeout(flyCartTimer)
  clearTimeout(cartBumpTimer)
  const targetRect = await queryRect('#cart-target')
  const systemInfo = uni.getSystemInfoSync()
  const origin = productFlyOrigin.value
  if (!targetRect || !origin) {
    cartBump.value = true
    cartBumpTimer = setTimeout(() => {
      cartBump.value = false
    }, 320)
    return
  }
  const target = rectCenter(targetRect)
  const ballSize = systemInfo.windowWidth * 64 / 750
  flyCartPosition.left = `${origin.x - ballSize / 2}px`
  flyCartPosition.top = `${origin.y - ballSize / 2}px`
  flyCartImage.value = image || ''
  flyCartVisible.value = false
  cartBump.value = false
  flyXAnimation.value = {}
  flyYAnimation.value = {}
  await nextTick()
  flyCartVisible.value = true
  await nextTick()

  const xAnimation = uni.createAnimation({ duration: 680, timingFunction: 'ease-out' })
  xAnimation.translateX(target.x - origin.x).step()
  flyXAnimation.value = xAnimation.export()

  const rise = Math.max(100, Math.min(220, Math.abs(target.y - origin.y) * 0.45))
  const yAnimation = uni.createAnimation({ duration: 300, timingFunction: 'ease-out' })
  yAnimation.translateY(-rise).scale(0.9, 0.9).rotate(-12).step()
  yAnimation.translateY(target.y - origin.y).scale(0.58, 0.58).rotate(16).step({
    duration: 380,
    timingFunction: 'ease-in'
  })
  flyYAnimation.value = yAnimation.export()

  flyCartTimer = setTimeout(() => {
    flyCartVisible.value = false
    cartBump.value = true
    cartBumpTimer = setTimeout(() => {
      cartBump.value = false
    }, 320)
  }, 700)
}

function queryRect(selector: string): Promise<UniApp.NodeInfo | null> {
  return new Promise(resolve => {
    uni.createSelectorQuery()
      .select(selector)
      .boundingClientRect(rect => resolve(Array.isArray(rect) ? rect[0] || null : rect || null))
      .exec()
  })
}

function rectCenter(rect: UniApp.NodeInfo) {
  return {
    x: Number(rect.left || 0) + Number(rect.width || 0) / 2,
    y: Number(rect.top || 0) + Number(rect.height || 0) / 2
  }
}

async function changeCartQuantity(item: CartItem, quantity: number) {
  if (cartBusy.value) return
  const shopId = shop.value.id
  cartBusy.value = true
  cartMutationShopId = shopId
  ++cartRequestId
  try {
    const updatedCart = quantity <= 0
      ? await removeCartItem(shopId, item.cartItemId)
      : await updateCartItem({ shopId, cartItemId: item.cartItemId, quantity })
    if (shop.value.id !== shopId) return
    cart.value = updatedCart
    if (!cart.value.items.length) cartVisible.value = false
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '更新购物车失败', icon: 'none' })
  } finally {
    cartMutationShopId = null
    cartBusy.value = false
  }
}

async function confirmClearCart() {
  const result = await uni.showModal({
    title: '清空购物车',
    content: '确定移除购物车中的全部商品吗？',
    confirmText: '清空'
  })
  if (!result.confirm) return
  if (cartBusy.value) return
  const shopId = shop.value.id
  cartBusy.value = true
  cartMutationShopId = shopId
  ++cartRequestId
  try {
    const updatedCart = await clearCart(shopId)
    if (shop.value.id !== shopId) return
    cart.value = updatedCart
    cartVisible.value = false
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '清空购物车失败', icon: 'none' })
  } finally {
    cartMutationShopId = null
    cartBusy.value = false
  }
}

function checkout() {
  if (!cart.value.totalCount) return
  uni.showToast({ title: '确认订单功能将在下一阶段开放', icon: 'none' })
}

function switchServiceMode(mode: ServiceMode) {
  if (mode === 'DELIVERY') {
    uni.showToast({ title: '外卖功能即将开放', icon: 'none' })
    return
  }
  serviceMode.value = mode
}

function showPreorderTiming() {
  uni.showModal({
    title: '预约取餐时间',
    content: '提交预约单时将重新获取门店可预约时段，并校验所选时间是否仍然有效。',
    showCancel: false
  })
}

function closeDetail() {
  detail.value = null
}

function openShopDetail() {
  if (!shop.value.id) return
  uni.navigateTo({ url: `/pages/shop/detail?id=${shop.value.id}` })
}

function switchShop() {
  uni.navigateTo({ url: '/pages/shop/select' })
}

function goBack() {
  uni.switchTab({ url: '/pages/index/index' })
}

function money(cents: number | null | undefined): string {
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
  display: flex;
  width: 54rpx;
  height: 54rpx;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  background: #f4f1eb;
  color: #3b352e;
}

.home-icon {
  position: relative;
  width: 31rpx;
  height: 31rpx;
}

.home-roof {
  position: absolute;
  top: 2rpx;
  left: 5rpx;
  width: 20rpx;
  height: 20rpx;
  border-top: 5rpx solid currentColor;
  border-left: 5rpx solid currentColor;
  border-radius: 3rpx;
  transform: rotate(45deg);
  box-sizing: border-box;
}

.home-body {
  position: absolute;
  right: 4rpx;
  bottom: 2rpx;
  left: 4rpx;
  height: 19rpx;
  border: 5rpx solid currentColor;
  border-top: none;
  border-radius: 2rpx 2rpx 5rpx 5rpx;
  box-sizing: border-box;
}

.home-door {
  position: absolute;
  bottom: -1rpx;
  left: 50%;
  width: 7rpx;
  height: 10rpx;
  border: 3rpx solid currentColor;
  border-bottom: none;
  border-radius: 3rpx 3rpx 0 0;
  transform: translateX(-50%);
  box-sizing: border-box;
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

.preorder-action {
  flex-shrink: 0;
  margin-left: 16rpx;
  padding: 8rpx 13rpx;
  border-radius: 16rpx;
  background: #e2ccb0;
  color: #7c5630;
  font-size: 16rpx;
  font-weight: 700;
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

/* Refined ordering layout */
.menu-page {
  display: flex;
  flex-direction: column;
  background: #f4f1eb;
}

.fixed-header {
  position: relative;
  z-index: 8;
  flex-shrink: 0;
  padding-bottom: 17rpx;
  background: #f4f1eb;
  box-shadow: 0 12rpx 32rpx rgba(44, 38, 29, 0.07);
}

.menu-header {
  height: calc(var(--status-bar-height) + 94rpx);
  padding: var(--status-bar-height) 24rpx 15rpx;
  background: #f4f1eb;
}

.back {
  width: 58rpx;
  height: 58rpx;
  border-radius: 20rpx;
  background: #fff;
  box-shadow: 0 8rpx 24rpx rgba(44, 38, 29, 0.06);
  font-size: 48rpx;
  line-height: 51rpx;
}

.shop-copy {
  margin-left: 16rpx;
}

.shop-name {
  max-width: 310rpx;
  font-size: 29rpx;
}

.shop-meta {
  display: flex;
  align-items: center;
  overflow: hidden;
  margin-top: 6rpx;
  color: #948b80;
  font-size: 18rpx;
  white-space: nowrap;
}

.status-dot {
  width: 10rpx;
  height: 10rpx;
  flex-shrink: 0;
  margin-right: 8rpx;
  border-radius: 50%;
  background: #4b9b68;
}

.status-dot.unavailable {
  background: #b98245;
}

.meta-divider {
  margin: 0 7rpx;
}

.shop-detail-link {
  flex-shrink: 0;
  padding: 10rpx 13rpx;
  border-radius: 18rpx;
  background: #e9e1d6;
  color: #79634c;
  font-size: 17rpx;
  font-weight: 700;
}

.service-card {
  display: flex;
  position: relative;
  align-items: center;
  justify-content: space-between;
  margin: 5rpx 24rpx 0;
  min-height: 178rpx;
  padding: 25rpx 21rpx 23rpx 44%;
  border-radius: 28rpx;
  background-image:
    linear-gradient(90deg, rgba(255, 249, 235, 0.04), rgba(255, 249, 235, 0.78) 45%, rgba(255, 249, 235, 0.94)),
    url('/static/brand/order-header.jpg');
  background-position: center;
  background-size: cover;
  box-shadow: 0 14rpx 38rpx rgba(43, 35, 26, 0.16);
  box-sizing: border-box;
}

.service-copy {
  min-width: 0;
  flex: 1;
  margin-top: 42rpx;
}

.service-kicker,
.service-title,
.service-description {
  display: block;
}

.service-kicker {
  color: #b96519;
  font-size: 13rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.service-title {
  margin-top: 6rpx;
  color: #66370f;
  font-size: 27rpx;
  font-weight: 850;
}

.service-description {
  overflow: hidden;
  margin-top: 5rpx;
  color: #a2754d;
  font-size: 16rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mode-switch {
  display: flex;
  position: absolute;
  top: 17rpx;
  right: 17rpx;
  flex-shrink: 0;
  margin-left: 0;
  padding: 5rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 5rpx 16rpx rgba(142, 83, 30, 0.1);
}

.mode-option {
  padding: 12rpx 15rpx;
  border-radius: 19rpx;
  color: #a37a54;
  font-size: 17rpx;
  font-weight: 700;
}

.mode-option.active {
  background: #e57d1d;
  color: #fff;
}

.preorder-bar {
  margin: 14rpx 24rpx 0;
  padding: 17rpx 20rpx;
  border: 2rpx solid #e7d4b9;
  border-radius: 25rpx;
  background: #f3e8d7;
  color: #4e3d2c;
}

.preorder-icon {
  position: relative;
  width: 42rpx;
  height: 42rpx;
  flex-shrink: 0;
  margin-right: 15rpx;
  border: 4rpx solid #926333;
  border-radius: 50%;
  box-sizing: border-box;
}

.preorder-icon .clock-hand {
  position: absolute;
  left: 17rpx;
  top: 17rpx;
  width: 3rpx;
  border-radius: 3rpx;
  background: #926333;
  transform-origin: 2rpx 2rpx;
}

.preorder-icon .clock-hour {
  height: 11rpx;
  transform: rotate(180deg);
}

.preorder-icon .clock-minute {
  height: 15rpx;
  transform: rotate(120deg);
}

.preorder-copy {
  min-width: 0;
  flex: 1;
}

.preorder-label {
  color: #9b7c59;
  font-size: 16rpx;
}

.preorder-value {
  overflow: hidden;
  margin-top: 3rpx;
  color: #59422c;
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preorder-arrow {
  color: #9a7044;
}

.search-box {
  margin: 15rpx 24rpx 0;
  border: 2rpx solid #ebe5dd;
  background: #fff;
  box-shadow: 0 8rpx 25rpx rgba(44, 38, 29, 0.04);
}

.search-box input {
  height: 72rpx;
}

.menu-body,
.menu-body.has-preorder {
  height: auto;
  min-height: 0;
  flex: 1;
}

.category-rail {
  width: 142rpx;
  padding-top: 14rpx;
  background: #ece7df;
  box-sizing: border-box;
}

.category-brand {
  width: 62rpx;
  height: 62rpx;
  margin: 2rpx auto 19rpx;
  border-radius: 21rpx;
  overflow: hidden;
  border: 3rpx solid #fff;
  background: #fff;
  box-shadow: 0 7rpx 20rpx rgba(143, 82, 24, 0.12);
  transform: rotate(-3deg);
}

.category-logo {
  width: 100%;
  height: 100%;
}

.category-item {
  min-height: 90rpx;
  justify-content: flex-start;
  padding: 14rpx 13rpx 14rpx 22rpx;
  color: #847b70;
  font-size: 20rpx;
  text-align: left;
}

.category-item.active {
  border-radius: 0 24rpx 24rpx 0;
  background: #f9f7f3;
}

.category-item.active::before {
  display: none;
}

.category-active-mark {
  width: 7rpx;
  height: 7rpx;
  flex-shrink: 0;
  margin-right: 10rpx;
  border-radius: 50%;
  background: transparent;
}

.category-item.active .category-active-mark {
  background: #b7793a;
  box-shadow: 0 0 0 6rpx rgba(183, 121, 58, 0.12);
}

.product-panel {
  padding: 0 18rpx;
  background: #f9f7f3;
}

.panel-heading {
  position: sticky;
  z-index: 4;
  top: 0;
  padding: 20rpx 0 17rpx;
  background: rgba(249, 247, 243, 0.97);
}

.panel-kicker,
.panel-title {
  display: block;
}

.panel-kicker {
  color: #b47d43;
  font-size: 12rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.panel-title {
  margin-top: 5rpx;
  font-size: 28rpx;
}

.panel-count {
  padding: 7rpx 11rpx;
  border-radius: 16rpx;
  background: #eee7dc;
  color: #8f8171;
  font-size: 16rpx;
}

.product-list {
  gap: 17rpx;
}

.product-item {
  min-height: 202rpx;
  padding: 15rpx;
  border: 2rpx solid #f0ebe4;
  border-radius: 27rpx;
  background: #fff;
  box-shadow: 0 9rpx 30rpx rgba(44, 38, 29, 0.05);
  box-sizing: border-box;
}

.product-image {
  width: 176rpx;
  height: 176rpx;
  border-radius: 22rpx;
}

.image-tag {
  position: absolute;
  top: 9rpx;
  left: 9rpx;
  max-width: 115rpx;
  overflow: hidden;
  padding: 5rpx 8rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.84);
  color: #8d6337;
  font-size: 13rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-copy {
  display: flex;
  flex-direction: column;
  margin-left: 15rpx;
}

.product-name-row {
  display: flex;
  align-items: center;
}

.product-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  font-size: 25rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spec-tip {
  flex-shrink: 0;
  margin-left: 7rpx;
  padding: 4rpx 7rpx;
  border-radius: 9rpx;
  background: #f2e7d7;
  color: #a16c32;
  font-size: 12rpx;
}

.product-description {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 8rpx;
  font-size: 17rpx;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.product-meta {
  display: flex;
  gap: 11rpx;
  margin-top: 8rpx;
  color: #aaa197;
  font-size: 15rpx;
}

.product-bottom {
  margin-top: auto;
  padding-top: 12rpx;
}

.price {
  font-size: 30rpx;
}

.add {
  width: 46rpx;
  height: 46rpx;
  box-shadow: 0 7rpx 16rpx rgba(28, 27, 23, 0.2);
  line-height: 42rpx;
}

.product-bottom-space {
  height: calc(35rpx + env(safe-area-inset-bottom));
}

.loading,
.empty {
  min-height: 480rpx;
}

.loading-ring {
  width: 45rpx;
  height: 45rpx;
  margin-bottom: 18rpx;
  border: 5rpx solid #e7dfd4;
  border-top-color: #a66f37;
  border-radius: 50%;
  animation: menu-rotate 0.8s linear infinite;
}

.empty-copy {
  margin-top: 8rpx;
  color: #b3aba0;
  font-size: 17rpx;
}

.retry-copy {
  margin-top: 20rpx;
  padding: 10rpx 20rpx;
  border-radius: 20rpx;
  background: #f1e4d3;
  color: #9d6a32;
}

.detail-mask {
  background: rgba(27, 24, 20, 0.58);
  backdrop-filter: blur(5rpx);
}

.detail-sheet {
  max-height: 92vh;
  border-radius: 42rpx 42rpx 0 0;
  background: #f7f4ee;
}

.sheet-handle {
  position: absolute;
  z-index: 5;
  top: 12rpx;
  left: 50%;
  margin: 0;
  background: rgba(255, 255, 255, 0.6);
  transform: translateX(-50%);
}

.detail-hero {
  position: relative;
  height: 335rpx;
  overflow: hidden;
}

.detail-image {
  width: 100%;
  height: 100%;
  border-radius: 0;
}

.large-drink {
  transform: scale(1.18) rotate(5deg);
}

.detail-image-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(22, 19, 16, 0.02), rgba(22, 19, 16, 0.78));
}

.detail-copy {
  position: absolute;
  right: 28rpx;
  bottom: 25rpx;
  left: 28rpx;
  z-index: 3;
  margin: 0;
  padding: 0;
}

.detail-kicker {
  color: #e6b779;
  font-size: 14rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.detail-name {
  margin-top: 6rpx;
  color: #fff;
  font-size: 36rpx;
}

.detail-description {
  overflow: hidden;
  max-width: 570rpx;
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.68);
  font-size: 18rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-row {
  margin-top: 10rpx;
}

.tag {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.82);
}

.close {
  display: flex;
  top: 22rpx;
  right: 22rpx;
  z-index: 5;
  width: 54rpx;
  height: 54rpx;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-radius: 50%;
  background: rgba(25, 23, 20, 0.55);
  color: #fff;
  font-size: 31rpx;
}

.spec-scroll {
  max-height: 39vh;
  padding: 6rpx 28rpx 0;
}

.spec-group {
  padding: 25rpx 0;
}

.spec-rule {
  padding: 5rpx 9rpx;
  border-radius: 11rpx;
  background: #ede7df;
}

.spec-rule.required {
  background: #f1e4d3;
  color: #9d6a32;
}

.option {
  min-width: 150rpx;
  padding: 15rpx 18rpx;
  border-color: #e8e1d8;
  background: #fff;
}

.option.selected {
  border-color: #9d6b37;
  background: #efe0cc;
  box-shadow: 0 7rpx 18rpx rgba(128, 84, 37, 0.08);
}

.detail-footer {
  padding-top: 19rpx;
}

.footer-label {
  max-width: 360rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.confirm-button {
  box-shadow: 0 9rpx 24rpx rgba(28, 27, 23, 0.18);
}

@keyframes menu-rotate {
  to { transform: rotate(360deg); }
}

/* Compact ordering layout based on the provided reference */
.fixed-header {
  padding-bottom: 12rpx;
  background: #fff;
  box-shadow: 0 4rpx 18rpx rgba(61, 44, 25, 0.06);
}

.brand-hero {
  padding-bottom: 18rpx;
  background-image:
    linear-gradient(90deg, rgba(255, 248, 232, 0.08), rgba(255, 248, 232, 0.42)),
    url('/static/brand/order-header.jpg');
  background-position: center;
  background-size: cover;
}

.menu-header {
  height: calc(var(--status-bar-height) + 92rpx);
  padding: var(--status-bar-height) 22rpx 12rpx;
  background: transparent;
}

.back {
  color: #bc661b;
}

.header-logo {
  width: 150rpx;
  height: 64rpx;
  margin-left: 15rpx;
  border-radius: 15rpx;
  background: rgba(255, 255, 255, 0.92);
}

.header-spacer {
  flex: 1;
}

.shop-detail-link {
  background: rgba(255, 255, 255, 0.88);
  color: #ad601e;
}

.service-card {
  min-height: 145rpx;
  margin: 0 22rpx;
  padding: 20rpx 20rpx 18rpx;
  border: 2rpx solid rgba(222, 139, 62, 0.2);
  border-radius: 27rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 10rpx 28rpx rgba(132, 77, 28, 0.12);
}

.service-copy {
  margin-top: 0;
  padding-right: 16rpx;
}

.shop-name-row {
  display: flex;
  align-items: center;
}

.shop-name {
  max-width: 350rpx;
  color: #3d352c;
  font-size: 27rpx;
}

.shop-arrow {
  color: #d17827;
}

.distance-row {
  display: flex;
  align-items: baseline;
  margin-top: 7rpx;
}

.distance-label {
  color: #a99e92;
  font-size: 16rpx;
}

.distance-value {
  margin-left: 6rpx;
  color: #e06428;
  font-size: 19rpx;
  font-weight: 800;
}

.shop-meta {
  margin-top: 12rpx;
  color: #8f8579;
  font-size: 16rpx;
}

.mode-switch {
  position: static;
  min-width: 176rpx;
  margin-left: 10rpx;
  padding: 3rpx;
  border: 2rpx solid #dc7a25;
  border-radius: 27rpx;
  background: #fff;
  box-shadow: none;
}

.mode-option {
  flex: 1;
  padding: 11rpx 12rpx;
  color: #d37221;
  font-size: 18rpx;
  text-align: center;
}

.mode-option.active {
  background: #e37d24;
  color: #fff;
}

.preorder-bar {
  margin: 12rpx 18rpx 0;
  padding: 13rpx 17rpx;
  border-radius: 20rpx;
}

.search-box {
  margin: 12rpx 18rpx 0;
  border: none;
  border-radius: 21rpx;
  background: #f4f2ef;
  box-shadow: none;
}

.search-box input {
  height: 65rpx;
}

.category-rail {
  width: 166rpx;
  padding-top: 0;
  background: #f3f3f3;
}

.category-item {
  min-height: 108rpx;
  justify-content: center;
  padding: 15rpx 12rpx;
  color: #8a8a8a;
  font-size: 20rpx;
  text-align: center;
}

.category-item.active {
  border-radius: 0 22rpx 22rpx 0;
  background: #fff;
  color: #3a352f;
}

.category-active-mark {
  display: none;
}

.product-panel {
  padding: 0 16rpx;
  background: #fff;
}

.panel-heading {
  padding: 18rpx 6rpx 13rpx;
  border-bottom: 1rpx solid #f1eee9;
  background: rgba(255, 255, 255, 0.97);
}

.panel-title {
  margin-top: 0;
  color: #4e463d;
  font-size: 21rpx;
  font-weight: 600;
}

.panel-count {
  padding: 0;
  background: transparent;
  color: #aaa096;
  font-size: 15rpx;
}

.product-list {
  gap: 0;
}

.product-item {
  min-height: 210rpx;
  padding: 19rpx 5rpx;
  border: none;
  border-bottom: 1rpx solid #f1eee9;
  border-radius: 0;
  box-shadow: none;
}

.product-image {
  width: 158rpx;
  height: 172rpx;
  border-radius: 12rpx;
  background: #faf7f2;
}

.product-copy {
  margin-left: 15rpx;
}

.product-name {
  font-size: 24rpx;
}

.spec-tip {
  background: #fff1e4;
  color: #dc7325;
}

.product-description {
  display: block;
  overflow: hidden;
  margin-top: 7rpx;
  color: #aaa198;
  font-size: 16rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  margin-top: 8rpx;
  color: #b7aea5;
  font-size: 14rpx;
}

.product-bottom {
  padding-top: 8rpx;
}

.price {
  color: #df6f22;
  font-size: 30rpx;
}

.price .from {
  color: #aaa198;
}

.add {
  width: 44rpx;
  height: 44rpx;
  background: #e57d22;
  box-shadow: none;
  line-height: 40rpx;
}

.image-tag {
  background: #ef7d35;
  color: #fff;
}

.product-bottom-space {
  height: 24rpx;
}

.cart-bar {
  display: flex;
  position: relative;
  z-index: 20;
  width: 100%;
  min-height: calc(96rpx + env(safe-area-inset-bottom));
  flex-shrink: 0;
  align-items: center;
  padding: 11rpx 18rpx calc(11rpx + env(safe-area-inset-bottom));
  border-top: 0;
  border-radius: 0;
  background: #fff;
  box-shadow: 0 -10rpx 28rpx rgba(55, 42, 28, 0.08);
  box-sizing: border-box;
  animation: cart-bar-enter 260ms ease-out both;
}

.cart-bar.bump {
  animation: cart-bar-bump 320ms ease-out both;
}

.cart-summary {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  height: 74rpx;
  padding-left: 18rpx;
  border-radius: 38rpx 0 0 38rpx;
  background: #28241f;
}

.cart-icon {
  position: relative;
  width: 54rpx;
  height: 54rpx;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(145deg, #f3953d, #dc6817);
  box-shadow: 0 8rpx 20rpx rgba(222, 108, 24, 0.25);
}

.cart-basket {
  position: absolute;
  top: 18rpx;
  left: 14rpx;
  width: 28rpx;
  height: 21rpx;
  border: 3rpx solid #fff;
  border-radius: 5rpx 5rpx 9rpx 9rpx;
  box-sizing: border-box;
}

.cart-basket::before {
  display: block;
  position: absolute;
  top: -11rpx;
  left: 5rpx;
  width: 11rpx;
  height: 11rpx;
  border-top: 3rpx solid #fff;
  border-left: 3rpx solid #fff;
  content: "";
  transform: rotate(45deg);
}

.cart-badge {
  position: absolute;
  top: -7rpx;
  right: -7rpx;
  min-width: 28rpx;
  height: 28rpx;
  padding: 0 5rpx;
  border: 3rpx solid #28241f;
  border-radius: 17rpx;
  background: #24211d;
  color: #fff;
  font-size: 14rpx;
  font-weight: 800;
  line-height: 28rpx;
  text-align: center;
  box-sizing: border-box;
}

.cart-total-copy {
  min-width: 0;
  margin-left: 14rpx;
}

.cart-total,
.cart-hint {
  display: block;
}

.cart-total {
  color: #fff;
  font-size: 28rpx;
  font-weight: 850;
}

.cart-total text {
  font-size: 17rpx;
}

.cart-hint {
  overflow: hidden;
  max-width: 270rpx;
  margin-top: 1rpx;
  color: rgba(255, 255, 255, 0.55);
  font-size: 15rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkout-button,
.cart-checkout {
  margin: 0;
  padding: 0;
  border-radius: 24rpx;
  background: #e57d22;
  color: #fff;
  font-size: 22rpx;
  font-weight: 800;
}

.checkout-button {
  width: 172rpx;
  height: 74rpx;
  border-radius: 0 38rpx 38rpx 0;
  line-height: 74rpx;
}

.checkout-button[disabled],
.cart-checkout[disabled] {
  background: #e9e4dc;
  color: #aaa197;
}

.cart-fly-x {
  position: fixed;
  z-index: 1100;
  width: 64rpx;
  height: 64rpx;
  pointer-events: none;
  transform: translateZ(0);
}

.cart-fly-y {
  width: 64rpx;
  height: 64rpx;
  transform: translateZ(0);
}

.cart-fly-image,
.cart-fly-placeholder {
  width: 64rpx;
  height: 64rpx;
  border: 4rpx solid #fff;
  border-radius: 50%;
  background: #e57d22;
  box-shadow: 0 8rpx 24rpx rgba(43, 34, 25, 0.26);
  box-sizing: border-box;
}

.cart-fly-placeholder {
  color: #fff;
  font-size: 42rpx;
  font-weight: 500;
  line-height: 54rpx;
  text-align: center;
}

.cart-mask {
  display: flex;
  position: fixed;
  z-index: 900;
  inset: 0;
  align-items: flex-end;
  background: rgba(27, 24, 20, 0.48);
  backdrop-filter: blur(3rpx);
  animation: cart-mask-enter 220ms ease-out both;
}

.cart-sheet {
  display: flex;
  width: 100%;
  height: 72vh;
  max-height: 920rpx;
  flex-direction: column;
  overflow: hidden;
  border-radius: 34rpx 34rpx 0 0;
  background: #fff;
  box-shadow: 0 -14rpx 45rpx rgba(33, 27, 20, 0.14);
  transform-origin: bottom center;
  animation: cart-sheet-enter 360ms cubic-bezier(0.22, 0.78, 0.28, 1) both;
}

.cart-sheet-handle {
  width: 72rpx;
  height: 8rpx;
  flex-shrink: 0;
  margin: 14rpx auto 0;
  border-radius: 8rpx;
  background: #e5dfd6;
}

.cart-sheet-heading {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 30rpx 20rpx;
  border-bottom: 1rpx solid #e9e3da;
}

.cart-sheet-title-row {
  display: flex;
  align-items: baseline;
}

.cart-sheet-title,
.cart-sheet-shop {
  display: block;
}

.cart-sheet-title {
  color: #2b2722;
  font-size: 32rpx;
  font-weight: 850;
}

.cart-sheet-count {
  margin-left: 12rpx;
  color: #a39a90;
  font-size: 16rpx;
}

.cart-sheet-shop {
  margin-top: 7rpx;
  color: #9c9388;
  font-size: 17rpx;
}

.clear-cart {
  padding: 10rpx 0 10rpx 18rpx;
  color: #a49b91;
  font-size: 18rpx;
}

.cart-items {
  height: 1px;
  min-height: 0;
  flex: 1;
  padding: 0 30rpx;
  background: #fff;
  box-sizing: border-box;
}

.cart-item {
  display: flex;
  position: relative;
  align-items: center;
  min-height: 132rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #e9e3da;
}

.cart-item-image {
  display: flex;
  width: 112rpx;
  height: 112rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 19rpx;
  background: linear-gradient(145deg, #f1dfc7, #d8b179);
  background-position: center;
  background-size: cover;
  color: #fff;
  font-size: 18rpx;
  font-weight: 900;
}

.cart-item-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
  padding-right: 12rpx;
}

.cart-item-name,
.cart-item-spec,
.cart-item-price {
  display: block;
}

.cart-item-name {
  overflow: hidden;
  color: #302b25;
  font-size: 23rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item-spec {
  overflow: hidden;
  max-width: 320rpx;
  margin-top: 6rpx;
  color: #a0988e;
  font-size: 16rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-item-price {
  margin-top: 10rpx;
  color: #dc701f;
  font-size: 22rpx;
  font-weight: 800;
}

.quantity-control {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-end;
  margin-bottom: 5rpx;
}

.quantity-button {
  display: flex;
  width: 44rpx;
  height: 44rpx;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #e57d22;
  color: #fff;
  font-size: 29rpx;
  line-height: 1;
  text-align: center;
}

.quantity-button.minus {
  border: 2rpx solid #ddd5ca;
  background: #fff;
  color: #7b7064;
  box-sizing: border-box;
}

.quantity-value {
  min-width: 52rpx;
  color: #413a32;
  font-size: 21rpx;
  font-weight: 750;
  text-align: center;
}

.cart-empty {
  display: flex;
  height: 100%;
  min-height: 350rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #9b9287;
  font-size: 20rpx;
}

.cart-empty-icon {
  width: 78rpx;
  height: 78rpx;
  margin-bottom: 18rpx;
  border-radius: 25rpx;
  background: #27231f;
  color: #e5b472;
  font-size: 19rpx;
  font-weight: 900;
  line-height: 78rpx;
  text-align: center;
}

.cart-list-space {
  height: 20rpx;
}

.cart-sheet-footer {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 118rpx;
  padding: 18rpx 30rpx calc(18rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #ece6dd;
  background: #fff;
  box-shadow: 0 -8rpx 24rpx rgba(44, 36, 28, 0.05);
  box-sizing: border-box;
}

.cart-footer-label,
.cart-footer-total {
  display: block;
}

.cart-footer-label {
  color: #9b9389;
  font-size: 16rpx;
}

.cart-footer-total {
  margin-top: 3rpx;
  color: #df701f;
  font-size: 32rpx;
  font-weight: 850;
}

.cart-checkout {
  width: 250rpx;
  height: 82rpx;
  line-height: 82rpx;
}

@keyframes cart-bar-enter {
  from { opacity: 0; transform: translateY(30rpx); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes cart-bar-bump {
  0% { transform: scale(1); }
  45% { transform: scale(1.025); }
  100% { transform: scale(1); }
}

@keyframes cart-mask-enter {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes cart-sheet-enter {
  from { opacity: 0; transform: translateY(100%); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
