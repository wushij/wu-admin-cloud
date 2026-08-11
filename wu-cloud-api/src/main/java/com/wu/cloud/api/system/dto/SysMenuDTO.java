package com.wu.cloud.api.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单跨服务 DTO，字段与前端 MenuTreeNode 类型对齐
 */
@Data
public class SysMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private String path;
    private String component;
    private Integer type;
    private String permission;
    private String icon;
    private Integer status;
    private Integer visible;

    /** 子节点（组装树形结构时使用） */
    private List<SysMenuDTO> children;
}
