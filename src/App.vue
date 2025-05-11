<template>
  <!-- 核心：路由视图容器 -->
  <!-- <div class="nav">
      <ul class="navlist">
        <li class="navlist-li" style="box-shadow: inset 3px 3px 5px gray, inset -3px -3px 5px gray;"><a href="#" @click="homeindex">首页</a></li>
        <li class="navlist-li"><a href="#" @click="preindex">预测展示</a></li>
        <li class="navlist-li"><a href="#" @click="dataindex">数据录入</a></li>
        <li class="navlist-li"><a href="#">用户</a>
          <ul class="droplist">
            <li><a href="#">用户中心</a></li>
            <li><a href="#">权限管理</a></li>
            <li><a href="#" @click="quit">退出登录</a></li>
          </ul>
        </li>
        <li class="navlist-li"><a href="">帮助</a></li>
      </ul>
    </div> -->
  <router-view></router-view>
</template>

<script setup>
/* eslint-disable */
// import router from './router';

// function homeindex(){
//   router.push('/home')
// }
// function preindex(){
//   router.push('/pre')
// }
// function dataindex(){
//   router.push('/hyinfo')
// }

// 不需要任何业务逻辑代码
// 整个文件只作为路由容器存在
</script>

<style>
/* 全局样式（可选） */
body {
  margin: 0;
  font-family: Arial, sans-serif;
}

/* 如果需要保留原登录页的背景图 */
body::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url(../public/background.jpg);
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  z-index: -1; /* 确保背景在内容下方 */
}
</style>