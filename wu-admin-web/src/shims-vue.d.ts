/* eslint-disable */
/**
 * Vue 单文件组件全局类型声明
 * 放置于 src 根目录下且不包含顶层 import/export，确保 IDE 与 TS Server 能将其识别为全局 Ambient 声明
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
