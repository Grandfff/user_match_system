<!-- 作用：定义所有页面共用的结构，比如顶部导航栏、底部footer、侧边栏，以及一个占位区域 <router-view/>，路由匹配到的页面会在这里渲染。 -->
<!-- 关键点：不管访问哪个页面，App.vue 的导航栏和底部永远保持不变，只有 <router-view/> 里的内容在变化。 -->
<!-- 显示的都是公共页面 大家页面都有的内容 -->
<script setup lang="ts">
import BasicLayout from "./layouts/BasicLayout.vue";

/**
 * Vant 4 ConfigProvider 全局主题 - 珊瑚橙品牌配色
 * 通过 themeVarsScope="global" 将变量设置到 <html> 元素，
 * 确保所有 Vant 组件（含 Teleport 弹出的 Toast/Dialog）均继承主题色
 */
const themeVars = {
  primaryColor: '#ff6b6b',
  buttonPrimaryBackground: 'linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%)',
  buttonPrimaryBorderColor: '#ff6b6b',
  activeColor: '#ff6b6b',
  tabbarItemActiveColor: '#ff6b6b',
};
</script>

<template>
  <van-config-provider :theme-vars="themeVars" theme-vars-scope="global">
    <BasicLayout />
  </van-config-provider>
</template>

<style>
</style>
