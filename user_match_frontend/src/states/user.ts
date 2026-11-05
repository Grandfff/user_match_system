import type { UserType } from '../models/user';

/**
 * 当前登录用户的全局状态（相当于 Java 中的 get/set）
 * 任意组件都可以 import 这两个方法，全局读写当前用户信息
 */
let currentUser: UserType | null = null;

/**
 * 设置当前登录用户
 * @param user 用户信息（退出登录时传 null）
 */
export const setCurrentUserState = (user: UserType | null): void => {
  currentUser = user;
};

/**
 * 获取当前登录用户
 * @returns 当前用户信息，未登录时为 null
 */
export const getCurrentUserState = (): UserType | null => {
  return currentUser;
};
