package com.wu.cloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.file.entity.CodegenTableDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 代码生成配置 Mapper 接口
 */
@Mapper
public interface CodegenTableMapper extends BaseMapper<CodegenTableDO> {

    /**
     * 查询回收站（已逻辑删除）的记录，绕过 @TableLogic 自动过滤
     */
    @Select("<script>" +
            "SELECT * FROM gen_table WHERE deleted = 1 " +
            "<if test='tableName != null and tableName != \"\"'>AND table_name LIKE CONCAT('%', #{tableName}, '%')</if> " +
            "ORDER BY id DESC" +
            "</script>")
    List<CodegenTableDO> selectRecycleList(@Param("tableName") String tableName);
}
