<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Tickets } from '@element-plus/icons-vue'
import { getProductListAPI, productDeleteByIdAPI } from '@/apis/product'
import type { PmsProduct, ProductQueryParam } from '@/types/product'

const router = useRouter()

const listQuery = ref<ProductQueryParam>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryName: '',
})
const list = ref<PmsProduct[]>([])
const total = ref(0)
const listLoading = ref(false)

const getList = async () => {
  listLoading.value = true
  try {
    const response = await getProductListAPI(listQuery.value)
    list.value = response.data.list
    total.value = response.data.total
  } finally {
    listLoading.value = false
  }
}

onMounted(() => {
  getList()
})

const handleSearchList = () => {
  listQuery.value.pageNum = 1
  getList()
}

const handleResetSearch = () => {
  listQuery.value = {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    categoryName: '',
  }
  getList()
}

const handleAddProduct = () => {
  router.push({ path: '/pms/addProduct' })
}

const handleUpdateProduct = (row: PmsProduct) => {
  router.push({ path: '/pms/updateProduct', query: { id: row.id } })
}

const handleDelete = async (row: PmsProduct) => {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await productDeleteByIdAPI(row.id!)
  ElMessage.success('删除成功')
  getList()
}

const handleSizeChange = (val: number) => {
  listQuery.value.pageNum = 1
  listQuery.value.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  listQuery.value.pageNum = val
  getList()
}

const formatStatus = (status?: string) => {
  switch (status) {
    case 'ON_SALE':
      return '上架中'
    case 'OFF_SHELF':
      return '已下架'
    default:
      return '草稿'
  }
}
</script>

<template>
  <div class="app-container">
    <el-card class="filter-container" shadow="never">
      <div>
        <el-icon class="el-icon-middle">
          <Search />
        </el-icon>
        <span>筛选搜索</span>
        <el-button style="float:right" type="primary" @click="handleSearchList()">
          查询搜索
        </el-button>
        <el-button style="float:right;margin-right: 15px" @click="handleResetSearch()">
          重置
        </el-button>
      </div>
      <div style="margin-top: 20px">
        <el-form :inline="true" :model="listQuery" label-width="120px">
          <el-form-item label="搜索内容：">
            <el-input v-model="listQuery.keyword" class="input-width" placeholder="商品名称或货号" />
          </el-form-item>
          <el-form-item label="商品分类：">
            <el-input v-model="listQuery.categoryName" class="input-width" placeholder="分类名称" />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="operate-container" shadow="never">
      <el-icon class="el-icon-middle">
        <Tickets />
      </el-icon>
      <span>数据列表</span>
      <el-button class="btn-add" @click="handleAddProduct()">
        添加
      </el-button>
    </el-card>

    <div class="table-container">
      <el-table :data="list" style="width: 100%;" v-loading="listLoading" border>
        <el-table-column label="编号" width="100" align="center">
          <template #default="scope">{{ scope.row.id }}</template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="220" align="center" prop="name" />
        <el-table-column label="货号" width="180" align="center">
          <template #default="scope">{{ scope.row.productSn }}</template>
        </el-table-column>
        <el-table-column label="分类" width="180" align="center">
          <template #default="scope">{{ scope.row.productCategoryName }}</template>
        </el-table-column>
        <el-table-column label="价格" width="120" align="center">
          <template #default="scope">¥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template #default="scope">{{ scope.row.stock }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">{{ formatStatus(scope.row.backendStatus) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="180" align="center">
          <template #default="scope">{{ scope.row.updateTime || scope.row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="scope">
            <el-button size="small" type="text" @click="handleUpdateProduct(scope.row)">编辑</el-button>
            <el-button size="small" type="text" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-container">
      <el-pagination
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="listQuery.pageNum"
        :page-size="listQuery.pageSize"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
      />
    </div>
  </div>
</template>
