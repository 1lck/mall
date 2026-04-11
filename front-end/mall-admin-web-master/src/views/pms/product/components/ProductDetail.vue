<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { getPruductUpdateInfoAPI, productCreateAPI, productUpdateByIdAPI } from '@/apis/product'
import type { PmsProductParam } from '@/types/product'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  isEdit: {
    type: Boolean,
    default: false,
  },
})

const formRef = ref<FormInstance>()
const saving = ref(false)
const productParam = ref<PmsProductParam>({
  name: '',
  productCategoryName: '',
  price: 0,
  stock: 0,
  description: '',
  pic: '',
  productSn: '',
  publishStatus: 0,
  backendStatus: 'DRAFT',
  flashPromotionCount: 0,
  flashPromotionId: 0,
  flashPromotionPrice: 0,
  flashPromotionSort: 0,
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  productCategoryName: [{ required: true, message: '请输入商品分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
}

onMounted(async () => {
  if (!props.isEdit) {
    return
  }

  const productId = Number(route.query.id)
  if (!productId) {
    return
  }

  const res = await getPruductUpdateInfoAPI(productId)
  productParam.value = {
    ...productParam.value,
    ...res.data,
  }
})

const submit = async () => {
  await formRef.value?.validate()
  await ElMessageBox.confirm('是否要提交该商品？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })

  saving.value = true
  try {
    if (props.isEdit) {
      await productUpdateByIdAPI(Number(route.query.id), productParam.value)
      ElMessage.success('商品已更新')
    } else {
      await productCreateAPI(productParam.value)
      ElMessage.success('商品已创建')
    }
    router.push({ path: '/pms/product' })
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-card class="form-container" shadow="never">
    <el-form ref="formRef" :model="productParam" :rules="rules" label-width="120px" class="form-inner-container">
      <el-form-item label="商品名称" prop="name">
        <el-input v-model="productParam.name" />
      </el-form-item>
      <el-form-item label="商品分类" prop="productCategoryName">
        <el-input v-model="productParam.productCategoryName" />
      </el-form-item>
      <el-form-item label="商品价格" prop="price">
        <el-input-number v-model="productParam.price" :min="0.01" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="库存" prop="stock">
        <el-input-number v-model="productParam.stock" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="productParam.backendStatus" placeholder="请选择状态">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="上架中" value="ON_SALE" />
          <el-option label="已下架" value="OFF_SHELF" />
        </el-select>
      </el-form-item>
      <el-form-item label="商品货号">
        <el-input v-model="productParam.productSn" placeholder="可留空，系统会生成" />
      </el-form-item>
      <el-form-item label="图片地址">
        <el-input v-model="productParam.pic" placeholder="可填写现成图片 URL" />
      </el-form-item>
      <el-form-item label="商品描述">
        <el-input v-model="productParam.description" type="textarea" :rows="4" />
      </el-form-item>
      <el-form-item>
        <el-button @click="router.push('/pms/product')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">
          {{ isEdit ? '保存修改' : '创建商品' }}
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style>
.form-container {
  width: 960px;
}

.form-inner-container {
  width: 720px;
  margin-top: 24px;
}
</style>
