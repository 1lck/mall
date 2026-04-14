import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/views/layout/Layout.vue'
import type { RouteRecordExt } from '@/types/router'

export const constantRouterMap: RouteRecordExt[] = [
  { path: '/404', component: () => import('@/views/normal/404/index.vue'), hidden: true },
  { path: '/login', component: () => import('@/views/normal/login/index.vue'), hidden: true },
  {
    path: '',
    component: Layout,
    redirect: '/home',
    meta: { title: '首页', icon: 'home' },
    children: [
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'dashboard' },
      },
    ],
  },
]

export const asyncRouterMap: RouteRecordExt[] = [
  {
    path: '/pms',
    component: Layout,
    redirect: '/pms/product',
    name: 'pms',
    meta: { title: '商品', icon: 'product' },
    children: [
      {
        path: 'product',
        name: 'product',
        component: () => import('@/views/pms/product/index.vue'),
        meta: { title: '商品列表', icon: 'product-list' },
      },
      {
        path: 'addProduct',
        name: 'addProduct',
        component: () => import('@/views/pms/product/add.vue'),
        meta: { title: '添加商品', icon: 'product-add' },
        hidden: true,
      },
      {
        path: 'updateProduct',
        name: 'updateProduct',
        component: () => import('@/views/pms/product/update.vue'),
        meta: { title: '修改商品', icon: 'product-add' },
        hidden: true,
      },
    ],
  },
  {
    path: '/oms',
    component: Layout,
    redirect: '/oms/order',
    name: 'oms',
    meta: { title: '订单', icon: 'order' },
    children: [
      {
        path: 'order',
        name: 'order',
        component: () => import('@/views/oms/order/index.vue'),
        meta: { title: '订单列表', icon: 'product-list' },
      },
      {
        path: 'outbox',
        name: 'outbox',
        component: () => import('@/views/oms/outbox/index.vue'),
        meta: { title: '消息观察', icon: 'product-list' },
      },
    ],
  },
  {
    path: '/ums',
    component: Layout,
    redirect: '/ums/admin',
    name: 'ums',
    meta: { title: '用户', icon: 'ums' },
    children: [
      {
        path: 'admin',
        name: 'admin',
        component: () => import('@/views/ums/admin/index.vue'),
        meta: { title: '用户管理', icon: 'ums-admin' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRouterMap,
})

export default router
