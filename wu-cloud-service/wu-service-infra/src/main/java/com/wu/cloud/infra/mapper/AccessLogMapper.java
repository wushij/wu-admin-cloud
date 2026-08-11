package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.AccessLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * API 访问日志 Mapper 接口
 */
@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLogDO> {
}
