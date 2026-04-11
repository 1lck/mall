import request from '@/utils/requestUtil'

function buildCategories(products) {
	const names = Array.from(new Set(products.map(item => item.categoryName).filter(Boolean))).sort()
	return names.map((name, index) => ({
		id: index + 1,
		name,
		children: []
	}))
}

function categoryIdByName(categories, categoryName) {
	const category = categories.find(item => item.name === categoryName)
	return category ? category.id : null
}

function mapProduct(item, categories) {
	return {
		id: item.id,
		productCategoryId: categoryIdByName(categories, item.categoryName),
		name: item.name,
		subTitle: item.description || '正在热卖，欢迎选购',
		pic: item.imageUrl || '/static/errorImage.jpg',
		price: Number(item.price),
		originalPrice: Number(item.price),
		sale: 0,
		stock: item.stock,
		productSn: item.productNo,
		productCategoryName: item.categoryName,
		description: item.description,
		status: item.status
	}
}

async function listProducts() {
	const response = await request({
		method: 'GET',
		url: '/api/v1/products'
	})
	const categories = buildCategories(response.data)
	return {
		categories,
		products: response.data.map(item => mapProduct(item, categories))
	}
}

export async function searchProductList(params) {
	const { categories, products } = await listProducts()
	let filtered = products.filter(item => item.status === 'ON_SALE')

	if (params?.productCategoryId) {
		filtered = filtered.filter(item => item.productCategoryId === Number(params.productCategoryId))
	}

	if (params?.sort === 3) {
		filtered = filtered.slice().sort((a, b) => a.price - b.price)
	} else if (params?.sort === 4) {
		filtered = filtered.slice().sort((a, b) => b.price - a.price)
	}

	const pageNum = params?.pageNum || 1
	const pageSize = params?.pageSize || 6
	const start = (pageNum - 1) * pageSize
	return {
		code: 200,
		message: 'ok',
		data: {
			list: filtered.slice(start, start + pageSize),
			pageNum,
			pageSize,
			total: filtered.length,
			totalPage: Math.max(Math.ceil(filtered.length / pageSize), 1)
		}
	}
}

export async function fetchCategoryTreeList() {
	const { categories } = await listProducts()
	return {
		code: 200,
		message: 'ok',
		data: categories.map(item => ({
			...item,
			children: [{
				id: item.id,
				name: item.name
			}]
		}))
	}
}

export async function fetchProductDetail(id) {
	const response = await request({
		method: 'GET',
		url: `/api/v1/products/${id}`
	})
	const product = {
		id: response.data.id,
		productCategoryId: 1,
		name: response.data.name,
		subTitle: response.data.description || '正在热卖，欢迎选购',
		pic: response.data.imageUrl || '/static/errorImage.jpg',
		albumPics: response.data.imageUrl || '',
		price: Number(response.data.price),
		originalPrice: Number(response.data.price),
		sale: 0,
		stock: response.data.stock,
		productSn: response.data.productNo,
		productCategoryName: response.data.categoryName,
		description: response.data.description,
		detailMobileHtml: `<p>${response.data.description || '暂无详情描述'}</p>`,
		serviceIds: '1,2,3',
		promotionType: 0,
		brandName: 'Mall',
	}

	return {
		code: 200,
		message: 'ok',
		data: {
			product,
			brand: {
				id: 1,
				name: response.data.categoryName || 'Mall',
				firstLetter: (response.data.categoryName || 'M')[0],
				logo: response.data.imageUrl || '/static/errorImage.jpg'
			},
			skuStockList: [{
				id: response.data.id,
				skuCode: response.data.productNo,
				price: Number(response.data.price),
				promotionPrice: Number(response.data.price),
				stock: response.data.stock,
				spData: JSON.stringify([{ key: '规格', value: '默认规格' }])
			}],
			productAttributeList: [{
				id: 1,
				name: '规格',
				type: 0,
				handAddStatus: 0,
				inputList: '默认规格'
			}, {
				id: 2,
				name: '分类',
				type: 1,
				handAddStatus: 0,
				inputList: ''
			}],
			productAttributeValueList: [{
				productAttributeId: 2,
				value: response.data.categoryName || '默认分类'
			}],
			productLadderList: [],
			productFullReductionList: []
		}
	}
}
