package com.howie.usermatch.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户-标签关联表
 *
 * @TableName user_tag
 */
@TableName(value = "user_tag")
@Data
public class UserTag {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 标签名
     */
    private String tag;

    /**
     * 创建时间
     */
    private Date createTime;

    public UserTag() {
    }

    public UserTag(Long userId, String tag) {
        this.userId = userId;
        this.tag = tag;
    }
}
