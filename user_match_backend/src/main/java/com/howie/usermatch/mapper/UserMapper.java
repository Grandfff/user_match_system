package com.howie.usermatch.mapper;

import com.howie.usermatch.model.domain.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 *
 *
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据标签集合精确匹配用户（走 user_tag 关联表 + tag 索引，避免 LIKE 全表扫描）
     *
     * @param tagNameList 标签列表（需同时全部命中）
     * @param tagCount    标签数量
     * @param limit       返回上限
     * @return 匹配用户列表（不含密码字段）
     */
    @Select("<script>" +
            "SELECT u.id, u.username, u.userAccount, u.avatarUrl, u.gender, " +
            "u.phone, u.email, u.userRole, u.userStatus, u.createTime, " +
            "u.tags, u.planetCode, u.profile " +
            "FROM user u " +
            "WHERE u.isDelete = 0 AND u.userStatus = 0 " +
            "AND u.id IN (" +
            "  SELECT ut.userId FROM user_tag ut " +
            "  WHERE ut.tag IN " +
            "  <foreach collection='tagNameList' item='tag' open='(' separator=',' close=')'>" +
            "    #{tag}" +
            "  </foreach>" +
            "  GROUP BY ut.userId " +
            "  HAVING COUNT(DISTINCT ut.tag) = #{tagCount}" +
            ") " +
            "ORDER BY u.id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<User> selectUsersByTags(@Param("tagNameList") List<String> tagNameList,
                                 @Param("tagCount") int tagCount,
                                 @Param("limit") int limit);

    /**
     * 心动模式候选预筛：查「和当前用户至少共一个标签」的用户，
     * 按共同标签数降序、取前 limit 个，只返回 id 和 tags 两个字段，用于后续精算编辑距离。
     * <p>
     * 走 user_tag 表的 idx_tag 索引做 IN 精确匹配，避免对 51 万用户全表扫描 + 全量计算。
     *
     * @param tagNameList 当前用户的标签列表
     * @param limit       候选人数上限
     * @return 候选用户（仅 id + tags 字段）
     */
    @Select("<script>" +
            "SELECT u.id, u.tags " +
            "FROM user u " +
            "INNER JOIN ( " +
            "  SELECT ut.userId " +
            "  FROM user_tag ut " +
            "  WHERE ut.tag IN " +
            "  <foreach collection='tagNameList' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "  GROUP BY ut.userId " +
            "  ORDER BY COUNT(DISTINCT ut.tag) DESC " +
            "  LIMIT #{limit}" +
            ") t ON u.id = t.userId " +
            "WHERE u.isDelete = 0 " +
            "</script>")
    List<User> selectMatchCandidates(@Param("tagNameList") List<String> tagNameList,
                                     @Param("limit") int limit);
}
