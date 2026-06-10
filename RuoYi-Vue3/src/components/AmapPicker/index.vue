<template>
  <div class="amap-picker">
    <el-alert
      v-if="mapError"
      :title="mapError"
      type="warning"
      :closable="false"
      show-icon
      class="map-alert"
    />

    <div class="map-toolbar">
      <el-input
        v-model="keyword"
        clearable
        placeholder="搜索门店名称、商圈或详细地址"
        @keyup.enter="searchPlaces"
      >
        <template #append>
          <el-button icon="Search" :loading="searching" @click="searchPlaces">搜索位置</el-button>
        </template>
      </el-input>
      <span class="map-tip">也可以直接点击地图选择门店位置</span>
    </div>

    <div v-if="searchResults.length" class="search-results">
      <button
        v-for="item in searchResults"
        :key="item.id || item.name + item.address"
        type="button"
        class="search-result"
        @click="selectPlace(item)"
      >
        <strong>{{ item.name }}</strong>
        <span>{{ item.address || item.district || "暂无详细地址" }}</span>
      </button>
    </div>

    <div class="map-shell">
      <div ref="mapContainer" class="map-container" />
      <div v-if="loading" class="map-loading">地图加载中...</div>
    </div>

    <div class="location-summary">
      <div>
        <span class="summary-label">当前选点</span>
        <strong>{{ selectedAddress || "尚未选择位置" }}</strong>
      </div>
      <span v-if="hasSelectedLocation" class="coordinates">
        {{ Number(longitude).toFixed(7) }}, {{ Number(latitude).toFixed(7) }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from "element-plus"
import { getShopMapConfig } from "@/api/admin/shop"

const props = defineProps({
  longitude: {
    type: [Number, String],
    default: 0
  },
  latitude: {
    type: [Number, String],
    default: 0
  },
  address: {
    type: String,
    default: ""
  }
})

const emit = defineEmits(["select"])

const mapContainer = ref()
const keyword = ref("")
const searchResults = ref([])
const searching = ref(false)
const loading = ref(false)
const mapError = ref("")
const selectedAddress = ref(props.address)

let map
let marker
let geocoder
let placeSearch
let amap
let amapLoader
let geocodeRequestId = 0

const hasSelectedLocation = computed(() => isValidLocation(props.longitude, props.latitude))

function isValidLocation(longitude, latitude) {
  const lng = Number(longitude)
  const lat = Number(latitude)
  return Number.isFinite(lng) && Number.isFinite(lat) && !(lng === 0 && lat === 0)
}

async function loadAmap() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (amapLoader) return amapLoader

  const response = await getShopMapConfig()
  const amapKey = String(response.data?.key || "").trim()
  const securityCode = String(response.data?.securityCode || "").trim()
  if (!amapKey) throw new Error("请先在后端配置 AMAP_WEB_KEY 后使用高德地图选点")

  if (securityCode) {
    window._AMapSecurityConfig = { securityJsCode: securityCode }
  }

  amapLoader = new Promise((resolve, reject) => {
    const script = document.createElement("script")
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(amapKey)}&plugin=AMap.Geocoder,AMap.PlaceSearch`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error("高德地图加载失败，请检查网络和密钥配置"))
    document.head.appendChild(script)
  })
  return amapLoader
}

async function initMap() {
  loading.value = true
  try {
    amap = await loadAmap()
    const initialCenter = hasSelectedLocation.value
      ? [Number(props.longitude), Number(props.latitude)]
      : [116.397428, 39.90923]

    map = new amap.Map(mapContainer.value, {
      zoom: hasSelectedLocation.value ? 16 : 11,
      center: initialCenter,
      resizeEnable: true
    })
    geocoder = new amap.Geocoder()
    placeSearch = new amap.PlaceSearch({ pageSize: 8, extensions: "base" })
    map.on("click", event => chooseLocation(event.lnglat))
    syncMarker(props.longitude, props.latitude)
  } catch (error) {
    mapError.value = error.message || "高德地图加载失败"
  } finally {
    loading.value = false
  }
}

function searchPlaces() {
  if (!keyword.value.trim()) {
    searchResults.value = []
    return
  }
  if (!placeSearch) {
    ElMessage.warning(mapError.value || "地图尚未加载完成")
    return
  }

  searching.value = true
  placeSearch.search(keyword.value.trim(), (status, result) => {
    searching.value = false
    if (status === "complete") {
      searchResults.value = result.poiList?.pois || []
      if (!searchResults.value.length) ElMessage.info("未搜索到匹配位置")
      return
    }
    searchResults.value = []
    ElMessage.warning("位置搜索失败，请更换关键词重试")
  })
}

function selectPlace(place) {
  if (!place.location) return
  const detail = [place.address, place.name].filter(Boolean).join("")
  chooseLocation(place.location, detail)
  searchResults.value = []
  keyword.value = place.name
}

function chooseLocation(location, detailAddress) {
  if (!map) return
  const coordinates = normalizeLocation(location)
  if (!coordinates) {
    ElMessage.warning("未能获取选点坐标，请重新选择")
    return
  }

  const { longitude, latitude } = coordinates
  setMarker(longitude, latitude, true)
  emit("select", coordinates)

  if (!geocoder) {
    ElMessage.warning("已选择经纬度，但地址解析服务尚未加载")
    return
  }

  const requestId = ++geocodeRequestId
  geocoder.getAddress([longitude, latitude], (status, result) => {
    if (requestId !== geocodeRequestId) return
    if (status !== "complete" || !result.regeocode) {
      ElMessage.warning("已选择经纬度，但地址解析失败，请手动补充地址")
      return
    }

    const component = result.regeocode.addressComponent || {}
    const city = Array.isArray(component.city) ? component.province : component.city
    const detail = detailAddress || [
      component.township,
      component.street,
      component.streetNumber
    ].filter(Boolean).join("")

    selectedAddress.value = result.regeocode.formattedAddress || detail
    emit("select", {
      longitude: Number(Number(longitude).toFixed(7)),
      latitude: Number(Number(latitude).toFixed(7)),
      province: component.province || "",
      city: city || "",
      district: component.district || "",
      address: detail || result.regeocode.formattedAddress || ""
    })
  })
}

function normalizeLocation(location) {
  const longitude = Number(
    typeof location?.getLng === "function" ? location.getLng() : location?.lng ?? location?.longitude
  )
  const latitude = Number(
    typeof location?.getLat === "function" ? location.getLat() : location?.lat ?? location?.latitude
  )
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return undefined
  return {
    longitude: Number(longitude.toFixed(7)),
    latitude: Number(latitude.toFixed(7))
  }
}

function setMarker(longitude, latitude, moveCenter) {
  const position = [Number(longitude), Number(latitude)]
  if (!marker) {
    marker = new amap.Marker({ position, anchor: "bottom-center" })
    map.add(marker)
  } else {
    marker.setPosition(position)
  }
  if (moveCenter) {
    map.setZoomAndCenter(16, position)
  }
}

function syncMarker(longitude, latitude) {
  if (!map || !isValidLocation(longitude, latitude)) return
  setMarker(longitude, latitude, true)
}

watch(
  () => [props.longitude, props.latitude],
  ([longitude, latitude]) => syncMarker(longitude, latitude)
)

watch(
  () => props.address,
  value => {
    selectedAddress.value = value
  }
)

onMounted(() => nextTick(initMap))

onBeforeUnmount(() => {
  geocodeRequestId++
  map?.destroy()
})
</script>

<style scoped lang="scss">
.amap-picker {
  width: 100%;
}

.map-alert {
  margin-bottom: 12px;
}

.map-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;

  .el-input {
    max-width: 620px;
  }
}

.map-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.search-results {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 190px;
  padding: 10px;
  margin-bottom: 12px;
  overflow-y: auto;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.search-result {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
  }

  span {
    overflow: hidden;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.map-shell {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}

.map-container {
  width: 100%;
  height: 360px;
}

.map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  background: var(--el-mask-color-extra-light);
}

.location-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  margin-top: 8px;
  background: var(--el-fill-color-light);
  border-radius: 6px;

  > div {
    display: flex;
    min-width: 0;
    gap: 10px;
  }

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.summary-label,
.coordinates {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .map-toolbar,
  .location-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .map-tip {
    white-space: normal;
  }

  .search-results {
    grid-template-columns: 1fr;
  }
}
</style>
