import request from '@/utils/requestUtil'

function mapProduct(item) {
	return {
		id: item.id,
		name: item.name,
		subTitle: item.description || '正在热卖，欢迎选购',
		pic: item.imageUrl || '/static/errorImage.jpg',
		price: Number(item.price),
		originalPrice: Number(item.price),
		sale: 0,
		stock: item.stock,
		productCategoryName: item.categoryName,
		status: item.status
	}
}

async function listOnSaleProducts() {
	const response = await request({
		method: 'GET',
		url: '/api/v1/products'
	})
	return response.data
		.filter(item => item.status === 'ON_SALE')
		.map(mapProduct)
}

export async function fetchContent() {
	const products = await listOnSaleProducts()
	const advertiseList = products.slice(0, 3).map((item, index) => ({
		id: item.id || index + 1,
		pic: item.pic
	}))

	return {
		code: 200,
		message: 'ok',
		data: {
			advertiseList,
			brandList: [],
			homeFlashPromotion: null,
			newProductList: products.slice(0, 6),
			hotProductList: products.slice(0, 6)
		}
	}
}

export async function fetchRecommendProductList(params) {
	const products = await listOnSaleProducts()
	const pageNum = params?.pageNum || 1
	const pageSize = params?.pageSize || 4
	const start = (pageNum - 1) * pageSize
	return {
		code: 200,
		message: 'ok',
		data: products.slice(start, start + pageSize)
	}
}

export async function fetchProductCateList(parentId) {
	const products = await listOnSaleProducts()
	const categories = Array.from(new Set(products.map(item => item.productCategoryName).filter(Boolean)))
	return {
		code: 200,
		message: 'ok',
		data: categories.map((name, index) => ({
			id: index + 1,
			name,
			parentId
		}))
	}
}

export async function fetchNewProductList(params) {
	return fetchRecommendProductList(params)
}

export async function fetchHotProductList(params) {
	return fetchRecommendProductList(params)
}
