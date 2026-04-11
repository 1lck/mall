import router from '@/router/index'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'
import usePermissionStore from '@/stores/permission'

const whiteList = ['/login']

router.beforeEach((to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (userStore.userInfo.token) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
      return
    }

    if (permissionStore.addRouters.length === 0) {
      permissionStore.generateRoutes({
        roles: userStore.userInfo.roles,
      })
      permissionStore.addRouters.forEach(route => {
        router.addRoute(route)
      })
      next({ ...to, replace: true })
      return
    }

    next()
    return
  }

  if (whiteList.includes(to.path)) {
    next()
    return
  }

  next('/login')
  NProgress.done()
})

router.afterEach(() => {
  NProgress.done()
})
