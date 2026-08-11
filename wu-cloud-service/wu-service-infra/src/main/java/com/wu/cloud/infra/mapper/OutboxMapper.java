package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.OutboxDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper（P1-4 修复）
 */
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxDO> {
}
