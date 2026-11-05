<template>
  <van-nav-bar
      left-arrow
      fixed
      placeholder
      @click-left="onClickLeft"
      @click-right="onClickRight"
  >
    <template #title>
      <div class="nav-title-wrapper" @click="onTitleClick">
        <img src="/logo.png" alt="logo" class="nav-logo" />
        <span class="nav-title-text">{{ title }}</span>
      </div>
    </template>
    <template #right>
      <van-icon name="search" size="18"/>
    </template>
  </van-nav-bar>
  <div id="content">
    <router-view/>
  </div>
  <!-- NoticeBar 提示条（仅列表页显示，每次登录仅提醒一次） -->
  <van-notice-bar
      v-if="isListPage && showNoticeBar"
      class="top-notice-bar"
      mode="closeable"
      left-icon="volume-o"
      text="双击标题回到顶部"
      @close="onCloseNoticeBar"
  />
  <van-tabbar route>
    <van-tabbar-item to="/" icon="home-o" name="index">主页</van-tabbar-item>
    <van-tabbar-item to="/team" icon="search" name="team">队伍</van-tabbar-item>
    <van-tabbar-item to="/user" icon="friends-o" name="user">个人</van-tabbar-item>
  </van-tabbar>

  <!-- FloatingBubble 悬浮创建按钮（仅队伍页显示） -->
  <div
      v-if="isTeamPage"
      class="floating-bubble"
      :style="bubbleStyle"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
      @click="onBubbleClick"
  >
    <van-icon name="plus" size="28" color="#fff" />
  </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from "vue-router";
import {ref, computed, onMounted, reactive} from "vue";
import routes from "../config/route";

const router = useRouter();
const routeInfo = useRoute();
const DEFAULT_TITLE = 'LinkDot';
const title = ref(DEFAULT_TITLE);

/** 当前是否在队伍页 */
const isTeamPage = computed(() => routeInfo.path === '/team');

/** 当前是否在列表页（主页 / 队伍页） */
const isListPage = computed(() => routeInfo.path === '/' || routeInfo.path === '/team');

/** NoticeBar 显示状态：每次登录仅显示一次，用户关闭后不再出现 */
const showNoticeBar = ref(sessionStorage.getItem('noticeBarClosed') !== 'true');

const onCloseNoticeBar = () => {
  sessionStorage.setItem('noticeBarClosed', 'true');
  showNoticeBar.value = false;
};

/**
 * 根据路由切换标题
 */
router.beforeEach((to) => {
  const toPath = to.path;
  const route = routes.find((route) => {
    return toPath == route.path;
  })
  title.value = route?.title ?? DEFAULT_TITLE;
})

const onClickLeft = () => {
  router.back();
};

const onClickRight = () => {
  router.push('/search')
};

/** 双击标题回到顶部（手动检测双击，兼容触屏和鼠标） */
let lastTitleClickTime = 0;
const onTitleClick = () => {
  const now = Date.now();
  if (now - lastTitleClickTime < 300) {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    lastTitleClickTime = 0;
  } else {
    lastTitleClickTime = now;
  }
};

// ========== FloatingBubble ==========
const BUBBLE_SIZE = 52;
const EDGE_GAP = 12;
const SNAP_THRESHOLD = 5; // 小于 5px 视为点击

const pos = reactive({ x: 0, y: 0 });
let startX = 0;
let startY = 0;
let startPosX = 0;
let startPosY = 0;
let isDragging = false;
let hasMoved = false;

const bubbleStyle = computed(() => ({
  left: `${pos.x}px`,
  top: `${pos.y}px`,
}));

onMounted(() => {
  // 初始化位置：右下角，距底边 120px（避开 tabbar）
  pos.x = window.innerWidth - BUBBLE_SIZE - EDGE_GAP;
  pos.y = window.innerHeight - BUBBLE_SIZE - 120;
});

const onTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0];
  startX = touch.clientX;
  startY = touch.clientY;
  startPosX = pos.x;
  startPosY = pos.y;
  isDragging = true;
  hasMoved = false;
};

const onTouchMove = (e: TouchEvent) => {
  if (!isDragging) return;
  e.preventDefault();
  const touch = e.touches[0];
  const dx = touch.clientX - startX;
  const dy = touch.clientY - startY;

  if (Math.abs(dx) > SNAP_THRESHOLD || Math.abs(dy) > SNAP_THRESHOLD) {
    hasMoved = true;
  }

  let newX = startPosX + dx;
  let newY = startPosY + dy;

  // 边界约束
  const maxX = window.innerWidth - BUBBLE_SIZE;
  const maxY = window.innerHeight - BUBBLE_SIZE;
  newX = Math.max(0, Math.min(newX, maxX));
  newY = Math.max(0, Math.min(newY, maxY));

  pos.x = newX;
  pos.y = newY;
};

const onTouchEnd = () => {
  isDragging = false;
  // 吸附到 X 轴方向最近的一边
  const halfWidth = window.innerWidth / 2;
  if (pos.x + BUBBLE_SIZE / 2 < halfWidth) {
    pos.x = EDGE_GAP;
  } else {
    pos.x = window.innerWidth - BUBBLE_SIZE - EDGE_GAP;
  }
};

const onBubbleClick = () => {
  if (!hasMoved) {
    router.push('/team/add');
  }
};

</script>

<style scoped>
#content {
  padding-bottom: 50px;
}

/* ========== NoticeBar 提示条 ========== */
.top-notice-bar {
  position: fixed;
  bottom: 50px;
  left: 0;
  right: 0;
  z-index: 99;
  border-radius: 0;
}

:deep(.top-notice-bar.van-notice-bar) {
  background: var(--color-card);
  color: var(--color-primary);
  border-top: 1px solid var(--color-border);
  border-bottom: none;
}

:deep(.top-notice-bar .van-notice-bar__content) {
  flex: 1;
  text-align: center;
}

/* ========== 顶部导航栏 ========== */
:deep(.van-nav-bar) {
  background: var(--color-card);
  z-index: 100;
}

:deep(.van-nav-bar::after) {
  border-color: var(--color-border);
}

:deep(.van-nav-bar__title) {
  color: var(--color-text);
  font-weight: 600;
}

.nav-title-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 16px;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.nav-logo {
  width: 28px;
  height: 28px;
  object-fit: contain;
  border-radius: 6px;
}

.nav-title-text {
  color: var(--color-text);
  font-weight: 600;
}

/* ========== 底部标签栏 ========== */
:deep(.van-tabbar) {
  background: var(--color-card);
  border-top: 1px solid var(--color-border);
}

:deep(.van-tabbar-item--active) {
  color: var(--color-primary);
}

/* ========== FloatingBubble 悬浮创建按钮 ========== */
.floating-bubble {
  position: fixed;
  z-index: 9999;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(255, 107, 107, 0.35);
  cursor: pointer;
  touch-action: none;
  transition: left 0.2s ease, top 0.05s;
  user-select: none;
}

.floating-bubble:active {
  transform: scale(0.92);
}
</style>
