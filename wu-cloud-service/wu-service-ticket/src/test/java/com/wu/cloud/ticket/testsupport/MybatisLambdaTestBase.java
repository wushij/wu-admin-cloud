package com.wu.cloud.ticket.testsupport;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wu.cloud.ticket.entity.ApprovalFormDO;
import com.wu.cloud.ticket.entity.ApprovalNodeDO;
import com.wu.cloud.ticket.entity.ApprovalRecordDO;
import com.wu.cloud.ticket.entity.ApprovalTaskDO;
import com.wu.cloud.ticket.entity.TicketDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

/**
 * 纯单元测试中构造 LambdaQueryWrapper / UpdateWrapper 前需初始化实体元数据。
 */
public abstract class MybatisLambdaTestBase {

    @BeforeAll
    static void initLambdaEntityCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TicketDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalFormDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalRecordDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalNodeDO.class);
        TableInfoHelper.initTableInfo(assistant, ApprovalTaskDO.class);
    }
}
