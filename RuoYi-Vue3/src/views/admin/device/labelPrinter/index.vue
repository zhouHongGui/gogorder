<template>
  <div class="app-container label-printer-page">
    <el-card shadow="never" class="query-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
        <el-form-item label="门店" prop="shopId">
          <el-select v-model="queryParams.shopId" placeholder="请选择门店" clearable filterable style="width: 220px">
            <el-option v-for="shop in shopOptions" :key="shop.id" :label="shop.name" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编号" prop="sn">
          <el-input v-model="queryParams.sn" placeholder="请输入飞鹅 SN" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="设备名称" prop="printerName">
          <el-input v-model="queryParams.printerName" placeholder="请输入设备名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8 action-row">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['admin:label-printer:add']">新增打印机</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="printerList" class="printer-table">
      <el-table-column label="门店" prop="shopName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.shopName || findShopName(row.shopId) || '-' }}</template>
      </el-table-column>
      <el-table-column label="设备编号 SN" prop="sn" width="160" show-overflow-tooltip />
      <el-table-column label="设备名称" prop="printerName" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.printerName || '未命名设备' }}</template>
      </el-table-column>
      <el-table-column label="标签尺寸" align="center" width="110">
        <template #default="{ row }">{{ row.printWidth }}×{{ row.printHeight }}mm</template>
      </el-table-column>
      <el-table-column label="启用状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="飞鹅状态" prop="feieStatus" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="row.feieStatus ? 'status-text' : 'muted-text'">{{ row.feieStatus || '未查询' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态查询时间" prop="lastStatusTime" width="170" align="center">
        <template #default="{ row }">{{ row.lastStatusTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="170" align="center" />
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button link type="primary" icon="Refresh" @click="handleQueryStatus(row)" v-hasPermi="['admin:label-printer:status']">查状态</el-button>
          <el-button link type="primary" icon="Printer" @click="handleTestPrint(row)" v-hasPermi="['admin:label-printer:test']">测试</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['admin:label-printer:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['admin:label-printer:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" width="680px" append-to-body destroy-on-close>
      <el-form ref="printerRef" :model="form" :rules="rules" label-width="120px">
        <el-alert
          class="form-tip"
          type="info"
          :closable="false"
          show-icon
          title="绑定时会同步调用飞鹅开放平台；设备密钥只保存到后端，不会在列表中回显。"
        />
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="绑定门店" prop="shopId">
              <el-select v-model="form.shopId" placeholder="请选择门店" filterable style="width: 100%">
                <el-option v-for="shop in shopOptions" :key="shop.id" :label="shop.name" :value="shop.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备编号 SN" prop="sn">
              <el-input v-model="form.sn" placeholder="请输入飞鹅打印机 SN" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="printerName">
              <el-input v-model="form.printerName" placeholder="如：南宁店标签机" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="设备密钥" prop="printerKey">
              <el-input
                v-model="form.printerKey"
                type="password"
                show-password
                autocomplete="new-password"
                maxlength="128"
                :placeholder="form.id ? '不修改密钥请留空' : '请输入飞鹅打印机密钥'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签宽度" prop="printWidth">
              <el-input-number v-model="form.printWidth" :min="20" :max="120" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签高度" prop="printHeight">
              <el-input-number v-model="form.printHeight" :min="20" :max="120" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用状态" prop="status">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" maxlength="200" show-word-limit placeholder="可记录设备摆放位置、用途等" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AdminLabelPrinter">
// 引入门店下拉数据接口（用于绑定门店选择和列表门店名兜底）
import { listShop } from "@/api/admin/shop"
// 引入标签打印机的增删改查与状态/测试接口
import {
  addLabelPrinter,
  delLabelPrinter,
  getLabelPrinter,
  listLabelPrinter,
  queryLabelPrinterStatus,
  testLabelPrinter,
  updateLabelPrinter
} from "@/api/admin/labelPrinter"

// 获取组件实例代理，用于访问全局挂载的 $modal/resetForm 等方法（Vue 3 setup 语法）
const { proxy } = getCurrentInstance()

const loading = ref(false) // 列表加载状态
const showSearch = ref(true) // 是否显示查询条件区
const open = ref(false) // 新增/修改弹窗是否打开
const title = ref("") // 弹窗标题（新增/修改）
const total = ref(0) // 列表总条数（分页用）
const printerList = ref([]) // 打印机列表数据
const shopOptions = ref([]) // 门店下拉选项

// 查询参数：分页 + 筛选条件
const queryParams = reactive({
  pageNum: 1, // 当前页码
  pageSize: 10, // 每页条数
  shopId: undefined, // 门店筛选
  sn: undefined, // 设备编号筛选
  printerName: undefined, // 设备名称筛选
  status: undefined // 启用状态筛选
})

const form = ref({}) // 新增/修改表单数据

// 启用状态下拉选项
const statusOptions = [
  { label: "启用", value: 1 },
  { label: "停用", value: 0 }
]

// 设备密钥自定义校验：仅新增时必填，修改时留空表示不修改密钥
const validatePrinterKey = (rule, value, callback) => {
  if (!form.value.id && !value) { // 新增（无 id）且未填密钥
    callback(new Error("新增打印机时设备密钥不能为空"))
    return
  }
  callback() // 校验通过
}

// 表单校验规则
const rules = {
  shopId: [{ required: true, message: "绑定门店不能为空", trigger: "change" }],
  sn: [{ required: true, message: "设备编号 SN 不能为空", trigger: "blur" }],
  printerKey: [{ validator: validatePrinterKey, trigger: "blur" }], // 自定义密钥校验
  printWidth: [{ required: true, message: "标签宽度不能为空", trigger: "change" }],
  printHeight: [{ required: true, message: "标签高度不能为空", trigger: "change" }]
}

// 拉取列表数据
function getList() {
  loading.value = true // 开始加载
  listLabelPrinter(queryParams).then(response => {
    printerList.value = response.rows || [] // 列表数据兜底空数组
    total.value = response.total || 0 // 总条数兜底 0
  }).finally(() => {
    loading.value = false // 无论成功失败都关闭加载态
  })
}

// 加载门店下拉（一次性拉取较多条目用于选择和名称兜底）
function loadShops() {
  listShop({ pageNum: 1, pageSize: 500 }).then(response => {
    shopOptions.value = response.rows || []
  })
}

// 重置表单为默认值（新增与修改前都会调用）
function reset() {
  form.value = {
    id: undefined,
    shopId: undefined,
    sn: "",
    printerKey: "",
    printerName: "",
    printWidth: 40, // 默认宽度 40mm
    printHeight: 50, // 默认高度 50mm，与后端/SQL 默认值一致
    status: 1, // 默认启用
    remark: ""
  }
  proxy.resetForm("printerRef") // 清空表单校验状态
}

// 点击搜索：回到第一页并重新查询
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

// 点击重置：清空查询条件后重新查询
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

// 点击新增：重置表单并打开弹窗
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增标签打印机"
}

// 点击修改：拉取详情后填充表单（密钥置空，不回显，留空表示不修改）
function handleUpdate(row) {
  reset()
  getLabelPrinter(row.id).then(response => {
    form.value = {
      ...response.data,
      printerKey: "" // 密钥不回显
    }
    open.value = true
    title.value = "修改标签打印机"
  })
}

// 提交表单：先校验，再按有无 id 判断新增或修改
function submitForm() {
  proxy.$refs["printerRef"].validate(valid => {
    if (!valid) { // 校验不通过
      return
    }
    const payload = { ...form.value }
    // 有 id 走修改，否则走新增
    const request = payload.id ? updateLabelPrinter(payload.id, payload) : addLabelPrinter(payload)
    request.then(() => {
      proxy.$modal.msgSuccess(payload.id ? "修改成功" : "新增成功")
      open.value = false // 关闭弹窗
      getList() // 刷新列表
    })
  })
}

// 取消：关闭弹窗并重置表单
function cancel() {
  open.value = false
  reset()
}

// 删除：二次确认后调用删除接口
function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除标签打印机"' + (row.printerName || row.sn) + '"？').then(() => {
    return delLabelPrinter(row.id) // 确认后调用删除
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功")
    getList() // 刷新列表
  }).catch(() => {}) // 用户取消时不报错
}

// 查询飞鹅状态：成功后提示并刷新列表
function handleQueryStatus(row) {
  queryLabelPrinterStatus(row.id).then(response => {
    proxy.$modal.msgSuccess("飞鹅状态：" + (response.data || "已查询"))
    getList()
  })
}

// 测试打印：发送测试标签并提示
function handleTestPrint(row) {
  testLabelPrinter(row.id).then(response => {
    proxy.$modal.msgSuccess(response.data || "测试标签已发送")
  })
}

// 根据门店 id 查找门店名（列表门店名为空时的兜底显示）
function findShopName(shopId) {
  const shop = shopOptions.value.find(item => item.id === shopId)
  return shop ? shop.name : ""
}

// 页面初始化：先加载门店下拉，再加载列表
loadShops()
getList()
</script>

<style scoped>
.label-printer-page {
  background: #f5f7fb;
}

.query-card {
  margin-bottom: 14px;
  border: 1px solid #edf0f7;
}

.action-row {
  align-items: center;
}

.printer-table {
  border-radius: 10px;
  overflow: hidden;
}

.form-tip {
  margin-bottom: 18px;
}

.status-text {
  color: #1f2d3d;
}

.muted-text {
  color: #909399;
}
</style>
