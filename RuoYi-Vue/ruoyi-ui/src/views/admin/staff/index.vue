<template>
  <div class="app-container">
    <el-form ref="queryForm" :model="queryParams" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="员工账号" prop="account">
        <el-input
          v-model="queryParams.account"
          placeholder="请输入员工账号"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="员工姓名" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入员工姓名"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          style="width: 220px"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="员工状态" clearable style="width: 160px">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['admin:shop:staff']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['admin:shop:staff']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['admin:shop:staff']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="staffList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="员工账号" align="center" prop="account" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="员工姓名" align="center" prop="nickname" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="手机号" align="center" prop="phone" width="130" />
      <el-table-column label="状态" align="center" width="100">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="0"
            :inactive-value="1"
            active-color="#13ce66"
            inactive-color="#ff4949"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="Token版本" align="center" prop="tokenVersion" width="100" />
      <el-table-column label="最后登录" align="center" prop="lastLoginTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.lastLoginTime) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['admin:shop:staff']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-s-shop"
            @click="handleAuth(scope.row)"
            v-hasPermi="['admin:shop:staff']"
          >门店授权</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['admin:shop:staff']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
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
          <el-input
            v-model="form.password"
            :placeholder="form.id ? '留空则不修改密码' : '请输入登录密码'"
            type="password"
            maxlength="20"
            show-password
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">正常</el-radio>
            <el-radio :label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="authTitle" :visible.sync="authOpen" width="860px" append-to-body>
      <el-alert
        title="员工必须至少绑定一个门店后，才能登录员工端。第一个绑定会被后端自动设为默认门店。"
        type="info"
        show-icon
        class="mb8"
      />
      <el-form ref="bindForm" :model="bindForm" :rules="bindRules" size="small" :inline="true" label-width="78px">
        <el-form-item label="选择门店" prop="shopIds">
          <el-select
            v-model="bindForm.shopIds"
            multiple
            collapse-tags
            filterable
            placeholder="请选择门店"
            style="width: 360px"
            @change="handleBindShopChange"
          >
            <el-option
              v-for="shop in availableShopOptions"
              :key="shop.id"
              :label="shop.name"
              :value="shop.id"
            >
              <span>{{ shop.name }}</span>
              <span class="shop-option-code">{{ shop.shopCode }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" prop="role">
          <el-select v-model="bindForm.role" placeholder="请选择岗位" style="width: 160px">
            <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
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
            icon="el-icon-plus"
            size="mini"
            :loading="bindSubmitting"
            :disabled="!authStaff || bindForm.shopIds.length === 0"
            @click="submitBind"
          >批量授权</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="authLoading" :data="staffShopList" border>
        <el-table-column label="门店ID" align="center" prop="shopId" width="90" />
        <el-table-column label="门店编码" align="center" prop="shopCode" width="130" :show-overflow-tooltip="true" />
        <el-table-column label="门店名称" align="center" prop="shopName" min-width="160" :show-overflow-tooltip="true" />
        <el-table-column label="地址" align="center" prop="address" min-width="220" :show-overflow-tooltip="true" />
        <el-table-column label="岗位" align="center" width="170">
          <template slot-scope="scope">
            <el-select v-model="scope.row.role" size="mini">
              <el-option v-for="role in roleOptions" :key="role.value" :label="role.label" :value="role.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="默认" align="center" width="90">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.isDefault === 1" type="success">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="220">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-check" @click="saveBinding(scope.row)">保存岗位</el-button>
            <el-button
              v-if="scope.row.isDefault !== 1"
              size="mini"
              type="text"
              icon="el-icon-star-off"
              @click="setDefaultBinding(scope.row)"
            >设默认</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="removeBinding(scope.row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listStaff, getStaff, addStaff, updateStaff, delStaff, listStaffShops } from '@/api/admin/staff'
import { listShop, addShopStaff, updateShopStaff, delShopStaff } from '@/api/admin/shop'

export default {
  name: 'ShopStaff',
  data() {
    const validatePassword = (rule, value, callback) => {
      if (!this.form.id && !value) {
        callback(new Error('登录密码不能为空'))
        return
      }
      if (value && (value.length < 5 || value.length > 20)) {
        callback(new Error('密码长度应为5到20个字符'))
        return
      }
      callback()
    }
    return {
      loading: true,
      authLoading: false,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      staffList: [],
      title: '',
      open: false,
      authOpen: false,
      authTitle: '',
      authStaff: null,
      staffShopList: [],
      shopOptions: [],
      bindSubmitting: false,
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
        role: 'STAFF',
        defaultShopId: undefined
      },
      roleOptions: [
        { label: '店员', value: 'STAFF' },
        { label: '店长', value: 'MANAGER' },
        { label: '督导', value: 'SUPERVISOR' },
        { label: '区域经理', value: 'AREA_MANAGER' },
        { label: '管理员', value: 'ADMIN' }
      ],
      rules: {
        account: [
          { required: true, message: '员工账号不能为空', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9_]{2,30}$/, message: '账号只能包含字母、数字和下划线，长度2到30位', trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '员工姓名不能为空', trigger: 'blur' },
          { max: 50, message: '员工姓名不能超过50个字符', trigger: 'blur' }
        ],
        phone: [
          { required: true, message: '手机号不能为空', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        password: [
          { validator: validatePassword, trigger: 'blur' }
        ],
        status: [
          { required: true, message: '员工状态不能为空', trigger: 'change' }
        ]
      },
      bindRules: {
        shopIds: [{ type: 'array', required: true, min: 1, message: '请选择门店', trigger: 'change' }],
        role: [{ required: true, message: '请选择岗位', trigger: 'change' }]
      }
    }
  },
  computed: {
    boundShopIds() {
      return this.staffShopList.map(item => item.shopId)
    },
    availableShopOptions() {
      return this.shopOptions.filter(shop => this.boundShopIds.indexOf(shop.id) === -1)
    },
    selectedShopOptions() {
      return this.availableShopOptions.filter(shop => this.bindForm.shopIds.indexOf(shop.id) !== -1)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listStaff(this.queryParams).then(response => {
        this.staffList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: undefined,
        account: undefined,
        nickname: undefined,
        phone: undefined,
        password: undefined,
        status: 0
      }
      this.resetForm('form')
    },
    resetBind() {
      this.bindForm = {
        shopIds: [],
        role: 'STAFF',
        defaultShopId: undefined
      }
      this.resetForm('bindForm')
    },
    handleBindShopChange(shopIds) {
      if (this.bindForm.defaultShopId && shopIds.indexOf(this.bindForm.defaultShopId) === -1) {
        this.bindForm.defaultShopId = undefined
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增门店员工'
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids[0]
      getStaff(id).then(response => {
        this.form = {
          id: response.data.id,
          account: response.data.account,
          nickname: response.data.nickname,
          phone: response.data.phone,
          password: undefined,
          status: response.data.status
        }
        this.open = true
        this.title = '修改门店员工'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const data = {
          nickname: this.form.nickname,
          phone: this.form.phone,
          status: this.form.status
        }
        if (this.form.password) {
          data.password = this.form.password
        }
        if (this.form.id) {
          updateStaff(this.form.id, data).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
          return
        }
        data.account = this.form.account
        data.password = this.form.password
        addStaff(data).then(() => {
          this.$modal.msgSuccess('新增成功，请继续为员工配置门店授权')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id ? [row.id] : this.ids
      this.$modal.confirm('确认删除选中的门店员工账号吗？已绑定门店的员工需要先解绑。').then(() => {
        return Promise.all(ids.map(id => delStaff(id)))
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleStatusChange(row) {
      const nextStatus = row.status
      const text = nextStatus === 0 ? '启用' : '停用'
      this.$modal.confirm('确认要"' + text + '"员工"' + row.nickname + '"吗？').then(() => {
        return updateStaff(row.id, {
          nickname: row.nickname,
          phone: row.phone,
          status: nextStatus
        })
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
      }).catch(() => {
        row.status = nextStatus === 0 ? 1 : 0
      })
    },
    handleAuth(row) {
      this.authStaff = row
      this.authTitle = '门店授权 - ' + row.nickname + '（' + row.account + '）'
      this.authOpen = true
      this.loadAuthData()
    },
    loadAuthData() {
      if (!this.authStaff) return
      this.authLoading = true
      Promise.all([
        listStaffShops(this.authStaff.id),
        listShop({ pageNum: 1, pageSize: 1000 })
      ]).then(([staffShopRes, shopRes]) => {
        this.staffShopList = staffShopRes.data || []
        this.shopOptions = shopRes.rows || []
        this.resetBind()
        this.authLoading = false
      }).catch(() => {
        this.authLoading = false
      })
    },
    submitBind() {
      this.$refs.bindForm.validate(async valid => {
        if (!valid || !this.authStaff) return
        const shopIds = this.bindForm.shopIds || []
        const defaultShopId = this.bindForm.defaultShopId || (this.staffShopList.length === 0 ? shopIds[0] : undefined)
        this.bindSubmitting = true
        try {
          for (const shopId of shopIds) {
            await addShopStaff(shopId, {
              staffId: this.authStaff.id,
              role: this.bindForm.role,
              isDefault: shopId === defaultShopId ? 1 : 0
            })
          }
          this.$modal.msgSuccess('已授权 ' + shopIds.length + ' 家门店')
          this.loadAuthData()
        } finally {
          this.bindSubmitting = false
        }
      })
    },
    saveBinding(row) {
      updateShopStaff(row.shopId, this.authStaff.id, {
        role: row.role,
        isDefault: row.isDefault
      }).then(() => {
        this.$modal.msgSuccess('岗位已保存')
        this.loadAuthData()
      })
    },
    setDefaultBinding(row) {
      updateShopStaff(row.shopId, this.authStaff.id, {
        role: row.role,
        isDefault: 1
      }).then(() => {
        this.$modal.msgSuccess('默认门店已更新')
        this.loadAuthData()
      })
    },
    removeBinding(row) {
      this.$modal.confirm('确认解除该员工与门店"' + row.shopName + '"的绑定吗？').then(() => {
        return delShopStaff(row.shopId, this.authStaff.id)
      }).then(() => {
        this.$modal.msgSuccess('解绑成功')
        this.loadAuthData()
      }).catch(() => {})
    }
  }
}
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
