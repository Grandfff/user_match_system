import myAxios from "../plugins/myAxios";
import { setCurrentUserState } from "../states/user";

export const getCurrentUser = async () => {
  // const currentUser = getCurrentUserState();
  // if (currentUser) {
  //     return currentUser;
  // }
  // 不存在则从远程获取
  const res = await myAxios.get('/user/current');
  if (res.code === 0) {
    setCurrentUserState(res.data);
    return res.data;
  }
  return null;
}

/**
 * 退出登录：调用后端登出接口并清空本地登录态
 */
export const userLogout = async () => {
  await myAxios.post('/user/logout');
  setCurrentUserState(null);
}

