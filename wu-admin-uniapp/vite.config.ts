import { defineConfig } from 'vite'
import _uni from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

// 兼容 ESM 与 CJS 下 Vite 插件导出的互操作
const uni = (typeof _uni === 'function' ? _uni : (_uni as any)?.default || _uni) as typeof _uni

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api'],
        additionalData: `@use "@/styles/variables.scss" as *;\n@use "@/styles/mixins.scss" as *;\n`,
      },
    },
  },
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
