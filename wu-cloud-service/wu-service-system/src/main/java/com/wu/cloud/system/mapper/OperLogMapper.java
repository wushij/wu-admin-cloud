package com.wu.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.system.entity.SysOperLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperLogMapper extends BaseMapper<SysOperLogDO> {
}
