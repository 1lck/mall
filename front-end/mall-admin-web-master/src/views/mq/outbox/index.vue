<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { dayjs, ElMessage, ElMessageBox } from 'element-plus'
import { Search, Tickets } from '@element-plus/icons-vue'
import { cleanupOutboxDebugEventsAPI, getOutboxEventListAPI, retryOutboxEventAPI } from '@/apis/outbox'
import type { OutboxEvent, OutboxEventStatus, OutboxQueryParam } from '@/types/outbox'

const listQuery = ref<OutboxQueryParam>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})
const allList = ref<OutboxEvent[]>([])
const list = ref<OutboxEvent[]>([])
const listLoading = ref(false)
const cleaning = ref(false)
const total = ref(0)

const statusOptions: { label: string; value: OutboxEventStatus }[] = [
  { label: '待发送', value: 'PENDING' },
  { label: '已发送', value: 'SENT' },
  { label: '待重试', value: 'FAILED' },
  { label: '死信', value: 'DEAD' },
]

const summary = computed(() => {
  const result: Record<OutboxEventStatus, number> = {
    PENDING: 0,
    SENT: 0,
    FAILED: 0,
    DEAD: 0,
  }

  for (const item of allList.value) {
    result[item.status] += 1
  }

  return result
})

const getList = async () => {
  listLoading.value = true
  try {
    const res = await getOutboxEventListAPI(listQuery.value)
    allList.value = res.rawList
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    listLoading.value = false
  }
}

onMounted(() => {
  getList()
})

const handleResetSearch = () => {
  listQuery.value = {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
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

const handleRetryEvent = async (row: OutboxEvent) => {
  await ElMessageBox.confirm('是否要手动重发这条 outbox 消息？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await retryOutboxEventAPI(row.id)
  ElMessage.success('已发起手动重发请求')
  getList()
}

const handleCleanupDebugEvents = async () => {
  await ElMessageBox.confirm('是否要清空这套调试功能生成的旧 outbox 数据？不会删除真实业务消息。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  cleaning.value = true
  try {
    const res = await cleanupOutboxDebugEventsAPI()
    ElMessage.success(`已清理 ${res.data} 条调试数据`)
    listQuery.value.pageNum = 1
    getList()
  } finally {
    cleaning.value = false
  }
}

const formatDateTime = (time?: string) => {
  if (!time) {
    return '暂无'
  }
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const formatStatusLabel = (status: OutboxEventStatus) => {
  if (status === 'PENDING') {
    return '待发送'
  }
  if (status === 'SENT') {
    return '已发送'
  }
  if (status === 'FAILED') {
    return '待重试'
  }
  return '死信'
}

const statusTagType = (status: OutboxEventStatus) => {
  if (status === 'SENT') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'warning'
  }
  if (status === 'DEAD') {
    return 'danger'
  }
  return 'info'
}

const canRetry = (status: OutboxEventStatus) => {
  return status === 'FAILED' || status === 'DEAD'
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
          <el-form-item label="投递状态：">
            <el-select v-model="listQuery.status" class="input-width" placeholder="全部" clearable>
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关键字：">
            <el-input
              v-model="listQuery.keyword"
              class="input-width"
              placeholder="eventId / 订单号 / topic"
              clearable
            />
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="summary-title">待发送</div>
          <div class="summary-value">{{ summary.PENDING }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="summary-title">已发送</div>
          <div class="summary-value">{{ summary.SENT }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="summary-title">待重试</div>
          <div class="summary-value">{{ summary.FAILED }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="summary-title">死信</div>
          <div class="summary-value dead">{{ summary.DEAD }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="operate-container" shadow="never">
      <el-icon class="el-icon-middle">
        <Tickets />
      </el-icon>
      <span>Outbox 数据列表</span>
      <el-button class="btn-add" :loading="cleaning" @click="handleCleanupDebugEvents()">清空调试数据</el-button>
    </el-card>

    <div class="table-container">
      <el-table :data="list" style="width: 100%" v-loading="listLoading" border>
        <el-table-column label="编号" width="90" align="center" prop="id" />
        <el-table-column label="消息ID" min-width="220" align="center" prop="eventId" />
        <el-table-column label="聚合标识" min-width="180" align="center" prop="aggregateId" />
        <el-table-column label="事件类型" width="180" align="center" prop="eventType" />
        <el-table-column label="Topic" min-width="180" align="center" prop="topic" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ formatStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重试次数" width="100" align="center" prop="retryCount" />
        <el-table-column label="下次重试时间" width="180" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.nextRetryAt) }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="180" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.sentAt) }}</template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="260" align="center">
          <template #default="scope">{{ scope.row.lastError || '暂无' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="scope">
            <el-button
              v-if="canRetry(scope.row.status)"
              type="text"
              size="small"
              @click="handleRetryEvent(scope.row)"
            >
              手动重发
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

<style scoped>
.summary-title {
  color: #909399;
  font-size: 14px;
}

.summary-value {
  margin-top: 10px;
  color: #303133;
  font-size: 28px;
  font-weight: 600;
}

.summary-value.dead {
  color: #f56c6c;
}
</style>
