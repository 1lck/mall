import request from '@/utils/requestUtil'

export async function memberLogin(data) {
	const response = await request({
		method: 'POST',
		url: '/api/v1/auth/login',
		data
	})

	return {
		code: 200,
		message: response.message,
		data: {
			token: response.data.token,
			tokenHead: 'Bearer '
		}
	}
}

export async function memberInfo() {
	const response = await request({
		method: 'GET',
		url: '/api/v1/auth/me'
	})

	return {
		code: 200,
		message: response.message,
		data: {
			id: response.data.id,
			username: response.data.username,
			nickname: response.data.nickname,
			icon: '/static/missing-face.png',
			integration: 0,
			growth: 0,
			role: response.data.role,
			status: response.data.status
		}
	}
}
