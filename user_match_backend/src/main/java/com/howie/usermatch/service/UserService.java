package com.howie.usermatch.service;

import com.howie.usermatch.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务
 *
 *
 *
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagList
     * @ return
     */
    List<User> searchUsersByTags(List<String> tagList);

    /**
     * 查询全部可用标签
     *
     * @return 标签列表
     */
    List<String> listAllTags();

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 更新用户信息
      * @param user
      * @param request
     * @return
     */
    int updateUser(User user,HttpServletRequest request);

    /**
     * 是否为管理员
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);
    /**
     * 更新用户信息
     * @param user
     * @param loginUser
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前用户信息
      * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    List<User> matchUsers(long num, User loginUser);

    /**
     * 删除用户（逻辑删除），并同步清理标签关联
     *
     * @param id 用户id
     * @return 是否删除成功
     */
    boolean deleteUser(long id);

    /**
     * 查询当前用户的标签列表
     *
     * @param loginUser 当前登录用户
     * @return 标签列表
     */
    List<String> listMyTags(User loginUser);

    /**
     * 给当前用户贴一个标签（已存在则幂等返回）
     *
     * @param tag 标签
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean addUserTag(String tag, User loginUser);

    /**
     * 删除当前用户的一个标签
     *
     * @param tag 标签
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean removeUserTag(String tag, User loginUser);

    /**
     * 编辑当前用户的一个标签（重命名 oldTag -> newTag）
     *
     * @param oldTag 原标签
     * @param newTag 新标签
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean updateUserTag(String oldTag, String newTag, User loginUser);
}
