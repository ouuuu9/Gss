import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    // 大依赖已 manualChunks 分包（element/echarts 为库固有体积且长期缓存），放宽告警阈值
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        // 大依赖独立分包：框架/组件库/图表库与业务代码分离，利于浏览器缓存且消除单 chunk 体积警告
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia', 'axios'],
          element: ['element-plus', '@element-plus/icons-vue'],
          echarts: ['echarts']
        }
      }
    }
  },
  server: {
    port: 5173,
    host: true,
    open: true,
    allowedHosts: true,
    proxy: {
      // 后端业务接口
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // WebSocket 大屏实时推送
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      },
      // Swagger 页面资源
      '/swagger-ui': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // OpenAPI接口描述文档
      '/v3/api-docs': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
