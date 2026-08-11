package com.wu.cloud.system.service;

import com.wu.cloud.system.entity.SysDeptDO;
import java.util.List;

public interface DeptService {
    List<SysDeptDO> getDeptList();
    SysDeptDO getDept(Long id);
    Long createDept(SysDeptDO deptDO);
    void updateDept(SysDeptDO deptDO);
    void deleteDept(Long id);

    /** 返回指定部门及其全部子孙部门 ID（含自身） */
    List<Long> listSelfAndDescendantIds(Long deptId);

    void updateStatus(Long id, Integer status);

    void moveDept(Long id, Long parentId);
}
