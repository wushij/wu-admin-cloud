import { defineConfig } from 'vitest/config'
import path from 'node:path'

/**
 * 独立 Vitest 配置：不加载 @dcloudio/vite-plugin-uni，避免单测拉起整套 uni 编译链。
 */
export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    environment: 'happy-dom',
    include: ['tests/unit/**/*.{test,spec}.ts'],
    setupFiles: ['tests/setup/uni-mock.ts'],
    clearMocks: true,
  },
})
