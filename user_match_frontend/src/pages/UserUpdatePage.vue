<template>
  <template v-if="user">
    <div class="update-page">
      <!-- 顶部用户信息卡片 -->
      <div class="user-header">
        <van-uploader
            v-model="fileList"
            :after-read="afterReadAvatar"
            :max-count="1"
            :preview-image="false"
            class="avatar-uploader"
        >
          <img class="avatar" :src="user.avatarUrl || DEFAULT_AVATAR_URL" alt="头像" />
        </van-uploader>
        <div class="user-info">
          <div class="user-name">{{ user.username }}</div>
          <div class="user-sub">账号：{{ user.userAccount }}</div>
        </div>
        <van-button size="small" round type="primary" class="save-btn" :loading="submitting" @click="onSubmit">
          保存修改
        </van-button>
      </div>

      <!-- 可编辑字段 -->
      <van-cell-group inset class="info-card" title="修改信息">
        <van-cell title="昵称" :value="formData.username || '未填写'" is-link @click="openDialog('username', '昵称')" />
        <van-cell title="性别" :value="genderText" is-link @click="openDialog('gender', '性别')" />
        <van-cell title="电话" :value="formData.phone || '未填写'" is-link @click="openDialog('phone', '电话')" />
        <van-cell title="邮箱" :value="formData.email || '未填写'" is-link @click="openDialog('email', '邮箱')" />
        <van-cell title="简介" :value="formData.profile || '未填写'" is-link @click="openDialog('profile', '简介')" />
      </van-cell-group>

      <!-- 只读信息 -->
      <van-cell-group inset class="info-card" title="账号信息">
        <van-cell title="注册时间" :value="formatDate(user.createTime)" />
      </van-cell-group>

      <!-- 编辑弹窗 -->
      <van-dialog
          v-model:show="dialogState.show"
          :title="`修改${dialogState.label}`"
          show-cancel-button
          confirm-button-text="确认"
          cancel-button-text="取消"
          @confirm="onDialogConfirm"
      >
        <div class="dialog-body">
          <!-- 性别选择 -->
          <van-radio-group v-if="dialogState.key === 'gender'" v-model="dialogState.value" direction="horizontal">
            <van-radio :name="1">男</van-radio>
            <van-radio :name="0">女</van-radio>
          </van-radio-group>
          <!-- 文本输入 -->
          <van-field
              v-else
              v-model="dialogState.value"
              :label="dialogState.label"
              :placeholder="`请输入${dialogState.label}`"
              :type="dialogState.key === 'phone' ? 'tel' : dialogState.key === 'email' ? 'email' : 'text'"
              autofocus
              clearable
          />
        </div>
      </van-dialog>
    </div>
  </template>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {showFailToast, showSuccessToast} from "vant";
import {getCurrentUser} from "../services/user";
import myAxios from "../plugins/myAxios";
import type {UserType} from "../models/user";
import {DEFAULT_AVATAR_URL} from "../constants/user";

const router = useRouter();

const user = ref<UserType | null>(null);

/** 头像上传文件列表（上传成功后清空，以便再次上传） */
const fileList = ref<any[]>([]);

/** 表单数据：只包含可编辑字段 */
const formData = reactive({
  username: '',
  gender: undefined as number | undefined,
  phone: '',
  email: '',
  profile: '',
});

/** 弹窗状态 */
const dialogState = reactive({
  show: false,
  key: '',
  label: '',
  value: '' as string | number | undefined,
});

/** 提交中状态，防止重复点击 */
const submitting = ref(false);

/** 性别文本映射 */
const genderText = computed(() => {
  if (formData.gender === 1) return '男';
  if (formData.gender === 0) return '女';
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

onMounted(async () => {
  const currentUser = await getCurrentUser();
  user.value = currentUser;
  if (currentUser) {
    formData.username = currentUser.username || '';
    formData.gender = currentUser.gender;
    formData.phone = currentUser.phone || '';
    formData.email = currentUser.email || '';
    formData.profile = currentUser.profile || '';
  }
});

/**
 * 头像上传回调：选中图片后上传到后端 OSS，成功后更新本地头像
 */
const afterReadAvatar = async (item: any) => {
  const file = item.file as File;
  if (!file) {
    return;
  }
  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await myAxios.post('/user/avatar', formData);
    if (res.code === 0 && res.data) {
      if (user.value) {
        user.value.avatarUrl = res.data;
      }
      showSuccessToast('头像上传成功');
    } else {
      showFailToast('头像上传失败');
    }
  } catch {
    showFailToast('头像上传失败');
  } finally {
    // 清空上传列表，保证可以再次点击更换头像
    fileList.value = [];
  }
};

/** 打开编辑弹窗 */
const openDialog = (key: string, label: string) => {
  dialogState.key = key;
  dialogState.label = label;
  dialogState.value = ((formData as Record<string, unknown>)[key] ?? '') as string | number | undefined;
  dialogState.show = true;
};

/** 弹窗确认，回写到 formData */
const onDialogConfirm = () => {
  (formData as Record<string, unknown>)[dialogState.key] = dialogState.value;
};

/** 提交修改 */
const onSubmit = async () => {
  const currentUser = await getCurrentUser();
  if (!currentUser) {
    showFailToast('用户未登录');
    return;
  }

  submitting.value = true;

  // 只提交有变更的字段
  const updateData: Record<string, unknown> = {id: currentUser.id};

  if (formData.username !== (currentUser.username || '')) {
    updateData.username = formData.username;
  }
  if (formData.gender !== currentUser.gender) {
    updateData.gender = formData.gender;
  }
  if (formData.phone !== (currentUser.phone || '')) {
    updateData.phone = formData.phone;
  }
  if (formData.email !== (currentUser.email || '')) {
    updateData.email = formData.email;
  }
  if (formData.profile !== (currentUser.profile || '')) {
    updateData.profile = formData.profile;
  }

  // 没有任何修改
  if (Object.keys(updateData).length <= 1) {
    showFailToast('未检测到任何修改');
    submitting.value = false;
    return;
  }

  try {
    const res = await myAxios.post('/user/update', updateData);
    if (res.code === 0 && res.data > 0) {
      showSuccessToast('修改成功');
      router.back();
    } else {
      showFailToast('修改失败');
    }
  } catch {
    showFailToast('修改失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.update-page {
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
  cursor: pointer;
  display: block;
}

/* 头像上传触发器：去掉 van-uploader 默认边框/背景，只保留头像本身 */
.avatar-uploader :deep(.van-uploader__upload) {
  margin: 0;
  border: none;
  background: transparent;
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

.save-btn {
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

/* ========== 弹窗内容 ========== */
.dialog-body {
  padding: 20px 24px 8px;
}
</style>
