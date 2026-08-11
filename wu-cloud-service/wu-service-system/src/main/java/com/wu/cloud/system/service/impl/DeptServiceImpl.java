package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.mapper.DeptMapper;
import com.wu.cloud.system.service.DeptService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DeptServiceImpl implements DeptService {

    @Resource
    private DeptMapper deptMapper;

    @Override
    public List<SysDeptDO> getDeptList() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>().orderByAsc(SysDeptDO::getSort));
    }

    @Override
    public SysDeptDO getDept(Long id) {
        return deptMapper.selectById(id);
    }

    @Override
    public Long createDept(SysDeptDO deptDO) {
        deptMapper.insert(deptDO);
        return deptDO.getId();
    }

    @Override
    public void updateDept(SysDeptDO deptDO) {
        deptMapper.updateById(deptDO);
    }

    @Override
    public void deleteDept(Long id) {
        deptMapper.deleteById(id);
    }

    @Override
    public List<Long> listSelfAndDescendantIds(Long deptId) {
        if (deptId == null) {
            return List.of();
        }
        List<SysDeptDO> all = getDeptList();
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SysDeptDO dept : all) {
            Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        collectDescendants(deptId, childrenMap, result, visited);
        return result;
    }

    private void collectDescendants(Long id, Map<Long, List<Long>> childrenMap,
                                    List<Long> result, Set<Long> visited) {
        if (id == null || !visited.add(id)) {
            return;
        }
        result.add(id);
        List<Long> children = childrenMap.getOrDefault(id, List.of());
        for (Long childId : children) {
            collectDescendants(childId, childrenMap, result, visited);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysDeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        dept.setStatus(status);
        deptMapper.updateById(dept);
    }

    @Override
    public void moveDept(Long id, Long parentId) {
        SysDeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(404, "部门不存在");
        }
        if (parentId != null && parentId.equals(id)) {
            throw new BusinessException("不能将部门移动到自身下");
        }
        if (parentId != null && parentId > 0) {
            List<Long> descendants = listSelfAndDescendantIds(id);
            if (descendants.contains(parentId)) {
                throw new BusinessException("不能将部门移动到其子部门下");
            }
            if (deptMapper.selectById(parentId) == null) {
                throw new BusinessException(404, "目标父部门不存在");
            }
        }
        dept.setParentId(parentId == null ? 0L : parentId);
        deptMapper.updateById(dept);
    }
}
