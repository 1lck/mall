import { defineStore } from 'pinia'
import { shallowRef } from 'vue'
import { asyncRouterMap, constantRouterMap } from '@/router/index'
import type { RouteRecordExt } from '@/types/router'

export const usePermissionStore = defineStore('permission', () => {
  const routers = shallowRef(constantRouterMap)
  const addRouters = shallowRef<RouteRecordExt[]>([])

  const generateRoutes = (data: { roles: string[] }) => {
    const accessedRouters = data.roles.includes('ADMIN') ? asyncRouterMap : []
    addRouters.value = accessedRouters
    routers.value = constantRouterMap.concat(accessedRouters)
  }

  return {
    routers,
    addRouters,
    generateRoutes,
  }
})

export default usePermissionStore
