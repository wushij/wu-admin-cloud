package com.wu.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.system.entity.SecurityEventDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 安全审计与反爬拦截事件 Mapper
 */
@Mapper
public interface SecurityEventMapper extends BaseMapper<SecurityEventDO> {

    /**
     * 物理清理过期的安全事件，避免审计表无限增长。
     */
    @Delete("DELETE FROM sys_security_event WHERE create_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
