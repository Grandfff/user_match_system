<template>
  <div class="team-page">
    <!-- Tab 标签页 -->
    <van-tabs v-model:active="activeTab" sticky swipeable @change="onTabChange">
      <van-tab title="全部队伍" name="all" />
      <van-tab title="我创建的" name="create" />
      <van-tab title="我加入的" name="join" />
    </van-tabs>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <van-field
          v-model="searchText"
          placeholder="搜索队伍名称或描述"
          left-icon="search"
          clearable
          @keyup.enter="resetList"
          @clear="resetList"
      />
    </div>

    <!-- 状态筛选 -->
    <div class="status-filter">
      <div
          v-for="opt in statusOptions"
          :key="opt.value"
          class="status-chip"
          :class="{ active: statusValue === opt.value }"
          @click="selectStatus(opt.value)"
      >
        {{ opt.text }}
      </div>
    </div>

    <!-- 队伍列表 -->
    <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
    >
      <div v-for="team in teamList" :key="team.id" class="team-card">
        <!-- 队伍名称 + 状态 -->
        <div class="team-card-top">
          <div class="team-name">{{ team.name }}</div>
          <van-tag :type="statusTagType(team.status)" plain>
            {{ teamStatusEnum[team.status] }}
          </van-tag>
        </div>

        <!-- 描述 -->
        <div class="team-desc" v-if="team.description">{{ team.description }}</div>

        <!-- 详细信息（仅创建时间 + 创建者） -->
        <div class="team-detail">
          <div class="detail-item" v-if="team.createTime">
            <span class="detail-label">创建时间</span>
            <span class="detail-value">{{ formatDate(team.createTime) }}</span>
          </div>
          <div class="detail-item" v-if="team.createUser">
            <span class="detail-label">创建者</span>
            <span class="detail-value">{{ team.createUser.username || '未知' }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="team-actions">
          <van-button
              v-if="team.userId !== currentUser?.id && !team.hasJoin"
              size="mini" round type="primary"
              @click="preJoinTeam(team)"
          >
            加入队伍
          </van-button>
          <van-button
              v-if="team.userId === currentUser?.id"
              size="mini" round plain
              @click="doUpdateTeam(team.id)"
          >
            更新队伍
          </van-button>
          <van-button
              v-if="team.userId !== currentUser?.id && team.hasJoin"
              size="mini" round plain type="warning"
              @click="doQuitTeam(team.id)"
          >
            退出队伍
          </van-button>
          <van-button
              v-if="team.userId === currentUser?.id"
              size="mini" round plain type="danger"
              @click="doDeleteTeam(team.id)"
          >
            解散队伍
          </van-button>
        </div>

        <!-- 底部：人数 + 过期时间 -->
        <div class="team-footer">
          <span class="footer-item">{{ team.hasJoinNum ?? 0 }} / {{ team.maxNum }} 人</span>
          <span class="footer-item" v-if="team.expireTime">{{ formatDate(team.expireTime) }} 到期</span>
        </div>
      </div>
    </van-list>

    <!-- 空状态 -->
    <van-empty v-if="!loading && teamList.length === 0" description="暂无队伍" />

    <!-- 密码弹窗 -->
    <van-dialog
        v-model:show="showPasswordDialog"
        title="请输入队伍密码"
        show-cancel-button
        confirm-button-text="确认"
        cancel-button-text="取消"
        @confirm="doJoinTeam"
        @cancel="doJoinCancel"
    >
      <div class="dialog-body">
        <van-field v-model="password" placeholder="请输入密码" autofocus />
      </div>
    </van-dialog>

  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import myAxios from "../plugins/myAxios";
import {showSuccessToast, showFailToast} from "vant";
import {getCurrentUser} from "../services/user";
import {teamStatusEnum} from "../constants/team";
import type {TeamType} from "../models/team";
import type {UserType} from "../models/user";

const router = useRouter();

const teamList = ref<TeamType[]>([]);
const currentUser = ref<UserType | null>(null);
const searchText = ref('');
const loading = ref(false);
const finished = ref(false);
const pageNum = ref(1);
const pageSize = 10;
const activeTab = ref('all');

// 状态筛选：-1 表示全部状态（公开 + 加密）
const statusValue = ref(-1);
const statusOptions = [
  {text: '全部状态', value: -1},
  {text: '公开', value: 0},
  {text: '加密', value: 2},
];

// 加入队伍相关
const showPasswordDialog = ref(false);
const password = ref('');
const joinTeamId = ref(0);

/**
 * 各 Tab 对应的 API 路径
 */
const tabApiMap: Record<string, string> = {
  all: '/team/list',
  create: '/team/list/my/create',
  join: '/team/list/my/join',
};

/**
 * 格式化日期为国人易读格式
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
 * 状态标签颜色：公开=success，私有=warning，加密=danger
 */
const statusTagType = (status: number) => {
  if (status === 0) return 'success';
  if (status === 1) return 'warning';
  return 'danger';
};

/**
 * 重置列表，重新从第一页加载（切换 Tab / 筛选 / 搜索 / 增删改后调用）
 */
const resetList = () => {
  teamList.value = [];
  pageNum.value = 1;
  finished.value = false;
};

/**
 * 切换 Tab：清空搜索并重新加载
 */
const onTabChange = () => {
  searchText.value = '';
  statusValue.value = -1;
  resetList();
};

/**
 * 切换状态筛选
 */
const selectStatus = (value: number) => {
  statusValue.value = value;
  resetList();
};

/**
 * van-list 加载回调：滚动到底部时自动触发
 */
const onLoad = async () => {
  const apiPath = tabApiMap[activeTab.value] || '/team/list';
  try {
    if (activeTab.value === 'all') {
      // 全部队伍：分页加载（后端返回 Page，含 records + total）
      const params: Record<string, any> = { pageNum: pageNum.value, pageSize };
      if (searchText.value) {
        params.searchText = searchText.value;
      }
      if (statusValue.value !== -1) {
        params.status = statusValue.value;
      }
      const res = await myAxios.get(apiPath, { params });
      if (res?.code === 0) {
        const records = res.data?.records ?? [];
        teamList.value = teamList.value.concat(records);
        pageNum.value++;
        if (teamList.value.length >= (res.data?.total ?? 0)) {
          finished.value = true;
        }
      } else {
        showFailToast('加载失败');
        finished.value = true;
      }
    } else {
      // 我创建的 / 我加入的：数据量小，一次加载全量（后端返回 List）
      const params: Record<string, any> = { pageNum: 1, pageSize: 50 };
      if (searchText.value) {
        params.searchText = searchText.value;
      }
      if (statusValue.value !== -1) {
        params.status = statusValue.value;
      }
      const res = await myAxios.get(apiPath, { params });
      if (res?.code === 0) {
        teamList.value = res.data ?? [];
      } else {
        showFailToast('加载失败');
      }
      finished.value = true;
    }
  } catch {
    showFailToast('网络异常');
    finished.value = true;
  } finally {
    loading.value = false;
  }
};

/**
 * 加入队伍前置判断（公开直接加入，加密需密码）
 */
const preJoinTeam = (team: TeamType) => {
  joinTeamId.value = team.id;
  if (team.status === 0) {
    doJoinTeam();
  } else {
    showPasswordDialog.value = true;
  }
};

const doJoinCancel = () => {
  joinTeamId.value = 0;
  password.value = '';
};

/**
 * 加入队伍
 */
const doJoinTeam = async () => {
  if (!joinTeamId.value) return;
  const res = await myAxios.post('/team/join', {
    teamId: joinTeamId.value,
    password: password.value,
  });
  if (res?.code === 0) {
    showSuccessToast('加入成功');
    doJoinCancel();
    resetList();
  } else {
    showFailToast('加入失败' + (res.description ? `，${res.description}` : ''));
  }
};

/**
 * 跳转更新队伍页
 */
const doUpdateTeam = (id: number) => {
  router.push({ path: '/team/update', query: { id } });
};

/**
 * 退出队伍
 */
const doQuitTeam = async (id: number) => {
  const res = await myAxios.post('/team/quit', { teamId: id });
  if (res?.code === 0) {
    showSuccessToast('退出成功');
    resetList();
  } else {
    showFailToast('操作失败' + (res.description ? `，${res.description}` : ''));
  }
};

/**
 * 解散队伍
 */
const doDeleteTeam = async (id: number) => {
  const res = await myAxios.post('/team/delete', { id });
  if (res?.code === 0) {
    showSuccessToast('解散成功');
    resetList();
  } else {
    showFailToast('操作失败' + (res.description ? `，${res.description}` : ''));
  }
};

onMounted(async () => {
  currentUser.value = await getCurrentUser();
  // van-list 挂载后会自动触发 onLoad 加载第一页
});
</script>

<style scoped>
.team-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: 24px;
}

/* ========== Tab 标签页 ========== */
:deep(.van-tabs__nav) {
  background: var(--color-card);
}

:deep(.van-tab--active) {
  font-weight: 600;
}

:deep(.van-tabs__line) {
  background: var(--color-primary);
  width: 40px !important;
}

/* ========== 搜索栏 ========== */
.search-bar {
  padding: 12px 16px;
}

.search-bar :deep(.van-cell) {
  border-radius: 20px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
}

/* ========== 状态筛选 ========== */
.status-filter {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
  overflow-x: auto;
}

.status-chip {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: 13px;
  color: var(--color-text-2);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.status-chip.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
  border-color: var(--color-primary);
}

/* ========== 队伍卡片 ========== */
.team-card {
  background: var(--color-card);
  margin: 0 12px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  padding: 14px;
  box-shadow: var(--shadow-card);
}

.team-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.team-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  flex: 1;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-desc {
  font-size: 14px;
  color: var(--color-text-2);
  margin-bottom: 10px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* ========== 详情行 ========== */
.team-detail {
  margin-bottom: 6px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 2px 0;
}

.detail-label {
  font-size: 12px;
  color: var(--color-text-3);
  width: 60px;
  flex-shrink: 0;
}

.detail-value {
  font-size: 12px;
  color: var(--color-text);
  flex: 1;
}

/* ========== 操作按钮 ========== */
.team-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  border-top: 1px solid var(--color-border);
  padding-top: 10px;
  margin-top: 2px;
}

/* ========== 底部：人数 + 过期时间 ========== */
.team-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.footer-item {
  font-size: 11px;
  color: var(--color-text-3);
}

/* ========== 弹窗 ========== */
.dialog-body {
  padding: 20px 24px 8px;
}
</style>
