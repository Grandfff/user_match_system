package com.howie.usermatch.contant;

/**
 * 用户常量
 *
 * 
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 用户推荐缓存 key 前缀
     */
    String USER_RECOMMEND_KEY = "howie:user:recommed:";

    /**
     * 心动模式匹配结果缓存 key 前缀
     */
    String USER_MATCH_KEY = "howie:user:match:";

    //  ------- 权限 --------

    /**
     * 默认权限
     */
    int DEFAULT_ROLE = 0;


    /**
     * 管理员权限
     */
    int ADMIN_ROLE = 1;

}
