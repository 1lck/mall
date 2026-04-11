export function fetchProductCouponList(productId) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: []
	})
}

export function addMemberCoupon(couponId) {
	return Promise.resolve({
		code: 200,
		message: '当前后端暂未实现优惠券',
		data: null
	})
}

export function fetchMemberCouponList(useStatus) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: []
	})
}
