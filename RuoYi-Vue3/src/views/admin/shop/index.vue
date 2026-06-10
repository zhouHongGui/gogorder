<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="门店名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入门店名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="联系电话" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="请输入联系电话" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="门店状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择门店状态" clearable style="width: 180px">
          <el-option v-for="item in shopStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['admin:shop:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="shopList">
      <el-table-column label="门店图片" align="center" width="90">
        <template #default="scope">
          <image-preview v-if="scope.row.image" :src="scope.row.image" :width="56" :height="56" />
          <span v-else>暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="门店编号" prop="shopCode" width="120" />
      <el-table-column label="门店名称" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="联系电话" prop="phone" width="135" />
      <el-table-column label="地址" min-width="240" show-overflow-tooltip>
        <template #default="scope">
          {{ formatAddress(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="营业时间" align="center" width="130">
        <template #default="scope">
          {{ formatTime(scope.row.openTime) }}-{{ formatTime(scope.row.closeTime) }}
        </template>
      </el-table-column>
      <el-table-column label="预约配置" align="center" width="180">
        <template #default="scope">
          提前{{ scope.row.preorderMinMinutes }}分钟 / {{ scope.row.preorderMaxDays }}天
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="150">
        <template #default="scope">
          <el-select
            v-model="scope.row.status"
            size="small"
            @change="value => handleStatusChange(scope.row, value)"
            v-hasPermi="['admin:shop:status']"
          >
            <el-option v-for="item in shopStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="包装费(分)" prop="packFee" align="center" width="100" />
      <el-table-column label="创建时间" prop="createTime" align="center" width="180" />
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['admin:shop:edit']">修改</el-button>
          <el-button link type="primary" icon="User" @click="handleStaff(scope.row)" v-hasPermi="['admin:shop:staff']">员工</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['admin:shop:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog
      :title="title"
      v-model="open"
      width="1120px"
      top="4vh"
      append-to-body
      destroy-on-close
      class="shop-edit-dialog"
    >
      <el-form ref="shopRef" :model="form" :rules="rules" label-width="125px">
        <section class="form-section">
          <div class="section-heading">
            <div>
              <h3>基础信息</h3>
              <p>设置门店对外展示的信息和联系电话</p>
            </div>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="门店编号" prop="shopCode">
                <el-input v-model="form.shopCode" :disabled="form.id != null" placeholder="如 GOG001" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="门店名称" prop="name">
                <el-input v-model="form.name" placeholder="请输入门店名称" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入门店联系电话" maxlength="20" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="门店图片" prop="image">
                <image-upload v-model="form.image" :limit="1" :file-size="5" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <div>
              <h3>地图位置</h3>
              <p>搜索地址或点击地图选点，系统将自动填写地址和经纬度</p>
            </div>
          </div>
          <el-form-item label-width="0">
            <amap-picker
              :longitude="form.longitude"
              :latitude="form.latitude"
              :address="formatAddress(form)"
              @select="handleLocationSelect"
            />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="省份" prop="province">
                <el-input v-model="form.province" placeholder="地图选点后自动填写" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="城市" prop="city">
                <el-input v-model="form.city" placeholder="地图选点后自动填写" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="区县" prop="district">
                <el-input v-model="form.district" placeholder="地图选点后自动填写" maxlength="50" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="详细地址" prop="address">
                <el-input v-model="form.address" placeholder="请输入街道、门牌号等详细地址" maxlength="255" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="经度" prop="longitude">
                <el-input-number v-model="form.longitude" :min="-180" :max="180" :precision="7" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="纬度" prop="latitude">
                <el-input-number v-model="form.latitude" :min="-90" :max="90" :precision="7" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <div>
              <h3>营业与预约</h3>
              <p>跨午夜营业时，开始时间可以晚于结束时间</p>
            </div>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="营业开始时间" prop="openTime">
                <el-time-picker v-model="form.openTime" format="HH:mm" value-format="HH:mm" placeholder="开始时间" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="营业结束时间" prop="closeTime">
                <el-time-picker v-model="form.closeTime" format="HH:mm" value-format="HH:mm" placeholder="结束时间" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最早预约(分钟)" prop="preorderMinMinutes">
                <el-input-number v-model="form.preorderMinMinutes" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最长预约(天)" prop="preorderMaxDays">
                <el-input-number v-model="form.preorderMaxDays" :min="1" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="制作提前(分钟)" prop="makeLeadMinutes">
                <el-input-number v-model="form.makeLeadMinutes" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-heading">
            <div>
              <h3>运营设置</h3>
              <p>设置门店状态、排序、包装费和门店公告</p>
            </div>
          </div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="包装费(分)" prop="packFee">
                <el-input-number v-model="form.packFee" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="排序" prop="sortOrder">
                <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="状态" prop="status">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option v-for="item in shopStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="门店公告" prop="notice">
                <el-input v-model="form.notice" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入营业通知、取餐说明等信息" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="staffTitle" v-model="staffOpen" width="820px" append-to-body>
      <el-form ref="staffRef" :model="staffForm" :rules="staffRules" :inline="true">
        <el-form-item label="员工" prop="userId">
          <el-select v-model="staffForm.userId" filterable :disabled="staffEditing" placeholder="请选择员工" style="width: 220px">
            <el-option v-for="user in userOptions" :key="user.userId" :label="user.nickName + ' (' + user.userName + ')'" :value="user.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="role">
          <el-select v-model="staffForm.role" style="width: 130px">
            <el-option v-for="item in staffRoleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认门店" prop="isDefault">
          <el-switch v-model="staffForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitStaff">{{ staffEditing ? '保存' : '关联' }}</el-button>
          <el-button v-if="staffEditing" @click="resetStaffForm">取消编辑</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="staffLoading" :data="staffList">
        <el-table-column label="账号" prop="userName" />
        <el-table-column label="姓名" prop="nickName" />
        <el-table-column label="手机号" prop="phonenumber" />
        <el-table-column label="岗位" align="center" width="100">
          <template #default="scope">{{ staffRoleLabel(scope.row.role) }}</template>
        </el-table-column>
        <el-table-column label="默认门店" align="center" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.isDefault === 1" type="success">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="150">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="editStaff(scope.row)">编辑</el-button>
            <el-button link type="primary" icon="Delete" @click="removeStaff(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="AdminShop">
import AmapPicker from "@/components/AmapPicker"
import { listUser } from "@/api/system/user"
import {
  addShop,
  addShopStaff,
  changeShopStatus,
  delShop,
  delShopStaff,
  getShop,
  listShop,
  listShopStaff,
  updateShop,
  updateShopStaff
} from "@/api/admin/shop"

const { proxy } = getCurrentInstance()

const shopStatusOptions = [
  { label: "休息中", value: 0 },
  { label: "营业中", value: 1 },
  { label: "暂停接单", value: 2 }
]
const staffRoleOptions = [
  { label: "店员", value: "STAFF" },
  { label: "店长", value: "MANAGER" },
  { label: "管理员授权", value: "ADMIN" }
]

const loading = ref(true)
const showSearch = ref(true)
const shopList = ref([])
const total = ref(0)
const open = ref(false)
const title = ref("")
const staffOpen = ref(false)
const staffLoading = ref(false)
const staffEditing = ref(false)
const staffTitle = ref("")
const currentShopId = ref()
const staffList = ref([])
const userOptions = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    phone: undefined,
    status: undefined
  },
  form: {},
  staffForm: {},
  rules: {
    shopCode: [{ required: true, message: "门店编号不能为空", trigger: "blur" }],
    name: [{ required: true, message: "门店名称不能为空", trigger: "blur" }],
    phone: [{ required: true, message: "联系电话不能为空", trigger: "blur" }],
    openTime: [{ required: true, message: "营业开始时间不能为空", trigger: "change" }],
    closeTime: [{ required: true, message: "营业结束时间不能为空", trigger: "change" }],
    preorderMinMinutes: [{ required: true, message: "最早预约分钟数不能为空", trigger: "change" }],
    preorderMaxDays: [{ required: true, message: "最长预约天数不能为空", trigger: "change" }],
    makeLeadMinutes: [{ required: true, message: "制作提前分钟数不能为空", trigger: "change" }],
    packFee: [{ required: true, message: "包装费不能为空", trigger: "change" }],
    sortOrder: [{ required: true, message: "排序不能为空", trigger: "change" }],
    status: [{ required: true, message: "状态不能为空", trigger: "change" }]
  },
  staffRules: {
    userId: [{ required: true, message: "请选择员工", trigger: "change" }],
    role: [{ required: true, message: "请选择岗位", trigger: "change" }]
  }
})

const { queryParams, form, staffForm, rules, staffRules } = toRefs(data)

function getList() {
  loading.value = true
  listShop(queryParams.value).then(response => {
    shopList.value = response.rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: undefined,
    shopCode: undefined,
    name: undefined,
    image: "",
    phone: undefined,
    province: "",
    city: "",
    district: "",
    address: "",
    longitude: 0,
    latitude: 0,
    openTime: "10:00",
    closeTime: "22:00",
    preorderMinMinutes: 30,
    preorderMaxDays: 7,
    makeLeadMinutes: 30,
    status: 1,
    notice: "",
    packFee: 100,
    sortOrder: 0
  }
  proxy.resetForm("shopRef")
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = "新增门店"
}

function handleUpdate(row) {
  reset()
  getShop(row.id).then(response => {
    form.value = response.data
    form.value.openTime = formatTime(form.value.openTime)
    form.value.closeTime = formatTime(form.value.closeTime)
    open.value = true
    title.value = "修改门店"
  })
}

function submitForm() {
  proxy.$refs.shopRef.validate(valid => {
    if (!valid) return
    const action = form.value.id ? updateShop(form.value.id, form.value) : addShop(form.value)
    action.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? "修改成功" : "新增成功")
      open.value = false
      getList()
    })
  })
}

function cancel() {
  open.value = false
  reset()
}

function handleLocationSelect(location) {
  Object.entries(location).forEach(([key, value]) => {
    if (value !== undefined) form.value[key] = value
  })
}

function handleStatusChange(row, status) {
  changeShopStatus(row.id, status).then(() => {
    proxy.$modal.msgSuccess("状态修改成功")
  }).catch(() => {
    getList()
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除门店"' + row.name + '"？').then(() => {
    return delShop(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList()
  }).catch(() => {})
}

function handleStaff(row) {
  currentShopId.value = row.id
  staffTitle.value = row.name + " - 员工关联"
  staffOpen.value = true
  resetStaffForm()
  loadStaff()
  if (!userOptions.value.length) {
    listUser({ pageNum: 1, pageSize: 1000 }).then(response => {
      userOptions.value = response.rows
    })
  }
}

function loadStaff() {
  staffLoading.value = true
  listShopStaff(currentShopId.value).then(response => {
    staffList.value = response.data
  }).finally(() => {
    staffLoading.value = false
  })
}

function resetStaffForm() {
  staffEditing.value = false
  staffForm.value = {
    userId: undefined,
    role: "STAFF",
    isDefault: 0
  }
  proxy.resetForm("staffRef")
}

function editStaff(row) {
  staffEditing.value = true
  staffForm.value = {
    userId: row.userId,
    role: row.role,
    isDefault: row.isDefault
  }
}

function submitStaff() {
  proxy.$refs.staffRef.validate(valid => {
    if (!valid) return
    const action = staffEditing.value
      ? updateShopStaff(currentShopId.value, staffForm.value.userId, {
          role: staffForm.value.role,
          isDefault: staffForm.value.isDefault
        })
      : addShopStaff(currentShopId.value, staffForm.value)
    action.then(() => {
      proxy.$modal.msgSuccess(staffEditing.value ? "员工关联修改成功" : "员工关联成功")
      resetStaffForm()
      loadStaff()
    })
  })
}

function removeStaff(row) {
  proxy.$modal.confirm('是否确认移除员工"' + row.nickName + '"？').then(() => {
    return delShopStaff(currentShopId.value, row.userId)
  }).then(() => {
    proxy.$modal.msgSuccess("移除成功")
    loadStaff()
  }).catch(() => {})
}

function formatAddress(row) {
  return [row.province, row.city, row.district, row.address].filter(Boolean).join("")
}

function formatTime(value) {
  return value ? String(value).slice(0, 5) : ""
}

function staffRoleLabel(value) {
  return staffRoleOptions.find(item => item.value === value)?.label || value
}

getList()
</script>

<style scoped lang="scss">
.form-section {
  padding: 20px 20px 2px;
  margin-bottom: 18px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 14px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h3 {
    margin: 0 0 5px;
    color: var(--el-text-color-primary);
    font-size: 16px;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

:deep(.shop-edit-dialog .el-dialog__body) {
  max-height: calc(92vh - 145px);
  padding-top: 10px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .form-section {
    padding: 14px 12px 0;
  }
}
</style>
