package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.OperLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper 接口
 */
@Mapper
public interface OperLogMapper extends BaseMapper<OperLogDO> {
}
