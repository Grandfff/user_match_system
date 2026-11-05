package com.howie.usermatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.howie.usermatch.common.ErrorCode;
import com.howie.usermatch.exception.BusinessException;
import com.howie.usermatch.mapper.UserMapper;
import com.howie.usermatch.mapper.UserTagMapper;
import com.howie.usermatch.model.domain.User;
import com.howie.usermatch.model.domain.UserTag;
import com.howie.usermatch.service.UserService;
import com.howie.usermatch.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.howie.usermatch.contant.UserConstant.ADMIN_ROLE;
import static com.howie.usermatch.contant.UserConstant.USER_LOGIN_STATE;
import static com.howie.usermatch.contant.UserConstant.USER_MATCH_KEY;
import static com.howie.usermatch.contant.UserConstant.USER_RECOMMEND_KEY;

/**
 * 用户服务实现类
 *
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTagMapper userTagMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "howie";

    /**
     * 复用 Gson 实例，避免每次请求重复创建
     */
    private static final Gson GSON = new Gson();

    /**
     * 标签搜索返回上限，防止一次性返回过多数据
     */
    private static final int MAX_SEARCH_RESULT = 500;

    /**
     * 心动模式候选人数上限：倒排索引预筛后，最多对这么多用户精算编辑距离
     */
    private static final int MATCH_CANDIDATE_LIMIT = 1000;

    /**
     * 心动模式匹配结果缓存过期时间（毫秒）
     */
    private static final long MATCH_CACHE_TTL = 30 * 60 * 1000L;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return -1;
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            return -1;
        }
        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 3. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            return -1;
        }
        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return null;
        }
        if (userAccount.length() < 4) {
            return null;
        }
        if (userPassword.length() < 8) {
            return null;
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            return null;
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            return null;
        }
        // 3. 用户脱敏
        User safetyUser = getSafetyUser(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        // 走 user_tag 关联表 + tag 索引精确匹配，避免 LIKE 全表扫描，也避免全量进内存
        List<User> userList = userMapper.selectUsersByTags(tagNameList, tagNameList.size(), MAX_SEARCH_RESULT);
        // 脱敏后返回
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public List<String> listAllTags() {
        return userTagMapper.selectDistinctTags();
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(User user, HttpServletRequest request) {
        // 获取当前登录用户，校验后更新
        User loginUser = getLoginUser(request);
        return updateUser(user, loginUser);
    }

    /**
     * 是否为管理员
     *
     * @param loginUser
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 仅管理员可查询
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 更新当前用户信息
     * @param user
     * @param loginUser
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        if (userId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不能为空");
        }
        //如果是管理员，允许更改所有用户
        //如果是个人用户，可以更改自己的信息
        if(!isAdmin(loginUser) && userId != loginUser.getId()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无权限修改其他用户信息");
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 1. 先更新数据库
        int result = userMapper.updateById(user);
        // 2. 同步标签到 user_tag 关联表（先删旧、再插新）
        if (user.getTags() != null) {
            userTagMapper.delete(new QueryWrapper<UserTag>().eq("userId", userId));
            List<String> tagList = GSON.fromJson(user.getTags(), new TypeToken<List<String>>() {
            }.getType());
            if (tagList != null) {
                for (String tag : tagList) {
                    if (StringUtils.isNotBlank(tag)) {
                        userTagMapper.insert(new UserTag(userId, tag));
                    }
                }
            }
        }
        // 3. 删除缓存，保证下次查询走数据库拿到最新数据
        deleteKeysByPrefix(USER_RECOMMEND_KEY + userId);
        redisTemplate.delete(USER_MATCH_KEY + userId);
        return result;
    }

    /**
     * 获取当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if(request == null){
            return null;
        }
        //2.校验用户是否合法
        User userObj = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj  == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 推荐匹配用户（心动模式）
     * <p>
     * 优化思路：
     * 1. 方案 A（倒排索引预筛）：用 user_tag 表先把「和当前用户有共同标签」的候选圈出来，
     *    把需要精算编辑距离的对象从 51 万压缩到最多 MATCH_CANDIDATE_LIMIT 个；
     * 2. 方案 B（Redis 缓存）：结果缓存到 Redis，命中直接返回，避免重复计算。
     *
     * @param num       期望返回的用户数量
     * @param loginUser 当前登录用户
     * @return 最相似的 top num 用户（脱敏）
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        // 1. 用数据库最新的用户标签（session 里的 loginUser 可能是修改前的旧快照）
        User dbLoginUser = this.getById(loginUser.getId());
        String tags = dbLoginUser != null ? dbLoginUser.getTags() : loginUser.getTags();
        List<String> tagList = parseTags(tags);
        // 没有标签就没有匹配依据，直接返回空
        if (tagList.isEmpty()) {
            return new ArrayList<>();
        }

        String redisKey = USER_MATCH_KEY + loginUser.getId();

        // 2. 方案 B：先查 Redis 缓存，命中直接返回，省掉 DB 查询 + 编辑距离计算
        List<User> cached = (List<User>) redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            return cached;
        }

        // 3. 方案 A：倒排索引预筛——只查「和当前用户有共同标签」的用户，
        //    按共同标签数降序取前 MATCH_CANDIDATE_LIMIT 个候选，避免全表扫描 + 全量计算
        List<User> candidates = userMapper.selectMatchCandidates(tagList, MATCH_CANDIDATE_LIMIT);

        // 4. 只对少数候选精算编辑距离
        Map<Long, Long> distanceMap = new HashMap<>();
        for (User candidate : candidates) {
            // 跳过自己
            if (candidate.getId().equals(loginUser.getId())) {
                continue;
            }
            List<String> candidateTags = parseTags(candidate.getTags());
            if (candidateTags.isEmpty()) {
                continue;
            }
            long distance = AlgorithmUtils.minDistance(tagList, candidateTags);
            distanceMap.put(candidate.getId(), distance);
        }

        // 5. 按编辑距离升序，取前 num 个最相似的用户 id
        List<Long> topUserIds = distanceMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(num)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 6. 第二阶段：只对匹配到的少量用户查完整信息，并按距离顺序重建结果
        List<User> result = new ArrayList<>();
        if (!topUserIds.isEmpty()) {
            Map<Long, User> idToUserMap = this.listByIds(topUserIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
            for (Long id : topUserIds) {
                User user = idToUserMap.get(id);
                if (user != null) {
                    result.add(getSafetyUser(user));
                }
            }
        }

        // 7. 写缓存（结果为空也缓存，避免空结果被反复重算）
        redisTemplate.opsForValue().set(redisKey, result, MATCH_CACHE_TTL, TimeUnit.MILLISECONDS);
        return result;
    }

    /**
     * 解析 tags JSON 字符串为标签列表
     *
     * @param tagsJson user.tags 字段的 JSON 字符串
     * @return 标签列表（空字符串 / null / 非法 JSON 均返回空列表）
     */
    private List<String> parseTags(String tagsJson) {
        if (StringUtils.isBlank(tagsJson)) {
            return new ArrayList<>();
        }
        List<String> tagList = GSON.fromJson(tagsJson, new TypeToken<List<String>>() {
        }.getType());
        return tagList == null ? new ArrayList<>() : tagList;
    }

    /**
     * 按前缀删除 Redis 缓存。
     * 用于删除某个用户的所有分页推荐缓存（key 形如 {prefix}:{pageNum}:{pageSize}）。
     * <p>
     * 说明：keys + 通配符在 key 量极大的生产环境会阻塞 Redis，建议换 SCAN；
     * 本项目单机、key 量小，直接用 keys 更简单直观。
     *
     * @param prefix key 前缀（不含通配符）
     */
    private void deleteKeysByPrefix(String prefix) {
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 删除用户（逻辑删除），并同步清理标签关联
     *
     * @param id 用户id
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(long id) {
        boolean result = this.removeById(id);
        if (result) {
            userTagMapper.delete(new QueryWrapper<UserTag>().eq("userId", id));
        }
        return result;
    }

    @Override
    public List<String> listMyTags(User loginUser) {
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User user = this.getById(loginUser.getId());
        if (user == null) {
            return new ArrayList<>();
        }
        String tags = user.getTags();
        if (StringUtils.isBlank(tags)) {
            return new ArrayList<>();
        }
        List<String> tagList = GSON.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        return tagList == null ? new ArrayList<>() : tagList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUserTag(String tag, User loginUser) {
        if (StringUtils.isBlank(tag)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        Long userId = loginUser.getId();
        List<String> tags = listMyTags(loginUser);
        if (tags.contains(tag)) {
            // 已有该标签，幂等返回
            return true;
        }
        tags.add(tag);
        updateUserTags(userId, tags);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserTag(String tag, User loginUser) {
        if (StringUtils.isBlank(tag)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        Long userId = loginUser.getId();
        List<String> tags = listMyTags(loginUser);
        if (!tags.remove(tag)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该标签不存在");
        }
        updateUserTags(userId, tags);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserTag(String oldTag, String newTag, User loginUser) {
        if (StringUtils.isAnyBlank(oldTag, newTag)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签不能为空");
        }
        Long userId = loginUser.getId();
        List<String> tags = listMyTags(loginUser);
        int index = tags.indexOf(oldTag);
        if (index < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原标签不存在");
        }
        if (tags.contains(newTag)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新标签已存在");
        }
        tags.set(index, newTag);
        updateUserTags(userId, tags);
        return true;
    }

    /**
     * 统一更新用户标签：同步 user.tags JSON 字段与 user_tag 关联表
     *
     * @param userId 用户 id
     * @param tags   最新的标签列表
     */
    private void updateUserTags(Long userId, List<String> tags) {
        // 1. 更新 user.tags JSON 字段
        User user = new User();
        user.setId(userId);
        user.setTags(GSON.toJson(tags));
        userMapper.updateById(user);
        // 2. 重建 user_tag 关联表（先删旧、再插新）
        userTagMapper.delete(new QueryWrapper<UserTag>().eq("userId", userId));
        if (tags != null) {
            for (String tag : tags) {
                if (StringUtils.isNotBlank(tag)) {
                    userTagMapper.insert(new UserTag(userId, tag));
                }
            }
        }
        // 3. 删除缓存，保证下次推荐查询走数据库拿到最新标签
        deleteKeysByPrefix(USER_RECOMMEND_KEY + userId);
        redisTemplate.delete(USER_MATCH_KEY + userId);
    }

}










