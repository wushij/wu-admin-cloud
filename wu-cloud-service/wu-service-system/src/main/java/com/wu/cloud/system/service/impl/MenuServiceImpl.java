package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.mapper.MenuMapper;
import com.wu.cloud.system.service.MenuService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class MenuServiceImpl implements MenuService {

    @Resource
    private MenuMapper menuMapper;

    @Override
    public List<SysMenuDO> getMenuList() {
        // 给认证中心 /auth/info 或工作台侧栏使用：需要“全量平铺菜单”，否则只拿到根节点会导致权限计算失败
        return getMenuListFlat(null, null, null);
    }

    @Override
    public List<SysMenuDO> getMenuList(String name, Integer status, Integer type) {
        // 给菜单管理页面使用：需要树形结构（children），以便前端根据 level 递归展示缩进
        List<SysMenuDO> all = getMenuListFlat(name, status, type);
        return buildTree(all);
    }

    private List<SysMenuDO> getMenuListFlat(String name, Integer status, Integer type) {
        LambdaQueryWrapper<SysMenuDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(SysMenuDO::getName, name.trim());
        }
        if (status != null) {
            wrapper.eq(SysMenuDO::getStatus, status);
        }
        if (type != null) {
            wrapper.eq(SysMenuDO::getType, type);
        }
        wrapper.orderByAsc(SysMenuDO::getSort).orderByAsc(SysMenuDO::getId);
        return menuMapper.selectList(wrapper);
    }

    private List<SysMenuDO> buildTree(List<SysMenuDO> all) {
        Map<Long, SysMenuDO> map = new LinkedHashMap<>();
        for (SysMenuDO m : all) {
            m.setChildren(new ArrayList<>());
            map.put(m.getId(), m);
        }
        List<SysMenuDO> roots = new ArrayList<>();
        for (SysMenuDO m : all) {
            Long pid = m.getParentId();
            if (pid == null || pid == 0 || !map.containsKey(pid)) {
                roots.add(m);
            } else {
                map.get(pid).getChildren().add(m);
            }
        }
        return roots;
    }

    @Override
    public List<SysMenuDO> getSimpleList() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                .select(SysMenuDO::getId, SysMenuDO::getName, SysMenuDO::getParentId,
                        SysMenuDO::getType, SysMenuDO::getSort, SysMenuDO::getStatus)
                .orderByAsc(SysMenuDO::getSort)
                .orderByAsc(SysMenuDO::getId));
    }

    @Override
    public SysMenuDO getMenu(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public Long createMenu(SysMenuDO menuDO) {
        menuMapper.insert(menuDO);
        return menuDO.getId();
    }

    @Override
    public void updateMenu(SysMenuDO menuDO) {
        menuMapper.updateById(menuDO);
    }

    @Override
    public void deleteMenu(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysMenuDO menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(404, "菜单不存在");
        }
        menu.setStatus(status);
        menuMapper.updateById(menu);
    }
}
