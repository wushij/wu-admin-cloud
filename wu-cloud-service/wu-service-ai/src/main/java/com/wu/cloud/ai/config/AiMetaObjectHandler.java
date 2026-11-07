package com.wu.cloud.ai.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wu.cloud.common.core.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 服务 MyBatis-Plus 自动填充（与既有规范对齐 createTime/username）
 */
@Component
public class AiMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String user = resolveUsername();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "creator", String.class, user);
        strictInsertFill(metaObject, "updater", String.class, user);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String user = resolveUsername();
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictUpdateFill(metaObject, "updater", String.class, user);
    }

    private static String resolveUsername() {
        String username = UserContextHolder.getUsername();
        return username != null ? username : "";
    }
}
