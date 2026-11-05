package com.howie.usermatch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.howie.usermatch.model.domain.UserTag;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-标签关联表 Mapper
 */
public interface UserTagMapper extends BaseMapper<UserTag> {

    /**
     * 查询全部标签（去重，按使用人数降序）
     *
     * @return 标签列表
     */
    @Select("SELECT tag FROM user_tag GROUP BY tag ORDER BY COUNT(*) DESC, tag ASC")
    List<String> selectDistinctTags();
}
