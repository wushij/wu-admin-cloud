package com.wu.cloud.file.service;

import com.wu.cloud.file.entity.SysFileGroupDO;

import java.util.List;
import java.util.Map;

public interface SysFileGroupService {

    List<SysFileGroupDO> getGroupTree();

    Map<String, Object> listWithUngroupedCount(String fileCategory);

    Long createGroup(SysFileGroupDO groupDO);

    void updateGroup(SysFileGroupDO groupDO);

    void deleteGroup(Long id);
}
