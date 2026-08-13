/// <reference types="@dcloudio/types" />
/// <reference types="@uni-helper/uni-app-types" />
import 'vue'

declare module '@vue/runtime-core' {
  type Hooks = App.AppInstance & Page.PageInstance;

  interface ComponentCustomOptions extends Hooks {

  }
}
