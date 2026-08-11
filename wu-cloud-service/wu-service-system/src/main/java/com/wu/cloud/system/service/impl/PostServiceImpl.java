package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysPostDO;
import com.wu.cloud.system.mapper.PostMapper;
import com.wu.cloud.system.service.PostService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    @Resource
    private PostMapper postMapper;

    @Override
    public Page<SysPostDO> getPostPage(PageQuery pageQuery, String postName) {
        Page<SysPostDO> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysPostDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(postName)) {
            wrapper.like(SysPostDO::getPostName, postName);
        }
        wrapper.orderByAsc(SysPostDO::getParentId).orderByAsc(SysPostDO::getSort);
        return postMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysPostDO> getPostTree() {
        List<SysPostDO> all = postMapper.selectList(new LambdaQueryWrapper<SysPostDO>()
                .orderByAsc(SysPostDO::getParentId).orderByAsc(SysPostDO::getSort));
        return buildTree(all, 0L);
    }

    @Override
    public List<SysPostDO> getPostList(String postName, Integer status) {
        LambdaQueryWrapper<SysPostDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(postName)) {
            wrapper.like(SysPostDO::getPostName, postName);
        }
        if (status != null) {
            wrapper.eq(SysPostDO::getStatus, status);
        }
        wrapper.orderByAsc(SysPostDO::getParentId).orderByAsc(SysPostDO::getSort);
        return postMapper.selectList(wrapper);
    }

    @Override
    public SysPostDO getPost(Long id) {
        return postMapper.selectById(id);
    }

    @Override
    public Long createPost(SysPostDO postDO) {
        if (postDO.getParentId() == null) {
            postDO.setParentId(0L);
        }
        postMapper.insert(postDO);
        return postDO.getId();
    }

    @Override
    public void updatePost(SysPostDO postDO) {
        postMapper.updateById(postDO);
    }

    @Override
    public void deletePost(Long id) {
        long childCount = postMapper.selectCount(new LambdaQueryWrapper<SysPostDO>().eq(SysPostDO::getParentId, id));
        if (childCount > 0) {
            throw new IllegalArgumentException("该岗位下存在子岗位，无法删除");
        }
        // BaseEntity 带有 @TableLogic，deleteById 走逻辑删除
        postMapper.deleteById(id);
    }

    @Override
    public void movePost(Long id, Long parentId) {
        SysPostDO postDO = new SysPostDO();
        postDO.setId(id);
        postDO.setParentId(parentId == null ? 0L : parentId);
        postMapper.updateById(postDO);
    }

    @Override
    public Page<SysPostDO> getRecyclePage(PageQuery pageQuery, String postName) {
        List<SysPostDO> all = postMapper.selectList(new LambdaQueryWrapper<SysPostDO>()
                .eq(SysPostDO::getDeleted, 1)
                .like(StringUtils.hasText(postName), SysPostDO::getPostName, postName)
                .orderByDesc(SysPostDO::getUpdateTime));
        Page<SysPostDO> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        page.setRecords(all.stream()
                .skip((pageQuery.getPageNo() - 1L) * pageQuery.getPageSize())
                .limit(pageQuery.getPageSize())
                .collect(Collectors.toList()));
        page.setTotal(all.size());
        return page;
    }

    @Override
    public void restorePost(Long id) {
        SysPostDO postDO = new SysPostDO();
        postDO.setId(id);
        postDO.setDeleted(0);
        postMapper.updateById(postDO);
    }

    @Override
    public void deletePostPermanent(Long id) {
        postMapper.deleteById(id);
    }

    private List<SysPostDO> buildTree(List<SysPostDO> all, Long rootId) {
        Map<Long, List<SysPostDO>> childrenMap = new HashMap<>();
        for (SysPostDO node : all) {
            Long pid = node.getParentId() == null ? 0L : node.getParentId();
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(node);
        }
        List<SysPostDO> tree = childrenMap.getOrDefault(rootId, new ArrayList<>());
        fillChildren(tree, childrenMap);
        return tree;
    }

    private void fillChildren(List<SysPostDO> nodes, Map<Long, List<SysPostDO>> childrenMap) {
        for (SysPostDO node : nodes) {
            List<SysPostDO> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
            node.setChildren(children);
            if (!children.isEmpty()) {
                fillChildren(children, childrenMap);
            }
        }
    }
}
