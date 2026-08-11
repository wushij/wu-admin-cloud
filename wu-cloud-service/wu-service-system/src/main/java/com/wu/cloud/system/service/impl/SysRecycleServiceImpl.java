package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysDictDataDO;
import com.wu.cloud.system.entity.SysDictTypeDO;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.mapper.SysRecycleMapper;
import com.wu.cloud.system.service.SysRecycleService;
import com.wu.cloud.system.vo.RecycleSummaryVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 回收站服务实现
 *
 * <p>分页在 SQL 层用 LIMIT 完成，避免依赖未注册的 MP 分页插件。</p>
 */
@Service
public class SysRecycleServiceImpl implements SysRecycleService {

    /** 单页最大条数，防止前端传入超大 pageSize 拖垮数据库 */
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private SysRecycleMapper sysRecycleMapper;

    // ==================== 用户 ====================

    @Override
    public Page<SysUserDO> getUserRecyclePage(PageQuery pageQuery, String keyword) {
        return getUserRecyclePage(pageQuery, null, null, null, null, keyword);
    }

    @Override
    public Page<SysUserDO> getUserRecyclePage(PageQuery pageQuery, String username, String mobile,
                                             Integer status, Long deptId) {
        return getUserRecyclePage(pageQuery, username, mobile, status, deptId, null);
    }

    private Page<SysUserDO> getUserRecyclePage(PageQuery pageQuery, String username, String mobile,
                                               Integer status, Long deptId, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedUsers(keyword, username, mobile, status, deptId);
        List<SysUserDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedUsers(keyword, username, mobile, status, deptId,
                (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreUser(Long id) {
        return sysRecycleMapper.restoreUser(id) > 0;
    }

    @Override
    public boolean purgeUser(Long id) {
        return sysRecycleMapper.purgeUser(id) > 0;
    }

    // ==================== 角色 ====================

    @Override
    public Page<SysRoleDO> getRoleRecyclePage(PageQuery pageQuery, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedRoles(keyword);
        List<SysRoleDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedRoles(keyword, (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreRole(Long id) {
        return sysRecycleMapper.restoreRole(id) > 0;
    }

    @Override
    public boolean purgeRole(Long id) {
        return sysRecycleMapper.purgeRole(id) > 0;
    }

    // ==================== 菜单 ====================

    @Override
    public Page<SysMenuDO> getMenuRecyclePage(PageQuery pageQuery, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedMenus(keyword);
        List<SysMenuDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedMenus(keyword, (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreMenu(Long id) {
        return sysRecycleMapper.restoreMenu(id) > 0;
    }

    @Override
    public boolean purgeMenu(Long id) {
        return sysRecycleMapper.purgeMenu(id) > 0;
    }

    // ==================== 部门 ====================

    @Override
    public Page<SysDeptDO> getDeptRecyclePage(PageQuery pageQuery, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedDepts(keyword);
        List<SysDeptDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedDepts(keyword, (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreDept(Long id) {
        return sysRecycleMapper.restoreDept(id) > 0;
    }

    @Override
    public boolean purgeDept(Long id) {
        return sysRecycleMapper.purgeDept(id) > 0;
    }

    // ==================== 字典类型 ====================

    @Override
    public Page<SysDictTypeDO> getDictTypeRecyclePage(PageQuery pageQuery, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedDictTypes(keyword);
        List<SysDictTypeDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedDictTypes(keyword, (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreDictType(Long id) {
        return sysRecycleMapper.restoreDictType(id) > 0;
    }

    @Override
    public boolean purgeDictType(Long id) {
        return sysRecycleMapper.purgeDictType(id) > 0;
    }

    // ==================== 字典数据 ====================

    @Override
    public Page<SysDictDataDO> getDictDataRecyclePage(PageQuery pageQuery, String dictType, String keyword) {
        int no = normalizePageNo(pageQuery);
        int size = normalizePageSize(pageQuery);
        long total = sysRecycleMapper.countDeletedDictData(dictType, keyword);
        List<SysDictDataDO> records = total == 0
                ? List.of()
                : sysRecycleMapper.selectDeletedDictData(dictType, keyword, (no - 1) * size, size);
        return buildPage(no, size, total, records);
    }

    @Override
    public boolean restoreDictData(Long id) {
        return sysRecycleMapper.restoreDictData(id) > 0;
    }

    @Override
    public boolean purgeDictData(Long id) {
        return sysRecycleMapper.purgeDictData(id) > 0;
    }

    // ==================== 汇总 ====================

    @Override
    public RecycleSummaryVO getSummary() {
        RecycleSummaryVO vo = new RecycleSummaryVO();
        vo.setUserCount(sysRecycleMapper.countDeletedUsers(null, null, null, null, null));
        vo.setRoleCount(sysRecycleMapper.countDeletedRoles(null));
        vo.setMenuCount(sysRecycleMapper.countDeletedMenus(null));
        vo.setDeptCount(sysRecycleMapper.countDeletedDepts(null));
        vo.setPostCount(sysRecycleMapper.countDeletedPosts());
        vo.setDictTypeCount(sysRecycleMapper.countDeletedDictTypes(null));
        vo.setDictDataCount(sysRecycleMapper.countDeletedDictData(null, null));
        vo.setTotalCount(vo.getUserCount() + vo.getRoleCount() + vo.getMenuCount()
                + vo.getDeptCount() + vo.getPostCount()
                + vo.getDictTypeCount() + vo.getDictDataCount());
        return vo;
    }

    // ==================== 内部工具 ====================

    private int normalizePageNo(PageQuery pageQuery) {
        if (pageQuery == null || pageQuery.getPageNo() == null || pageQuery.getPageNo() < 1) {
            return 1;
        }
        return pageQuery.getPageNo();
    }

    private int normalizePageSize(PageQuery pageQuery) {
        if (pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1) {
            return 10;
        }
        return Math.min(pageQuery.getPageSize(), MAX_PAGE_SIZE);
    }

    private <T> Page<T> buildPage(int no, int size, long total, List<T> records) {
        Page<T> page = new Page<>(no, size, total);
        page.setRecords(records);
        return page;
    }
}
