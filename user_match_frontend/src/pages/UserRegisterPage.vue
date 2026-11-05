<template>
  <div class="register-page">
    <!-- 顶部品牌区域 -->
    <div class="brand-area">
      <img src="/logo.png" alt="LinkDot" class="logo-img" />
      <div class="brand-title">加入 LinkDot</div>
      <div class="brand-sub">填写以下信息，加入我们</div>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <van-form @submit="onSubmit">
        <van-field
            v-model="userAccount"
            name="userAccount"
            label="账号"
            placeholder="请输入账号"
            left-icon="user-o"
            :rules="[{ required: true, message: '请填写账号' }]"
        />
        <van-field
            v-model="userPassword"
            type="password"
            name="userPassword"
            label="密码"
            placeholder="请输入密码"
            left-icon="lock"
            :rules="[{ required: true, message: '请填写密码' }]"
        />
        <van-field
            v-model="checkPassword"
            type="password"
            name="checkPassword"
            label="确认密码"
            placeholder="请再次输入密码"
            left-icon="shield-o"
            :rules="[{ required: true, message: '请确认密码' }]"
        />
        <div class="btn-area">
          <van-button round block type="primary" native-type="submit" class="submit-btn" :loading="submitting">
            注 册
          </van-button>
        </div>
      </van-form>
    </div>

    <!-- 底部切换 -->
    <div class="switch-area">
      <span class="switch-text">已有账号？</span>
      <span class="switch-link" @click="router.push('/user/login')">返回登录</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import {ref} from "vue";
import myAxios from "../plugins/myAxios";
import {showSuccessToast, showFailToast} from "vant";

const router = useRouter();

const userAccount = ref('');
const userPassword = ref('');
const checkPassword = ref('');
const submitting = ref(false);

const onSubmit = async () => {
  // 前端校验：两次密码是否一致
  if (userPassword.value !== checkPassword.value) {
    showFailToast('两次输入的密码不一致');
    return;
  }

  submitting.value = true;

  try {
    const res = await myAxios.post('/user/register', {
      userAccount: userAccount.value,
      userPassword: userPassword.value,
      checkPassword: checkPassword.value,
    });

    if (res.code === 0 && res.data > 0) {
      showSuccessToast('注册成功');
      router.replace('/user/login');
    } else {
      // 显示后端传回的失败原因
      showFailToast(res.description || res.message || '注册失败');
    }
  } catch {
    showFailToast('注册失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20px;
}

/* ========== 顶部品牌区域 ========== */
.brand-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 56px;
  padding-bottom: 28px;
}

.logo-img {
  width: 88px;
  height: 88px;
  object-fit: contain;
  margin-bottom: 16px;
  border-radius: 16px;
}

.brand-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 6px;
}

.brand-sub {
  font-size: 14px;
  color: var(--color-text-2);
}

/* ========== 表单卡片 ========== */
.form-card {
  width: 100%;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 8px 0;
  box-shadow: var(--shadow-card);
}

.form-card :deep(.van-field__left-icon) {
  color: var(--color-text-3);
  margin-right: 4px;
}

.form-card :deep(.van-cell) {
  padding: 14px 20px;
}

.form-card :deep(.van-field__label) {
  color: var(--color-text);
  font-weight: 500;
}

.btn-area {
  padding: 20px 20px 12px;
}

.submit-btn {
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: var(--gradient-primary);
  border: none;
  letter-spacing: 4px;
}

/* ========== 底部切换 ========== */
.switch-area {
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-2);
}

.switch-link {
  color: var(--color-primary);
  font-weight: 500;
  margin-left: 4px;
  cursor: pointer;
}

.switch-link:active {
  opacity: 0.7;
}
</style>
