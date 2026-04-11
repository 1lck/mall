import { Button, Card, Descriptions, Form, Input, InputNumber, Select, Space, Typography, Upload, message } from 'antd'
import type { UploadProps } from 'antd'
import { useState } from 'react'

import { ApiError } from '../../../shared/api/http'
import { uploadProductImage } from '../service'
import type {
  CreateProductInput,
  ProductEditSummary,
  ProductStatus,
  UpdateProductInput,
} from '../types'
import { formatOrderDateTime } from '../../orders/utils'

interface BaseProductFormProps {
  submitting?: boolean
}

interface CreateProductFormProps extends BaseProductFormProps {
  mode: 'create'
  initialValues?: Partial<CreateProductInput>
  summary?: never
  onSubmit: (values: CreateProductInput) => Promise<void>
}

interface EditProductFormProps extends BaseProductFormProps {
  mode: 'edit'
  initialValues?: Partial<UpdateProductInput>
  summary?: ProductEditSummary
  onSubmit: (values: UpdateProductInput) => Promise<void>
}

type ProductFormProps = CreateProductFormProps | EditProductFormProps

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '在售', value: 'ON_SALE' },
  { label: '已下架', value: 'OFF_SHELF' },
] satisfies Array<{ label: string; value: ProductStatus }>

type UploadRequestOption = Parameters<NonNullable<UploadProps['customRequest']>>[0]

function isCreateMode(props: ProductFormProps): props is CreateProductFormProps {
  return props.mode === 'create'
}

export function ProductForm(props: ProductFormProps) {
  const { mode, initialValues, summary, submitting = false } = props
  const [form] = Form.useForm<CreateProductInput & UpdateProductInput>()
  const currentImageUrl = Form.useWatch('imageUrl', form)
  const currentName = Form.useWatch('name', form)
  const [isUploading, setIsUploading] = useState(false)

  async function handleFinish(values: CreateProductInput & UpdateProductInput) {
    if (isCreateMode(props)) {
      await props.onSubmit({
        name: values.name,
        categoryName: values.categoryName,
        price: values.price,
        stock: values.stock,
        description: values.description,
        imageUrl: values.imageUrl,
      })
      return
    }

    await props.onSubmit({
      name: values.name,
      categoryName: values.categoryName,
      price: values.price,
      stock: values.stock,
      status: values.status,
      description: values.description,
      imageUrl: values.imageUrl,
    })
  }

  async function handleCustomUpload(options: UploadRequestOption) {
    const file = options.file

    if (!(file instanceof File)) {
      message.error('当前上传文件无效，请重新选择。')
      options.onError?.(new Error('Invalid file type'))
      return
    }

    try {
      setIsUploading(true)
      const result = await uploadProductImage(file)
      form.setFieldValue('imageUrl', result.imageUrl)
      message.success('商品图片上传成功。')
      options.onSuccess?.(result)
    } catch (error) {
      const uploadError = error instanceof ApiError ? error : new Error('商品图片上传失败。')
      message.error(uploadError.message)
      options.onError?.(uploadError)
    } finally {
      setIsUploading(false)
    }
  }

  function handleRemoveImage() {
    form.setFieldValue('imageUrl', undefined)
  }

  const previewAlt = currentName ? `${currentName} 商品图片` : '商品图片预览'

  return (
    <Card variant="borderless">
      <Space orientation="vertical" size={24} className="page-stack">
        <div>
          <Typography.Title level={2}>{mode === 'create' ? '新建商品' : '编辑商品'}</Typography.Title>
          <Typography.Paragraph type="secondary">
            商品模块已接入真实后端接口，表单字段和校验规则与后端保持一致。
          </Typography.Paragraph>
        </div>

        {mode === 'edit' && summary ? (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="数据库 ID">{summary.id}</Descriptions.Item>
            <Descriptions.Item label="商品编号">{summary.productNo}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatOrderDateTime(summary.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatOrderDateTime(summary.updatedAt)}</Descriptions.Item>
          </Descriptions>
        ) : null}

        <Form<CreateProductInput & UpdateProductInput>
          form={form}
          layout="vertical"
          initialValues={initialValues}
          onFinish={handleFinish}
        >
          <Form.Item<CreateProductInput & UpdateProductInput>
            label="商品名称"
            name="name"
            rules={[
              { required: true, message: '请输入商品名称。' },
              { max: 120, message: '商品名称长度不能超过 120 个字符。' },
            ]}
          >
            <Input placeholder="请输入商品名称" />
          </Form.Item>

          <Form.Item<CreateProductInput & UpdateProductInput>
            label="分类名称"
            name="categoryName"
            rules={[
              { required: true, message: '请输入分类名称。' },
              { max: 100, message: '分类名称长度不能超过 100 个字符。' },
            ]}
          >
            <Input placeholder="请输入分类名称" />
          </Form.Item>

          <Space size={16} wrap>
            <Form.Item<CreateProductInput & UpdateProductInput>
              label="价格"
              name="price"
              rules={[
                { required: true, message: '请输入商品价格。' },
                {
                  validator: async (_, value) => {
                    if (value === undefined || value === null || value > 0) {
                      return
                    }

                    throw new Error('商品价格必须大于 0。')
                  },
                },
              ]}
            >
              <InputNumber min={0.01} precision={2} style={{ width: 220 }} placeholder="6999.00" />
            </Form.Item>

            <Form.Item<CreateProductInput & UpdateProductInput>
              label="库存"
              name="stock"
              rules={[
                { required: true, message: '请输入库存。' },
                {
                  validator: async (_, value) => {
                    if (value === undefined || value === null || value >= 0) {
                      return
                    }

                    throw new Error('库存不能小于 0。')
                  },
                },
              ]}
            >
              <InputNumber min={0} precision={0} style={{ width: 220 }} placeholder="88" />
            </Form.Item>

            {mode === 'edit' ? (
              <Form.Item<CreateProductInput & UpdateProductInput>
                label="商品状态"
                name="status"
                rules={[{ required: true, message: '请选择商品状态。' }]}
              >
                <Select options={statusOptions} style={{ width: 220 }} />
              </Form.Item>
            ) : null}
          </Space>

          <Form.Item<CreateProductInput & UpdateProductInput>
            label="商品描述"
            name="description"
            rules={[{ max: 500, message: '商品描述长度不能超过 500 个字符。' }]}
          >
            <Input.TextArea rows={5} maxLength={500} placeholder="请输入商品描述" />
          </Form.Item>

          <Form.Item label="商品图片" extra="选择图片后会立即上传，并自动回填到商品表单。">
            <Space direction="vertical" size={12}>
              <Upload
                accept="image/*"
                maxCount={1}
                showUploadList={false}
                customRequest={handleCustomUpload}
              >
                <Button loading={isUploading}>上传商品图片</Button>
              </Upload>

              {currentImageUrl ? (
                <div className="product-image-field">
                  <img src={currentImageUrl} alt={previewAlt} className="product-image-preview" />
                  <Space size={12} wrap>
                    <Typography.Text ellipsis style={{ maxWidth: 440 }}>
                      {currentImageUrl}
                    </Typography.Text>
                    <Button type="link" onClick={handleRemoveImage}>
                      移除图片
                    </Button>
                  </Space>
                </div>
              ) : (
                <Typography.Text type="secondary">暂未上传商品图片</Typography.Text>
              )}
            </Space>
          </Form.Item>

          <Form.Item<CreateProductInput & UpdateProductInput> name="imageUrl" hidden>
            <Input />
          </Form.Item>

          <Space size={12}>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {mode === 'create' ? '创建商品' : '保存修改'}
            </Button>
          </Space>
        </Form>
      </Space>
    </Card>
  )
}
