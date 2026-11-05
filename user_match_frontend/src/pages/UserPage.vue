<template>
  <template v-if="user">
    <div class="user-page">
      <!-- 顶部用户信息卡片 -->
      <div class="user-header">
        <img class="avatar" :src="user.avatarUrl || DEFAULT_AVATAR_URL" alt="头像" />
        <div class="user-info">
          <div class="user-name">{{ user.username }}</div>
          <div class="user-sub">账号：{{ user.userAccount }}</div>
        </div>
        <van-button size="small" round type="primary" class="edit-btn" @click="router.push('/user/update')">
          修改信息
        </van-button>
      </div>

      <!-- 个人信息 -->
      <van-cell-group inset class="info-card" title="个人信息">
        <van-cell title="昵称" :value="user.username || '未填写'" />
        <van-cell title="账号" :value="user.userAccount" />
        <van-cell title="邮箱" :value="user.email || '未填写'" />
        <van-cell title="电话" :value="user.phone || '未填写'" />
        <van-cell title="性别" :value="genderText" />
        <van-cell title="简介" :value="user.profile || '未填写'" />
        <van-cell title="注册时间" :value="formatDate(user.createTime)" />
      </van-cell-group>

      <!-- 我的标签 -->
      <van-cell-group inset class="info-card" title="我的标签">
        <div class="tag-area">
          <van-tag
              v-for="tag in myTags"
              :key="tag"
              closeable
              size="medium"
              type="primary"
              class="my-tag"
              @click="openEditTag(tag)"
              @close="doDeleteTag(tag)"
          >
            {{ tag }}
          </van-tag>
          <van-tag size="medium" type="primary" plain class="tag-add" @click="showTagPanel = true">
            + 添加
          </van-tag>
          <div v-if="myTags.length === 0" class="empty-tip">还没有标签，点击「添加」选择或新建</div>
        </div>
      </van-cell-group>

      <!-- 登出按钮 -->
      <div class="logout-area">
        <van-button round block type="danger" @click="onLogout">退出登录</van-button>
      </div>
    </div>

    <!-- 添加标签面板 -->
    <van-action-sheet v-model:show="showTagPanel" title="添加标签">
      <div class="tag-panel">
        <div class="tag-panel-label">从已有标签选择（点击添加）</div>
        <div class="tag-cloud">
          <van-tag
              v-for="tag in allTags"
              :key="tag"
              :type="myTags.includes(tag) ? 'primary' : 'default'"
              size="large"
              class="tag-item"
              @click="doAddTag(tag)"
          >
            {{ tag }}
          </van-tag>
          <van-empty v-if="allTags.length === 0" description="暂无标签" />
        </div>
        <van-divider>新增标签</van-divider>
        <div class="tag-new">
          <van-field v-model="newTag" placeholder="输入数据库中没有的新标签" />
          <van-button size="small" type="primary" class="tag-new-btn" @click="doCreateTag">新增</van-button>
        </div>
      </div>
    </van-action-sheet>

    <!-- 编辑标签弹窗 -->
    <van-dialog
        v-model:show="showEditDialog"
        title="编辑标签"
        show-cancel-button
        @confirm="doUpdateTag"
    >
      <div class="dialog-body">
        <van-field v-model="editNewTag" :placeholder="`原标签：${editOldTag}`" />
      </div>
    </van-dialog>
  </template>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {showSuccessToast, showFailToast} from "vant";
import {getCurrentUser, userLogout} from "../services/user";
import myAxios from "../plugins/myAxios";
import type {UserType} from "../models/user";
import {DEFAULT_AVATAR_URL} from "../constants/user";

const user = ref<UserType | null>(null);

const router = useRouter();

// 我的标签
const myTags = ref<string[]>([]);
// 全部标签（标签池）
const allTags = ref<string[]>([]);

// 添加标签面板
const showTagPanel = ref(false);
const newTag = ref('');

// 编辑标签弹窗
const showEditDialog = ref(false);
const editOldTag = ref('');
const editNewTag = ref('');

// 性别：0 - 女，1 - 男（后端 gender 为 Integer）
const genderText = computed(() => {
  const gender = user.value?.gender;
  if (gender === 0) return '女';
  if (gender === 1) return '男';
  return '未设置';
});

/**
 * 格式化日期为国人易读格式：2024年01月15日 10:30
 */
const formatDate = (dateStr: string | Date | undefined): string => {
  if (!dateStr) return '未知';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return '未知';
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${year}年${month}月${day}日 ${hours}:${minutes}`;
};

/**
 * 加载我的标签 + 全部标签
 */
const loadTags = async () => {
  const myRes = await myAxios.get('/user/tags/my').catch(() => null);
  if (myRes?.code === 0) {
    myTags.value = myRes.data ?? [];
  }
  const allRes = await myAxios.get('/user/tags').catch(() => null);
  if (allRes?.code === 0) {
    allTags.value = allRes.data ?? [];
  }
};

/**
 * 贴一个已有标签
 */
const doAddTag = async (tag: string) => {
  const res = await myAxios.post('/user/tag/add', {tag});
  if (res?.code === 0) {
    showSuccessToast('添加成功');
    await loadTags();
  } else {
    showFailToast('添加失败' + (res.description ? `，${res.description}` : ''));
  }
};

/**
 * 新增一个全新标签
 */
const doCreateTag = async () => {
  const tag = newTag.value.trim();
  if (!tag) {
    showFailToast('请输入标签');
    return;
  }
  const res = await myAxios.post('/user/tag/add', {tag});
  if (res?.code === 0) {
    showSuccessToast('新增成功');
    newTag.value = '';
    await loadTags();
  } else {
    showFailToast('新增失败' + (res.description ? `，${res.description}` : ''));
  }
};

/**
 * 删除我的一个标签
 */
const doDeleteTag = async (tag: string) => {
  const res = await myAxios.post('/user/tag/delete', {tag});
  if (res?.code === 0) {
    showSuccessToast('删除成功');
    await loadTags();
  } else {
    showFailToast('删除失败' + (res.description ? `，${res.description}` : ''));
  }
};

/**
 * 打开编辑标签弹窗
 */
const openEditTag = (tag: string) => {
  editOldTag.value = tag;
  editNewTag.value = '';
  showEditDialog.value = true;
};

/**
 * 编辑（重命名）标签
 */
const doUpdateTag = async () => {
  const tag = editNewTag.value.trim();
  if (!tag) {
    showFailToast('请输入新标签');
    return;
  }
  const res = await myAxios.post('/user/tag/update', {oldTag: editOldTag.value, newTag: tag});
  if (res?.code === 0) {
    showSuccessToast('编辑成功');
    await loadTags();
  } else {
    showFailToast('编辑失败' + (res.description ? `，${res.description}` : ''));
  }
};

onMounted(async () => {
  user.value = await getCurrentUser();
  await loadTags();
});

/**
 * 退出登录：调用后端登出接口，清空本地登录态并跳转到登录页
 */
const onLogout = async () => {
  await userLogout();
  showSuccessToast('退出成功');
  router.replace('/user/login');
};
</script>

<style scoped>
.user-page {
  padding-bottom: 24px;
  background-color: var(--color-bg);
  min-height: 100vh;
}

/* ========== 顶部用户信息卡片 ========== */
.user-header {
  display: flex;
  align-items: center;
  padding: 20px 16px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 14px;
  flex-shrink: 0;
  border: 2px solid var(--color-border);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.user-sub {
  font-size: 13px;
  color: var(--color-text-2);
}

.edit-btn {
  flex-shrink: 0;
}

/* ========== 卡片组统一样式 ========== */
.info-card {
  margin-top: 12px;
}

/* 卡片组标题左移 */
:deep(.van-cell-group__title) {
  padding-left: 16px;
  color: var(--color-text);
  font-size: 15px;
  font-weight: 600;
}

/* 单元格左侧无内边距 */
:deep(.van-cell) {
  padding-left: 16px;
}

/* 标题区域：比例 1，靠左 */
:deep(.van-cell__title) {
  padding-left: 0;
  flex: 1;
  color: var(--color-text);
  font-size: 14px;
}

/* 值区域：比例 4，右对齐 */
:deep(.van-cell__value) {
  flex: 4;
  color: var(--color-text-2);
  font-size: 14px;
  text-align: right;
}

/* ========== 我的标签 ========== */
.tag-area {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
}

.my-tag {
  cursor: pointer;
}

.tag-add {
  cursor: pointer;
}

.empty-tip {
  font-size: 13px;
  color: var(--color-text-3);
}

/* ========== 添加标签面板 ========== */
.tag-panel {
  padding: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

.tag-panel-label {
  font-size: 13px;
  color: var(--color-text-2);
  margin-bottom: 10px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  cursor: pointer;
}

.tag-new {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-new .van-field {
  flex: 1;
}

.tag-new-btn {
  flex-shrink: 0;
}

/* ========== 编辑标签弹窗 ========== */
.dialog-body {
  padding: 20px 24px 8px;
}

/* 登出按钮区域 */
.logout-area {
  margin: 24px 16px;
}
</style>
