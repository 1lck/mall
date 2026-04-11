export function createReadHistory(data) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data
	})
}

export function fetchReadHistoryList(params) {
	return Promise.resolve({
		code: 200,
		message: 'ok',
		data: []
	})
}

export function clearReadHistory() {
	return Promise.resolve({
		code: 200,
		message: '已清空',
		data: null
	})
}
