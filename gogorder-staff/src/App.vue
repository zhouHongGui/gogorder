<script lang="ts">
export default {
  onLaunch() {
    console.info('gogorder 门店员工端启动')
  }
}
</script>

<style lang="scss">
page {
  min-height: 100%;
  background: #f7f4ee;
  color: #292620;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

button::after {
  border: none;
}

.safe-page {
  min-height: 100vh;
  box-sizing: border-box;
}

.primary-button {
  height: 92rpx;
  border-radius: 46rpx;
  background: #181711;
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 92rpx;
}

.primary-button[disabled] {
  background: #bdb9ae;
  color: #f4f1e9;
}
</style>
