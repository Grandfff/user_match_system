package com.howie.usermatch.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 标签操作请求
 */
@Data
public class TagRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签（用于贴标签 / 删除标签）
     */
    private String tag;

    /**
     * 原标签（用于编辑标签）
     */
    private String oldTag;

    /**
     * 新标签（用于编辑标签）
     */
    private String newTag;
}
