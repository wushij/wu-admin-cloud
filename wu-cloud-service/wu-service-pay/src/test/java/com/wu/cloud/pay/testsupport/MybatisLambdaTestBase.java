package com.wu.cloud.pay.testsupport;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wu.cloud.pay.entity.PayOrderDO;
import com.wu.cloud.pay.entity.PayRefundDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

/**
 * 纯单元测试中构造 LambdaQueryWrapper / UpdateWrapper 前需初始化实体元数据。
 */
public abstract class MybatisLambdaTestBase {

    @BeforeAll
    static void initLambdaEntityCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PayOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, PayRefundDO.class);
    }
}
