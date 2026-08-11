package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.EmailLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 邮件发送记录 Mapper（SQL 层分页）
 */
@Mapper
public interface EmailLogMapper extends BaseMapper<EmailLogDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_email_log",
            "WHERE 1=1",
            "<if test='email != null and email != \"\"'>",
            "  AND email LIKE CONCAT('%', #{email}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<EmailLogDO> selectPage(@Param("email") String email,
                                @Param("status") Integer status,
                                @Param("offset") long offset,
                                @Param("limit") long limit);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_email_log",
            "WHERE 1=1",
            "<if test='email != null and email != \"\"'>",
            "  AND email LIKE CONCAT('%', #{email}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "</script>"
    })
    long selectCount(@Param("email") String email,
                     @Param("status") Integer status);
}
