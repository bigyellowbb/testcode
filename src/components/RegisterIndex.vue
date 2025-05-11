<template>
  <body>
    <div class="login_main">
      <h1 style="text-align: center; justify-content: center; padding: 20px;">新用户注册</h1>
      <div class="form_container">
        <div class="form-group">
          <label>用户名：</label>
          <input v-model="user.userName" type="text">
        </div>
        <div class="form-group">
          <label>密&emsp;码：</label>
          <input v-model="user.userPassword" type="password">
        </div>
        <div class="form-group">
          <label>邮&emsp;箱：</label>
          <input v-model="user.userEmail" type="email">
        </div>
        <div class="button_container">
          <button type="button" @click="handleRegister">注册</button>
          <router-link to="/login">
            <button type="button">返回</button>
          </router-link>
        </div>
        <p v-if="message" :class="messageClass">{{ message }}</p>
      </div>
    </div>
  </body>
</template>

<script setup>
/* eslint-disable */
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter()
const message = ref('')
const messageClass = ref('')

const user = ref({
  userName: '',
  userPassword: '',
  userEmail: ''
})

const validateForm = () => {
  if (!user.value.userName.trim() || !user.value.userPassword.trim() || !user.value.userEmail.trim()) {
    alert('表单项不能为空')
    return false
  }
  return true
}

const handleRegister = async () => {
  if (!validateForm()) return
  
  try {
    // 修改请求字段为驼峰式
    const response = await axios.post('http://localhost:8080/register', {
      userName: user.value.userName,
      userPassword: user.value.userPassword,
      userEmail: user.value.userEmail,
    })

    if (response.data.code === 200) {
      message.value = '注册成功! 2秒后跳转到登录页'
      messageClass.value = 'success'
      setTimeout(() => router.push('/login'), 2000)
    } else {
      message.value = response.data.message || '注册失败'
      messageClass.value = 'error'
    }
  } catch (error) {
    message.value = error.response?.data?.message || '注册失败，请检查网络'
    messageClass.value = 'error'
  }

}
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


.button_container {
  display: flex;
  gap: 1rem;
  margin-top: 15px;
  text-align: center;
  justify-content: center;
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

.success {
  color: green;
  font-size: 0.9em;
  text-align: center;
}

.error {
  color: red;
  font-size: 0.9em;
  text-align: center;
}

/* 调整标签样式 */
/* label {
  font-weight: 500;
  color: darkblue;
} */

/* 统一链接样式 */
router-link {
  text-decoration: none;
  width: 100%;
}
</style>