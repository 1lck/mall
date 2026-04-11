package com.mall.system;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台首页统计响应。
 */
public record AdminDashboardResponse(
	SummaryCards summaryCards,
	List<PendingTask> pendingTasks,
	ProductOverview productOverview,
	UserOverview userOverview,
	OrderStatistics orderStatistics,
	List<OrderTrendItem> orderTrend
) {

	public record SummaryCards(
		long todayOrderCount,
		BigDecimal todaySalesAmount,
		BigDecimal yesterdaySalesAmount
	) {
	}

	public record PendingTask(
		String label,
		long count
	) {
	}

	public record ProductOverview(
		long offShelfCount,
		long onSaleCount,
		long lowStockCount,
		long totalCount
	) {
	}

	public record UserOverview(
		long todayNewCount,
		long yesterdayNewCount,
		long monthNewCount,
		long totalCount
	) {
	}

	public record OrderStatistics(
		long monthOrderCount,
		long weekOrderCount,
		BigDecimal monthSalesAmount,
		BigDecimal weekSalesAmount
	) {
	}

	public record OrderTrendItem(
		String date,
		long orderCount,
		BigDecimal orderAmount
	) {
	}
}
