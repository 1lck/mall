package com.mall.system;

import com.mall.modules.order.domain.OrderStatus;
import com.mall.modules.order.persistence.entity.OrderEntity;
import com.mall.modules.order.persistence.mapper.OrderMapper;
import com.mall.modules.product.domain.ProductStatus;
import com.mall.modules.product.persistence.entity.ProductEntity;
import com.mall.modules.product.persistence.mapper.ProductMapper;
import com.mall.modules.user.persistence.entity.UserEntity;
import com.mall.modules.user.persistence.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 管理后台首页聚合服务，负责把订单、商品、用户数据整理成看板视图。
 */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

	/** 库存低于该阈值时会被统计为低库存商品。 */
	private static final int LOW_STOCK_THRESHOLD = 10;
	/** 趋势图里的日期输出格式。 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

	private final OrderMapper orderRepository;
	private final ProductMapper productRepository;
	private final UserMapper userRepository;

	public AdminDashboardService(
		OrderMapper orderRepository,
		ProductMapper productRepository,
		UserMapper userRepository
	) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
	}

	/**
	 * 组装后台首页需要的聚合统计数据。
	 */
	public AdminDashboardResponse getDashboard(LocalDate requestedStartDate, LocalDate requestedEndDate) {
		// 查询参数为空时，默认返回最近 7 天的趋势数据。
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zoneId);
		LocalDate startDate = requestedStartDate != null ? requestedStartDate : today.minusDays(6);
		LocalDate endDate = requestedEndDate != null ? requestedEndDate : today;

		// 如果前端把开始和结束时间传反了，后端这里做一次兜底纠正。
		if (startDate.isAfter(endDate)) {
			LocalDate temp = startDate;
			startDate = endDate;
			endDate = temp;
		}

		// 当前版本直接全量加载再做内存聚合，逻辑直观，后续数据量变大时再下推到 SQL。
		List<OrderEntity> orders = orderRepository.findAll();
		List<ProductEntity> products = productRepository.findAll();
		List<UserEntity> users = userRepository.findAll();

		LocalDate yesterday = today.minusDays(1);
		LocalDate rollingWeekStart = today.minusDays(6);
		LocalDate monthStart = today.withDayOfMonth(1);

		long todayOrderCount = orders.stream()
			.filter(order -> toLocalDate(order.getCreatedAt(), zoneId).isEqual(today))
			.count();
		BigDecimal todaySalesAmount = sumPaidOrdersInRange(orders, today, today, zoneId);
		BigDecimal yesterdaySalesAmount = sumPaidOrdersInRange(orders, yesterday, yesterday, zoneId);

		List<AdminDashboardResponse.PendingTask> pendingTasks = List.of(
			new AdminDashboardResponse.PendingTask("待支付订单", countOrdersByStatus(orders, OrderStatus.CREATED)),
			new AdminDashboardResponse.PendingTask("已支付订单", countOrdersByStatus(orders, OrderStatus.PAID)),
			new AdminDashboardResponse.PendingTask("已取消订单", countOrdersByStatus(orders, OrderStatus.CANCELLED)),
			new AdminDashboardResponse.PendingTask("今日新增商品", countProductsCreatedInRange(products, today, today, zoneId)),
			new AdminDashboardResponse.PendingTask("在售商品", countProductsByStatus(products, ProductStatus.ON_SALE)),
			new AdminDashboardResponse.PendingTask("草稿商品", countProductsByStatus(products, ProductStatus.DRAFT)),
			new AdminDashboardResponse.PendingTask("今日新增用户", countUsersCreatedInRange(users, today, today, zoneId)),
			new AdminDashboardResponse.PendingTask("停用账号", users.stream().filter(user -> "DISABLED".equals(user.getStatus().name())).count()),
			new AdminDashboardResponse.PendingTask("管理员账号", users.stream().filter(user -> "ADMIN".equals(user.getRole().name())).count())
		);

		AdminDashboardResponse.ProductOverview productOverview = new AdminDashboardResponse.ProductOverview(
			countProductsByStatus(products, ProductStatus.OFF_SHELF),
			countProductsByStatus(products, ProductStatus.ON_SALE),
			products.stream().filter(product -> product.getStock() != null && product.getStock() <= LOW_STOCK_THRESHOLD).count(),
			products.size()
		);

		AdminDashboardResponse.UserOverview userOverview = new AdminDashboardResponse.UserOverview(
			countUsersCreatedInRange(users, today, today, zoneId),
			countUsersCreatedInRange(users, yesterday, yesterday, zoneId),
			countUsersCreatedInRange(users, monthStart, today, zoneId),
			users.size()
		);

		AdminDashboardResponse.OrderStatistics orderStatistics = new AdminDashboardResponse.OrderStatistics(
			countOrdersCreatedInRange(orders, monthStart, today, zoneId),
			countOrdersCreatedInRange(orders, rollingWeekStart, today, zoneId),
			sumPaidOrdersInRange(orders, monthStart, today, zoneId),
			sumPaidOrdersInRange(orders, rollingWeekStart, today, zoneId)
		);

		List<AdminDashboardResponse.OrderTrendItem> orderTrend = buildOrderTrend(orders, startDate, endDate, zoneId);

		return new AdminDashboardResponse(
			new AdminDashboardResponse.SummaryCards(todayOrderCount, todaySalesAmount, yesterdaySalesAmount),
			pendingTasks,
			productOverview,
			userOverview,
			orderStatistics,
			orderTrend
		);
	}

	/**
	 * 构建指定日期区间的订单趋势数据。
	 */
	private List<AdminDashboardResponse.OrderTrendItem> buildOrderTrend(
		List<OrderEntity> orders,
		LocalDate startDate,
		LocalDate endDate,
		ZoneId zoneId
	) {
		List<AdminDashboardResponse.OrderTrendItem> result = new ArrayList<>();
		LocalDate current = startDate;

		while (!current.isAfter(endDate)) {
			LocalDate date = current;
			long orderCount = countOrdersCreatedInRange(orders, date, date, zoneId);
			BigDecimal orderAmount = sumPaidOrdersInRange(orders, date, date, zoneId);
			result.add(new AdminDashboardResponse.OrderTrendItem(
				DATE_FORMATTER.format(date),
				orderCount,
				orderAmount
			));
			current = current.plusDays(1);
		}

		result.sort(Comparator.comparing(AdminDashboardResponse.OrderTrendItem::date));
		return result;
	}

	/**
	 * 统计指定状态的订单数量。
	 */
	private long countOrdersByStatus(List<OrderEntity> orders, OrderStatus status) {
		return orders.stream().filter(order -> order.getStatus() == status).count();
	}

	/**
	 * 统计指定状态的商品数量。
	 */
	private long countProductsByStatus(List<ProductEntity> products, ProductStatus status) {
		return products.stream().filter(product -> product.getStatus() == status).count();
	}

	/**
	 * 统计指定日期区间内创建的订单数量。
	 */
	private long countOrdersCreatedInRange(List<OrderEntity> orders, LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
		return orders.stream()
			.filter(order -> isWithinRange(toLocalDate(order.getCreatedAt(), zoneId), startDate, endDate))
			.count();
	}

	/**
	 * 统计指定日期区间内创建的商品数量。
	 */
	private long countProductsCreatedInRange(
		List<ProductEntity> products,
		LocalDate startDate,
		LocalDate endDate,
		ZoneId zoneId
	) {
		return products.stream()
			.filter(product -> isWithinRange(toLocalDate(product.getCreatedAt(), zoneId), startDate, endDate))
			.count();
	}

	/**
	 * 统计指定日期区间内创建的用户数量。
	 */
	private long countUsersCreatedInRange(List<UserEntity> users, LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
		return users.stream()
			.filter(user -> isWithinRange(toLocalDate(user.getCreatedAt(), zoneId), startDate, endDate))
			.count();
	}

	/**
	 * 统计指定日期区间内已支付订单的销售额。
	 */
	private BigDecimal sumPaidOrdersInRange(List<OrderEntity> orders, LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
		return orders.stream()
			.filter(order -> order.getStatus() == OrderStatus.PAID)
			.filter(order -> isWithinRange(toLocalDate(order.getCreatedAt(), zoneId), startDate, endDate))
			.map(OrderEntity::getTotalAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/**
	 * 按系统时区把时间戳转换成日期。
	 */
	private LocalDate toLocalDate(java.time.Instant instant, ZoneId zoneId) {
		return instant.atZone(zoneId).toLocalDate();
	}

	/**
	 * 判断指定日期是否落在查询区间内。
	 */
	private boolean isWithinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
		return !date.isBefore(startDate) && !date.isAfter(endDate);
	}
}
