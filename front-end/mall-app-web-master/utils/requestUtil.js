import Request from '@/js_sdk/luch-request/request.js'
import { API_BASE_URL } from '@/utils/appConfig.js';

const http = new Request()

http.setConfig((config) => {
	config.baseUrl = API_BASE_URL
	config.header = {
		...config.header
	}
	return config
})

http.validateStatus = (statusCode) => {
	return statusCode >= 200 && statusCode < 300
}

function normalizeResponse(response) {
	const payload = response.data
	if (payload && typeof payload === 'object' && 'success' in payload && 'data' in payload) {
		return {
			code: payload.success ? 200 : response.statusCode,
			message: payload.message || '',
			data: payload.data
		}
	}
	return payload
}

http.interceptor.request((config) => {
	const token = uni.getStorageSync('token');
	if (token) {
		config.header = {
			'Authorization': token.startsWith('Bearer ') ? token : `Bearer ${token}`,
			...config.header
		}
	}
	return config
})

http.interceptor.response((response) => {
	const res = normalizeResponse(response)
	if (res.code !== 200) {
		uni.showToast({
			title: res.message || '请求失败',
			duration: 1500,
			icon: 'none'
		})
		if (response.statusCode === 401) {
			uni.showModal({
				title: '提示',
				content: '你已被登出，可以取消继续留在该页面，或者重新登录',
				confirmText: '重新登录',
				cancelText: '取消',
				success: function(modalRes) {
					if (modalRes.confirm) {
						uni.navigateTo({
							url: '/pages/public/login'
						})
					}
				}
			});
		}
		return Promise.reject(response);
	}
	return res;
}, (response) => {
	uni.showToast({
		title: response.errMsg || '请求失败',
		duration: 1500,
		icon: 'none'
	})
	return Promise.reject(response);
})

export function request(options = {}) {
	return http.request(options);
}

export default request
