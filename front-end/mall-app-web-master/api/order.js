import request from '@/utils/requestUtil'

function mapStatus(status) {
	if (status === 'PAID') {
		return 3
	}
	if (status === 'CANCELLED') {
		return 4
	}
	return 0
}

function mapBackendStatus(status) {
	if (status === 3) {
		return 'PAID'
	}
	if (status === 4) {
		return 'CANCELLED'
	}
	return 'CREATED'
}

function toOrderItem(order) {
	return {
		id: order.id,
		productPic: '/static/errorImage.jpg',
		productName: order.remark || `订单 ${order.orderNo}`,
		productAttr: JSON.stringify([{ key: '类型', value: '练习订单' }]),
		productQuantity: 1,
		productPrice: Number(order.totalAmount)
	}
}

function mapOrder(order) {
	return {
		id: order.id,
		orderSn: order.orderNo,
		createTime: order.createdAt,
		status: mapStatus(order.status),
		payAmount: Number(order.totalAmount),
		totalAmount: Number(order.totalAmount),
		receiverName: '当前用户',
		receiverPhone: '',
		orderItemList: [toOrderItem(order)],
		note: order.remark || ''
	}
}

async function fetchBackendOrder(orderId) {
	const response = await request({
		method: 'GET',
		url: `/api/v1/orders/${orderId}`
	})
	return response.data
}

export async function generateConfirmOrder(data) {
	const cartIds = JSON.parse(data || '[]')
	const firstId = Array.isArray(cartIds) && cartIds.length > 0 ? cartIds[0] : null
	let cartPromotionItemList = []
	let calcAmount = {
		totalAmount: 0,
		freightAmount: 0,
		promotionAmount: 0,
		payAmount: 0
	}

	if (firstId) {
		const productResponse = await request({
			method: 'GET',
			url: `/api/v1/products/${firstId}`
		})
		const product = productResponse.data
		cartPromotionItemList = [{
			id: product.id,
			productId: product.id,
			productPic: product.imageUrl || '/static/errorImage.jpg',
			productName: product.name,
			productAttr: JSON.stringify([{ key: '规格', value: '默认规格' }]),
			promotionMessage: product.description || '当前商品支持直接下单',
			price: Number(product.price),
			quantity: 1
		}]
		calcAmount.totalAmount = Number(product.price)
		calcAmount.payAmount = Number(product.price)
	}

	return {
		code: 200,
		message: 'ok',
		data: {
			memberReceiveAddressList: [{
				id: 1,
				name: '默认联系人',
				phoneNumber: '',
				province: '',
				city: '',
				region: '',
				detailAddress: '当前版本暂未接入地址系统',
				defaultStatus: 1
			}],
			cartPromotionItemList,
			couponHistoryDetailList: [],
			calcAmount,
			integrationConsumeSetting: {
				couponStatus: 0,
				deductionPerAmount: 100
			},
			memberIntegration: 0
		}
	}
}

export async function generateOrder(data) {
	let totalAmount = Number(data?.totalAmount || 0)
	let remark = data?.remark || '商城前台订单'

	if (!totalAmount && Array.isArray(data?.cartIds) && data.cartIds.length > 0) {
		const productResponse = await request({
			method: 'GET',
			url: `/api/v1/products/${data.cartIds[0]}`
		})
		totalAmount = Number(productResponse.data.price)
		remark = productResponse.data.name
	}

	const response = await request({
		method: 'POST',
		url: '/api/v1/orders',
		data: {
			totalAmount: totalAmount || 0.01,
			remark
		}
	})

	return {
		code: 200,
		message: 'ok',
		data: {
			order: mapOrder(response.data)
		}
	}
}

export async function fetchOrderList(params) {
	const response = await request({
		method: 'GET',
		url: '/api/v1/orders'
	})
	let orders = response.data.map(mapOrder)
	if (params && params.status !== undefined && params.status !== -1) {
		orders = orders.filter(item => item.status === params.status)
	}
	const pageNum = params?.pageNum || 1
	const pageSize = params?.pageSize || 5
	const start = (pageNum - 1) * pageSize
	return {
		code: 200,
		message: 'ok',
		data: {
			list: orders.slice(start, start + pageSize),
			pageNum,
			pageSize,
			total: orders.length
		}
	}
}

export async function payOrderSuccess(data) {
	const order = await fetchBackendOrder(data.orderId)
	await request({
		method: 'PUT',
		url: `/api/v1/orders/${data.orderId}`,
		data: {
			totalAmount: Number(order.totalAmount),
			status: 'PAID',
			remark: order.remark || '支付成功'
		}
	})
	return {
		code: 200,
		message: '支付成功',
		data: null
	}
}

export async function fetchOrderDetail(orderId) {
	const order = await fetchBackendOrder(orderId)
	return {
		code: 200,
		message: 'ok',
		data: mapOrder(order)
	}
}

export async function cancelUserOrder(data) {
	const order = await fetchBackendOrder(data.orderId)
	await request({
		method: 'PUT',
		url: `/api/v1/orders/${data.orderId}`,
		data: {
			totalAmount: Number(order.totalAmount),
			status: 'CANCELLED',
			remark: order.remark || '用户取消订单'
		}
	})
	return {
		code: 200,
		message: '订单已取消',
		data: null
	}
}

export async function confirmReceiveOrder(data) {
	const order = await fetchBackendOrder(data.orderId)
	await request({
		method: 'PUT',
		url: `/api/v1/orders/${data.orderId}`,
		data: {
			totalAmount: Number(order.totalAmount),
			status: 'PAID',
			remark: order.remark || '确认收货'
		}
	})
	return {
		code: 200,
		message: '已确认收货',
		data: null
	}
}

export async function deleteUserOrder(data) {
	await request({
		method: 'DELETE',
		url: `/api/v1/orders/${data.orderId}`
	})
	return {
		code: 200,
		message: '删除成功',
		data: null
	}
}

export function fetchAliapyStatus(params) {
	return Promise.resolve({
		code: 200,
		message: '当前环境未接入支付宝查询',
		data: {
			outTradeNo: params?.outTradeNo || '',
			tradeStatus: 'UNKNOWN'
		}
	})
}
