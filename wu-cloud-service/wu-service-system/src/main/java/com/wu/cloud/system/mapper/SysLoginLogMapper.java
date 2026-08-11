package com.wu.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.system.entity.SysLoginLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper 接口
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLogDO> {
}
