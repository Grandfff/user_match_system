<template>
  <div class="search-page">
    <!-- 搜索模式切换 -->
    <div class="mode-switch">
      <div
          v-for="mode in modeOptions"
          :key="mode.value"
          class="mode-item"
          :class="{ active: searchMode === mode.value }"
          @click="switchMode(mode.value)"
      >
        {{ mode.text }}
      </div>
    </div>

    <van-search
        v-model="searchText"
        show-action
        :placeholder="searchMode === 'tag' ? '请输入要搜索的标签' : '请输入用户名'"
        @search="onSearch"
        @cancel="onCancel"
    />

    <!-- 搜标签模式 -->
    <template v-if="searchMode === 'tag'">
      <van-divider content-position="left">已选标签</van-divider>
      <div v-if="activeIds.length === 0" class="empty-tip">请选择标签</div>
      <van-row gutter="16" class="selected-tags">
        <van-col v-for="tag in activeIds" :key="tag">
          <van-tag closeable size="medium" type="primary" @close="doClose(tag)">
            {{ tag }}
          </van-tag>
        </van-col>
      </van-row>
      <van-divider content-position="left">选择标签</van-divider>
      <div class="tag-cloud">
        <van-tag
            v-for="tag in filteredTagList"
            :key="tag"
            :type="activeIds.includes(tag) ? 'primary' : 'default'"
            size="large"
            class="tag-item"
            @click="toggleTag(tag)"
        >
          {{ tag }}
        </van-tag>
        <van-empty v-if="filteredTagList.length === 0" description="暂无标签" />
      </div>
      <div class="search-action">
        <van-button block round type="primary" @click="doSearchResult">搜索</van-button>
      </div>
    </template>

    <!-- 搜用户模式 -->
    <template v-else>
      <div class="search-action">
        <van-button block round type="primary" :loading="searching" @click="onSearch">搜索用户</van-button>
      </div>
      <div class="user-result">
        <user-card-list :user-list="searchUserList" :loading="false" />
        <van-empty v-if="!searching && searchUserList.length === 0" description="输入用户名搜索用户" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import {useRouter} from "vue-router";
import myAxios from "../plugins/myAxios";
import {showFailToast} from "vant";
import UserCardList from "../components/UserCardList.vue";
import type {UserType} from "../models/user";

const router = useRouter();

const searchText = ref('');

// 搜索模式：tag = 搜标签，user = 搜用户
const searchMode = ref<'tag' | 'user'>('tag');
const modeOptions: { text: string; value: 'tag' | 'user' }[] = [
  {text: '搜标签', value: 'tag'},
  {text: '搜用户', value: 'user'},
];

// 后端返回的全部可用标签
const tagList = ref<string[]>([]);

// 已选中的标签
const activeIds = ref<string[]>([]);

// 搜用户结果
const searchUserList = ref<UserType[]>([]);
const searching = ref(false);

// 从后端拉取全部标签（扁平展示，去掉父级分类）
onMounted(async () => {
  const res = await myAxios.get('/user/tags').catch((err) => {
    console.error('/user/tags error', err);
    showFailToast('标签加载失败');
    return null;
  });
  if (res?.data) {
    tagList.value = res.data;
  }
});

// 按搜索文本过滤标签
const filteredTagList = computed(() => {
  const val = searchText.value.trim();
  if (!val) {
    return tagList.value;
  }
  return tagList.value.filter(tag => tag.includes(val));
});

/**
 * 切换搜索模式
 */
const switchMode = (mode: 'tag' | 'user') => {
  if (searchMode.value === mode) {
    return;
  }
  searchMode.value = mode;
  searchText.value = '';
  if (mode === 'user') {
    searchUserList.value = [];
  }
};

/**
 * 搜索触发（回车 / 点搜索图标 / 点搜索用户按钮）
 */
const onSearch = async () => {
  if (searchMode.value === 'user') {
    const username = searchText.value.trim();
    if (!username) {
      showFailToast('请输入用户名');
      return;
    }
    searching.value = true;
    const res = await myAxios.get('/user/search', {params: {username}})
        .catch(() => {
          showFailToast('搜索失败');
          return null;
        });
    searching.value = false;
    if (res?.data) {
      res.data.forEach((user: UserType) => {
        if (user.tags) {
          user.tags = JSON.parse(user.tags) as any;
        }
      });
      searchUserList.value = res.data;
    }
  }
  // 标签模式：过滤已由 filteredTagList 自动完成
};

const onCancel = () => {
  searchText.value = '';
};

// 点击标签切换选中状态
const toggleTag = (tag: string) => {
  if (activeIds.value.includes(tag)) {
    activeIds.value = activeIds.value.filter(item => item !== tag);
  } else {
    activeIds.value = [...activeIds.value, tag];
  }
};

// 移除已选标签
const doClose = (tag: string) => {
  activeIds.value = activeIds.value.filter(item => item !== tag);
};

/**
 * 标签模式：跳转到搜索结果页
 */
const doSearchResult = () => {
  router.push({
    path: '/user/list',
    query: {
      tags: activeIds.value
    }
  });
};

</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 24px;
}

/* ========== 搜索模式切换 ========== */
.mode-switch {
  display: flex;
  gap: 10px;
  padding: 12px 16px 0;
}

.mode-item {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  color: var(--color-text-2);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-item.active {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  font-weight: 600;
}

.empty-tip {
  padding: 0 16px 8px;
  font-size: 13px;
  color: var(--color-text-3);
}

.selected-tags {
  padding: 0 16px 8px;
}

.selected-tags .van-col {
  margin: 0 8px 8px 0;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 16px 12px;
}

.tag-item {
  cursor: pointer;
}

.search-action {
  padding: 12px 16px;
}

.user-result {
  padding-bottom: 16px;
}
</style>
