<template>
  <view class="map-shell">
    <!-- #ifdef H5 -->
    <view :id="mapContainerId" class="map-surface" />
    <view v-if="mapError" class="map-error">
      <text>{{ mapError }}</text>
    </view>
    <!-- #endif -->

    <!-- #ifndef H5 -->
    <map
      class="map-surface"
      :longitude="center.longitude"
      :latitude="center.latitude"
      :markers="nativeMarkers"
      :scale="14"
      show-location
      @markertap="handleNativeMarkerTap"
    />
    <!-- #endif -->

    <view class="map-summary">
      <view class="map-summary-dot" />
      <text v-if="nearbyShops.length">地图中显示附近 {{ nearbyShops.length }} 家门店</text>
      <text v-else>暂无包含有效经纬度的门店</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadAmap } from '../../utils/amap'
import type { AmapApi, AmapMap, AmapMarker } from '../../utils/amap'
import type { Coordinates, Shop } from '../../types/shop'

interface NativeMapMarker extends Coordinates {
  id: number
  title: string
  iconPath: string
  width: number
  height: number
  callout: {
    content: string
    color: string
    fontSize: number
    borderRadius: number
    padding: number
    bgColor: string
    display: 'ALWAYS' | 'BYCLICK'
  }
}

const props = defineProps<{
  shops: Shop[]
  center: Coordinates
  focusedShopId?: number
}>()

const emit = defineEmits<{
  markerTap: [shopId: number]
}>()

const mapContainerId = `gogorder-shop-map-${getCurrentInstance()?.uid ?? Date.now()}`
const mapError = ref('')
const mappableShops = computed(() => props.shops.filter(hasCoordinates))
const nearbyShops = computed(() => {
  const withinTenKilometers = mappableShops.value.filter(shop => shop.distance !== null
    && shop.distance !== undefined
    && shop.distance <= 10_000)
  return (withinTenKilometers.length ? withinTenKilometers : mappableShops.value.slice(0, 5)).slice(0, 8)
})
const nativeMarkers = computed<NativeMapMarker[]>(() => nearbyShops.value.map(shop => ({
  id: shop.id,
  title: shop.name,
  longitude: Number(shop.longitude),
  latitude: Number(shop.latitude),
  iconPath: '/static/brand/store-logo.png',
  width: props.focusedShopId === shop.id ? 34 : 28,
  height: props.focusedShopId === shop.id ? 38 : 32,
  callout: {
    content: shop.name,
    color: '#332b22',
    fontSize: 12,
    borderRadius: 8,
    padding: 6,
    bgColor: '#fffaf1',
    display: props.focusedShopId === shop.id ? 'ALWAYS' : 'BYCLICK'
  }
})))

let amap: AmapApi | undefined
let map: AmapMap | undefined

function hasCoordinates(shop: Shop): boolean {
  const longitude = Number(shop.longitude)
  const latitude = Number(shop.latitude)
  return Number.isFinite(longitude) && Number.isFinite(latitude) && !(longitude === 0 && latitude === 0)
}

function handleNativeMarkerTap(event: { detail: { markerId: number | string } }) {
  emit('markerTap', Number(event.detail.markerId))
}

function markerContent(shop: Shop): string {
  const focused = props.focusedShopId === shop.id
  const size = focused ? 38 : 32
  const border = focused ? 3 : 2
  const shadow = focused ? '0 6px 18px rgba(168,87,20,.36)' : '0 4px 12px rgba(44,38,29,.24)'
  return `<div style="position:relative;width:${size}px;height:${size + 7}px;">
    <div style="position:absolute;left:50%;bottom:2px;width:10px;height:10px;background:#e57d22;transform:translateX(-50%) rotate(45deg);border-radius:2px;"></div>
    <div style="position:absolute;inset:0 0 7px;display:flex;align-items:center;justify-content:center;overflow:hidden;border:${border}px solid #e57d22;border-radius:50%;background:#fff;box-shadow:${shadow};box-sizing:border-box;">
      <img src="/static/brand/store-logo.png" style="width:88%;height:88%;object-fit:contain;" />
    </div>
  </div>`
}

/** H5 端初始化高德地图：加载 AMap → 创建 Map 实例 → 同步门店标记。失败时记录错误信息。 */
async function initH5Map() {
  if (typeof window === 'undefined' || typeof document === 'undefined') return
  if (!document.getElementById(mapContainerId)) {
    mapError.value = '地图容器初始化失败'
    return
  }
  try {
    amap = await loadAmap()
    map = new amap.Map(mapContainerId, {
      zoom: 14,
      center: [props.center.longitude, props.center.latitude],
      resizeEnable: true
    })
    syncH5Markers()
  } catch (error) {
    mapError.value = error instanceof Error ? error.message : '地图加载失败'
  }
}

/** 同步 H5 地图标记：清空后重建附近门店标记，绑定点击事件，并复位中心点。 */
function syncH5Markers() {
  if (!map || !amap) return
  map.clearMap()
  const markers = nearbyShops.value.map(shop => {
    const marker = new amap!.Marker({
      position: [Number(shop.longitude), Number(shop.latitude)],
      anchor: 'bottom-center',
      title: shop.name,
      content: markerContent(shop)
    })
    marker.on('click', () => emit('markerTap', shop.id))
    return marker
  })
  if (markers.length) {
    map.add(markers)
  }
  map.setZoomAndCenter(14, [props.center.longitude, props.center.latitude])
}

watch(
  () => [props.shops, props.focusedShopId],
  syncH5Markers,
  { deep: true }
)

watch(
  () => props.center,
  center => {
    map?.setZoomAndCenter(14, [center.longitude, center.latitude])
  },
  { deep: true }
)

onMounted(() => nextTick(initH5Map))

onBeforeUnmount(() => {
  map?.destroy()
})
</script>

<style lang="scss" scoped>
.map-shell,
.map-surface {
  position: relative;
  width: 100%;
  height: 100%;
}

.map-error {
  display: flex;
  position: absolute;
  inset: 0;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
  background: #e9e3d9;
  color: #786f64;
  font-size: 21rpx;
  text-align: center;
  box-sizing: border-box;
}

.map-summary {
  display: flex;
  position: absolute;
  left: 16rpx;
  bottom: 16rpx;
  z-index: 2;
  align-items: center;
  padding: 10rpx 15rpx;
  border-radius: 22rpx;
  background: rgba(28, 27, 23, 0.84);
  color: #fff;
  font-size: 18rpx;
  pointer-events: none;
}

.map-summary-dot {
  width: 11rpx;
  height: 11rpx;
  margin-right: 9rpx;
  border: 4rpx solid #f0c58e;
  border-radius: 50%;
}
</style>
