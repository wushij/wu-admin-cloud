package com.wu.cloud.message.testsupport;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wu.cloud.message.entity.ChatGroupDO;
import com.wu.cloud.message.entity.ChatGroupMemberDO;
import com.wu.cloud.message.entity.ChatGroupMessageDO;
import com.wu.cloud.message.entity.ChatMessageDO;
import com.wu.cloud.message.entity.UserBlacklistDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;

/**
 * 纯单元测试中构造 LambdaQueryWrapper 前需初始化实体元数据（与既有规范对齐 MybatisLambdaTestBase）。
 */
public abstract class MybatisLambdaTestBase {

    @BeforeAll
    static void initLambdaEntityCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, UserBlacklistDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatMessageDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupMemberDO.class);
        TableInfoHelper.initTableInfo(assistant, ChatGroupMessageDO.class);
    }
}
