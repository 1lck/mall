export function createProductCollection(data) {
	return Promise.resolve({
		code: 200,
		message: '收藏成功',
		data
	})
}

export function deleteProductCollection(params) {
	return Promise.resolve({
		code: 200,
		message: '取消收藏成功',
		data: null
	})
}

export function fetchProductCollectionList(params) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: []
	})
}

export function productCollectionDetail(params) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: null
	})
}

export function clearProductCollection() {
	return Promise.resolve({
		code: 200,
		message: '已清空',
		data: null
	})
}
