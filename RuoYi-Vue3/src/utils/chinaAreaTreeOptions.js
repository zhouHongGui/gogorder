import { areaList } from "@vant/area-data"

function buildAreaTreeOptions() {
  const { province_list: provinces, city_list: cities, county_list: counties } = areaList
  const cityEntries = Object.entries(cities)
  const countyEntries = Object.entries(counties)

  return Object.entries(provinces).map(([provinceCode, provinceName]) => {
    const provincePrefix = provinceCode.slice(0, 2)
    const children = cityEntries
      .filter(([cityCode]) => cityCode.slice(0, 2) === provincePrefix)
      .map(([cityCode, cityName]) => {
        const cityPrefix = cityCode.slice(0, 4)
        return {
          value: cityName,
          label: cityName,
          children: countyEntries
            .filter(([countyCode]) => countyCode.slice(0, 4) === cityPrefix)
            .map(([, countyName]) => ({
              value: countyName,
              label: countyName
            }))
        }
      })

    return {
      value: provinceName,
      label: provinceName,
      children
    }
  })
}

export const chinaAreaTreeOptions = buildAreaTreeOptions()
