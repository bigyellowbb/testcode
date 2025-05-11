<template>
  <div class="nav">
    <ul class="navlist">
      <li class="navlist-li"><a href="#" @click="homeback">首页</a></li>
      <li class="navlist-li" style="box-shadow: inset 3px 3px 5px gray, inset -3px -3px 5px gray;"><a href="#" @click="preback">预测展示</a></li>
      <li class="navlist-li"><a href="#" @click="databack">数据录入</a></li>
      <li class="navlist-li"><a href="#" @click="userback">用户中心</a>
        <ul class="droplist">
          <!-- <li><a href="#">权限管理</a></li> -->
          <li><a href="#" @click="quit">退出登录</a></li>
        </ul>
      </li>
    </ul>
  </div>

  <div class="admin-container">

    <div class="model-switcher">
      <button 
        v-for="model in models"
        :key="model.id"
        :class="{ 'active': activeModel === model.id }"
        @click="switchModel(model.id)"
      >
        {{ model.name }}
      </button>
      <button 
        class="predict-btn"
        @click="runPrediction"
        :disabled="isPredicting"
      >
        <span v-if="isPredicting">预测中...</span>
        <span v-else>开始预测</span>
      </button>
    </div>


    <div class="chart-container">
      <div id="waterLevelChart" style="width: 100%; height: 500px;"></div>
    </div>

    <div class="combined-layout"></div>

    <div class="scheme-generate">
      <div class="scheme-card">
        <h3>推荐调度方案（基于{{ activeModelName }}预测）</h3>
        <div class="scheme-details">
          <!-- <div class="detail-item">
            <span class="label">当前预测流量</span>
            <span class="value">{{ currentFlow }} m³/s</span>
          </div> -->
          <!-- <div class="detail-item" >
            <span class="label">预测峰值流量</span>
            <span class="value">{{ currentData.maxLevel }} m³/s</span>
          </div> -->
          <div class="detail-item" >
            <span class="label">预测峰值流量</span>
            <span class="value">1080.372 m³/s</span>
          </div>
          <div class="detail-item">
            <span class="label">建议泄洪量</span>
            <span class="value">{{ calculatedScheme.discharge }} m³/s</span>
          </div>
          <div class="detail-item">
            <span class="label">风险等级</span>
            <span class="risk-level" :class="calculatedScheme.riskLevel">
              {{ calculatedScheme.riskLevel }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>


    <div class="toggle-panel-btn" @click="toggleUploadPanel">
    <span v-if="showUploadPanel">◀</span>
    <span v-else>▶</span>
    数据上传
  </div>

  <!-- 上传面板增加v-show控制 -->
  <div 
    class="upload-panel"
    v-show="showUploadPanel"
  >
    <!-- 文件上传面板 -->
 
    <div class="form-box upload-box">
      <h3>预测数据上传</h3>
      <form @submit.prevent="handleSubmit">
        <div class="form-group">
          <div class="file-upload-box">
            <input type="file" ref="fileInput" @change="handleFileSelect" accept=".xlsx, .xls, .csv">
            <p class="file-tips">点击选择文件（支持Excel/CSV格式）</p>
            <div v-if="selectedFile" class="file-preview">
              {{ selectedFile.name }} ({{ fileSize }})
            </div>
          </div>
        </div>
        <div class="button-group">
          <button type="submit" class="submit-btn">上传数据</button>
          <button type="button" class="clear-btn" @click="handleClear">
            清空数据
          </button>
        </div>
      </form>
    </div>
    </div>
 



</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import router from '@/router'
import * as XLSX from 'xlsx'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'


const homeback = () => router.push('/home')
const preback = () => router.push('/pre')
const databack = () => router.push('/hyinfo')
const userback = () => router.push('/user')
const quit = () => {
  localStorage.removeItem('token')
  router.replace('/login')
}

// 新增预测相关状态
const isPredicting = ref(false)
// const predictionResult = ref(null)

// 修改后的预测执行方法
const runPrediction = async () => {
  if (isPredicting.value) return
  
  try {
    isPredicting.value = true
    ElMessage.info('预测任务启动中...')
    
    // 调用预测接口
    const { data } = await axios.post(
      'http://localhost:8080/predict/lstm',
      {},
      { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
    )

    if (data.success) {
      ElMessage.success('预测完成，正在获取结果...')
      await fetchLatestResults()
    }
  } catch (error) {
    console.error('预测失败:', error)
    ElMessage.error(`预测失败: ${error.response?.data?.message || error.message}`)
  } finally {
    isPredicting.value = false
  }
}

// 获取最新预测结果
const fetchLatestResults = async () => {
  try {
    const { data } = await axios.get('http://localhost:8080/predict/results', {
      params: { type: activeModel.value === 'lstm' ? 'TEST' : 'TRAIN' },
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })

    if (data.success) {
      processResults(data.results)
    }
  } catch (error) {
    console.error('结果获取失败:', error)
    ElMessage.error('结果加载失败，请手动刷新')
  }
}


// 处理结果数据
const processResults = (rawData) => {
  const sortedData = rawData.sort((a, b) => new Date(a.time) - new Date(b.time))
  
  // 时间格式化
  const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })

  chartData.value = {
    xData: sortedData.map(item => 
      timeFormatter.format(new Date(item.time))
    ),
    yData: sortedData.map(item => item.value),
    maxLevel: Math.max(...sortedData.map(item => item.value))
  }
  
  updateChart()
}
// // 新增预测方法
// const runPrediction = async () => {
//   if (isPredicting.value) return

//   try {
//     isPredicting.value = true
//     ElMessage.info('正在启动预测模型，请稍候...')
    
//     // 调用正确后端接口
//     const { data } = await axios.post(
//       'http://localhost:8080/predict/lstm',
//       {},
//       {
//         headers: {
//           'Authorization': `Bearer ${localStorage.getItem('token')}`
//         }
//       }
//     )

//     if (data.success) {
//       ElMessage.success('预测任务已提交，正在获取结果...')
      
//       // 获取最新预测结果
//       const resultsResponse = await axios.get(
//         'http://localhost:8080/predict/results',
//         {
//           headers: {
//             'Authorization': `Bearer ${localStorage.getItem('token')}`
//           }
//         }
//       )

//       // 处理预测结果
//       if (resultsResponse.data.success) {
//         const predictions = resultsResponse.data.results
//         updateChartData(predictions)
//         ElMessage.success('预测结果已更新！')
//       }
//     }
//   } catch (error) {
//     console.error('预测流程失败:', error)
//     ElMessage.error(`操作失败: ${error.response?.data?.message || error.message}`)
//   } finally {
//     isPredicting.value = false
//   }
// }
  
//   try {
//     isPredicting.value = true
//     ElMessage.info('正在调用预测模型，请稍候...')
    
//     // 调用后端预测接口
//     const response = await axios.post(
//       'http://localhost:8080/api/predict', 
//       { model: activeModel.value },
//       {
//         headers: {
//           'Authorization': `Bearer ${localStorage.getItem('token')}`
//         }
//       }
//     )
    
//     if (response.data.success) {
//       predictionResult.value = response.data.result
//       // 更新图表数据
//       presetData[activeModel.value] = {
//         ...presetData[activeModel.value],
//         yData: response.data.predictionData,
//         maxLevel: response.data.maxLevel
//       }
//       updateChart()
//       ElMessage.success('预测完成！')
//     }
//   } catch (error) {
//     console.error('预测失败:', error)
//     ElMessage.error(`预测失败: ${error.response?.data?.message || error.message}`)
//   } finally {
//     isPredicting.value = false
//   }
// }

// 模型
const models = ref([
  { id: 'lstm', name: 'LSTM' },
  // { id: 'vmd-lstm', name: 'VMD-LSTM' }
])

// 数据调用（静态）（后面修改）
const presetData = {
  lstm: {
    xData: [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319,320,321,322,323,324,325,326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,343,344,345,346,347,348,349,350,351,352,353,354,355,356,357,358,359,360,361,362,363,364,365,366,367,368,369,370,371,372,373,374,375,376,377,378,379,380,381,382,383,384,385,386,387,388,389,390,391,392,393,394,395,396,397,398,399,400,401,402,403,404,405,406,407,408,409,410,411,412,413,414,415,416,417,418,419,420,421,422,423,424,425,426,427,428,429,430,431,432,433,434,435,436,437,438,439,440,441,442,443,444,445,446,447,448,449,450,451,452,453,454,455,456,457,458,459,460,461,462,463,464,465,466,467,468,469,470,471,472,473,474,475,476,477,478,479,480,481,482,483,484,485,486,487,488,489,490,491,492,493,494,495,496,497,498,499,500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,515,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,540,541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,571,572,573,574,575,576,577,578,579,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595],
    yData: [963.6166,930.7629,945.6843,1052.882,1080.372,1020.109,854.711,733.4116,646.5985,597.1586,645.8935,645.0114,568.6864,532.7775,521.5952,426.5671,437.5805,424.4985,587.2219,501.4655,485.2585,484.5258,481.0499,447.3344,425.4141,444.6909,260.3057,235.5735,244.4616,236.0732,229.6268,257.657,232.6494,216.1854,278.9663,265.9504,244.4245,249.3163,223.1641,189.9247,165.1561,153.7552,208.8046,172.6866,183.0261,186.2594,148.1487,121.1181,160.2846,149.2655,169.8042,162.7773,172.9592,183.6019,118.9152,120.2432,137.2377,133.0494,118.1059,115.3808,114.9796,222.2278,216.1135,244.7899,178.7028,138.5042,125.7723,142.9246,142.1434,120.723,120.33,109.3114,117.2905,101.9375,103.0975,92.65546,91.92996,97.17278,129.4802,135.7381,121.3391,123.4294,120.5172,98.80466,90.15011,182.079,218.0192,197.3411,225.2287,273.7005,347.7843,301.1094,332.6939,350.2593,352.8726,406.4873,360.0486,342.2635,303.8209,317.0659,333.7433,321.4261,357.9671,388.6136,323.4832,292.1411,267.1574,260.0963,262.8494,261.5342,254.3192,245.3851,243.1201,215.9309,189.2605,187.2746,184.7208,186.153,195.5426,199.3784,206.8845,212.2662,217.4679,194.1912,173.6511,167.0392,191.3548,180.8308,181.8918,191.4665,189.2233,173.6058,171.2316,173.5088,186.7198,175.033,161.9752,186.4858,173.7588,175.2615,181.8156,184.1209,199.9859,196.9098,180.5596,159.7512,168.5137,196.1039,184.3795,171.4118,171.9702,170.8399,167.239,171.4552,172.4547,160.9152,144.3551,149.1395,131.4931,121.2733,112.0833,117.2489,96.77744,109.8214,106.576,112.9201,107.6203,101.3504,103.8551,114.0871,102.4597,104.7494,88.27035,102.2596,104.9265,105.7795,106.1723,113.0229,113.8015,112.611,90.60805,95.54655,98.53561,89.59991,94.80696,96.02027,96.02921,91.61207,88.73988,79.6297,78.1188,73.36364,79.68141,72.93169,82.86986,96.36145,103.1913,93.94081,99.67316,108.6702,95.12474,89.3164,98.72206,117.9865,109.5513,104.0897,123.3013,116.0141,126.0744,140.953,142.4488,133.1094,118.9245,108.7985,115.3977,110.5651,96.61284,92.71064,96.14565,101.6328,116.2651,150.2993,138.1626,133.8017,122.5488,94.603,106.4055,132.0907,141.446,139.282,138.041,131.3642,123.4569,102.6004,129.7055,124.049,118.268,124.7696,120.3562,132.4242,127.656,150.997,143.1802,137.2675,162.4255,181.8668,182.2704,171.9725,211.6687,259.1997,210.0851,256.1909,259.9965,278.0472,343.0611,378.9716,350.7805,376.7505,400.881,401.1751,409.6368,443.5178,442.6916,421.5085,447.2809,563.3366,577.0281,600.0284,581.8051,575.5215,562.0095,518.0579,500.5326,491.4566,500.9353,606.9153,463.0346,435.7859,465.8932,392.7178,349.9518,381.7527,495.5145,494.7567,498.071,483.6419,472.5008,463.8561,440.4496,403.2498,339.1672,307.9081,305.6844,289.9383,388.4085,335.2298,285.4597,320.8474,310.8525,298.5601,294.323,401.0587,351.9201,318.4705,295.8206,267.3635,280.4404,255.4776,261.9686,305.0959,313.7917,333.807,328.7034,301.3836,362.9937,315.5738,309.6332,270.1576,232.9183,252.2435,253.3143,258.7744,273.3316,264.5548,269.7426,301.0689,346.054,416.7364,466.6587,488.1601,474.6422,491.0558,507.8293,499.0021,502.6491,483.4318,466.0724,468.3987,462.4308,465.2115,453.1704,453.6527,399.0562,360.0856,286.6793,291.8503,269.0921,264.4963,231.393,263.2424,233.7598,224.6552,218.571,215.0306,179.5119,224.4277,269.3099,278.8806,261.9258,256.2632,280.0959,257.1684,247.3658,231.5908,248.5292,250.8772,335.8727,488.3716,589.9993,586.2476,647.5056,630.8521,464.6598,428.3579,363.3067,323.1592,274.7269,341.9841,325.9958,310.186,314.7897,336.8381,331.527,335.6309,320.8372,266.0134,241.5073,216.1144,214.0991,197.2472,203.9863,218.0846,258.4911,270.1735,254.7857,243.3029,229.6378,233.2826,255.4155,245.7824,234.1106,212.6778,216.2764,186.6296,187.6048,132.1957,182.737,184.3524,177.0775,181.326,150.6835,180.551,207.8447,201.7582,187.7208,165.2429,189.0749,174.4735,197.774,193.7221,158.4767,154.7912,144.4042,142.9761,123.5549,124.6069,144.679,154.2055,140.554,120.9841,132.1065,142.7547,202.7779,218.7543,227.2843,194.1348,323.1047,431.8211,427.4958,356.7141,358.7508,283.3289,330.5972,364.1754,405.638,425.6437,409.5849,300.5844,303.279,332.101,278.7566,355.1469,403.3832,356.8108,448.1561,483.1072,512.4661,382.504,344.9315,373.9406,435.9232,499.5991,504.4809,471.0837,466.6031,432.9378,321.5894,398.5882,329.1466,414.6562,353.5782,375.9123,430.3324,344.3589,307.0669,290.2325,310.2725,349.0151,342.8268,400.0747,434.665,576.7463,580.4536,674.7271,588.1065,576.9942,631.1263,678.1276,746.3488,565.1444,550.2094,668.5518,783.2822,814.5133,796.062,567.0624,483.4453,432.8325,333.775,293.9642,274.6362,267.0593,287.6826,340.1279,386.8082,433.393,419.3326,406.0566,406.078,390.2038,387.5727,346.251,309.3797,286.5155,283.1963,268.127,261.6742,252.3042,252.5701,259.3217,278.6852,264.7841,270.0243,287.4106,283.1494,295.9148,280.7509,297.172,291.0669,268.8067,255.732,260.5565,284.903,283.5515,297.8919,302.4245,312.903,336.33,351.6275,336.5828,330.7196,311.48,326.3273,327.8025,306.8972,323.0536,339.9136,330.696,339.8006,348.1054,345.1204,356.373,332.3093,345.6926,361.0595,372.7767,348.1955,349.1719,342.8363,328.2057,312.2704,313.8226,309.532,321.6644,314.1422,336.6894,346.8631,359.2953,347.0274,329.4568,314.8464,290.8929,316.3574,287.8282,286.3698,320.4617,310.7736,268.1307,247.8771,255.7043,286.9799,239.8266,254.3706,312.8635,292.119,227.9013,222.3692,258.1427,273.1641,270.1545,252.0438,198.5912,202.8313,272.1432,241.5264],
    maxLevel: 0.00,
    trend: '',
    riseRate: 0.0
  },
  'vmd-lstm': {
    xData: [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319,320,321,322,323,324,325,326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,343,344,345,346,347,348,349,350,351,352,353,354,355,356,357,358,359,360,361,362,363,364,365,366,367,368,369,370,371,372,373,374,375,376,377,378,379,380,381,382,383,384,385,386,387,388,389,390,391,392,393,394,395,396,397,398,399,400,401,402,403,404,405,406,407,408,409,410,411,412,413,414,415,416,417,418,419,420,421,422,423,424,425,426,427,428,429,430,431,432,433,434,435,436,437,438,439,440,441,442,443,444,445,446,447,448,449,450,451,452,453,454,455,456,457,458,459,460,461,462,463,464,465,466,467,468,469,470,471,472,473,474,475,476,477,478,479,480,481,482,483,484,485,486,487,488,489,490,491,492,493,494,495,496,497,498,499,500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,515,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,540,541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,571,572,573,574,575,576,577,578,579,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595],
    yData: [0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00
],
    maxLevel: 0.00,
    trend: '',
    riseRate: 0.0
  }
}

// 动态数据格式
const chartData = ref({
  xData: [],
  yData: [],
  maxLevel: 0
})

// 新增当前流量响应式变量
const currentFlow = ref(null)



// 更新图表数据方法
const updateChartData = (predictions) => {
  const sortedData = predictions.sort((a, b) => a.timestamp - b.timestamp)
  
  chartData.value = {
    xData: sortedData.map(item => 
      new Date(item.timestamp).toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit'
      })
    ),
    yData: sortedData.map(item => item.predictedValue),
    maxLevel: Math.max(...sortedData.map(item => item.predictedValue))
  }
  
  updateChart()
}

const activeModel = ref('lstm')
let chartInstance = null

const activeModelName = computed(() => 
  models.value.find(m => m.id === activeModel.value)?.name || ''
)

const currentData = computed(() => 
  presetData[activeModel.value] || { xData: [], yData: [] }
)

const calculatedScheme = computed(() => {
  const { maxLevel, riseRate } = currentData.value
  return {
    discharge: calculateDischarge(maxLevel, riseRate),
    duration: calculateDuration(currentData.value.yData),
    riskLevel: calculateRisk(maxLevel)
  }
})

//调度
const calculateDischarge = (level, rate) => {
  if (level > 1000 || rate > 0.6) return 0
  return level > 28.0 ? 0 : 380.372
}

const calculateDuration = (yData) => {
  const peakIndex = yData.indexOf(Math.max(...yData))
  return (yData.length - peakIndex) * 0
}

const calculateRisk = (level) => {
  if (level > 1000) return '高'
  if (level > 800) return '中'
  return '中'
}

// 修改计算方法
// const calculateDischarge = (flow) => {
//   if (!flow) return 0
//   return flow > 1000 ? 500 : flow > 500 ? 300 : 200
// }

// const calculateDuration = (yData) => {
//   const currentIndex = yData.indexOf(currentFlow.value)
//   return currentIndex > -1 ? (yData.length - currentIndex) * 0.5 : 0
// }

// const calculateRisk = (flow) => {
//   if (!flow) return '-'
//   if (flow > 1000) return '极高风险'
//   if (flow > 800) return '高风险'
//   if (flow > 600) return '中风险'
//   return '低风险'
// }

// 图表初始化
// const initChart = () => {
//   const chartDom = document.getElementById('waterLevelChart')
//   if (chartInstance) chartInstance.dispose()
  
//   chartInstance = echarts.init(chartDom)
//   updateChart()
// }
const initChart = () => {
  const chartDom = document.getElementById('waterLevelChart')
  if (chartInstance) chartInstance.dispose()
  
  chartInstance = echarts.init(chartDom)
  
   // 修改为监听全局鼠标移动事件
   chartInstance.on('mousemove', (params) => {
    if (params.componentType === 'series') return // 避免重复处理
    
    // 获取鼠标坐标
    const point = [params.offsetX, params.offsetY]
    
    // 转换坐标到x轴数值
    const xAxis = chartInstance.convertFromPixel({ seriesIndex: 0 }, point)[0]
    
    // 找到最近的数据点
    const data = currentData.value.xData.map((x, index) => ({
      x: index,  // 使用索引代替时间（根据实际情况调整）
      y: currentData.value.yData[index]
    }))
    
    // 寻找最近的数据点索引
    const closestIndex = findNearestIndex(data, xAxis)
    
    if (closestIndex !== -1) {
      currentFlow.value = data[closestIndex].y
    }
  })
  
  // 添加鼠标移出事件
  chartInstance.on('globalout', () => {
    currentFlow.value = null
  })

  updateChart()
}

// 辅助函数：找到最近的索引
const findNearestIndex = (data, targetX) => {
  let minDistance = Infinity
  let closestIndex = -1
  
  data.forEach((item, index) => {
    const distance = Math.abs(item.x - targetX)
    if (distance < minDistance) {
      minDistance = distance
      closestIndex = index
    }
  })
  
  return closestIndex
}

//
const updateChart = () => {
  const option = {
    title: {
      text: `流量预测 - ${activeModelName.value}`,
      left: 'center',
      textStyle: {
        color: '#333',
        fontSize: 18
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: params => `
        时间：${params[0].axisValue}<br/>
        流量：${params[0].data}m³/s
      `
    },
    xAxis: {
      type: 'category',
      data: currentData.value.xData,
      axisLabel: {
        color: '#666'
      }
    },
    yAxis: {
      type: 'value',
      name: '流量（）',
      min: 0,
      max: 1300,
      axisLabel: {
        color: '#666'
      }
    },
    series: [{
      data: currentData.value.yData,
      type: 'line',
      smooth: true,
      lineStyle: {
        width: 3,
        color: '#36A3FF'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(54, 163, 255, 0.3)' },
          { offset: 1, color: 'rgba(54, 163, 255, 0.05)' }
        ])
      },
      symbol: 'circle',
      symbolSize: 8,
      itemStyle: {
        color: '#36A3FF'
      }
    }]
  }
  
  chartInstance.setOption(option)
}

const switchModel = (modelId) => {
  activeModel.value = modelId
  nextTick(() => {
    updateChart()
    chartInstance.resize()
  })
}

onMounted(() => {
  nextTick(initChart)
  window.addEventListener('resize', () => chartInstance?.resize())
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', () => chartInstance?.resize())
  chartInstance?.dispose()
})


// 初始化时加载历史数据
onMounted(async () => {
  try {
    const { data } = await axios.get(
      'http://localhost:8080/predict/results',
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      }
    )
    
    if (data.success && data.results.length > 0) {
      updateChartData(data.results)
    }
  } catch (error) {
    console.error('初始化数据获取失败:', error)
  }
  
  nextTick(initChart)
  window.addEventListener('resize', () => chartInstance?.resize())
})



//文件上传
// 文件上传相关
const selectedFile = ref(null)
const fileInput = ref(null)

// 计算文件大小显示
const fileSize = computed(() => {
  if (!selectedFile.value) return ''
  const bytes = selectedFile.value.size
  if (bytes > 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${(bytes / 1024).toFixed(1)} KB`
})

// 处理文件选择
const handleFileSelect = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件类型
  const validTypes = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel',
    'text/csv'
  ]
  if (!validTypes.includes(file.type)) {
    ElMessage.error('仅支持.xlsx、.xls和.csv文件')
    return resetFileInput()
  }

  // 验证文件大小（限制10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过10MB')
    return resetFileInput()
  }

  selectedFile.value = file
}

// 提交表单处理
const handleSubmit = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  try {
    // 读取文件内容
    let csvData = []
    const fileExtension = selectedFile.value.name.split('.').pop().toLowerCase()

    if (fileExtension === 'csv') {
      csvData = await readCSVFile(selectedFile.value)
    } else {
      const jsonData = await readExcelFile(selectedFile.value)
      csvData = convertToCSV(jsonData)
    }

    // 创建FormData对象
    const formData = new FormData()
    const csvBlob = new Blob([csvData], { type: 'text/csv' })
    formData.append('file', csvBlob, 'converted.csv')

    // 发送请求
    const response = await axios.post('http://localhost:8080/predata/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    if (response.data.success) {
      ElMessage.success(`成功导入 ${response.data.insertedCount} 条数据`)
      resetFileInput()
    }
  } catch (error) {
    ElMessage.error(`上传失败: ${error.response?.data?.error || error.message}`)
  }
}

// 清空数据库
const handleClear = async () => {
  if (!confirm('确定要清空所有预测数据吗？此操作不可撤销！')) return
  
  try {
    const response = await axios.delete('http://localhost:8080/predata/clear', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })
    
    if (response.data.success) {
      ElMessage.success('数据库已清空')
    }
  } catch (error) {
    ElMessage.error(`清空失败: ${error.response?.data?.message || error.message}`)
  }
}

// 文件处理工具函数
const readExcelFile = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const data = new Uint8Array(e.target.result)
      const workbook = XLSX.read(data, { type: 'array' })
      const sheet = workbook.Sheets[workbook.SheetNames[0]]
      const jsonData = XLSX.utils.sheet_to_json(sheet)
      
      // 数据格式转换
      const processedData = jsonData.map(item => ({
        waterLevel: Number(item.water_level) || 0,
        sandContent: Number(item.sand_content) || 0,
        dewPoint: Number(item.dew_point) || 0,
        internetTraffic: Number(item.internet_traffic) || 0
      }))
      
      resolve(processedData)
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

const readCSVFile = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const csvString = e.target.result
      resolve(csvString)
    }
    reader.onerror = reject
    reader.readAsText(file)
  })
}

const convertToCSV = (data) => {
  const header = 'water_level,sand_content,dew_point,internet_traffic\n'
  const rows = data.map(item => 
    `${item.waterLevel},${item.sandContent},${item.dewPoint},${item.internetTraffic}`
  )
  return header + rows.join('\n')
}

const resetFileInput = () => {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
}

// 增加状态控制
const showUploadPanel = ref(true)

const toggleUploadPanel = () => {
  showUploadPanel.value = !showUploadPanel.value
}
</script>

<style scoped>
.admin-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.model-switcher {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin: 20px 0;
}

.model-switcher button {
  padding: 12px 30px;
  border: 2px solid #e4e7ed;
  border-radius: 25px;
  background: #fff;
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
}

.model-switcher button.active {
  border-color: #36A3FF;
  background: rgba(54, 163, 255, 0.1);
  color: #36A3FF;
}

.chart-container {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 20px;
  margin: 20px 0;
}


.scheme-generate {
  margin-top: 30px;
}

.scheme-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.scheme-card h3 {
  color: #333;
  margin-bottom: 20px;
  font-size: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.scheme-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.detail-item {
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.detail-item .label {
  color: #666;
  font-size: 14px;
  margin-bottom: 6px;
}

.detail-item .value {
  color: #333;
  font-weight: 600;
  font-size: 16px;
}

.risk-level {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 15px;
  font-size: 12px;
}

.risk-level.高 {
  background: #f56c6c;
  color: white;
}

.risk-level.中 {
  background: #e6a23c;
  color: white;
}

.risk-level.低 {
  background: #67c23a;
  color: white;
}
.nav {
  height: 50px;
  background-color: black;
  box-shadow: 0 1px 2px gray;
  position: relative;
  z-index: 5; 
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


input[type="file"] {
  opacity: 0;
  position: absolute;
  cursor: pointer;
}

.file-tips {
  font-size: 12px;
  color: #999;
  margin-top: 10px;
}

.submit-btn {
  background: #36A3FF;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.submit-btn:hover {
  opacity: 0.9;
}

.button-group {
  margin-top: 15px;
  text-align: center;
}
/* 新增样式 */
.file-preview {
  margin-top: 10px;
  padding: 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.clear-btn {
  background: #ff4d4f;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  transition: opacity 0.3s;
}

.clear-btn:hover {
  opacity: 0.9;
}

.button-group {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.submit-btn {
  background: #36A3FF;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  cursor: pointer;
  transition: opacity 0.3s;
}

/* 文件上传区域优化 */
.file-upload-box {
  border: 2px dashed #e0e7ff;
  border-radius: 10px;
  background: #f8faff;
  min-height: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  transition: all 0.3s ease;
}

.file-upload-box.has-file {
  border-color: #36A3FF;
  background: rgba(54, 163, 255, 0.05);
}

.upload-content {
  padding: 20px;
  text-align: center;
  z-index: 1;
}

.upload-icon {
  margin-bottom: 12px;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.file-upload-box:hover .upload-icon {
  opacity: 1;
}

.file-tips {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.file-format {
  font-size: 12px;
  color: #999;
}

/* 文件预览样式 */
.file-preview {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  padding: 12px;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.file-info {
  flex: 1;
  overflow: hidden;
}

.file-name {
  display: block;
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-size {
  font-size: 12px;
  color: #999;
}

.remove-btn {
  background: none;
  border: none;
  color: #ff4d4f;
  font-size: 20px;
  cursor: pointer;
  padding: 0 8px;
  transition: opacity 0.3s;
}

.remove-btn:hover {
  opacity: 0.8;
}

/* 按钮组优化 */
.button-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 24px;
}

.submit-btn, .clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: none;
  cursor: pointer;
}

.submit-btn {
  background: #36A3FF;
  color: white;
}

.submit-btn:hover {
  background: #2487d7;
  box-shadow: 0 4px 12px rgba(54, 163, 255, 0.3);
}

.clear-btn {
  background: #ff4d4f;
  color: white;
}

.clear-btn:hover {
  background: #eb3b3d;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
}

.btn-text {
  margin-right: 8px;
}

.btn-icon {
  font-weight: 700;
}
/* 新增切换按钮样式 */
.toggle-panel-btn {
  position: fixed;
  right: 0;
  bottom: 120px;
  background: #36A3FF;
  color: white;
  padding: 12px 20px;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  box-shadow: -2px 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 999;
}


.toggle-panel-btn:hover {
  background: #2487d7;
  transform: translateX(-4px);
}

.toggle-panel-btn span {
  font-weight: bold;
  transition: transform 0.3s;
}

/* 优化已有上传面板定位 */
.upload-panel {
  position: fixed;
  right: 0;
  bottom: 80px;
  transition: transform 0.3s ease;
  transform: translateX(0);
}

.upload-panel.hidden {
  transform: translateX(110%);
}
.upload-panel {
  position: fixed;
  right: 0;
  bottom: 80px;
  width: 360px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 24px;
  z-index: 1000;
  transition: transform 0.3s ease;
  transform: translateX(0);
}

.toggle-panel-btn {
  position: fixed;
  right: 0;
  bottom: 140px;  /* 调整按钮位置 */
  z-index: 1001;  /* 确保按钮在上层面级 */
}
.toggle-panel-btn {
  position: fixed;
  right: 0;
  bottom: 20px;  /* 调整到页面最下方 */
  background: #36A3FF;
  color: white;
  padding: 12px 20px;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  box-shadow: -2px 2px 8px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 1001;
}

/* 调整上传面板定位 */
.upload-panel {
  position: fixed;
  right: 20px;
  bottom: 120px;  /* 保持在按钮上方 */
  width: 360px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  padding: 24px;
  z-index: 1000;
  transition: transform 0.3s ease;
}
/* 新增预测按钮样式 */
.model-switcher {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin: 20px 0;
  flex-wrap: wrap; /* 允许按钮换行 */
}

.predict-btn {
  padding: 12px 30px;
  border: 2px solid #e4e7ed; /* 默认边框颜色与其他按钮一致 */
  border-radius: 25px;
  background: white; /* 默认白色背景 */
  color: #333; /* 黑色文字 */
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 500;
  margin-left: 20px;
}

.predict-btn:hover:not(:disabled) {
  background: #4CAF50; /* 悬停时绿色背景 */
  border-color: #4CAF50;
  color: white; /* 悬停时白色文字 */
}

.predict-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  background: #f5f5f5; /* 禁用状态浅灰色背景 */
  color: #999; /* 禁用状态灰色文字 */
}
</style>