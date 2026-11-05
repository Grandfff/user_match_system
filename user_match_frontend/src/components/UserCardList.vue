<template>
  <van-skeleton title avatar :row="3" :loading="props.loading" v-for="user in props.userList">
    <van-card
        :desc="user.profile"
        :title="user.username || user.userAccount"
        :thumb="user.avatarUrl || DEFAULT_AVATAR_URL"
    >
      <template #tags>
        <van-tag plain type="primary" v-for="tag in user.tags" style="margin-right: 8px; margin-top: 8px">
          {{ tag }}
        </van-tag>
      </template>
      <template #footer>
        <van-button size="mini" round type="primary" plain @click="onContact(user)">联系我</van-button>
      </template>
    </van-card>
  </van-skeleton>

  <!-- 查看对方联系方式弹窗 -->
  <van-dialog
      v-model:show="showContact"
      :title="contactUser ? `${contactUser.username} 的联系方式` : '联系方式'"
      show-cancel-button
      confirm-button-text="知道了"
      @confirm="showContact = false"
  >
    <div class="contact-body" v-if="contactUser">
      <a v-if="contactUser.phone" class="contact-item" :href="`tel:${contactUser.phone}`">
        电话：{{ contactUser.phone }}
      </a>
      <a v-if="contactUser.email" class="contact-item" :href="`mailto:${contactUser.email}`">
        邮箱：{{ contactUser.email }}
      </a>
      <div v-if="!contactUser.phone && !contactUser.email" class="contact-empty">
        该用户暂未填写联系方式
      </div>
    </div>
  </van-dialog>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {useRouter} from "vue-router";
import {showConfirmDialog, showFailToast} from "vant";
import type {UserType} from "../models/user";
import {DEFAULT_AVATAR_URL} from "../constants/user";
import {getCurrentUser} from "../services/user";

interface UserCardListProps {
  userList: UserType[];
  loading?: boolean;
}

const props = withDefaults(defineProps<UserCardListProps>(), {
  userList: () => [] as UserType[],
  loading: true,
});

const router = useRouter();

/** 是否展示对方联系方式弹窗 */
const showContact = ref(false);

/** 当前查看联系方式的目标用户 */
const contactUser = ref<UserType | null>(null);

/**
 * 点击「联系我」：
 * 1. 先获取当前登录用户，未登录则提示登录；
 * 2. 自己还没填电话/邮箱时，提醒先去完善（完善后才能查看他人联系方式）；
 * 3. 否则展示对方电话 + 邮箱。
 */
const onContact = async (user: UserType) => {
  let current: UserType | null = null;
  try {
    current = await getCurrentUser();
  } catch {
    current = null;
  }
  if (!current) {
    showFailToast('请先登录');
    return;
  }

  // 自己没填电话或邮箱 → 提醒完善
  if (!current.phone || !current.email) {
    try {
      await showConfirmDialog({
        title: '完善联系方式',
        message: '你还没有填写电话或邮箱，请先完善自己的联系方式后再查看他人信息',
        confirmButtonText: '去完善',
        cancelButtonText: '取消',
      });
      router.push('/user/update');
    } catch {
      // 用户点击取消，不处理
    }
    return;
  }

  // 展示对方联系方式
  contactUser.value = user;
  showContact.value = true;
};
</script>

<style scoped>
:deep(.van-card) {
  margin: 0 12px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  background: var(--color-card);
}

:deep(.van-card__thumb) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.van-card__title) {
  color: var(--color-text);
  font-weight: 600;
}

:deep(.van-card__desc) {
  color: var(--color-text-2);
}

/* ========== 联系方式弹窗 ========== */
.contact-body {
  padding: 20px 24px 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contact-item {
  color: var(--color-text);
  font-size: 15px;
  text-decoration: none;
}

.contact-empty {
  color: var(--color-text-3);
  font-size: 14px;
  text-align: center;
}
</style>
