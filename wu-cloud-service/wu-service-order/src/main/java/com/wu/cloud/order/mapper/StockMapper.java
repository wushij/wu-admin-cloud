package com.wu.cloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.order.entity.StockDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockMapper extends BaseMapper<StockDO> {
}
