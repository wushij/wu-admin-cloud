package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.SmsLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 短信发送记录 Mapper（SQL 层分页）
 */
@Mapper
public interface SmsLogMapper extends BaseMapper<SmsLogDO> {

    @Select({
            "<script>",
            "SELECT * FROM sys_sms_log",
            "WHERE 1=1",
            "<if test='phone != null and phone != \"\"'>",
            "  AND phone LIKE CONCAT('%', #{phone}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY create_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<SmsLogDO> selectPage(@Param("phone") String phone,
                              @Param("status") Integer status,
                              @Param("offset") long offset,
                              @Param("limit") long limit);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_sms_log",
            "WHERE 1=1",
            "<if test='phone != null and phone != \"\"'>",
            "  AND phone LIKE CONCAT('%', #{phone}, '%')",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "</script>"
    })
    long selectCount(@Param("phone") String phone,
                     @Param("status") Integer status);
}
