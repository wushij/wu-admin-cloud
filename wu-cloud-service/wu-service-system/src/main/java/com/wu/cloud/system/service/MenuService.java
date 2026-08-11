package com.wu.cloud.system.service;

import com.wu.cloud.system.entity.SysMenuDO;

import java.util.List;

public interface MenuService {
    List<SysMenuDO> getMenuList();

    List<SysMenuDO> getMenuList(String name, Integer status, Integer type);

    List<SysMenuDO> getSimpleList();

    SysMenuDO getMenu(Long id);

    Long createMenu(SysMenuDO menuDO);

    void updateMenu(SysMenuDO menuDO);

    void deleteMenu(Long id);

    void updateStatus(Long id, Integer status);
}
