package com.howie.usermatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.howie.usermatch.common.BaseResponse;
import com.howie.usermatch.common.ErrorCode;
import com.howie.usermatch.common.ResultUtils;
import com.howie.usermatch.exception.BusinessException;
import com.howie.usermatch.model.domain.User;
import com.howie.usermatch.model.request.TagRequest;
import com.howie.usermatch.model.request.UserLoginRequest;
import com.howie.usermatch.model.request.UserRegisterRequest;
import com.howie.usermatch.service.UserService;
import com.howie.usermatch.utils.AliOssUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.howie.usermatch.contant.UserConstant.ADMIN_ROLE;
import static com.howie.usermatch.contant.UserConstant.USER_LOGIN_STATE;
import static com.howie.usermatch.contant.UserConstant.USER_RECOMMEND_KEY;

/**
 * 用户接口
 *
 *
 */
//@Controller	告诉 Spring：“这个类是一个控制器，前端发来的请求由我来处理。” @ResponseBody	告诉 Spring：“类里所有方法返回的数据，不要跳转到页面，直接转成 JSON 扔给前端。”
@RestController // "我是处理前端请求的控制器，并且我返回的数据是 JSON 格式"
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AliOssUtil aliOssUtil;

    /**
     * 推荐接口缓存过期时间（毫秒）
     */
    private static final long RECOMMEND_CACHE_TTL = 30000L;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 校验
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前用户
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }


    /**
     * 推荐用户（主页展示所有用户，脱敏后返回）
     * 引入 Redis 缓存：先查缓存，缓存未命中再查数据库并写入缓存，实现不同用户看到不同数据
     *
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        // 0. 分页参数兜底，防止非法值
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 8;
        }
        // 1. 获取当前登录用户 id，作为缓存 key 的一部分，实现不同用户看到不同数据（未登录使用默认 0）
        long userId = 0L;
        try {
            User loginUser = userService.getLoginUser(request);
            userId = loginUser.getId();
        } catch (BusinessException e) {
            // 未登录时 getLoginUser 会抛 NOT_LOGIN，缓存 key 使用默认 0
        }
        // 缓存 key 必须带上分页参数，否则不同页会命中同一份缓存，导致翻页返回重复数据
        String redisKey = USER_RECOMMEND_KEY + userId + ":" + pageNum + ":" + pageSize;

        // 2. 先查询 Redis 缓存，命中则直接返回
        Page<User> userPage = (Page<User>) redisTemplate.opsForValue().get(redisKey);
        if (userPage != null) {
            return ResultUtils.success(userPage);
        }

        // 3. 缓存未命中，查询数据库（Page 构造器第一个参数是页码，从 1 开始，不是偏移量）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        Page<User> page = new Page<>(pageNum, pageSize);
        userPage = userService.page(page, queryWrapper);
        // 脱敏处理，避免泄露密码等信息
        List<User> safetyUsers = userPage.getRecords().stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toList());
        userPage.setRecords(safetyUsers);

        // 4. 写入缓存，并设置过期时间
        redisTemplate.opsForValue().set(redisKey, userPage, RECOMMEND_CACHE_TTL, TimeUnit.MILLISECONDS);
        return ResultUtils.success(userPage);
    }

    /**
     * 心动模式：根据标签编辑距离匹配最相似的用户
     *
     * @param num     期望返回的用户数量
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            num = 10;
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, loginUser));
    }

    /**
     * 上传头像：接收前端图片 → 上传 OSS → 返回图片 URL 并更新当前用户头像
     *
     * @param file    头像图片文件（multipart 表单字段名 file）
     * @param request
     * @return 头像可访问 URL
     */
    @PostMapping("/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能为空");
        }
        User loginUser = userService.getLoginUser(request);
        // 1. 上传 OSS，得到可访问 URL
        String avatarUrl = aliOssUtil.upload(file);
        // 2. 更新当前用户头像（复用 updateUser，自动做权限校验 + 缓存失效）
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setAvatarUrl(avatarUrl);
        userService.updateUser(updateUser, request);
        return ResultUtils.success(avatarUrl);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username) {
        if (StringUtils.isBlank(username)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("username", username);
        // 限制返回数量，避免模糊搜索一次性返回过多
        queryWrapper.last("LIMIT 50");
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.deleteUser(id);
        return ResultUtils.success(b);
    }


    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    private boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 根据标签搜索用户（内存过滤）
     * @param tagNameList 用户拥有的标签
     * @return
     */
    @GetMapping("/search/tags")
    @CrossOrigin
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList))    {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 查询全部可用标签
     *
     * @return 标签列表
     */
    @GetMapping("/tags")
    public BaseResponse<List<String>> listAllTags() {
        return ResultUtils.success(userService.listAllTags());
    }

    /**
     * 查询当前用户的标签列表
     *
     * @param request
     * @return 标签列表
     */
    @GetMapping("/tags/my")
    public BaseResponse<List<String>> listMyTags(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.listMyTags(loginUser));
    }

    /**
     * 给当前用户贴一个标签（已有标签或新增标签）
     *
     * @param tagRequest
     * @param request
     * @return 是否成功
     */
    @PostMapping("/tag/add")
    public BaseResponse<Boolean> addUserTag(@RequestBody TagRequest tagRequest, HttpServletRequest request) {
        if (tagRequest == null || StringUtils.isBlank(tagRequest.getTag())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.addUserTag(tagRequest.getTag(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 删除当前用户的一个标签
     *
     * @param tagRequest
     * @param request
     * @return 是否成功
     */
    @PostMapping("/tag/delete")
    public BaseResponse<Boolean> removeUserTag(@RequestBody TagRequest tagRequest, HttpServletRequest request) {
        if (tagRequest == null || StringUtils.isBlank(tagRequest.getTag())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.removeUserTag(tagRequest.getTag(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 编辑当前用户的一个标签（重命名 oldTag -> newTag）
     *
     * @param tagRequest
     * @param request
     * @return 是否成功
     */
    @PostMapping("/tag/update")
    public BaseResponse<Boolean> updateUserTag(@RequestBody TagRequest tagRequest, HttpServletRequest request) {
        if (tagRequest == null || StringUtils.isAnyBlank(tagRequest.getOldTag(), tagRequest.getNewTag())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.updateUserTag(tagRequest.getOldTag(), tagRequest.getNewTag(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 个人信息修改
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUserInfo(@RequestBody User user, HttpServletRequest request) {
        //1.校验
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //3.更新
        int result = userService.updateUser(user,request);
        return ResultUtils.success(result);
    }
}
