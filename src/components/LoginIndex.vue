<template>
  <body>
    <div class="login_main">
      <h1 style="text-align: center; justify-content: center; padding: 20px;" >欢迎，请先登录</h1>
      <div class="form_container">
        <div class="form-group">
          用户名:<input v-model="user.userName" type="text" />
        </div>
        <div class="form-group">
          密&emsp;码:<input v-model="user.userPassword" type="password" />
        </div> 
        <p style="font-size: x-small;">新用户请先注册</p>
        <div class="button_container">
          <button @click="handleLogin">登录</button>
          <button @click="PushRegister">注册</button>
        </div>
      </div>
    </div>
  </body>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router' 
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'

// import router from './router';

//
const user =ref({
  userName: '',
  userPassword: ''
});

const router = useRouter() 

const validateForm = () => {
  if (!user.value.userName.trim() || !user.value.userPassword.trim()) {
    alert('用户名和密码不能为空')
    return false
  }
  return true
}

//登录，登录失败时的错误码返回有问题。
const handleLogin = async () => {

  if(!validateForm()) return
  try {
    const response = await axios.post('http://localhost:8080/login', user.value)
    if (response.data) {
      // alert('登录成功')
      ElMessage.success('登录成功')
      router.push('/home')
    } else {
      alert('用户名或密码错误')
      
    }
  } catch (error) {
    alert('登录失败')
    // ElMessage.error('登录失败，请检查用户名密码')
  }
};

//注册
const PushRegister = async () => {
  router.push('/register')
};

</script>

<style scoped>
body {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  min-height: 100vh; 
  background-image: url(../../public/background.jpg);
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.form_container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

/* .form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
} */

.login_main {
  width: 300px;
  background-color: lightblue;
  border: 2px solid darkblue;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
  position: relative;
  z-index: 1;
}

.button_container {
  display: flex;
  gap: 1rem;
}

button {
  padding: 10px 20px;
  background-color: darkblue;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button:hover {
  background-color: blue;
}
</style>