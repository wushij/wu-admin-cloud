package com.wu.cloud.codegen.service;

import com.wu.cloud.codegen.domain.GenTable;

import java.util.Map;

/**
 * 代码生成服务
 */
public interface CodegenService {

    /**
     * 预览生成结果
     *
     * @param table 表结构与生成配置
     * @return key 为相对文件路径，value 为文件内容
     */
    Map<String, String> preview(GenTable table);

    /**
     * 生成代码并打包为 ZIP
     *
     * @param table 表结构与生成配置
     * @return ZIP 字节内容
     */
    byte[] generateZip(GenTable table);
}
