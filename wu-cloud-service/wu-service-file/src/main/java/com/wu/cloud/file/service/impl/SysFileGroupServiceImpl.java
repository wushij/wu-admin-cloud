package com.wu.cloud.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.entity.SysFileGroupDO;
import com.wu.cloud.file.mapper.SysFileGroupMapper;
import com.wu.cloud.file.mapper.SysFileMapper;
import com.wu.cloud.file.service.SysFileGroupService;
import com.wu.cloud.file.service.SysFileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysFileGroupServiceImpl implements SysFileGroupService {

    @Resource
    private SysFileGroupMapper sysFileGroupMapper;
    @Resource
    private SysFileMapper sysFileMapper;
    @Resource
    private SysFileService sysFileService;

    @Override
    public List<SysFileGroupDO> getGroupTree() {
        return sysFileGroupMapper.selectList(new LambdaQueryWrapper<SysFileGroupDO>()
                .orderByAsc(SysFileGroupDO::getSort)
                .orderByAsc(SysFileGroupDO::getId));
    }

    @Override
    public Map<String, Object> listWithUngroupedCount(String fileCategory) {
        List<SysFileGroupDO> groups = getGroupTree();
        for (SysFileGroupDO group : groups) {
            group.setFileCount((int) sysFileService.countForGroupSidebar(group.getId(), false, fileCategory));
        }
        long ungroupedCount = sysFileService.countForGroupSidebar(null, true, fileCategory);
        Map<String, Object> result = new HashMap<>(2);
        result.put("groups", groups);
        result.put("ungroupedCount", ungroupedCount);
        return result;
    }

    @Override
    public Long createGroup(SysFileGroupDO groupDO) {
        if (groupDO == null || !StringUtils.hasText(groupDO.getName())) {
            throw new BusinessException("分组名称不能为空");
        }
        if (groupDO.getSort() == null) {
            groupDO.setSort(0);
        }
        sysFileGroupMapper.insert(groupDO);
        return groupDO.getId();
    }

    @Override
    public void updateGroup(SysFileGroupDO groupDO) {
        if (groupDO == null || groupDO.getId() == null) {
            throw new BusinessException("分组ID不能为空");
        }
        if (!StringUtils.hasText(groupDO.getName())) {
            throw new BusinessException("分组名称不能为空");
        }
        sysFileGroupMapper.updateById(groupDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long id) {
        sysFileMapper.update(null, new LambdaUpdateWrapper<SysFileDO>()
                .eq(SysFileDO::getGroupId, id)
                .set(SysFileDO::getGroupId, null));
        sysFileGroupMapper.deleteById(id);
    }
}
