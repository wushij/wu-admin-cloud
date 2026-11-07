package com.wu.cloud.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.pay.entity.PayOutboxMessageDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付结果通知本地消息 Mapper
 */
@Mapper
public interface PayOutboxMessageMapper extends BaseMapper<PayOutboxMessageDO> {
}
