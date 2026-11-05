import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 1. 引入按需引入相关的插件
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from '@vant/auto-import-resolver'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 2. 配置自动导入 Vue 内置 API 和 Vant 组件
    AutoImport({
      resolvers: [VantResolver()],
    }),
    // 3. 配置自动导入 Vant 组件及其样式
    Components({
      resolvers: [VantResolver()],
    }),
  ],
})