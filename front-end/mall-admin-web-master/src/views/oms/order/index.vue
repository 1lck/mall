<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Tickets } from '@element-plus/icons-vue'
import { getOrderListAPI, orderDeleteByIdsAPI, orderUpdateCloseAPI } from '@/apis/order'
import type { OmsOrder, OrderQueryParam } from '@/types/order'

const listQuery = ref<OrderQueryParam>({
  pageNum: 1,
  pageSize: 10,
  orderSn: '',
})
const list = ref<OmsOrder[]>([])
const listLoading = ref(false)
const total = ref(0)

const getList = async () => {
  listLoading.value = true
  try {
    const response = await getOrderListAPI(listQuery.value)
    list.value = response.data.list
    total.value = response.data.total
  } finally {
    listLoading.value = false
  }
}

onMounted(() => {
  getList()
})

const formatStatus = (value: number) => {
  if (value === 3) {
    return '已完成'
  }
  if (value === 4) {
    return '已关闭'
  }
  return '待付款'
}

const handleResetSearch = () => {
  listQuery.value = {
    pageNum: 1,
    pageSize: 10,
    orderSn: '',
  }
  getList()
}

const handleSearchList = () => {
  listQuery.value.pageNum = 1
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

const handleCloseOrder = async (row: OmsOrder) => {
  await orderUpdateCloseAPI({ ids: String(row.id), note: '后台关闭订单' })
  ElMessage.success('订单已关闭')
  getList()
}

const handleDeleteOrder = async (row: OmsOrder) => {
  await ElMessageBox.confirm('是否要进行该删除操作?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await orderDeleteByIdsAPI({ ids: String(row.id) })
  ElMessage.success('删除成功')
  getList()
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
          <el-form-item label="订单编号：">
            <el-input v-model="listQuery.orderSn" class="input-width" placeholder="请输入订单编号" />
          </el-form-item>
          <el-form-item label="订单状态：">
            <el-select v-model="listQuery.status" class="input-width" placeholder="全部" clearable>
              <el-option label="待付款" :value="0" />
              <el-option label="已完成" :value="3" />
              <el-option label="已关闭" :value="4" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="operate-container" shadow="never">
      <el-icon class="el-icon-middle">
        <Tickets />
      </el-icon>
      <span>数据列表</span>
    </el-card>

    <div class="table-container">
      <el-table :data="list" style="width: 100%;" v-loading="listLoading" border>
        <el-table-column label="编号" width="100" align="center">
          <template #default="scope">{{ scope.row.id }}</template>
        </el-table-column>
        <el-table-column label="订单编号" width="220" align="center">
          <template #default="scope">{{ scope.row.orderSn }}</template>
        </el-table-column>
        <el-table-column label="用户编号" width="120" align="center">
          <template #default="scope">{{ scope.row.memberId }}</template>
        </el-table-column>
        <el-table-column label="订单金额" width="120" align="center">
          <template #default="scope">¥{{ scope.row.totalAmount }}</template>
        </el-table-column>
        <el-table-column label="订单状态" width="120" align="center">
          <template #default="scope">{{ formatStatus(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="200" align="center">
          <template #default="scope">{{ scope.row.createTime }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="220" align="center">
          <template #default="scope">{{ scope.row.note || '暂无备注' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button v-if="scope.row.status === 0" size="small" type="text" @click="handleCloseOrder(scope.row)">
              关闭订单
            </el-button>
            <el-button v-if="scope.row.status === 4" size="small" type="text" @click="handleDeleteOrder(scope.row)">
              删除订单
            </el-button>
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
