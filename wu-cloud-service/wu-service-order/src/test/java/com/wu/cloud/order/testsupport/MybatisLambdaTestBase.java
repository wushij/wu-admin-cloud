package com.wu.cloud.order.testsupport;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wu.cloud.order.entity.OrderItemDO;
import com.wu.cloud.order.entity.StockDO;
import com.wu.cloud.order.entity.TradeOrderDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

/**
 * 纯单元测试中构造 LambdaQueryWrapper / UpdateWrapper 前需初始化实体元数据。
 */
public abstract class MybatisLambdaTestBase {

    @BeforeAll
    static void initLambdaEntityCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StockDO.class);
        TableInfoHelper.initTableInfo(assistant, TradeOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemDO.class);
    }
}
