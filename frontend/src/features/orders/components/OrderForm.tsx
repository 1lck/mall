import { Button, Card, Descriptions, Form, Input, InputNumber, Select, Space, Typography } from 'antd'

import type { AuthUser } from '../../auth/types'
import type { CreateOrderInput, EditOrderSummary, OrderStatus, UpdateOrderInput } from '../types'
import { formatOrderDateTime, orderStatusLabels } from '../utils'

interface BaseOrderFormProps {
  submitting?: boolean
}

interface CreateOrderFormProps extends BaseOrderFormProps {
  mode: 'create'
  initialValues?: Partial<CreateOrderInput>
  currentUser: AuthUser
  orderSummary?: never
  onSubmit: (values: CreateOrderInput) => Promise<void>
}

interface EditOrderFormProps extends BaseOrderFormProps {
  mode: 'edit'
  initialValues?: Partial<UpdateOrderInput>
  orderSummary?: EditOrderSummary
  onSubmit: (values: UpdateOrderInput) => Promise<void>
}

type OrderFormProps = CreateOrderFormProps | EditOrderFormProps

const statusOptions = Object.entries(orderStatusLabels).map(([value, label]) => ({
  value: value as OrderStatus,
  label,
}))

const roleLabels: Record<AuthUser['role'], string> = {
  USER: '普通用户',
  ADMIN: '管理员',
}

function isCreateMode(props: OrderFormProps): props is CreateOrderFormProps {
  return props.mode === 'create'
}

export function OrderForm(props: OrderFormProps) {
  const { mode, initialValues, orderSummary, submitting = false } = props
  const [form] = Form.useForm<CreateOrderInput & UpdateOrderInput>()

  async function handleFinish(values: CreateOrderInput & UpdateOrderInput) {
    if (isCreateMode(props)) {
      await props.onSubmit({
        totalAmount: values.totalAmount,
        remark: values.remark,
      })
      return
    }

    await props.onSubmit({
      totalAmount: values.totalAmount,
      status: values.status,
      remark: values.remark,
    })
  }

  return (
    <Card variant="borderless">
      <Space orientation="vertical" size={24} className="page-stack">
        <div>
          <Typography.Title level={2}>{mode === 'create' ? '新建订单' : '编辑订单'}</Typography.Title>
          <Typography.Paragraph type="secondary">
            订单管理模块已接入真实后端接口，前端校验规则与后端保持一致。
          </Typography.Paragraph>
        </div>

        {mode === 'edit' && orderSummary ? (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="数据库 ID">{orderSummary.id}</Descriptions.Item>
            <Descriptions.Item label="订单编号">{orderSummary.orderNo}</Descriptions.Item>
            <Descriptions.Item label="用户 ID">{orderSummary.userId}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatOrderDateTime(orderSummary.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间" span={2}>
              {formatOrderDateTime(orderSummary.updatedAt)}
            </Descriptions.Item>
          </Descriptions>
        ) : null}

        {mode === 'create' && isCreateMode(props) ? (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="当前用户">{props.currentUser.nickname}</Descriptions.Item>
            <Descriptions.Item label="用户名">{props.currentUser.username}</Descriptions.Item>
            <Descriptions.Item label="角色">{roleLabels[props.currentUser.role]}</Descriptions.Item>
            <Descriptions.Item label="用户 ID">{props.currentUser.id}</Descriptions.Item>
          </Descriptions>
        ) : null}

        <Form<CreateOrderInput & UpdateOrderInput>
          form={form}
          layout="vertical"
          initialValues={initialValues}
          onFinish={handleFinish}
        >
          <Form.Item<CreateOrderInput & UpdateOrderInput>
            label="订单总金额"
            name="totalAmount"
            rules={[
              { required: true, message: '请输入订单总金额。' },
              {
                validator: async (_, value) => {
                  if (value === undefined || value === null || value > 0) {
                    return
                  }

                  throw new Error('订单总金额必须大于 0。')
                },
              },
            ]}
          >
            <InputNumber min={0.01} precision={2} style={{ width: 240 }} placeholder="199.90" />
          </Form.Item>

          {mode === 'edit' ? (
            <Form.Item<CreateOrderInput & UpdateOrderInput>
              label="订单状态"
              name="status"
              rules={[{ required: true, message: '请选择订单状态。' }]}
            >
              <Select options={statusOptions} style={{ width: 240 }} />
            </Form.Item>
          ) : null}

          <Form.Item<CreateOrderInput & UpdateOrderInput>
            label="备注"
            name="remark"
            rules={[{ max: 255, message: '备注长度不能超过 255 个字符。' }]}
          >
            <Input.TextArea rows={5} maxLength={255} placeholder="请输入订单备注" />
          </Form.Item>

          <Space size={12}>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {mode === 'create' ? '创建订单' : '保存修改'}
            </Button>
          </Space>
        </Form>
      </Space>
    </Card>
  )
}
