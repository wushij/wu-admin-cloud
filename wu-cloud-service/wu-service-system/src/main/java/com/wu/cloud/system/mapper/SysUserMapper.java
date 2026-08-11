package com.wu.cloud.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.system.entity.SysUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户表 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserDO> {
}
