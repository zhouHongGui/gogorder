<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="商品分类" name="category">
        <el-button type="primary" plain icon="Plus" @click="openCategory()" v-hasPermi="['admin:category:add']">新增分类</el-button>
        <el-table :data="categories" class="mt12">
          <el-table-column label="分类名称" prop="name" />
          <el-table-column label="排序" prop="sortOrder" width="100" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCategory(row)" v-hasPermi="['admin:category:edit']">修改</el-button>
              <el-button link type="danger" @click="removeCategory(row)" v-hasPermi="['admin:category:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="规格模板" name="spec">
        <el-button type="primary" plain icon="Plus" @click="openSpec()" v-hasPermi="['admin:spec:add']">新增模板</el-button>
        <el-table :data="specTemplates" class="mt12">
          <el-table-column label="规格名称" prop="name" />
          <el-table-column label="类型" width="90" align="center">
            <template #default="{ row }">{{ row.type === 1 ? '单选' : '多选' }}</template>
          </el-table-column>
          <el-table-column label="选择范围" width="130" align="center">
            <template #default="{ row }">{{ row.isRequired === 1 ? '必选' : '可选' }} {{ row.minSelect }}-{{ row.maxSelect }}</template>
          </el-table-column>
          <el-table-column label="选项数" prop="optionCount" width="90" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="240" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openOptions(row)" v-hasPermi="['admin:spec:list']">选项</el-button>
              <el-button link type="primary" @click="openSpec(row)" v-hasPermi="['admin:spec:edit']">修改</el-button>
              <el-button link type="danger" @click="removeSpec(row)" v-hasPermi="['admin:spec:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商品库" name="product">
        <el-form :inline="true" :model="productQuery">
          <el-form-item label="商品名称"><el-input v-model="productQuery.name" clearable @keyup.enter="loadProducts" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="productQuery.categoryId" clearable style="width: 180px">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="loadProducts">查询</el-button></el-form-item>
          <el-form-item><el-button type="primary" plain icon="Plus" @click="openProduct()" v-hasPermi="['admin:product:add']">新增商品</el-button></el-form-item>
        </el-form>
        <el-table :data="products">
          <el-table-column label="商品主图" width="90" align="center">
            <template #default="{ row }">
              <image-preview v-if="row.image" :src="row.image" :width="56" :height="56" />
              <span v-else>暂无</span>
            </template>
          </el-table-column>
          <el-table-column label="商品名称" prop="name" min-width="160" />
          <el-table-column label="基础价(元)" width="110" align="center">
            <template #default="{ row }">¥{{ formatYuan(row.basePrice) }}</template>
          </el-table-column>
          <el-table-column label="规格模板" min-width="180">
            <template #default="{ row }">{{ formatSpecTemplateNames(row.specTemplateIds) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProduct(row)" v-hasPermi="['admin:product:edit']">修改</el-button>
              <el-button link type="danger" @click="removeProduct(row)" v-hasPermi="['admin:product:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="productTotal > 0" :total="productTotal" v-model:page="productQuery.pageNum" v-model:limit="productQuery.pageSize" @pagination="loadProducts" />
      </el-tab-pane>

      <el-tab-pane label="门店商品" name="shopProduct">
        <el-form :inline="true">
          <el-form-item label="门店">
            <el-select v-model="currentShopId" filterable style="width: 240px" @change="loadShopProducts">
              <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="assignOpen = true" :disabled="!currentShopId" v-hasPermi="['admin:shop-product:assign']">分配商品</el-button></el-form-item>
          <el-form-item><el-button type="danger" plain @click="batchRemoveShopProduct" :disabled="!selectedShopProducts.length" v-hasPermi="['admin:shop-product:remove']">批量移除</el-button></el-form-item>
        </el-form>
        <el-table :data="shopProducts" @selection-change="selectedShopProducts = $event">
          <el-table-column type="selection" width="50" />
          <el-table-column label="商品图片" width="90" align="center">
            <template #default="{ row }">
              <image-preview v-if="row.productImage" :src="row.productImage" :width="56" :height="56" />
              <span v-else>暂无</span>
            </template>
          </el-table-column>
          <el-table-column label="商品名称" prop="productName" min-width="160" />
          <el-table-column label="基础价" prop="basePrice" width="90" align="center" />
          <el-table-column label="门店价" width="100" align="center">
            <template #default="{ row }">{{ row.price == null ? '使用基础价' : row.price }}</template>
          </el-table-column>
          <el-table-column label="有效售价" prop="effectivePrice" width="100" align="center" />
          <el-table-column label="库存" width="100" align="center">
            <template #default="{ row }">{{ row.stock === -1 ? '无限' : row.stock }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">{{ row.status === 1 ? '上架' : '下架' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="openShopProduct(row)" v-hasPermi="['admin:shop-product:edit']">设置</el-button>
              <el-button link type="primary" @click="openStock(row)" v-hasPermi="['admin:shop-product:stock']">调库存</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="shopProductTotal > 0" :total="shopProductTotal" v-model:page="shopProductQuery.pageNum" v-model:limit="shopProductQuery.pageSize" @pagination="loadShopProducts" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog :title="categoryForm.id ? '修改分类' : '新增分类'" v-model="categoryOpen" width="480px">
      <el-form :model="categoryForm" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="categoryForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="categoryForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="categoryOpen = false">取消</el-button><el-button type="primary" @click="saveCategory">确定</el-button></template>
    </el-dialog>

    <el-dialog :title="specForm.id ? '修改规格模板' : '新增规格模板'" v-model="specOpen" width="560px">
      <el-form :model="specForm" label-width="120px">
        <el-form-item label="规格名称"><el-input v-model="specForm.name" maxlength="50" /></el-form-item>
        <el-form-item label="规格类型"><el-radio-group v-model="specForm.type"><el-radio :value="1">单选</el-radio><el-radio :value="2">多选</el-radio></el-radio-group></el-form-item>
        <el-form-item label="是否必选"><el-switch v-model="specForm.isRequired" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="最少选择"><el-input-number v-model="specForm.minSelect" :min="0" /></el-form-item>
        <el-form-item label="最多选择"><el-input-number v-model="specForm.maxSelect" :min="1" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="specForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="specForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="specOpen = false">取消</el-button><el-button type="primary" @click="saveSpec">确定</el-button></template>
    </el-dialog>

    <el-dialog :title="optionTitle" v-model="optionOpen" width="860px">
      <el-form :inline="true" :model="optionForm">
        <el-form-item label="选项名"><el-input v-model="optionForm.label" style="width: 140px" /></el-form-item>
        <el-form-item label="加价"><el-input-number v-model="optionForm.priceAdd" :min="0" style="width: 120px" /></el-form-item>
        <el-form-item label="默认"><el-switch v-model="optionForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveOption">{{ optionForm.id ? '保存' : '新增' }}</el-button><el-button v-if="optionForm.id" @click="resetOption">取消编辑</el-button></el-form-item>
      </el-form>
      <el-table :data="options">
        <el-table-column label="optionId" prop="optionId" min-width="260" />
        <el-table-column label="选项名" prop="label" />
        <el-table-column label="加价(分)" prop="priceAdd" width="100" align="center" />
        <el-table-column label="默认" width="80" align="center"><template #default="{ row }">{{ row.isDefault === 1 ? '是' : '否' }}</template></el-table-column>
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template></el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }"><el-button link type="primary" @click="editOption(row)">修改</el-button><el-button link type="danger" @click="disableOption(row)" :disabled="row.status === 0">禁用</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="productForm.id ? '修改商品' : '新增商品'" v-model="productOpen" width="720px">
      <el-form :model="productForm" label-width="110px">
        <el-form-item label="商品名称"><el-input v-model="productForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="商品主图"><image-upload v-model="productForm.image" :limit="1" :file-size="5" /></el-form-item>
        <el-form-item label="基础价格(分)"><el-input-number v-model="productForm.basePrice" :min="0" /></el-form-item>
        <el-form-item label="关联分类"><el-select v-model="productForm.categoryIds" multiple style="width: 100%"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="规格模板"><el-select v-model="productForm.specTemplateIds" multiple style="width: 100%"><el-option v-for="item in specTemplates" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="标签"><el-input v-model="productForm.tagsText" placeholder="多个标签用逗号分隔" /></el-form-item>
        <el-form-item label="详情图"><image-upload v-model="productForm.imagesUpload" :limit="8" :file-size="5" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="productForm.description" type="textarea" maxlength="500" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="productForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="productOpen = false">取消</el-button><el-button type="primary" @click="saveProduct">确定</el-button></template>
    </el-dialog>

    <el-dialog title="分配商品" v-model="assignOpen" width="600px">
      <el-select v-model="assignProductIds" multiple filterable style="width: 100%" placeholder="请选择商品">
        <el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <template #footer><el-button @click="assignOpen = false">取消</el-button><el-button type="primary" @click="saveAssign">确定</el-button></template>
    </el-dialog>

    <el-dialog title="门店商品设置" v-model="shopProductOpen" width="480px">
      <el-form :model="shopProductForm" label-width="100px">
        <el-form-item label="商品"><span>{{ shopProductForm.productName }}</span></el-form-item>
        <el-form-item label="门店售价"><el-input-number v-model="shopProductForm.price" :min="0" placeholder="留空使用基础价" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="shopProductForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="shopProductForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shopProductOpen = false">取消</el-button><el-button type="primary" @click="saveShopProduct">确定</el-button></template>
    </el-dialog>

    <el-dialog title="库存调整" v-model="stockOpen" width="500px">
      <el-form :model="stockForm" label-width="100px">
        <el-form-item label="目标库存"><el-input-number v-model="stockForm.stock" :min="-1" /><span class="ml8">-1 表示无限库存</span></el-form-item>
        <el-form-item label="调整原因"><el-input v-model="stockForm.reason" maxlength="200" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="stockOpen = false">取消</el-button><el-button type="primary" @click="saveStock">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="AdminProduct">
import { listShop } from "@/api/admin/shop"
import {
  addCategory, addProduct, addSpecOption, addSpecTemplate, adjustShopProductStock, assignShopProduct,
  delCategory, delProduct, delSpecTemplate, disableSpecOption, getProduct, listCategory, listProduct,
  listShopProduct, listSpecOption, listSpecTemplate, removeShopProducts, updateCategory, updateProduct,
  updateShopProduct, updateSpecOption, updateSpecTemplate
} from "@/api/admin/product"

const { proxy } = getCurrentInstance()
const activeTab = ref("category")
const categories = ref([])
const specTemplates = ref([])
const options = ref([])
const products = ref([])
const shops = ref([])
const shopProducts = ref([])
const selectedShopProducts = ref([])
const currentShopId = ref()
const productTotal = ref(0)
const shopProductTotal = ref(0)
const categoryOpen = ref(false)
const specOpen = ref(false)
const optionOpen = ref(false)
const productOpen = ref(false)
const assignOpen = ref(false)
const shopProductOpen = ref(false)
const stockOpen = ref(false)
const optionTitle = ref("")
const currentTemplateId = ref()
const assignProductIds = ref([])
const stockShopProductId = ref()

const productQuery = reactive({ pageNum: 1, pageSize: 10, name: undefined, categoryId: undefined })
const shopProductQuery = reactive({ pageNum: 1, pageSize: 10 })
const categoryForm = reactive({})
const specForm = reactive({})
const optionForm = reactive({})
const productForm = reactive({})
const shopProductForm = reactive({})
const stockForm = reactive({})

function handleTabChange(name) {
  if (name === "category") loadCategories()
  if (name === "spec") loadSpecs()
  if (name === "product") loadProducts()
  if (name === "shopProduct") loadShopProducts()
}
function loadCategories() { return listCategory().then(r => categories.value = r.data) }
function loadSpecs() { return listSpecTemplate().then(r => specTemplates.value = r.data) }
function loadProducts() { return listProduct(productQuery).then(r => { products.value = r.rows; productTotal.value = r.total }) }
function loadShops() { return listShop({ pageNum: 1, pageSize: 1000 }).then(r => { shops.value = r.rows; if (!currentShopId.value && r.rows.length) currentShopId.value = r.rows[0].id }) }
function loadShopProducts() {
  if (!currentShopId.value) return
  listShopProduct(currentShopId.value, shopProductQuery).then(r => { shopProducts.value = r.rows; shopProductTotal.value = r.total })
}
function replace(target, source) { Object.keys(target).forEach(key => delete target[key]); Object.assign(target, source) }
function formatYuan(value) { return (Number(value || 0) / 100).toFixed(2) }
function formatSpecTemplateNames(ids) {
  if (!ids?.length) return "无规格"
  const nameMap = new Map(specTemplates.value.map(item => [String(item.id), item.name]))
  return ids.map(id => nameMap.get(String(id)) || `未知规格(${id})`).join("、")
}

function openCategory(row) { replace(categoryForm, row ? { ...row } : { name: "", sortOrder: 0, status: 1 }); categoryOpen.value = true }
function saveCategory() {
  const action = categoryForm.id ? updateCategory(categoryForm.id, categoryForm) : addCategory(categoryForm)
  action.then(() => { categoryOpen.value = false; proxy.$modal.msgSuccess("保存成功"); loadCategories() })
}
function removeCategory(row) { proxy.$modal.confirm(`确认删除分类“${row.name}”吗？`).then(() => delCategory(row.id)).then(() => { proxy.$modal.msgSuccess("删除成功"); loadCategories() }).catch(() => {}) }

function openSpec(row) { replace(specForm, row ? { ...row } : { name: "", type: 1, isRequired: 1, minSelect: 1, maxSelect: 1, sortOrder: 0, status: 1 }); specOpen.value = true }
function saveSpec() {
  const action = specForm.id ? updateSpecTemplate(specForm.id, specForm) : addSpecTemplate(specForm)
  action.then(() => { specOpen.value = false; proxy.$modal.msgSuccess("保存成功"); loadSpecs() })
}
function removeSpec(row) { proxy.$modal.confirm(`确认删除规格模板“${row.name}”吗？`).then(() => delSpecTemplate(row.id)).then(() => { proxy.$modal.msgSuccess("删除成功"); loadSpecs() }).catch(() => {}) }
function openOptions(row) { currentTemplateId.value = row.id; optionTitle.value = `${row.name} - 规格选项`; optionOpen.value = true; resetOption(); loadOptions() }
function loadOptions() { listSpecOption(currentTemplateId.value).then(r => options.value = r.data) }
function resetOption() { replace(optionForm, { templateId: currentTemplateId.value, label: "", priceAdd: 0, isDefault: 0, sortOrder: 0, status: 1 }) }
function editOption(row) { replace(optionForm, { ...row }) }
function saveOption() {
  const action = optionForm.id ? updateSpecOption(optionForm.id, optionForm) : addSpecOption(optionForm)
  action.then(() => { proxy.$modal.msgSuccess("保存成功"); resetOption(); loadOptions(); loadSpecs() })
}
function disableOption(row) { proxy.$modal.confirm(`确认禁用选项“${row.label}”吗？`).then(() => disableSpecOption(row.id)).then(() => { proxy.$modal.msgSuccess("已禁用"); loadOptions(); loadSpecs() }).catch(() => {}) }

function openProduct(row) {
  if (!row) { replace(productForm, { name: "", image: "", basePrice: 0, categoryIds: [], specTemplateIds: [], tagsText: "", imagesUpload: "", description: "", status: 1 }); productOpen.value = true; return }
  getProduct(row.id).then(r => { const p = r.data; replace(productForm, { ...p, tagsText: (p.tags || []).join(","), imagesUpload: (p.images || []).join(",") }); productOpen.value = true })
}
function saveProduct() {
  const payload = { ...productForm, tags: (productForm.tagsText || "").split(",").map(v => v.trim()).filter(Boolean), images: (productForm.imagesUpload || "").split(",").map(v => v.trim()).filter(Boolean) }
  delete payload.tagsText; delete payload.imagesUpload
  const action = payload.id ? updateProduct(payload.id, payload) : addProduct(payload)
  action.then(() => { productOpen.value = false; proxy.$modal.msgSuccess("保存成功"); loadProducts() })
}
function removeProduct(row) { proxy.$modal.confirm(`确认删除商品“${row.name}”吗？`).then(() => delProduct(row.id)).then(() => { proxy.$modal.msgSuccess("删除成功"); loadProducts() }).catch(() => {}) }

function saveAssign() { assignShopProduct({ shopId: currentShopId.value, productIds: assignProductIds.value }).then(() => { assignOpen.value = false; assignProductIds.value = []; proxy.$modal.msgSuccess("分配完成"); loadShopProducts() }) }
function openShopProduct(row) { replace(shopProductForm, { ...row }); shopProductOpen.value = true }
function saveShopProduct() { updateShopProduct(shopProductForm.id, { price: shopProductForm.price, status: shopProductForm.status, sortOrder: shopProductForm.sortOrder }).then(() => { shopProductOpen.value = false; proxy.$modal.msgSuccess("保存成功"); loadShopProducts() }) }
function openStock(row) { stockShopProductId.value = row.id; replace(stockForm, { stock: row.stock, reason: "" }); stockOpen.value = true }
function saveStock() { adjustShopProductStock(stockShopProductId.value, { requestId: crypto.randomUUID(), stock: stockForm.stock, reason: stockForm.reason }).then(() => { stockOpen.value = false; proxy.$modal.msgSuccess("库存调整成功"); loadShopProducts() }) }
function batchRemoveShopProduct() { const ids = selectedShopProducts.value.map(v => v.id); proxy.$modal.confirm(`确认移除选中的 ${ids.length} 个门店商品吗？`).then(() => removeShopProducts(ids)).then(() => { proxy.$modal.msgSuccess("移除成功"); loadShopProducts() }).catch(() => {}) }

Promise.all([loadCategories(), loadSpecs(), loadProducts(), loadShops()]).then(loadShopProducts)
</script>

<style scoped>
.mt12 { margin-top: 12px; }
.ml8 { margin-left: 8px; color: var(--el-text-color-secondary); }
</style>
