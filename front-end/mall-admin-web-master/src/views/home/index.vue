<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import img_home_order from '@/assets/images/home_order.png'
import img_home_today_amount from '@/assets/images/home_today_amount.png'
import img_home_yesterday_amount from '@/assets/images/home_yesterday_amount.png'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import VChart from 'vue-echarts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import { getAdminDashboardAPI } from '@/apis/dashboard'
import type { DashboardResponse } from '@/types/dashboard'

use([
  CanvasRenderer,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
])

const createEmptyDashboard = (): DashboardResponse => ({
  summaryCards: {
    todayOrderCount: 0,
    todaySalesAmount: 0,
    yesterdaySalesAmount: 0,
  },
  pendingTasks: [],
  productOverview: {
    offShelfCount: 0,
    onSaleCount: 0,
    lowStockCount: 0,
    totalCount: 0,
  },
  userOverview: {
    todayNewCount: 0,
    yesterdayNewCount: 0,
    monthNewCount: 0,
    totalCount: 0,
  },
  orderStatistics: {
    monthOrderCount: 0,
    weekOrderCount: 0,
    monthSalesAmount: 0,
    weekSalesAmount: 0,
  },
  orderTrend: [],
})

const dashboard = ref<DashboardResponse>(createEmptyDashboard())
const loading = ref(false)

const createDefaultDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  return [start, end] as Date[]
}

const datePickerRange = ref<Date[]>(createDefaultDateRange())

const shortcuts = [
  {
    text: '最近一周',
    value: () => createDefaultDateRange(),
  },
  {
    text: '最近一月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(end.getDate() - 29)
      return [start, end]
    },
  },
]

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatCurrency = (amount: number) => `￥${amount.toFixed(2)}`

const fetchDashboard = async () => {
  loading.value = true

  try {
    const [start, end] = datePickerRange.value
    const res = await getAdminDashboardAPI({
      startDate: start ? formatDate(start) : undefined,
      endDate: end ? formatDate(end) : undefined,
    })
    dashboard.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})

const pendingTasks = computed(() => dashboard.value.pendingTasks)

const productOverviewItems = computed(() => [
  { label: '已下架', value: dashboard.value.productOverview.offShelfCount },
  { label: '已上架', value: dashboard.value.productOverview.onSaleCount },
  { label: '库存紧张', value: dashboard.value.productOverview.lowStockCount },
  { label: '全部商品', value: dashboard.value.productOverview.totalCount },
])

const userOverviewItems = computed(() => [
  { label: '今日新增', value: dashboard.value.userOverview.todayNewCount },
  { label: '昨日新增', value: dashboard.value.userOverview.yesterdayNewCount },
  { label: '本月新增', value: dashboard.value.userOverview.monthNewCount },
  { label: '用户总数', value: dashboard.value.userOverview.totalCount },
])

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: {
      type: 'cross',
    },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dashboard.value.orderTrend.map(item => item.date),
  },
  yAxis: [
    {
      type: 'value',
      name: '订单数量',
      position: 'left',
    },
    {
      type: 'value',
      name: '订单金额',
      position: 'right',
    },
  ],
  series: [
    {
      name: '订单数量',
      type: 'line',
      areaStyle: {},
      data: dashboard.value.orderTrend.map(item => item.orderCount),
      smooth: true,
      itemStyle: {
        color: '#409EFF',
      },
    },
    {
      name: '订单金额',
      type: 'line',
      yAxisIndex: 1,
      areaStyle: {},
      data: dashboard.value.orderTrend.map(item => item.orderAmount),
      smooth: true,
      itemStyle: {
        color: '#67C23A',
      },
    },
  ],
}))
</script>

<template>
  <div class="app-container">
    <div class="summary-layout">
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="total-frame">
            <img :src="img_home_order" class="total-icon">
            <div class="total-title">今日订单总数</div>
            <div class="total-value">{{ dashboard.summaryCards.todayOrderCount }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="total-frame">
            <img :src="img_home_today_amount" class="total-icon">
            <div class="total-title">今日销售总额</div>
            <div class="total-value">{{ formatCurrency(dashboard.summaryCards.todaySalesAmount) }}</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="total-frame">
            <img :src="img_home_yesterday_amount" class="total-icon">
            <div class="total-title">昨日销售总额</div>
            <div class="total-value">{{ formatCurrency(dashboard.summaryCards.yesterdaySalesAmount) }}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="un-handle-layout">
      <div class="layout-title">待处理事务</div>
      <div class="un-handle-content">
        <el-row :gutter="20">
          <el-col
            v-for="task in pendingTasks"
            :key="task.label"
            :span="8"
          >
            <div class="un-handle-item">
              <span class="font-medium">{{ task.label }}</span>
              <span style="float: right" class="color-danger">({{ task.count }})</span>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <div class="overview-layout">
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="out-border">
            <div class="layout-title">商品总览</div>
            <div class="overview-content">
              <el-row>
                <el-col
                  v-for="item in productOverviewItems"
                  :key="item.label"
                  :span="6"
                  class="color-danger overview-item-value"
                >
                  {{ item.value }}
                </el-col>
              </el-row>
              <el-row class="font-medium">
                <el-col
                  v-for="item in productOverviewItems"
                  :key="item.label"
                  :span="6"
                  class="overview-item-title"
                >
                  {{ item.label }}
                </el-col>
              </el-row>
            </div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="out-border">
            <div class="layout-title">用户总览</div>
            <div class="overview-content">
              <el-row>
                <el-col
                  v-for="item in userOverviewItems"
                  :key="item.label"
                  :span="6"
                  class="color-danger overview-item-value"
                >
                  {{ item.value }}
                </el-col>
              </el-row>
              <el-row class="font-medium">
                <el-col
                  v-for="item in userOverviewItems"
                  :key="item.label"
                  :span="6"
                  class="overview-item-title"
                >
                  {{ item.label }}
                </el-col>
              </el-row>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <div class="statistics-layout">
      <div class="layout-title">订单统计</div>
      <el-row>
        <el-col :span="4">
          <div class="statistics-metrics">
            <div class="statistics-metric">
              <div class="statistics-label">本月订单总数</div>
              <div class="statistics-value">{{ dashboard.orderStatistics.monthOrderCount }}</div>
            </div>
            <div class="statistics-metric">
              <div class="statistics-label">本周订单总数</div>
              <div class="statistics-value">{{ dashboard.orderStatistics.weekOrderCount }}</div>
            </div>
            <div class="statistics-metric">
              <div class="statistics-label">本月销售总额</div>
              <div class="statistics-value">{{ formatCurrency(dashboard.orderStatistics.monthSalesAmount) }}</div>
            </div>
            <div class="statistics-metric">
              <div class="statistics-label">本周销售总额</div>
              <div class="statistics-value">{{ formatCurrency(dashboard.orderStatistics.weekSalesAmount) }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="20">
          <div class="chart-layout">
            <el-date-picker
              v-model="datePickerRange"
              style="float: right; z-index: 1"
              size="small"
              type="daterange"
              align="right"
              unlink-panels
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :shortcuts="shortcuts"
              @change="fetchDashboard"
            />
            <div class="chart-wrapper">
              <v-chart v-if="!loading" :option="chartOption" autoresize />
              <div v-else class="chart-loading">
                <el-skeleton :rows="5" animated />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  margin: 40px;
}

.summary-layout,
.total-layout,
.overview-layout,
.statistics-layout {
  margin-top: 20px;
}

.total-frame {
  border: 1px solid #dcdfe6;
  padding: 20px;
  height: 100px;
}

.total-icon {
  color: #409eff;
  width: 60px;
  height: 60px;
}

.total-title {
  position: relative;
  font-size: 16px;
  color: #909399;
  left: 70px;
  top: -50px;
}

.total-value {
  position: relative;
  font-size: 18px;
  color: #606266;
  left: 70px;
  top: -40px;
}

.un-handle-layout,
.statistics-layout {
  margin-top: 20px;
  border: 1px solid #dcdfe6;
}

.layout-title {
  color: #606266;
  padding: 15px 20px;
  background: #f2f6fc;
  font-weight: bold;
}

.un-handle-content {
  padding: 10px 40px 20px;
}

.un-handle-item {
  border-bottom: 1px solid #ebeef5;
  padding: 14px 10px;
}

.out-border {
  border: 1px solid #dcdfe6;
}

.overview-content {
  padding: 40px;
}

.overview-item-value {
  font-size: 24px;
  text-align: center;
}

.overview-item-title {
  margin-top: 10px;
  text-align: center;
}

.statistics-metrics {
  padding: 20px;
}

.statistics-metric + .statistics-metric {
  margin-top: 20px;
}

.statistics-label {
  color: #909399;
  font-size: 14px;
}

.statistics-value {
  color: #606266;
  font-size: 24px;
  padding: 10px 0;
}

.chart-layout {
  padding: 10px;
  border-left: 1px solid #dcdfe6;
}

.chart-wrapper {
  height: 400px;
}

.chart-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;
}
</style>
