export function addCartItem(data) {
	return Promise.resolve({
		code: 200,
		message: '当前版本暂未接入购物车，建议直接购买',
		data
	})
}

export function fetchCartList() {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: []
	})
}

export function deletCartItem(params) {
	return Promise.resolve({
		code: 200,
		message: '删除成功',
		data: null
	})
}

export function updateQuantity(params) {
	return Promise.resolve({
		code: 200,
		message: '更新成功',
		data: null
	})
}

export function clearCartList() {
	return Promise.resolve({
		code: 200,
		message: '已清空',
		data: null
	})
}
