package com.wu.cloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysDictDataDO;
import com.wu.cloud.system.entity.SysDictTypeDO;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.vo.RecycleSummaryVO;

/**
 * system 服务回收站能力
 *
 * <p>覆盖 user / role / menu / dept / dictType / dictData 六个模块。
 * post、notice 等模块的回收站已在各自 Service 中实现，此处不重复。</p>
 */
public interface SysRecycleService {

    Page<SysUserDO> getUserRecyclePage(PageQuery pageQuery, String keyword);

    Page<SysUserDO> getUserRecyclePage(PageQuery pageQuery, String username, String mobile,
                                       Integer status, Long deptId);

    boolean restoreUser(Long id);

    boolean purgeUser(Long id);

    Page<SysRoleDO> getRoleRecyclePage(PageQuery pageQuery, String keyword);

    boolean restoreRole(Long id);

    boolean purgeRole(Long id);

    Page<SysMenuDO> getMenuRecyclePage(PageQuery pageQuery, String keyword);

    boolean restoreMenu(Long id);

    boolean purgeMenu(Long id);

    Page<SysDeptDO> getDeptRecyclePage(PageQuery pageQuery, String keyword);

    boolean restoreDept(Long id);

    boolean purgeDept(Long id);

    Page<SysDictTypeDO> getDictTypeRecyclePage(PageQuery pageQuery, String keyword);

    boolean restoreDictType(Long id);

    boolean purgeDictType(Long id);

    Page<SysDictDataDO> getDictDataRecyclePage(PageQuery pageQuery, String dictType, String keyword);

    boolean restoreDictData(Long id);

    boolean purgeDictData(Long id);

    /**
     * 回收站汇总统计（仅统计本服务数据源内的表）
     */
    RecycleSummaryVO getSummary();
}
