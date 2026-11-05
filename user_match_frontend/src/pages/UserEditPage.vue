<template>
  <div class="edit-page">
    <van-form @submit="onSubmit">
      <van-field
          v-model="editUser.currentValue"
          :name="editUser.editKey"
          :label="editUser.editName"
          :placeholder="`请输入${editUser.editName}`"
      />
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit">
          提交
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import { ref } from "vue";
import myAxios from "../plugins/myAxios";
import {showSuccessToast, showFailToast} from "vant";
import {getCurrentUser} from "../services/user";

const route = useRoute();
const router = useRouter();

const editUser = ref({
  editKey: String(route.query.editKey ?? ''),
  currentValue: String(route.query.currentValue ?? ''),
  editName: String(route.query.editName ?? ''),
})

const onSubmit = async () => {
  const currentUser = await getCurrentUser();

  if (!currentUser) {
    showFailToast('用户未登录');
    return;
  }

  console.log(currentUser, '当前用户')

  const res = await myAxios.post('/user/update', {
    'id': currentUser.id,
    [editUser.value.editKey as string]: editUser.value.currentValue,
  })
  console.log(res, '更新请求');
  if (res.code === 0 && res.data > 0) {
    showSuccessToast('修改成功');
    router.back();
  } else {
    showFailToast('修改错误');
  }
};

</script>

<style scoped>
.edit-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 12px 0 24px;
}
</style>
