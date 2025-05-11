<template>
  <div class="nav">
    <ul class="navlist">
      <li class="navlist-li" style="box-shadow: inset 3px 3px 5px gray, inset -3px -3px 5px gray;"><a href="#" @click="homeback">首页</a></li>
      <li class="navlist-li"><a href="#" @click="preback">预测展示</a></li>
      <li class="navlist-li"><a href="#" @click="databack">数据录入</a></li>
      <li class="navlist-li"><a href="#" @click="userback">用户中心</a>
        <ul class="droplist">
          <!-- <li><a href="#">权限管理</a></li> -->
          <li><a href="#" @click="quit">退出登录</a></li>
        </ul>
      </li>
    </ul>
  </div>
  <h1 class="logotext" style="text-align: center; justify-content: center; position: absolute; margin-left: 4%;">水库水雨情预报调度系统</h1>
  <div class="data-manager">
    <div class="toolbar">
      <div class="search-box">
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="输入水库名称搜索"
          @keyup.enter="handleSearch"
        >
        <button @click="handleSearch">搜索</button>
        <button @click="resetSearch">重置</button>
      </div>
    </div>


    <div class="data-controls">
      <button @click="prevPage" :disabled="currentPage <= 1">上一页</button>
      <span>第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage >= totalPages">下一页</button>
    </div>

    
    <div class="data-table">
      <table>
        <thead>
          <tr>
            <th>水库名称</th>
            <th>水位(m)</th>
            <th>含沙量(kg/m³)</th>
            <th>露点(℃)</th>
            <th>降水量(mm)</th>
            <th>海压(hPa)</th>
            <th>温度(℃)</th>
            <th>能见度(km)</th>
            <th>风速(m/s)</th>
            <th>流量(m³/s)</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="7" class="loading-text">数据加载中...</td>
          </tr>
          <tr v-else-if="tableData.length === 0">
            <td colspan="7" class="empty-text">暂无数据</td>
          </tr>
          <!-- <tr v-else v-for="item in tableData" :key="item.id"> -->
            <tr v-else v-for="(item, index) in tableData" :key="item.id + '_' + index">
            
            <td>{{ item?.hy_name ?? '未知水库' }}</td>
            <td>{{ item?.water_level ?? 'N/A' }}</td>
            <td>{{ item?.sand_content ?? 'N/A' }}</td>
            <td>{{ item?.dew_point ?? 'N/A' }}</td>
            <td>{{ item?.precipitation ?? 'N/A' }}</td>
            <td>{{ item?.sea_pressure ?? 'N/A' }}</td>
            <td>{{ item?.temperature ?? 'N/A' }}</td>
            <td>{{ item?.visibility ? (item.visibility) : 'N/A' }}</td>
            <td>{{ item?.wind_speed ?? 'N/A' }}</td>
            <td>{{ item?.internet_traffic ?? 'N/A' }}</td>
            <td>
              <button @click="openEdit(item)">编辑</button>
              <button @click="deleteItem(item.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div v-if="showEditDialog" class="dialog">
    <div class="dialog-content">
      <h3>编辑数据</h3>
      <form @submit.prevent="saveEdit">
        <div class="form-group">
          <label>水库名称:</label>
          <input v-model="currentEdit.hy_name" required>
        </div>
        <div class="form-group">
          <label>水位(m):</label>
          <input v-model="currentEdit.water_level" type="number" step="0.01" required>
        </div>
        <div class="form-group">
          <label>含沙量(kg/m³):</label>
          <input v-model="currentEdit.sand_content" type="number" step="0.001" required>
        </div>
        <div class="form-group">
          <label>露点(℃):</label>
          <input v-model="currentEdit.dew_point" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>降水量(mm):</label>
          <input v-model="currentEdit.precipitation" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>海压(hPa):</label>
          <input v-model="currentEdit.sea_pressure" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>温度(℃):</label>
          <input v-model="currentEdit.temperature" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>能见度(km):</label>
          <input v-model="currentEdit.visibility" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>风速(m/s):</label>
          <input v-model="currentEdit.wind_speed" type="number" step="0.1" required>
        </div>
        <div class="form-group">
          <label>流量(m³/s):</label>
          <input v-model="currentEdit.internet_traffic" type="number" step="0.1" required>
        </div>
        <div class="dialog-buttons">
          <button type="button" @click="showEditDialog = false">取消</button>
          <button type="submit">保存</button>
        </div>
      </form>
      <button class="close-dialog" @click="showEditDialog = false">×</button>
    </div>
  </div>
</template>

<script setup>
 /* eslint-disable */ 
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'



const userStore = useUserStore()


// 状态管理
const tableData = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(false)
const pageSize = 10

const fetchData = async () => {
  try {
    loading.value = true;
    const response = await axios.get('http://localhost:8080/hyinfo', {
    params: {
    hyName: searchKeyword.value, // 搜索关键词参数
    currentPage: currentPage.value, // 传递1-based页码
    pageSize: pageSize // 每页数量参数
  }
});
const { data = [], totalPages: tp = 1, currentPage: cp = 1 } = response.data || {};
    // 数据结构
  tableData.value = response.data?.data || [];
  totalPages.value = response.data?.totalPages || 1;
  currentPage.value = response.data?.currentPage || 1;
    if (!Array.isArray(data)) throw new Error('响应数据非数组');
    
  } catch (error) {
    console.error('[ERROR] 请求失败:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  currentPage.value = 1
  fetchData()
})

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData({ hyName: searchKeyword.value })
}

const resetSearch = () => {
  searchKeyword.value = ''
  fetchData()
}

//分页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchData()
  }
}
//nextPage
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    fetchData(); 
  }
};

// 对话框
const showEditDialog = ref(false)
const currentEdit = reactive({})

const openEdit = (item) => {
  Object.assign(currentEdit, item)
  showEditDialog.value = true
}

const saveEdit = async () => {
  try {
    await axios.put(`http://localhost:8080/hyinfo/${currentEdit.id}`, currentEdit)
    showEditDialog.value = false
    await fetchData()
  } catch (error) {
    alert(`保存失败: ${error.response?.data?.message || error.message}`)
  }
}

const deleteItem = async (id) => {
  if (!confirm('确定要删除这条记录吗？')) return
  try {
    await axios.delete(`http://localhost:8080/hyinfo/${id}`)
    await fetchData()
  } catch (error) {
    alert(`删除失败: ${error.response?.data?.message || error.message}`)
  }
}

onMounted(() => {
  currentPage.value = 1;
  fetchData();
});

// 用户操作
const quit = () => {
  localStorage.removeItem('token')
  userStore.logout()
  router.replace('/login')
}
const homeback = () => router.push('/home')
const databack = () => router.push('/hyinfo')
const preback = () => router.push('/pre')
const userback = () => router.push('/user')

</script>


 <style>
*{
  margin: 0;
  padding: 0;
  text-decoration: none;
  list-style: none;
}
body{
  background-image: url("../../public/background.jpg");
  background-repeat: no-repeat;
  background-size: cover;
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
  position: relative; 
}

.droplist {
  display: none;
  position: absolute; 
  top: 100%;        
  left: 0;
  height: auto;
  width: 100%;       
  background: black;
  box-shadow: 0 1px 2px gray;
  border-radius: 6px;
  z-index: 6;        
}

.navlist-li:hover .droplist {
  display: block;
  margin-top: 0;     
}

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
}
.navlist a{
  color: white;
}
.navlist a:hover{
  background: gray;
  display: block;
}
.droplist{
  background: black;
  box-shadow: 0 1px 2px gray;
  display: none;
  border-radius: 6px;
  overflow: hidden;
}
.droplist li{
  border-bottom: 1px solid gray;
}
.navlist-li:hover .droplist{
  display: block;
}
.droplist li{
  border-top: 1px solid black;
} */
.logotext{
  font-size: 188%;
  position: absolute;
  margin-left: 42%;
  margin-top: 1%;
  color: black;
  text-shadow: 0px 1px 2px gray;
}
.animated{
  float: left;
  height: 50%;
  width: 17%;
  position: absolute;
  margin-top: 2%;
  margin-left: 2%;
  border: 5px solid gray;
  background-color: white;
} 
.animated table tbody tr td{
  width: 3%;
  border: 1px solid black;
} 
.img1,.img2{
  height: 50%;
  width: 50%; 
  margin-top: 2%;
  margin-left: 21%;
  position: absolute;
  border: 5px solid gray;
  background-color: white;
}
.info{
  height: 50%;
  width: 25%;
  position: absolute;
  margin-top: 2%;
  margin-left: 73%;
  border: 5px solid gray;
  background-color: white;
}
.search{
  margin-left: 10px;
}
#do-search,#do-clear{
  width: 50px;
}
.introduce{
  height: 20%;
  width: 96%;
  margin-left: 2%;
  position: absolute;
  border: 5px solid gray;
  top: 70%;
  background-color: white;
}
.data-manager {
  width: 90%;
  margin: 20px auto;
  background: white;
  margin-top: 150px;
  margin-left: 4%;
  position: absolute;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.toolbar {
  margin-bottom: 20px;
}

.search-box {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  color: black;
}

.search-box input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.data-table {
  max-height: 600px;
  overflow-y: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  border: 1px solid #eee;
  text-align: left;
}

th {
  background-color: #f8f9fa;
}

button {
  padding: 6px 12px;
  margin: 2px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  opacity: 0.8;
}

.dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.dialog-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
}

.form-group input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.dialog-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.logotext {
  text-align: center;
  margin: 20px 0;
  color: #333;
}
.data-table {
  min-height: 300px; /* 最小高度保障可见性 */
  overflow-y: auto;
  position: relative; /* 移除绝对定位 */
}

/* 修复表格行高度 */
table tr td {
  padding: 12px;
  line-height: 1.5;
}
.data-table {
  min-height: 400px; /* 确保足够高度 */
  position: static; /* 移除绝对定位 */
}
table {
  border: 1px solid #ddd;
}
th, td {
  border: 1px solid #ddd !important;
}
</style>