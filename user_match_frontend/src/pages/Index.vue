<template>
  <div class="index-page">
    <van-cell center title="心动模式" class="match-cell">
      <template #right-icon>
        <van-switch v-model="isMatchMode" size="24" />
      </template>
    </van-cell>
    <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
    >
      <user-card-list :user-list="userList" :loading="false"/>
    </van-list>
    <van-empty v-if="!loading && (!userList || userList.length < 1)" description="数据为空"/>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import myAxios from "../plugins/myAxios";
import {showFailToast, showSuccessToast} from "vant";
import UserCardList from "../components/UserCardList.vue";
import type {UserType} from "../models/user";

const router = useRouter();
const isMatchMode = ref<boolean>(false);

const userList = ref<UserType[]>([]);
const loading = ref(false);
const finished = ref(false);
const pageNum = ref(1);
const pageSize = 8;

/**
 * 重置列表，重新从第一页加载（切换模式时调用）
 */
const resetList = () => {
  userList.value = [];
  pageNum.value = 1;
  finished.value = false;
};

/**
 * 解析每条用户数据的 tags 字段（JSON 字符串 → 数组）
 */
const parseTags = (list: UserType[]) => {
  list.forEach((user) => {
    if (user.tags) {
      user.tags = JSON.parse(user.tags) as any;
    }
  });
};

/**
 * van-list 加载回调：滚动到底部时自动触发
 */
const onLoad = async () => {
  if (isMatchMode.value) {
    // 心动模式：一次性匹配，不翻页
    const userListData = await myAxios.get('/user/match', { params: { num: 10 } })
        .then((response) => response?.data)
        .catch(() => { showFailToast('请求失败'); return null; });
    if (userListData) {
      parseTags(userListData);
      userList.value = userListData;
    }
    finished.value = true;
  } else {
    // 普通模式：分页加载
    const res = await myAxios.get('/user/recommend', {
      params: { pageSize, pageNum: pageNum.value },
    })
        .then((response) => response?.data)
        .catch(() => { showFailToast('请求失败'); return null; });
    if (res?.records) {
      parseTags(res.records);
      userList.value = userList.value.concat(res.records);
      pageNum.value++;
      if (userList.value.length >= res.total) {
        finished.value = true;
      }
    } else {
      finished.value = true;
    }
  }
  loading.value = false;
};

// 切换心动模式时重置并重新加载。
// 注意：van-list 只有在 finished 由 true 变 false 时才会自动触发 load；
// 若列表还没到底（finished 本就是 false），它不会自动重新加载，导致切换后列表空白。
// 所以这里手动置 loading=true 并触发一次 onLoad（置 true 同时能避免 van-list 重复触发）。
let skipCloseToast = false;

watch(isMatchMode, async (newVal) => {
  if (newVal) {
    const myTags = await myAxios.get('/user/tags/my')
        .then((res) => res?.data)
        .catch(() => { showFailToast('请求失败'); return null; });
    if (!myTags || myTags.length === 0) {
      skipCloseToast = true;
      isMatchMode.value = false;
      showFailToast('请先添加标签后再开启心动模式');
      router.push('/user');
      return;
    }
    showSuccessToast('心动模式已开启');
  } else {
    if (skipCloseToast) {
      skipCloseToast = false;
    } else {
      showSuccessToast('心动模式已关闭');
    }
  }
  resetList();
  loading.value = true;
  onLoad();
});
</script>

<style scoped>
.index-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 24px;
}

.match-cell {
  margin: 12px 12px 4px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-card);
}
</style>
