<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="员工账号" prop="account">
        <el-input v-model="queryParams.account" placeholder="请输入员工账号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="员工姓名" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入员工姓名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['admin:shop:staff']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['admin:shop:staff']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['admin:shop:staff']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="staffList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" prop="id" align="center" width="80" />
      <el-table-column label="员工账号" prop="account" min-width="120" show-overflow-tooltip />
      <el-table-column label="员工姓名" prop="nickname" min-width="120" show-overflow-tooltip />
      <el-table-column label="手机号" prop="phone" width="130" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="0"
            :inactive-value="1"
            @change="value => handleStatusChange(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="Token版本" prop="tokenVersion" align="center" width="100" />
      <el-table-column label="最后登录" prop="lastLoginTime" align="center" width="170">
        <template #default="{ row }">{{ parseTime(row.lastLoginTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" align="center" width="170">
        <template #default="{ row }">{{ parseTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['admin:shop:staff']">修改</el-button>
          <el-button link type="primary" icon="House" @click="handleAuth(row)" v-hasPermi="['admin:shop:staff']">门店授权</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['admin:shop:staff']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="staffRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="员工账号" prop="account" v-if="!form.id">
          <el-input v-model="form.account" placeholder="请输入员工账号" maxlength="30" />
        </el-form-item>
        <el-form-item label="员工账号" v-else>
          <el-input v-model="form.account" disabled />
        </el-form-item>
        <el-form-item label="员工姓名" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入员工姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" :placeholder="form.id ? '留空则不修改密码' : '请输入登录密码'" type="password" maxlength="20" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="authTitle" v-model="authOpen" width="920px" append-to-body>
      <el-alert title="员工必须至少绑定一个门店后才能登录员工端；首次绑定会自动成为默认门店。" type="info" show-icon class="mb8" />
      <el-form ref="bindRef" :model="bindForm" :rules="bindRules" :inline="true">
        <el-form-item label="门店" prop="shopIds">
          <el-select
            v-model="bindForm.shopIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            filterable
            placeholder="请选择门店"
            style="width: 360px"
            @change="handleBindShopChange"
          >
            <el-option v-for="shop in availableShopOptions" :key="shop.id" :label="shop.name" :value="shop.id">
              <span>{{ shop.name }}</span>
              <span class="shop-option-code">{{ shop.shopCode }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="默认门店">
          <el-select v-model="bindForm.defaultShopId" clearable placeholder="不指定则自动处理" style="width: 220px">
            <el-option v-for="shop in selectedShopOptions" :key="shop.id" :label="shop.name" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="Plus"
            :loading="bindSubmitting"
            :disabled="!authStaff || bindForm.shopIds.length === 0"
            @click="submitBind"
          >批量授权</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="authLoading" :data="staffShopList" border>
        <el-table-column label="门店ID" prop="shopId" align="center" width="90" />
        <el-table-column label="门店编码" prop="shopCode" align="center" width="130" show-overflow-tooltip />
        <el-table-column label="门店名称" prop="shopName" min-width="160" show-overflow-tooltip />
        <el-table-column label="地址" prop="address" min-width="220" show-overflow-tooltip />
        <el-table-column label="默认" align="center" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="240">
          <template #default="{ row }">
            <el-button v-if="row.isDefault !== 1" link type="primary" icon="Star" @click="setDefaultBinding(row)">设默认</el-button>
            <el-button link type="danger" icon="Delete" @click="removeBinding(row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="ShopStaff">
import { addStaff, delStaff, getStaff, listStaff, listStaffShops, updateStaff } from "@/api/admin/staff"
import { addShopStaff, delShopStaff, listShop, updateShopStaff } from "@/api/admin/shop"

const { proxy } = getCurrentInstance()

const validatePassword = (rule, value, callback) => {
  if (!form.value.id && !value) {
    callback(new Error("登录密码不能为空"))
    return
  }
  if (value && (value.length < 5 || value.length > 20)) {
    callback(new Error("密码长度应为5到20个字符"))
    return
  }
  callback()
}

const loading = ref(false)
const authLoading = ref(false)
const showSearch = ref(true)
const staffList = ref([])
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const open = ref(false)
const title = ref("")
const authOpen = ref(false)
const authTitle = ref("")
const authStaff = ref(null)
const staffShopList = ref([])
const shopOptions = ref([])
const bindSubmitting = ref(false)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    account: undefined,
    nickname: undefined,
    phone: undefined,
    status: undefined
  },
  form: {},
  bindForm: {
    shopIds: [],
    defaultShopId: undefined
  },
  rules: {
    account: [
      { required: true, message: "员工账号不能为空", trigger: "blur" },
      { pattern: /^[a-zA-Z0-9_]{2,30}$/, message: "账号只能包含字母、数字和下划线，长度2到30位", trigger: "blur" }
    ],
    nickname: [
      { required: true, message: "员工姓名不能为空", trigger: "blur" },
      { max: 50, message: "员工姓名不能超过50个字符", trigger: "blur" }
    ],
    phone: [
      { required: true, message: "手机号不能为空", trigger: "blur" },
      { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" }
    ],
    password: [{ validator: validatePassword, trigger: "blur" }],
    status: [{ required: true, message: "员工状态不能为空", trigger: "change" }]
  },
  bindRules: {
    shopIds: [{ type: "array", required: true, min: 1, message: "请选择门店", trigger: "change" }]
  }
})

const { queryParams, form, bindForm, rules, bindRules } = toRefs(data)

const boundShopIds = computed(() => staffShopList.value.map(item => item.shopId))
const availableShopOptions = computed(() => shopOptions.value.filter(shop => !boundShopIds.value.includes(shop.id)))
const selectedShopOptions = computed(() => {
  const selectedIds = bindForm.value.shopIds || []
  return availableShopOptions.value.filter(shop => selectedIds.includes(shop.id))
})

function getList() {
  loading.value = true
  listStaff(queryParams.value).then(response => {
    staffList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function reset() {
  form.value = {
    id: undefined,
    account: undefined,
    nickname: undefined,
    phone: undefined,
    password: undefined,
    status: 0
  }
  proxy.resetForm("staffRef")
}

function resetBind() {
  bindForm.value = {
    shopIds: [],
    defaultShopId: undefined
  }
  proxy.resetForm("bindRef")
}

function handleBindShopChange(shopIds) {
  if (bindForm.value.defaultShopId && !shopIds.includes(bindForm.value.defaultShopId)) {
    bindForm.value.defaultShopId = undefined
  }
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = selection.length === 0
}

function handleAdd() {
  reset()
  title.value = "新增门店员工"
  open.value = true
}

function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getStaff(id).then(response => {
    form.value = {
      id: response.data.id,
      account: response.data.account,
      nickname: response.data.nickname,
      phone: response.data.phone,
      password: undefined,
      status: response.data.status
    }
    title.value = "修改门店员工"
    open.value = true
  })
}

function cancel() {
  open.value = false
  reset()
}

function submitForm() {
  proxy.$refs["staffRef"].validate(valid => {
    if (!valid) return
    const payload = {
      nickname: form.value.nickname,
      phone: form.value.phone,
      status: form.value.status
    }
    if (form.value.password) {
      payload.password = form.value.password
    }
    if (form.value.id) {
      updateStaff(form.value.id, payload).then(() => {
        proxy.$modal.msgSuccess("修改成功")
        open.value = false
        getList()
      })
      return
    }
    payload.account = form.value.account
    payload.password = form.value.password
    addStaff(payload).then(() => {
      proxy.$modal.msgSuccess("新增成功，请继续配置门店授权")
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  const deleteIds = row?.id ? [row.id] : ids.value
  proxy.$modal.confirm("确认删除选中的门店员工账号吗？已绑定门店的员工需要先解绑。").then(() => {
    return Promise.all(deleteIds.map(id => delStaff(id)))
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList()
  }).catch(() => {})
}

function handleStatusChange(row, value) {
  const text = value === 0 ? "启用" : "停用"
  proxy.$modal.confirm(`确认要${text}员工"${row.nickname}"吗？`).then(() => {
    return updateStaff(row.id, {
      nickname: row.nickname,
      phone: row.phone,
      status: value
    })
  }).then(() => {
    proxy.$modal.msgSuccess(`${text}成功`)
  }).catch(() => {
    row.status = value === 0 ? 1 : 0
  })
}

function handleAuth(row) {
  authStaff.value = row
  authTitle.value = `门店授权 - ${row.nickname}（${row.account}）`
  authOpen.value = true
  loadAuthData()
}

function loadAuthData() {
  if (!authStaff.value) return
  authLoading.value = true
  Promise.all([
    listStaffShops(authStaff.value.id),
    listShop({ pageNum: 1, pageSize: 1000 })
  ]).then(([staffShopRes, shopRes]) => {
    staffShopList.value = staffShopRes.data || []
    shopOptions.value = shopRes.rows || []
    resetBind()
  }).finally(() => {
    authLoading.value = false
  })
}

function submitBind() {
  proxy.$refs["bindRef"].validate(async valid => {
    if (!valid || !authStaff.value) return
    const shopIds = bindForm.value.shopIds || []
    const defaultShopId = bindForm.value.defaultShopId || (staffShopList.value.length === 0 ? shopIds[0] : undefined)
    bindSubmitting.value = true
    try {
      for (const shopId of shopIds) {
        await addShopStaff(shopId, {
          staffId: authStaff.value.id,
          isDefault: shopId === defaultShopId ? 1 : 0
        })
      }
      proxy.$modal.msgSuccess(`已授权 ${shopIds.length} 家门店`)
      loadAuthData()
    } finally {
      bindSubmitting.value = false
    }
  })
}

function setDefaultBinding(row) {
  updateShopStaff(row.shopId, authStaff.value.id, {
    isDefault: 1
  }).then(() => {
    proxy.$modal.msgSuccess("默认门店已更新")
    loadAuthData()
  })
}

function removeBinding(row) {
  proxy.$modal.confirm(`确认解除该员工与门店"${row.shopName}"的绑定吗？`).then(() => {
    return delShopStaff(row.shopId, authStaff.value.id)
  }).then(() => {
    proxy.$modal.msgSuccess("解绑成功")
    loadAuthData()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.shop-option-code {
  float: right;
  color: #8492a6;
  font-size: 13px;
}
</style>
