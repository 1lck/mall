import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLoginAPI, adminLogoutAPI, getAdminInfoAPI } from '@/apis/admin'
import type { LoginParam, UserInfo } from '@/types/admin'

export const useUserStore = defineStore(
  'user',
  () => {
    const userInfo = ref<UserInfo>({
      id: 0,
      username: '',
      nickname: '',
      password: '',
      avatar: '',
      roles: [],
      token: '',
      role: '',
    })

    const userLogin = async (loginParam: LoginParam) => {
      const res = await adminLoginAPI(loginParam)
      userInfo.value.token = res.data.token
      userInfo.value.username = loginParam.username
      userInfo.value.password = loginParam.password
      await getUserInfo()
    }

    const getUserInfo = async () => {
      const res = await getAdminInfoAPI()
      if (res.data.role !== 'ADMIN') {
        throw new Error('当前账号不是管理员，无法进入后台。')
      }

      userInfo.value.id = res.data.id
      userInfo.value.username = res.data.username
      userInfo.value.nickname = res.data.nickname
      userInfo.value.role = res.data.role
      userInfo.value.roles = res.data.roles
      userInfo.value.avatar = `https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(res.data.nickname || res.data.username)}`
    }

    const userLogout = async () => {
      await adminLogoutAPI()
      fedLogout()
    }

    const fedLogout = () => {
      userInfo.value.token = ''
      userInfo.value.roles = []
      userInfo.value.role = ''
      userInfo.value.nickname = ''
      userInfo.value.avatar = ''
    }

    return {
      userInfo,
      userLogin,
      getUserInfo,
      userLogout,
      fedLogout,
    }
  },
  {
    persist: true,
  },
)
