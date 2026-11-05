// 作用：创建 Vue 应用实例，挂载全局插件（路由、状态管理、UI库等），最后挂载到 index.html 的 #app 元素上。
//关键点：这个文件只执行一次，是整个应用的起点。

import { createApp } from 'vue'
import './style.css'
import 'vant/es/toast/style'
import 'vant/es/notify/style'
import { TabbarItem, Icon, Search, Tag, Divider, TreeSelect, Cell, CellGroup, Button, Card, Form, Field, ConfigProvider } from 'vant';
import App from './App.vue'
import { createWebHistory, createRouter } from 'vue-router'
import routes from './config/route'
import './global.css'
// 创建路由实例（Web History 模式，URL 带路径，如 /Team）
const router = createRouter({
  history: createWebHistory(),
  routes,
})

const app = createApp(App)
app.use(router)
app.use(TabbarItem);
app.use(Icon);
app.use(Search);
app.use(Tag);
app.use(Divider);
app.use(TreeSelect);
app.use(Cell);
app.use(CellGroup);
app.use(Button);
app.use(Card);
app.use(Form);
app.use(Field);
app.use(ConfigProvider);

app.mount('#app')

