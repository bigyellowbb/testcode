<template>
  <div class="nav">
      <ul class="navlist">
        <li class="navlist-li"><a href="#" @click="homeback">首页</a></li>
        <li class="navlist-li"><a href="#" @click="preback">预测展示</a></li>
        <li class="navlist-li" style="box-shadow: inset 3px 3px 5px gray, inset -3px -3px 5px gray;"><a href="#" @click="databack">数据录入</a></li>
        <li class="navlist-li"><a href="#"  @click="userback">用户中心</a>
          <ul class="droplist">
            <li><a href="#" @click="quit">退出登录</a></li>
          </ul>
        </li>
        <!-- <li class="navlist-li"><a href="">帮助</a></li> -->
      </ul>
    </div>
  
  <div class="admin-container">
    <h2 class="admin-title">水库水雨情数据录入</h2>
    <div class="form-box">
      <form @submit.prevent="submitForm">
        <div class="form-row">
          <div class="form-group">
            <label>水库名称：</label>
            <input type="text" v-model="formData.hy_name" required>
          </div>
          
          <div class="form-group">
            <label>水位高度(m)：</label>
            <input type="number" v-model="formData.water_level" step="0.01">
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>含沙量(kg/m³)：</label>
            <input type="number" v-model="formData.sand_content" step="0.001">
          </div>
          
          <div class="form-group">
            <label>露点温度(℃)：</label>
            <input type="number" v-model="formData.dew_point" step="0.1">
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>降水量(mm)：</label>
            <input type="number" v-model="formData.precipitation" step="0.1">
          </div>
          
          <div class="form-group">
            <label>海压(hPa)：</label>
            <input type="number" v-model="formData.sea_pressure" step="0.01">
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>温度(℃)：</label>
            <input type="number" v-model="formData.temperature" step="0.1">
          </div>
          
          <div class="form-group">
            <label>能见度(km)：</label>
            <input type="number" v-model="formData.visibility" step="0.01">
          </div>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>风速(m/s)：</label>
            <input type="number" v-model="formData.wind_speed" step="0.1">
          </div>
          
          <div class="form-group">
            <label>流量(MB)：</label>
            <input type="number" v-model="formData.internet_traffic" step="0.1">
          </div>
        </div>
        <div class="button-group">
          <button type="submit" class="submit-btn">提交</button>
          <button type="button" @click="resetForm" class="reset-btn">重置</button>
        </div>
      </form>
    </div>

        <!-- 文件上传区域 -->
    <div class="form-box upload-box">
      <h3>批量上传数据文件</h3>
      <form @submit.prevent="submitFile">
        <div class="form-group">
          <div class="file-upload-box">
            <input
              type="file"
              @change="handleFileUpload"
              accept=".xlsx, .xls, .csv"
            >
            <p class="file-tips">支持Excel/CSV格式（自动转换为CSV）</p>
          </div>
        </div>
        <div class="button-group">
          <button type="submit" class="submit-btn">上传文件</button>
        </div>
      </form>
    </div>
  </div>

</template>

<script setup>
/* eslint-disable */
import axios from 'axios'
import router from '@/router'
import { ref } from 'vue'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus' 
import 'element-plus/dist/index.css'

// 数据
const formData = ref({
  hy_name: '',
  water_level: 0.00,
  sand_content: 0.00,
  dew_point: 0.00,
  precipitation: 0.00,
  sea_pressure: 0.00,
  temperature: 0.00,
  visibility: 0.00,
  wind_speed: 0.00,
  internet_traffic: 0.00
})

// 文件上传
const selectedFile = ref(null)

const handleFileUpload = (event) => {
  selectedFile.value = event.target.files[0]
}

// 提交JSON格式数据
const submitForm = async () => {
  try {
    const response = await axios.post(
      'http://localhost:8080/hyinfo',
      formData.value,
      {
        headers: {
          'Content-Type': 'application/json', 
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      }
    )

    if (response.data.success) {
      // alert('数据提交成功！')
      ElMessage.success('数据提交成功！')
      resetForm()
    }
  } catch (error) {
    console.error('表单提交失败:', error)
    // alert(error.response?.data?.message || '提交失败')
    ElMessage.error(error.response?.data?.message || '提交失败')
  }
}

const submitFile = async () => {
  if (!selectedFile.value) {
    // alert('请先选择文件')
    ElMessage.warning('请先选择文件')
    return
  }

  try {
    const form = new FormData()
    const reader = new FileReader()

    reader.onload = async (e) => {
      try {
        let csvData
        if (selectedFile.value.name.match(/\.(xlsx|xls)$/)) {
          const data = new Uint8Array(e.target.result)
          const workbook = XLSX.read(data, { type: 'array' })
          const worksheet = workbook.Sheets[workbook.SheetNames[0]]
          csvData = XLSX.utils.sheet_to_csv(worksheet)
        } else {
          csvData = e.target.result
        }

        const csvBlob = new Blob([csvData], { type: 'text/csv' })
        form.append('file', csvBlob, 'converted.csv')

        await axios.post(
          'http://localhost:8080/hyinfo/upload',
          form,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
              Authorization: `Bearer ${localStorage.getItem('token')}`
            }
          }
        )
        // alert('文件上传成功！')
        ElMessage.success('文件上传成功')
      } catch (error) {
        console.error('文件上传失败:', error)
        // alert(error.response?.data?.message || '文件处理失败')
        ElMessage.error('文件读取失败: ' + error.message)
      }
    }

    if (selectedFile.value.name.endsWith('.csv')) {
      reader.readAsText(selectedFile.value)
    } else {
      reader.readAsArrayBuffer(selectedFile.value)
    }
  } catch (error) {
    // alert('文件读取失败: ' + error.message)
    ElMessage.error('文件读取失败: ' + error.message)
  }
}


const resetForm = () => {
  // 重置数据
  formData.value = {
    hy_name: '',
    water_level: 0.00,
    sand_content: 0.00,
    dew_point: 0.00,
    precipitation: 0.00,
    sea_pressure: 0.00,
    temperature: 0.00,
    visibility: 0.00,
    wind_speed: 0.00,
    internet_traffic: 0.00
  }
  
  selectedFile.value = null
  const fileInput = document.querySelector('input[type="file"]')
  if (fileInput) fileInput.value = ''
}

const homeback = () => router.push('/home')
const preback = () => router.push('/pre')
const databack = () => router.push('/hyinfo')
const userback = () => router.push('/user')
const quit = () => {
  localStorage.removeItem('token')
  router.replace('/login')
}

</script>

<style scoped>
    .upload-box {
      margin-top: 30px;
      border-top: 2px solid #eee;
      padding-top: 20px;
    }

    .upload-box h3 {
      color: #666;
      margin-bottom: 15px;
    }
    .admin-container {
    position: relative;
    min-height: 100vh;
    padding: 100px 20px 40px;
    margin-top: 50px; 
    background-image: url('../../public/background.jpg');
    background-repeat: no-repeat;
    background-size: cover;
    background-attachment: fixed;
    }

    .back-button {
    position: absolute;
    top: 20px;
    left: 20px;
    color: darkblue;
    text-decoration: none;
    font-size: 14px;
    transition: color 0.3s;
    }

    .back-button:hover {
    color: #337ecc;
    }

    .admin-title {
    text-align: center;
    margin-bottom: 40px;
    color: #333;
    }

    .form-box {
    max-width: 800px;
    margin: 0 auto;
    margin-top: 2%;
    padding: 30px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    }

    .centered-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
    }

    .form-row {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 30px;
    }

    .form-group label {
    display: block;
    margin-bottom: 8px;
    color: #606266;
    font-size: 14px;
    }

    .form-group input {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    transition: border-color 0.3s;
    }

    .form-group input:focus {
    outline: none;
    border-color: #409eff;
    }

    .button-group {
    display: flex;
    justify-content: center;
    gap: 20px;
    margin-top: 30px;
    }

    .submit-btn,
    .reset-btn {
    padding: 12px 36px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.3s;
    }

    .submit-btn {
    background-color: #409eff;
    color: white;
    }

    .submit-btn:hover {
    background-color: #337ecc;
    }

    .reset-btn {
    background-color: #f0f2f5;
    color: #606266;
    }

    .reset-btn:hover {
    background-color: #e4e6e9;
    }

.file-upload-box {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px 12px;
  background: white;
  transition: border-color 0.3s;
}

.file-upload-box:hover {
  border-color: #409eff;
}

.file-upload-box input[type="file"] {
  width: 100%;
  padding: 8px 0;
  cursor: pointer;
}

.file-tips {
  color: #606266;
  font-size: 12px;
  margin-top: 8px;
  line-height: 1.5;
}

.form-row:last-child {
  margin-bottom: 20px;
}
.nav {
  height: 50px;
  background-color: black;
  box-shadow: 0 1px 2px gray;
  position: relative;
  z-index: 5; /* 保持原有层级 */
}

.navlist-li {
  float: left;
  width: 25%;
  line-height: 50px;
  text-align: center;
  position: relative; /* 新增定位上下文 */
}

.droplist {
  display: none;
  position: absolute; /* 改为绝对定位 */
  top: 100%;         /* 从父元素底部开始 */
  left: 0;
  height: auto;
  width: 100%;       /* 保持原有宽度 */
  background: black;
  box-shadow: 0 1px 2px gray;
  border-radius: 6px;
  z-index: 6;        /* 高于父级 */
}

/* 优化悬停效果 */
.navlist-li:hover .droplist {
  display: block;
  margin-top: 0;     /* 防止位移 */
}

/* 保持其他原有样式不变 */
.navlist a {
  color: white;
}

.navlist a:hover {
  background: gray;
  display: block;
}

.droplist li {
  border-bottom: 1px solid gray;
}

.droplist li:last-child {
  border-bottom: none;
}
/* .nav{
  height: 50px;
  background-color: black;
  box-shadow: 0 1px 2px gray;
  position: relative;
  z-index: 5;
}
.navlist-li{
  float: left;
  width: 25%;
  line-height: 50px;
  text-align: center;
} */
</style>