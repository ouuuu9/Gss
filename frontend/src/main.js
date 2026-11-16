import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误兜底：组件渲染/生命周期异常都记录原始错误（含 info 与组件实例），
// 同时给用户一句可读提示，避免整页白屏且没有任何反馈；不静默吞掉错误。
app.config.errorHandler = (err, instance, info) => {
  console.error('[全局错误]', err, '\n出错位置:', info, '\n组件实例:', instance)
  ElMessage.error('页面出错，请刷新重试')
}

app.mount('#app')
