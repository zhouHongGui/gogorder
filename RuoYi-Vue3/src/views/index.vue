<template>
  <div class="app-container home">
    <el-card class="welcome-card">
      <h1>欢迎使用 gogorder</h1>
      <p>面向门店运营的点单管理系统。</p>
    </el-card>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="12" :lg="6" v-for="item in features" :key="item.title">
        <el-card class="feature-card" shadow="hover">
          <svg-icon :icon-class="item.icon" class="feature-icon" />
          <h3>{{ item.title }}</h3>
          <p>{{ item.description }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
const features = [
  { title: '商品管理', description: '维护菜品、分类、规格与上下架状态', icon: 'shopping' },
  { title: '订单管理', description: '处理订单、退款与订单状态流转', icon: 'list' },
  { title: '门店管理', description: '配置门店信息、营业状态与人员权限', icon: 'system' },
  { title: '经营分析', description: '查看销售数据与经营趋势', icon: 'chart' }
]
</script>

<style scoped lang="scss">
.home {
  .welcome-card {
    margin-bottom: 20px;
    background: linear-gradient(135deg, #2563eb, #0f766e);
    color: #fff;

    h1 {
      margin: 0 0 12px;
      font-size: 30px;
    }

    p {
      margin: 0;
      opacity: 0.9;
    }
  }

  .feature-card {
    min-height: 180px;
    margin-bottom: 20px;

    .feature-icon {
      width: 36px;
      height: 36px;
      color: #2563eb;
    }

    h3 {
      margin: 18px 0 10px;
    }

    p {
      color: #64748b;
      line-height: 1.6;
    }
  }
}
</style>
