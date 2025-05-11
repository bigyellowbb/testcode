import { createRouter, createWebHistory } from 'vue-router'
import HomeIndex from '@/components/HomeIndex.vue'
import RegisterIndex from '@/components/RegisterIndex.vue'
import HyInfoAdd from '@/components/HyInfoAdd.vue'
import PreIndex from '@/components/PreIndex.vue'
import UserIndex from '@/components/UserIndex.vue'


const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/components/LoginIndex.vue')
  },
  {
    path: '/User',
    name: 'user',
    component: UserIndex
  },
  {
    path: '/pre',
    name: 'Pre',
    component: PreIndex
  },
  {
    path: '/hyinfo',
    name: 'HyInfo',
    component: HyInfoAdd
  },
  {
    path: '/home',
    name: 'Home',
    component: HomeIndex
  },
  {
    path: '/register',
    name: 'Register',
    component: RegisterIndex
  },
  {
    path: '/', // 根路径重定向
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router