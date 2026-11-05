<template>
  <div class="login-page">
    <!-- 顶部品牌区域 -->
    <div class="brand-area">
      <img src="/logo.png" alt="LinkDot" class="logo-img" />
      <div class="brand-title">LinkDot</div>
      <div class="brand-sub">登录你的账号，开始探索</div>
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
            :rules="[{ required: true, message: '请填写用户名' }]"
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
        <div class="btn-area">
          <van-button round block type="primary" native-type="submit" class="submit-btn">
            登 录
          </van-button>
        </div>
      </van-form>
    </div>

    <!-- 底部切换 -->
    <div class="switch-area">
      <span class="switch-text">还没有账号？</span>
      <span class="switch-link" @click="router.push('/user/register')">立即注册</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import {ref} from "vue";
import myAxios from "../plugins/myAxios";
import {showSuccessToast, showFailToast} from "vant";

const router = useRouter();
const route = useRoute();

const userAccount = ref('');
const userPassword = ref('');

const onSubmit = async () => {
  const res = await myAxios.post('/user/login', {
    userAccount: userAccount.value,
    userPassword: userPassword.value,
  })
  console.log(res, '用户登录');
  if (res.code === 0 && res.data) {
    showSuccessToast('登录成功');
    const redirectUrl = route.query?.redirect as string ?? '/';
    window.location.href = redirectUrl;
  } else {
    showFailToast('登录失败');
  }
};

</script>

<style scoped>
.login-page {
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
  padding-top: 72px;
  padding-bottom: 36px;
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
