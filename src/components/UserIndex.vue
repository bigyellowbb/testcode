<template>
  <div class="nav">
    <ul class="navlist">
      <li class="navlist-li"><a href="#" @click="homeback">首页</a></li>
      <li class="navlist-li"><a href="#" @click="preback">预测展示</a></li>
      <li class="navlist-li"><a href="#" @click="databack">数据录入</a></li>
      <li class="navlist-li"  style="box-shadow: inset 3px 3px 5px gray, inset -3px -3px 5px gray;"><a href="#" @click="userback">用户中心</a>
        <ul class="droplist">
          <!-- <li><a href="#">权限管理</a></li> -->
          <li><a href="#" @click="quit">退出登录</a></li>
        </ul>
      </li>
    </ul>
  </div>

  <div class="user-container">
    <div class="user-card profile-card">
      <div class="profile-info">
        <h2>{{ user.username }}
          <span class="role-tag" :class="'role-' + user.role">
            {{ roleDisplayName[user.role] }}
          </span>
        </h2>
        <!-- <p>注册时间: {{ formatDate(user.createTime) }}</p> -->
        <div class="stats">
          <div class="stat-item">
            <!-- <span class="stat-value">{{ user.operationCount }}</span> -->
            <!-- <span class="stat-label">操作次数</span> -->
          </div>
        </div>
      </div>
    </div>

    <!-- 权限管理入口 -->
    <div v-if="user.role === 'admin'" class="user-card permission-card">
      <div class="section-header">
        <h3><i class="icon-shield"></i> 权限管理</h3>
        <button class="manage-btn" @click="showPermissionDialog = true">
          打开管理面板
        </button>
      </div>
    </div>

    <div class="user-card">
      <h3 class="card-title"><i class="icon-security"></i> 账户安全</h3>
      <ul class="security-list">
        <li>
          <span>登录密码</span>
          <button class="btn-modify" @click="showPasswordDialog">修改</button>
        </li>
        <li>
        <span>绑定手机</span>
        <span class="bind-info">{{ user.mobile || '未绑定' }}</span>
        <button 
          class="btn-modify" 
          @click="openMobileDialog"
          :disabled="isModifying"
        >
          {{ isModifying ? '保存中...' : '修改' }}
        </button>
      </li>
        <li>
          <span>绑定邮箱</span>
          <span class="bind-info">{{ user.email || '未绑定' }}</span>
        </li>
      </ul>
      <!-- 手机修改弹窗 -->
    <div v-if="showMobileDialog" class="mobile-dialog">
      <div class="dialog-content">
        <h3>修改绑定手机</h3>
        <div class="form-group">
          <label>新手机号：</label>
          <input
            v-model="newMobile"
            type="tel"
            placeholder="请输入11位手机号"
            maxlength="11"
          />
        </div>
        <div class="dialog-buttons">
          <button class="cancel-btn" @click="closeDialog">取消</button>
          <button class="confirm-btn" @click="submitMobile">确认修改</button>
        </div>
      </div>
    </div>
    </div>

    <div class="user-card">
      <h3 class="card-title"><i class="icon-log"></i> 最近操作</h3>
      <ul class="log-list">
        <li v-for="(log, index) in operationLogs" :key="index">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-action">{{ log.action }}</span>
          <span class="log-ip">{{ log.ip }}</span>
        </li>
      </ul>
    </div>

    <!-- 权限管理弹窗 -->
    <div v-if="showPermissionDialog" class="permission-dialog">
      <div class="dialog-content">
        <h3>用户权限管理</h3>
        <div class="search-box">
          <input 
            v-model="searchQuery"
            placeholder="搜索用户..."
            type="search"
            class="search-input"
          >
        </div>
        <table class="user-table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>当前权限</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filteredUsers" :key="u.id">
              <td>{{ u.username }}</td>
              <td>
                <select 
                  v-model="u.role" 
                  :disabled="u.id === user.id"
                  class="role-select"
                >
                  <option value="admin">管理员</option>
                  <option value="user">普通用户</option>
                  <option value="guest">游客</option>
                </select>
              </td>
              <td>
                <button 
                  class="save-btn"
                  @click="savePermission(u)" 
                  :disabled="u.id === user.id || !isModified(u)"
                >
                  {{ isModified(u) ? '保存' : '已保存' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <button class="close-btn" @click="closePermissionDialog">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import router from '@/router'


const homeback = () => router.push('/home')
const preback = () => router.push('/pre')
const databack = () => router.push('/hyinfo')
const userback = () => router.push('/user')
const quit = () => {
  localStorage.removeItem('token')
  router.replace('/login')
}

// 新增操作日志声明
const operationLogs = ref([])

// 用户数据初始化
const user = ref({
  id: 0,
  username: '32101204',
  role: 'admin',
  mobile: '',
  email: '3044953443@qq.com'
})

const allUsers = ref([])
const originalUsers = ref([])

onMounted(async () => {
  try {
    // 获取当前用户（带认证头）
    const userRes = await axios.get('http://localhost:8080/current-user', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        'X-CSRF-Token': localStorage.getItem('csrfToken') // 假设 CSRF 令牌存储在 localStorage
      }
    });
        // 请求头验证
    console.log('请求头:', {
      Authorization: `Bearer ${localStorage.getItem('token')}`
    });
    

    if (!localStorage.getItem('token')) {
      alert('未登录，请先登录');
      router.push('/login');
      return;
    }

    // 处理401未授权
    if (userRes.data.code === 401) {
      alert('登录已过期');
      router.push('/login');
      return;
    }

    if (userRes.data.code === 403) {
      alert('您没有权限访问此资源');
      router.push('/login'); // 或跳转到无权访问页面
      return;
    }

    console.log('后端返回的数据:', userRes.data);

    // 更新用户数据
    // user.value = userRes.data;
    user.value = userRes.data.data; // 假设后端格式为 { data: { ...user } }

    // 获取操作日志（合并到此处）
    const logsRes = await axios.get('http://localhost:8080/api/operation-logs', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`
      }
    });
    operationLogs.value = logsRes.data;

    // 如果是管理员，加载用户列表（带认证头）
    if (user.value.role === 'admin') {
      const usersRes = await axios.get('http://localhost:8080/users', {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      });
      allUsers.value = usersRes.data;
      originalUsers.value = JSON.parse(JSON.stringify(usersRes.data));
    }
  } catch (error) {
    console.error('初始化失败:', error);
    alert('系统错误，请刷新重试');
  }
});

// 权限映射
const roleDisplayName = {
  admin: '管理员',
  user: '普通用户',
  guest: '游客'
}

// 权限管理
const showPermissionDialog = ref(false)
const searchQuery = ref('')

// 过滤用户列表？
const filteredUsers = computed(() => {
  return allUsers.value.filter(u => 
    u.username.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// 修改保存权限方法
// const savePermission = async (user) => {
//   try {
//     await axios.put(`/users/${user.id}/role`, { role: user.role })
//     const index = originalUsers.value.findIndex(u => u.id === user.id)
//     originalUsers.value[index].role = user.role
//   } catch (error) {
//     console.error('保存失败:', error)
//   }
// }
// 修改后（真实请求）：
const savePermission = async (user) => {
  try {
    // 调用真实API接口，添加认证头
    await axios.put(
      `http://localhost:8080/users/${user.id}/role`,
      { role: user.role },
      {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      }
    );
    
    // 更新原始数据
    const index = originalUsers.value.findIndex(u => u.id === user.id);
    if (index !== -1) {
      originalUsers.value[index].role = user.role;
    }
  } catch (error) {
    console.error('保存失败:', error);
    alert('权限更新失败，请检查网络或权限');
  }
}
// 修改后（正确代码）
const isModified = (user) => { // 参数名改为 user
  const originalUser = originalUsers.value.find(u => u.id === user.id) // 变量名改为 originalUser
  return originalUser ? originalUser.role !== user.role : false
}

const closePermissionDialog = () => {
  searchQuery.value = ''
  showPermissionDialog.value = false
}
// 手机修改相关状态
const showMobileDialog = ref(false)
const newMobile = ref('')
const isModifying = ref(false)

// 打开弹窗
const openMobileDialog = () => {
  showMobileDialog.value = true
  newMobile.value = user.value.mobile.replace(/\D/g, '') // 去除掩码字符
}

// 关闭弹窗
const closeDialog = () => {
  showMobileDialog.value = false
  newMobile.value = ''
}

// 提交修改
const submitMobile = async () => {
  if (!validateMobile()) return
  
  isModifying.value = true
  try {
    // 模拟API请求
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    user.value.mobile = formatDisplayMobile(newMobile.value)
    closeDialog()
    console.log('手机号修改成功:', newMobile.value)
  } finally {
    isModifying.value = false
  }
}

// 手机号验证
const validateMobile = () => {
  const valid = /^1[3-9]\d{9}$/.test(newMobile.value)
  if (!valid) alert('请输入有效的手机号码')
  return valid
}

// 格式化显示
const formatDisplayMobile = (mobile) => {
  return mobile.replace(/(\d{3})(\d{4})(\d{4})/, '$1​**​​**​$3')
}


</script>

<style scoped>
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

.user-container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

.user-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.profile-card {
  border-left: 4px solid #3498db;
}

.permission-card {
  border-left: 4px solid #2ecc71;
}

.role-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 15px;
  font-size: 0.8em;
  margin-left: 10px;
}

.role-admin {
  background: #e74c3c;
  color: white;
}

.role-user {
  background: #3498db;
  color: white;
}

.role-guest {
  background: #95a5a6;
  color: white;
}

.permission-dialog {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  padding: 25px;
  border-radius: 10px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}

.search-box {
  margin: 15px 0;
}

.search-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
}

.user-table {
  width: 100%;
  margin: 15px 0;
  border-collapse: collapse;
}

.user-table th,
.user-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.role-select {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
}

.save-btn {
  padding: 6px 15px;
  background: #27ae60;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.save-btn:disabled {
  background: #95a5a6;
  cursor: not-allowed;
}

.close-btn {
  float: right;
  padding: 8px 20px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 15px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.manage-btn {
  background: #2ecc71;
  color: white;
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.manage-btn:hover {
  background: #27ae60;
}

.security-list li {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.btn-modify {
  background: #3498db;
  color: white;
  padding: 6px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.log-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
}

.log-time {
  width: 80px;
}

.log-ip {
  color: #95a5a6;
}

@media (max-width: 768px) {
  .dialog-content {
    width: 95%;
    padding: 15px;
  }
  
  .user-table th,
  .user-table td {
    padding: 8px;
    font-size: 0.9em;
  }
  
  .manage-btn {
    padding: 6px 15px;
  }
}
</style>