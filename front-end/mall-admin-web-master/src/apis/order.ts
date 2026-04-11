import http from '@/utils/http'
import type { CommonPage } from '@/types/common'
import type {
  OmsMoneyInfoParam,
  OmsOrder,
  OmsOrderDeliveryParam,
  OmsOrderDetail,
  OmsReceiverInfoParam,
  OrderQueryParam,
} from '@/types/order'

type BackendOrder = {
  id: number
  orderNo: string
  userId: number
  totalAmount: number | string
  status: 'CREATED' | 'PAID' | 'CANCELLED'
  remark?: string
  createdAt: string
  updatedAt: string
}

function paginate<T>(items: T[], pageNum: number, pageSize: number): CommonPage<T> {
  const safePageNum = Math.max(pageNum, 1)
  const safePageSize = Math.max(pageSize, 1)
  const start = (safePageNum - 1) * safePageSize
  const list = items.slice(start, start + safePageSize)
  const total = items.length

  return {
    pageNum: safePageNum,
    pageSize: safePageSize,
    total,
    totalPage: Math.max(Math.ceil(total / safePageSize), 1),
    list,
  }
}

function mapStatus(status: BackendOrder['status']) {
  switch (status) {
    case 'PAID':
      return 3
    case 'CANCELLED':
      return 4
    default:
      return 0
  }
}

function mapOrder(item: BackendOrder): OmsOrder {
  return {
    id: item.id,
    memberId: item.userId,
    orderSn: item.orderNo,
    createTime: item.createdAt,
    memberUsername: `用户#${item.userId}`,
    totalAmount: Number(item.totalAmount),
    payAmount: Number(item.totalAmount),
    freightAmount: 0,
    discountAmount: 0,
    payType: item.status === 'PAID' ? 1 : 0,
    sourceType: 1,
    status: mapStatus(item.status),
    orderType: 0,
    promotionInfo: item.remark || '',
    receiverProvince: '',
    note: item.remark,
    paymentTime: item.status === 'PAID' ? item.updatedAt : '',
    deliveryTime: '',
    receiveTime: '',
    commentTime: '',
    modifyTime: item.updatedAt,
    backendStatus: item.status,
  }
}

async function getOrderById(id: number) {
  return http<BackendOrder>({
    method: 'GET',
    url: `/api/v1/orders/${id}`,
  })
}

export async function getOrderListAPI(params: OrderQueryParam) {
  const res = await http<BackendOrder[]>({
    method: 'GET',
    url: '/api/v1/orders',
  })
  const keyword = params.orderSn?.trim().toLowerCase()
  const filtered = res.data
    .map(mapOrder)
    .filter(item => {
      if (params.status !== undefined && item.status !== params.status) {
        return false
      }

      if (keyword && !(item.orderSn ?? '').toLowerCase().includes(keyword)) {
        return false
      }

      return true
    })

  return {
    code: 200,
    message: 'ok',
    data: paginate(filtered, params.pageNum, params.pageSize),
  }
}

export async function orderUpdateCloseAPI(params: { ids: string; note: string }) {
  const ids = params.ids
    .split(',')
    .map(item => Number(item))
    .filter(item => !Number.isNaN(item))

  await Promise.all(
    ids.map(async id => {
      const res = await getOrderById(id)
      return http({
        method: 'PUT',
        url: `/api/v1/orders/${id}`,
        data: {
          totalAmount: res.data.totalAmount,
          status: 'CANCELLED',
          remark: params.note || res.data.remark || '',
        },
      })
    }),
  )

  return {
    code: 200,
    message: 'ok',
    data: null,
  }
}

export async function orderDeleteByIdsAPI(params: { ids: string }) {
  const ids = params.ids
    .split(',')
    .map(item => Number(item))
    .filter(item => !Number.isNaN(item))

  await Promise.all(
    ids.map(id =>
      http({
        method: 'DELETE',
        url: `/api/v1/orders/${id}`,
      }),
    ),
  )

  return {
    code: 200,
    message: 'ok',
    data: null,
  }
}

export async function getOrderDetailByIdAPI(id: number) {
  const res = await getOrderById(id)
  return {
    code: 200,
    message: 'ok',
    data: {
      ...mapOrder(res.data),
      orderItemList: [],
      historyList: [],
    } as OmsOrderDetail,
  }
}

export function orderUpdateReceiverInfoAPI(data: OmsReceiverInfoParam) {
  return getOrderById(data.orderId).then(res =>
    http({
      method: 'PUT',
      url: `/api/v1/orders/${data.orderId}`,
      data: {
        totalAmount: res.data.totalAmount,
        status: data.status === 4 ? 'CANCELLED' : res.data.status,
        remark: res.data.remark || '前端暂未接入收货信息编辑',
      },
    }),
  )
}

export function orderUpdateMoneyInfoAPI(data: OmsMoneyInfoParam) {
  return getOrderById(data.orderId).then(() =>
    http({
      method: 'PUT',
      url: `/api/v1/orders/${data.orderId}`,
      data: {
        totalAmount: Math.max(data.freightAmount + data.discountAmount, 0.01),
        status: data.status === 4 ? 'CANCELLED' : 'CREATED',
        remark: '前端暂未接入费用编辑',
      },
    }),
  )
}

export function orderUpdateDeliveryAPI(data: OmsOrderDeliveryParam[]) {
  return Promise.resolve({
    code: 200,
    message: '当前后端暂未实现发货流程',
    data,
  })
}

export function orderUpdateNoteAPI(params: { id: number; note: string; status: number }) {
  return getOrderById(params.id).then(res =>
    http({
      method: 'PUT',
      url: `/api/v1/orders/${params.id}`,
      data: {
        totalAmount: res.data.totalAmount,
        status: params.status === 4 ? 'CANCELLED' : res.data.status,
        remark: params.note,
      },
    }),
  )
}
