<template>
  <div id="teamAddPage">
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
            v-model="addTeamData.name"
            name="name"
            label="队伍名"
            placeholder="请输入队伍名"
            :rules="[{ required: true, message: '请输入队伍名' }]"
        />
        <van-field
            v-model="addTeamData.description"
            rows="4"
            autosize
            label="队伍描述"
            type="textarea"
            placeholder="请输入队伍描述"
        />
        <van-field
            is-link
            readonly
            name="datetimePicker"
            label="过期时间"
            :placeholder="expireTimeText"
            @click="onOpenPicker"
        />
        <van-popup v-model:show="showPicker" position="bottom">
          <van-picker-group
              title="请选择过期时间"
              :tabs="['选择日期', '选择时间']"
              @confirm="onConfirmPicker"
              @cancel="showPicker = false"
          >
            <van-date-picker v-model="pickerDate" :min-date="minDate" />
            <van-time-picker v-model="pickerTime" />
          </van-picker-group>
        </van-popup>
        <van-field name="stepper" label="最大人数">
          <template #input>
            <van-stepper v-model="addTeamData.maxNum" max="10" min="3"/>
          </template>
        </van-field>
        <van-field name="radio" label="队伍状态">
          <template #input>
            <van-radio-group v-model="addTeamData.status" direction="horizontal">
              <van-radio name="0">公开</van-radio>
              <van-radio name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
            v-if="Number(addTeamData.status) === 2"
            v-model="addTeamData.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入队伍密码"
            :rules="[{ required: true, message: '请填写密码' }]"
        />
      </van-cell-group>
      <div class="btn-area">
        <van-button round block type="primary" native-type="submit" class="submit-btn">
          提交
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">

import {useRouter} from "vue-router";
import {computed, ref} from "vue";
import myAxios from "../plugins/myAxios";
import {showSuccessToast} from "vant";

const router = useRouter();
// 展示日期选择器
const showPicker = ref(false);

const initFormData = {
  "name": "",
  "description": "",
  "expireTime": null as Date | null,
  "maxNum": 3,
  "password": "",
  "status": 0,
}

const minDate = new Date();
const pickerDate = ref([String(minDate.getFullYear()), String(minDate.getMonth() + 1).padStart(2, '0'), String(minDate.getDate()).padStart(2, '0')]);
const pickerTime = ref(['00', '00']);

// 需要用户填写的表单数据
const addTeamData = ref({...initFormData})

// 过期时间的展示文本（用于只读字段的 placeholder）
const expireTimeText = computed(() => {
  const t = addTeamData.value.expireTime;
  if (!t) return '点击选择过期时间';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())} ${pad(t.getHours())}:${pad(t.getMinutes())}`;
});

// 打开日期时间选择器，同步当前过期时间
const onOpenPicker = () => {
  const expire = addTeamData.value.expireTime;
  const d = expire ? new Date(expire) : new Date();
  pickerDate.value = [String(d.getFullYear()), String(d.getMonth() + 1).padStart(2, '0'), String(d.getDate()).padStart(2, '0')];
  pickerTime.value = [String(d.getHours()).padStart(2, '0'), String(d.getMinutes()).padStart(2, '0')];
  showPicker.value = true;
};

// 确认选择：拼接日期和时间，写回过期时间
const onConfirmPicker = () => {
  const [year, month, day] = pickerDate.value;
  const [hour, minute] = pickerTime.value;
  addTeamData.value.expireTime = new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute));
  showPicker.value = false;
};

// 提交
const onSubmit = async () => {
  const postData = {
    ...addTeamData.value,
    status: Number(addTeamData.value.status)
  }
  // todo 前端参数校验
  const res = await myAxios.post("/team/add", postData);
  if (res?.code === 0 && res.data){
    showSuccessToast('添加成功');
    router.push({
      path: '/team',
      replace: true,
    });
  } else {
    showSuccessToast('添加失败');
  }
}
</script>

<style scoped>
#teamAddPage {
  min-height: 100vh;
  background: var(--color-bg);
  padding: 12px 0 24px;
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
</style>
