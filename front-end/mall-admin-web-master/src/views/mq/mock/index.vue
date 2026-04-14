<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Tickets } from '@element-plus/icons-vue'
import { createOutboxDemoBatchAPI, createSingleOutboxDebugEventAPI } from '@/apis/outbox'
import type { OutboxDebugEventType } from '@/types/outbox'

const creating = ref(false)
const customAggregateId = ref('')

const handleCreateDemoBatch = async () => {
  creating.value = true
  try {
    await createOutboxDemoBatchAPI()
    ElMessage.success('已生成一组消息调试数据，可以去消息观察页查看状态变化')
  } finally {
    creating.value = false
  }
}

const handleCreateSingleEvent = async (type: OutboxDebugEventType, label: string) => {
  creating.value = true
  try {
    await createSingleOutboxDebugEventAPI({
      type,
      aggregateId: customAggregateId.value.trim() || undefined,
    })
    ElMessage.success(`已生成单条${label}调试消息`)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>Mock 数据操作</span>
        </div>
      </template>

      <el-alert
        title="这里专门放消息调试用的模拟数据操作，避免把这些按钮混在订单页面或观察页面里。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="filter-row">
        <span class="filter-label">自定义聚合标识</span>
        <el-input
          v-model="customAggregateId"
          placeholder="可选，比如 ORD-DEBUG-001；不填则自动生成"
          clearable
          class="aggregate-input"
        />
      </div>

      <div class="action-panel">
        <el-card shadow="hover" class="action-card">
          <div class="action-title">
            <el-icon><Tickets /></el-icon>
            <span>生成一组演示数据</span>
          </div>
          <div class="action-desc">
            会一次生成 SENT、FAILED、DEAD，以及一条稳定失败的调试消息，方便你观察重试和错误回写。
          </div>
          <el-button type="primary" :loading="creating" @click="handleCreateDemoBatch()">
            立即生成
          </el-button>
        </el-card>

        <el-card shadow="hover" class="action-card">
          <div class="action-title">
            <el-icon><Tickets /></el-icon>
            <span>单条快捷生成</span>
          </div>
          <div class="action-desc">
            适合你按一种状态一条条造数据。上面的聚合标识如果填写了，就会用你指定的值，方便打断点和反复观察。
          </div>
          <div class="button-group">
            <el-button :loading="creating" @click="handleCreateSingleEvent('SENT', '已发送')">
              生成 SENT
            </el-button>
            <el-button :loading="creating" @click="handleCreateSingleEvent('FAILED', '待重试')">
              生成 FAILED
            </el-button>
            <el-button :loading="creating" @click="handleCreateSingleEvent('DEAD', '死信')">
              生成 DEAD
            </el-button>
            <el-button type="danger" :loading="creating" @click="handleCreateSingleEvent('IMMEDIATE_FAIL', '即时失败')">
              生成 IMMEDIATE_FAIL
            </el-button>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.card-header {
  font-size: 16px;
  font-weight: 600;
}

.action-panel {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.action-card {
  max-width: 520px;
  width: 100%;
}

.action-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.action-desc {
  margin: 12px 0 18px;
  color: #606266;
  line-height: 1.7;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}

.filter-label {
  flex-shrink: 0;
  color: #606266;
}

.aggregate-input {
  max-width: 420px;
}

.button-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
