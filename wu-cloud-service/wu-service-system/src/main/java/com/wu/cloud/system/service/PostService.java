package com.wu.cloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysPostDO;

import java.util.List;

public interface PostService {

    /**
     * 岗位分页
     */
    Page<SysPostDO> getPostPage(PageQuery pageQuery, String postName);

    /**
     * 岗位树形结构
     */
    List<SysPostDO> getPostTree();

    /**
     * 岗位列表（不分页）
     */
    List<SysPostDO> getPostList(String postName, Integer status);

    /**
     * 获取单个岗位
     */
    SysPostDO getPost(Long id);

    /**
     * 新建岗位
     */
    Long createPost(SysPostDO postDO);

    /**
     * 更新岗位
     */
    void updatePost(SysPostDO postDO);

    /**
     * 逻辑删除岗位（禁止删除含有子节点的岗位）
     */
    void deletePost(Long id);

    /**
     * 移动岗位（调整父节点）
     */
    void movePost(Long id, Long parentId);

    /**
     * 回收站列表
     */
    Page<SysPostDO> getRecyclePage(PageQuery pageQuery, String postName);

    /**
     * 从回收站恢复
     */
    void restorePost(Long id);

    /**
     * 物理删除（回收站清空）
     */
    void deletePostPermanent(Long id);
}
