package com.mall.system;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台首页统计响应。
 */
public record AdminDashboardResponse(
	/** 顶部摘要卡片。 */
	SummaryCards summaryCards,
	/** 首页待处理事项。 */
	List<PendingTask> pendingTasks,
	/** 商品概览统计。 */
	ProductOverview productOverview,
	/** 用户概览统计。 */
	UserOverview userOverview,
	/** 订单统计摘要。 */
	OrderStatistics orderStatistics,
	/** 订单趋势图数据。 */
	List<OrderTrendItem> orderTrend
) {

	/**
	 * 看板顶部的摘要卡片数据。
	 */
	public record SummaryCards(
		/** 今日订单数。 */
		long todayOrderCount,
		/** 今日销售额。 */
		BigDecimal todaySalesAmount,
		/** 昨日销售额。 */
		BigDecimal yesterdaySalesAmount
	) {
	}

	/**
	 * 首页待处理事项项。
	 */
	public record PendingTask(
		/** 事项名称。 */
		String label,
		/** 事项数量。 */
		long count
	) {
	}

	/**
	 * 商品维度概览数据。
	 */
	public record ProductOverview(
		/** 已下架商品数量。 */
		long offShelfCount,
		/** 在售商品数量。 */
		long onSaleCount,
		/** 低库存商品数量。 */
		long lowStockCount,
		/** 商品总数。 */
		long totalCount
	) {
	}

	/**
	 * 用户维度概览数据。
	 */
	public record UserOverview(
		/** 今日新增用户数。 */
		long todayNewCount,
		/** 昨日新增用户数。 */
		long yesterdayNewCount,
		/** 本月新增用户数。 */
		long monthNewCount,
		/** 用户总数。 */
		long totalCount
	) {
	}

	/**
	 * 订单统计摘要。
	 */
	public record OrderStatistics(
		/** 本月订单数。 */
		long monthOrderCount,
		/** 最近一周订单数。 */
		long weekOrderCount,
		/** 本月销售额。 */
		BigDecimal monthSalesAmount,
		/** 最近一周销售额。 */
		BigDecimal weekSalesAmount
	) {
	}

	/**
	 * 订单趋势图中的单日数据点。
	 */
	public record OrderTrendItem(
		/** 日期字符串。 */
		String date,
		/** 当日订单数。 */
		long orderCount,
		/** 当日销售额。 */
		BigDecimal orderAmount
	) {
	}
}
