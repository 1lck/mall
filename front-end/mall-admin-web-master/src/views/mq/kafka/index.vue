<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { dayjs, ElMessage } from 'element-plus'
import { Connection, RefreshRight, Tickets, WarningFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getKafkaConsoleOverviewAPI, getKafkaTopicMessagesAPI } from '@/apis/kafka'
import { createOutboxDemoBatchAPI, sendPaymentSucceededDebugMessageAPI } from '@/apis/outbox'
import type { KafkaConsoleOverview, KafkaMessageView, KafkaPartitionStatus, KafkaTopicStatus, KafkaTopicView } from '@/types/kafka'

const router = useRouter()

const createEmptyOverview = (): KafkaConsoleOverview => ({
  enabled: false,
  bootstrapServers: '',
  consumerGroup: '',
  summary: {
    topicCount: 0,
    existingTopicCount: 0,
    laggingTopicCount: 0,
    totalPartitionCount: 0,
    totalLag: 0,
  },
  topics: [],
})

const overview = ref<KafkaConsoleOverview>(createEmptyOverview())
const selectedTopicName = ref('')
const selectedPartition = ref<number | undefined>()
const messageLimit = ref(20)
const overviewLoading = ref(false)
const messagesLoading = ref(false)
const actionLoading = ref(false)
const autoRefresh = ref(true)
const directSendOrderNo = ref('ORD-CONSUMER-FAIL-001')
const directSendAmount = ref(99.9)
const messages = ref<KafkaMessageView[]>([])
const messageDrawerVisible = ref(false)
const activeMessage = ref<KafkaMessageView | null>(null)

let refreshTimer: number | undefined

const selectedTopic = computed<KafkaTopicView | undefined>(() =>
  overview.value.topics.find(item => item.topicName === selectedTopicName.value),
)

const partitionOptions = computed(() => selectedTopic.value?.partitions ?? [])

const summaryItems = computed(() => [
  {
    label: '观察中的 Topic',
    value: overview.value.summary.topicCount,
    emphasis: 'neutral',
  },
  {
    label: '已创建 Topic',
    value: overview.value.summary.existingTopicCount,
    emphasis: 'success',
  },
  {
    label: '异常 Topic',
    value: overview.value.summary.laggingTopicCount,
    emphasis: overview.value.summary.laggingTopicCount > 0 ? 'danger' : 'neutral',
  },
  {
    label: '总 Lag',
    value: overview.value.summary.totalLag,
    emphasis: overview.value.summary.totalLag > 0 ? 'warning' : 'success',
  },
])

const fetchOverview = async () => {
  overviewLoading.value = true
  try {
    const res = await getKafkaConsoleOverviewAPI()
    overview.value = res.data
    if (!selectedTopicName.value || !overview.value.topics.some(item => item.topicName === selectedTopicName.value)) {
      const preferredTopic = overview.value.topics.find(item => item.exists) ?? overview.value.topics[0]
      selectedTopicName.value = preferredTopic?.topicName ?? ''
    }
    if (selectedPartition.value != null && !partitionOptions.value.some(item => item.partition === selectedPartition.value)) {
      selectedPartition.value = undefined
    }
  } finally {
    overviewLoading.value = false
  }
}

const fetchMessages = async () => {
  if (!selectedTopicName.value) {
    messages.value = []
    return
  }

  messagesLoading.value = true
  try {
    const res = await getKafkaTopicMessagesAPI({
      topic: selectedTopicName.value,
      partition: selectedPartition.value,
      limit: messageLimit.value,
    })
    messages.value = res.data.messages
  } finally {
    messagesLoading.value = false
  }
}

const handleRefresh = async () => {
  await fetchOverview()
  await fetchMessages()
}

const handleSelectTopic = async (topicName: string) => {
  if (selectedTopicName.value === topicName) {
    return
  }
  selectedTopicName.value = topicName
}

const handleOpenMessage = (row: KafkaMessageView) => {
  activeMessage.value = row
  messageDrawerVisible.value = true
}

const handleCreateDemoBatch = async () => {
  actionLoading.value = true
  try {
    await createOutboxDemoBatchAPI()
    ElMessage.success('已生成一组调试消息')
    await handleRefresh()
  } finally {
    actionLoading.value = false
  }
}

const handleDirectSend = async () => {
  const orderNo = directSendOrderNo.value.trim()
  if (!orderNo) {
    ElMessage.warning('请先输入订单号')
    return
  }

  actionLoading.value = true
  try {
    await sendPaymentSucceededDebugMessageAPI({
      orderNo,
      amount: directSendAmount.value,
    })
    ElMessage.success(`已直接发送支付成功消息：${orderNo}`)
    await handleRefresh()
  } finally {
    actionLoading.value = false
  }
}

const formatTopicStatus = (status: KafkaTopicStatus) => {
  if (status === 'HEALTHY') {
    return '健康'
  }
  if (status === 'LAGGING') {
    return '积压'
  }
  return '未创建'
}

const topicStatusType = (status: KafkaTopicStatus) => {
  if (status === 'HEALTHY') {
    return 'success'
  }
  if (status === 'LAGGING') {
    return 'warning'
  }
  return 'info'
}

const formatPartitionStatus = (status: KafkaPartitionStatus) => {
  if (status === 'CAUGHT_UP') {
    return '已追平'
  }
  if (status === 'LAGGING') {
    return '追赶中'
  }
  return '未分配'
}

const partitionStatusType = (status: KafkaPartitionStatus) => {
  if (status === 'CAUGHT_UP') {
    return 'success'
  }
  if (status === 'LAGGING') {
    return 'warning'
  }
  return 'info'
}

const formatValueFormat = (format: KafkaMessageView['valueFormat']) => {
  if (format === 'JSON') {
    return 'JSON'
  }
  if (format === 'EMPTY') {
    return '空值'
  }
  return '文本'
}

const valueFormatType = (format: KafkaMessageView['valueFormat']) => {
  if (format === 'JSON') {
    return 'success'
  }
  if (format === 'EMPTY') {
    return 'info'
  }
  return 'warning'
}

const formatDateTime = (timestamp: string) => dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')

const previewValue = (value: string | null) => {
  if (!value) {
    return '空值'
  }
  const normalized = value.replace(/\s+/g, ' ').trim()
  return normalized.length > 88 ? `${normalized.slice(0, 88)}...` : normalized
}

const resolveProgress = (topic: KafkaTopicView, partition: KafkaTopicView['partitions'][number]) => {
  if (!topic.exists || partition.endOffset <= 0 || partition.committedOffset == null) {
    return 0
  }
  return Math.min(Math.round((partition.committedOffset / partition.endOffset) * 100), 100)
}

const setupAutoRefresh = () => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = undefined
  }
  if (!autoRefresh.value) {
    return
  }
  refreshTimer = window.setInterval(() => {
    handleRefresh()
  }, 8000)
}

watch(selectedTopicName, async () => {
  selectedPartition.value = undefined
  await fetchMessages()
})

watch([selectedPartition, messageLimit], async () => {
  await fetchMessages()
})

watch(autoRefresh, () => {
  setupAutoRefresh()
})

onMounted(async () => {
  await handleRefresh()
  setupAutoRefresh()
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<template>
  <div class="app-container kafka-console-page">
    <section class="console-hero" v-loading="overviewLoading">
      <div class="hero-copy">
        <div class="hero-kicker">Kafka Control Room</div>
        <h1>更容易读懂的 Kafka 观察页</h1>
        <p>
          把 topic 健康度、分区 lag、最近消息和调试动作收在一个工作面里，
          先看到风险，再决定要不要发消息、切分区或回到 outbox 继续排查。
        </p>
        <div class="hero-meta">
          <span>
            <el-icon><Connection /></el-icon>
            {{ overview.bootstrapServers || '未配置 bootstrap servers' }}
          </span>
          <span>
            <el-icon><Tickets /></el-icon>
            {{ overview.consumerGroup || '未配置 consumer group' }}
          </span>
        </div>
      </div>

      <div class="hero-actions">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="手动刷新" />
        <el-button type="primary" @click="handleRefresh()">
          <el-icon><RefreshRight /></el-icon>
          刷新状态
        </el-button>
        <el-button @click="router.push('/mq/outbox')">打开 Outbox 观察</el-button>
        <el-button @click="router.push('/mq/mock')">打开 Mock 数据</el-button>
      </div>
    </section>

    <section class="summary-strip">
      <article
        v-for="item in summaryItems"
        :key="item.label"
        class="summary-item"
        :class="`is-${item.emphasis}`"
      >
        <div class="summary-label">{{ item.label }}</div>
        <div class="summary-value">{{ item.value }}</div>
      </article>
    </section>

    <section class="console-workspace">
      <aside class="topic-rail">
        <div class="rail-header">
          <div>
            <div class="rail-title">Topic 观察</div>
            <div class="rail-subtitle">先看哪条链路正在积压或未创建</div>
          </div>
        </div>

        <div class="topic-list">
          <button
            v-for="topic in overview.topics"
            :key="topic.topicName"
            type="button"
            class="topic-item"
            :class="{ active: topic.topicName === selectedTopicName }"
            @click="handleSelectTopic(topic.topicName)"
          >
            <div class="topic-item-header">
              <div class="topic-name">{{ topic.topicName }}</div>
              <el-tag :type="topicStatusType(topic.status)" effect="dark" round>
                {{ formatTopicStatus(topic.status) }}
              </el-tag>
            </div>
            <div class="topic-stats">
              <span>配置分区 {{ topic.configuredPartitions ?? '--' }}</span>
              <span>实际分区 {{ topic.actualPartitions }}</span>
            </div>
            <div class="topic-stats">
              <span>总 lag {{ topic.totalLag }}</span>
              <span>{{ topic.exists ? 'Kafka 已创建' : '等待创建' }}</span>
            </div>
          </button>
        </div>
      </aside>

      <main class="topic-main" v-if="selectedTopic">
        <section class="selected-topic-shell">
          <div class="selected-topic-header">
            <div>
              <div class="selected-kicker">Selected Topic</div>
              <h2>{{ selectedTopic.topicName }}</h2>
              <p>
                {{ selectedTopic.exists ? '下面按分区展示当前消费进度和最近消息。' : '这个 topic 目前还没在 Kafka 中创建，先确认发送链路是否已经启动。' }}
              </p>
            </div>
            <div class="selected-flags">
              <el-tag :type="topicStatusType(selectedTopic.status)" effect="light">
                {{ formatTopicStatus(selectedTopic.status) }}
              </el-tag>
              <el-tag effect="light">配置分区 {{ selectedTopic.configuredPartitions ?? '--' }}</el-tag>
              <el-tag effect="light">实际分区 {{ selectedTopic.actualPartitions }}</el-tag>
              <el-tag effect="light">总 lag {{ selectedTopic.totalLag }}</el-tag>
            </div>
          </div>

          <div class="partition-grid">
            <article
              v-for="partition in selectedTopic.partitions"
              :key="partition.partition"
              class="partition-card"
            >
              <div class="partition-topline">
                <div class="partition-title">Partition {{ partition.partition }}</div>
                <el-tag :type="partitionStatusType(partition.status)" effect="plain" round>
                  {{ formatPartitionStatus(partition.status) }}
                </el-tag>
              </div>
              <el-progress
                :percentage="resolveProgress(selectedTopic, partition)"
                :stroke-width="10"
                :show-text="false"
                :status="partition.status === 'CAUGHT_UP' ? 'success' : undefined"
              />
              <div class="partition-metrics">
                <span>Committed {{ partition.committedOffset ?? '--' }}</span>
                <span>End {{ partition.endOffset }}</span>
                <span class="lag-value">Lag {{ partition.lag }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="detail-grid">
          <div class="messages-panel">
            <div class="panel-header">
              <div>
                <div class="panel-title">最近消息</div>
                <div class="panel-subtitle">按最新时间倒序读取，可按分区聚焦异常消息</div>
              </div>
              <div class="toolbar-inline">
                <el-select v-model="selectedPartition" clearable placeholder="全部分区" style="width: 120px">
                  <el-option
                    v-for="item in partitionOptions"
                    :key="item.partition"
                    :label="`Partition ${item.partition}`"
                    :value="item.partition"
                  />
                </el-select>
                <el-select v-model="messageLimit" style="width: 120px">
                  <el-option :value="10" label="最近 10 条" />
                  <el-option :value="20" label="最近 20 条" />
                  <el-option :value="50" label="最近 50 条" />
                </el-select>
                <el-button @click="fetchMessages()">
                  <el-icon><RefreshRight /></el-icon>
                  刷新消息
                </el-button>
              </div>
            </div>

            <el-table
              :data="messages"
              border
              stripe
              v-loading="messagesLoading"
              class="message-table"
              @row-click="handleOpenMessage"
            >
              <el-table-column label="时间" width="180">
                <template #default="{ row }">{{ formatDateTime(row.timestamp) }}</template>
              </el-table-column>
              <el-table-column label="分区" width="90" prop="partition" />
              <el-table-column label="Offset" width="110" prop="offset" />
              <el-table-column label="Key" min-width="180">
                <template #default="{ row }">{{ row.key || '空 key' }}</template>
              </el-table-column>
              <el-table-column label="格式" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="valueFormatType(row.valueFormat)" effect="light">
                    {{ formatValueFormat(row.valueFormat) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Value 预览" min-width="360">
                <template #default="{ row }">{{ previewValue(row.value) }}</template>
              </el-table-column>
            </el-table>
          </div>

          <aside class="actions-panel">
            <div class="panel-header compact">
              <div>
                <div class="panel-title">快捷操作</div>
                <div class="panel-subtitle">把最常用的调试动作收进同一页</div>
              </div>
            </div>

            <div class="action-card action-card-primary">
              <div class="action-card-title">生成一组演示消息</div>
              <p>一次性造出 SENT、FAILED、DEAD 等场景，方便你马上观察 lag、消息预览和 outbox 状态。</p>
              <el-button type="primary" :loading="actionLoading" @click="handleCreateDemoBatch()">
                立即生成
              </el-button>
            </div>

            <div class="action-card">
              <div class="action-card-title">直接发送支付成功消息</div>
              <p>不经过 outbox，适合你直接练消费者、重投和异常消息观察。</p>
              <div class="action-form">
                <el-input v-model="directSendOrderNo" placeholder="例如 ORD-CONSUMER-FAIL-001" clearable />
                <el-input-number
                  v-model="directSendAmount"
                  :min="0.01"
                  :precision="2"
                  :step="1"
                  controls-position="right"
                />
                <el-button type="success" :loading="actionLoading" @click="handleDirectSend()">
                  发送到 Kafka
                </el-button>
              </div>
            </div>

            <div class="notice-card">
              <div class="notice-title">
                <el-icon><WarningFilled /></el-icon>
                <span>使用提醒</span>
              </div>
              <ul>
                <li>Topic 有多个分区后，只有消费者并发也开起来，才会真正并行消费。</li>
                <li>Value 格式显示为“文本”时，通常值得优先检查是否是脏消息或非对象 JSON。</li>
                <li>如果这里看到 lag 长期不降，再去 Outbox 观察页看失败原因和重试状态。</li>
              </ul>
            </div>
          </aside>
        </section>
      </main>

      <main class="topic-main empty-state" v-else>
        <el-empty description="当前没有可观察的 Kafka topic" />
      </main>
    </section>

    <el-drawer
      v-model="messageDrawerVisible"
      title="消息详情"
      direction="rtl"
      size="42%"
    >
      <template v-if="activeMessage">
        <div class="drawer-meta">
          <el-tag effect="light">{{ activeMessage.topic }}</el-tag>
          <el-tag effect="light">Partition {{ activeMessage.partition }}</el-tag>
          <el-tag effect="light">Offset {{ activeMessage.offset }}</el-tag>
          <el-tag :type="valueFormatType(activeMessage.valueFormat)" effect="light">
            {{ formatValueFormat(activeMessage.valueFormat) }}
          </el-tag>
        </div>

        <div class="drawer-block">
          <div class="drawer-label">时间</div>
          <div class="drawer-content">{{ formatDateTime(activeMessage.timestamp) }}</div>
        </div>

        <div class="drawer-block">
          <div class="drawer-label">Key</div>
          <pre class="drawer-code">{{ activeMessage.key || '空 key' }}</pre>
        </div>

        <div class="drawer-block">
          <div class="drawer-label">Value</div>
          <pre class="drawer-code">{{ activeMessage.value || '空值' }}</pre>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.kafka-console-page {
  background:
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.16), transparent 32%),
    radial-gradient(circle at top right, rgba(103, 194, 58, 0.12), transparent 28%),
    linear-gradient(180deg, #f6f9ff 0%, #f4f6fb 100%);
  min-height: calc(100vh - 84px);
}

.console-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(9, 20, 44, 0.96), rgba(27, 58, 112, 0.92)),
    linear-gradient(180deg, #1f2937, #111827);
  color: #f8fbff;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.18);
}

.hero-kicker,
.selected-kicker {
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.66);
}

.console-hero h1,
.selected-topic-header h2 {
  margin: 10px 0 12px;
  font-size: 30px;
  line-height: 1.15;
}

.console-hero p,
.selected-topic-header p {
  margin: 0;
  max-width: 720px;
  line-height: 1.8;
  color: rgba(239, 246, 255, 0.82);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 18px;
  color: rgba(239, 246, 255, 0.88);
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  min-width: 220px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.summary-item {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.16);
  backdrop-filter: blur(8px);
}

.summary-item.is-success {
  background: linear-gradient(180deg, rgba(240, 253, 244, 0.95), rgba(255, 255, 255, 0.9));
}

.summary-item.is-warning {
  background: linear-gradient(180deg, rgba(255, 247, 237, 0.95), rgba(255, 255, 255, 0.9));
}

.summary-item.is-danger {
  background: linear-gradient(180deg, rgba(254, 242, 242, 0.95), rgba(255, 255, 255, 0.9));
}

.summary-label {
  font-size: 13px;
  color: #64748b;
}

.summary-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.console-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
  align-items: start;
}

.topic-rail,
.selected-topic-shell,
.messages-panel,
.actions-panel {
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 16px 40px rgba(148, 163, 184, 0.12);
}

.topic-rail {
  padding: 18px;
}

.rail-title,
.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.rail-subtitle,
.panel-subtitle {
  margin-top: 4px;
  color: #64748b;
  line-height: 1.6;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.topic-item {
  width: 100%;
  text-align: left;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.topic-item:hover,
.topic-item.active {
  transform: translateY(-2px);
  border-color: rgba(64, 158, 255, 0.35);
  box-shadow: 0 12px 24px rgba(64, 158, 255, 0.12);
}

.topic-item-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.topic-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.5;
  word-break: break-all;
}

.topic-stats {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.topic-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.selected-topic-shell {
  padding: 22px;
}

.selected-topic-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.selected-flags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  align-content: flex-start;
}

.partition-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.partition-card {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff, #ffffff);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.partition-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.partition-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.partition-metrics {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  font-size: 13px;
  color: #475569;
}

.lag-value {
  font-weight: 700;
  color: #f59e0b;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.messages-panel,
.actions-panel {
  padding: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.toolbar-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.message-table {
  border-radius: 16px;
  overflow: hidden;
}

.actions-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.action-card,
.notice-card {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.action-card-primary {
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.96), rgba(255, 255, 255, 0.94));
}

.action-card-title,
.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.action-card p,
.notice-card ul {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.75;
}

.action-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-top: 14px;
}

.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-card ul {
  padding-left: 18px;
}

.notice-card li + li {
  margin-top: 8px;
}

.drawer-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.drawer-block + .drawer-block {
  margin-top: 18px;
}

.drawer-label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.drawer-content {
  color: #0f172a;
}

.drawer-code {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  background: #0f172a;
  color: #e2e8f0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.empty-state {
  padding: 40px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
}

@media (max-width: 1280px) {
  .partition-grid,
  .detail-grid,
  .summary-strip,
  .console-workspace {
    grid-template-columns: 1fr;
  }

  .hero-actions,
  .selected-flags {
    align-items: flex-start;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .console-hero,
  .selected-topic-header,
  .panel-header {
    flex-direction: column;
  }

  .kafka-console-page {
    padding-bottom: 30px;
  }
}
</style>
