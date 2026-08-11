package com.wu.cloud.ai.tool;

/**
 * 工具执行上下文
 * <p>身份信息一律由服务端从会话注入，工具内部只能使用此处的 userId/username，严禁信任大模型传入的用户标识参数。</p>
 */
public class AiToolContext {

    private final Long userId;
    private final String username;

    public AiToolContext(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
