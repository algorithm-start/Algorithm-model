/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '@vue-flow/core' {
  export const VueFlow: any
  export const Handle: any
  export const Position: any
  export const useVueFlow: any
  export type GraphNode = any
  export type Connection = any
}

declare module '@vue-flow/background' {
  export const Background: any
}

declare module '@vue-flow/controls' {
  export const Controls: any
}
