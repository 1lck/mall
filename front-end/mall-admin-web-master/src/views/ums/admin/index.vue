<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dayjs, ElMessage, ElMessageBox } from 'element-plus'
import { Search, Tickets } from '@element-plus/icons-vue'
import { adminRegisterAPI, adminUpdateStatusByIdAPI, getAdminListAPI } from '@/apis/admin.ts'
import type { UmsAdmin } from '@/types/admin'
import type { PageParam } from '@/types/common'

const listQuery = ref<PageParam>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})
const list = ref<UmsAdmin[]>([])
const listLoading = ref(false)
const total = ref(0)

const getList = async () => {
  listLoading.value = true
  try {
    const res = await getAdminListAPI(listQuery.value)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    listLoading.value = false
  }
}

onMounted(() => {
  getList()
})

const admin = ref<UmsAdmin>({
  username: '',
  password: '',
  nickName: '',
  role: 'USER',
  status: 1,
})
const dialogVisible = ref(false)

const handleResetSearch = () => {
  listQuery.value.pageNum = 1
  listQuery.value.keyword = ''
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

const handleAdd = () => {
  dialogVisible.value = true
  admin.value = {
    username: '',
    password: '',
    nickName: '',
    role: 'USER',
    status: 1,
  }
}

const handleStatusChange = async (row: UmsAdmin) => {
  try {
    await ElMessageBox.confirm('是否要修改该状态?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminUpdateStatusByIdAPI(row.id!, { status: row.status })
    ElMessage.success('修改成功')
  } catch (error) {
    getList()
  }
}

const handleDialogConfirm = async () => {
  await ElMessageBox.confirm('是否要确认创建该用户？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await adminRegisterAPI(admin.value)
  ElMessage.success('添加成功')
  dialogVisible.value = false
  getList()
}

const formatDateTime = (time?: string) => {
  if (!time) {
    return 'N/A'
  }
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
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
        <el-button style="float: right" @click="handleSearchList()" type="primary">
          查询搜索
        </el-button>
        <el-button style="float: right; margin-right: 15px" @click="handleResetSearch()">
          重置
        </el-button>
      </div>
      <div style="margin-top: 15px">
        <el-form :inline="true" :model="listQuery" label-width="120px">
          <el-form-item label="搜索内容：">
            <el-input v-model="listQuery.keyword" class="input-width" placeholder="帐号或昵称" clearable />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="operate-container" shadow="never">
      <el-icon class="el-icon-middle">
        <Tickets />
      </el-icon>
      <span>数据列表</span>
      <el-button class="btn-add" @click="handleAdd()">添加</el-button>
    </el-card>

    <div class="table-container">
      <el-table :data="list" style="width: 100%;" v-loading="listLoading" border>
        <el-table-column label="编号" width="100" align="center">
          <template #default="scope">{{ scope.row.id }}</template>
        </el-table-column>
        <el-table-column label="帐号" align="center">
          <template #default="scope">{{ scope.row.username }}</template>
        </el-table-column>
        <el-table-column label="昵称" align="center">
          <template #default="scope">{{ scope.row.nickName }}</template>
        </el-table-column>
        <el-table-column label="角色" align="center">
          <template #default="scope">{{ scope.row.role }}</template>
        </el-table-column>
        <el-table-column label="添加时间" width="180" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="是否启用" width="140" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(scope.row)"
            />
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

    <el-dialog v-model="dialogVisible" title="添加用户" width="40%">
      <el-form :model="admin" label-width="120px">
        <el-form-item label="帐号">
          <el-input v-model="admin.username" style="width: 260px" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="admin.nickName" style="width: 260px" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="admin.password" type="password" style="width: 260px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="admin.role" style="width: 260px">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-radio-group v-model="admin.status">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleDialogConfirm()">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped></style>
