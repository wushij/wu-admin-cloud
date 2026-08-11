package com.wu.cloud.infra.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 内置任务模板
 */
@Data
public class JobTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String name;
    private String category;
    private String description;
    private String invokeTarget;
    private String cronExpression;
    private String jobGroup;
    private String relatedModule;
    private String icon;
}
