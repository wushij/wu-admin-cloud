package com.wu.cloud.api.infra.feign.recycle;

import com.wu.cloud.common.core.domain.R;

import java.util.Map;

/**
 * 回收站计数内部端点契约（各业务服务实现 /inner/recycle/count）。
 * 各目标服务的 Feign 客户端继承本接口，保证签名统一。
 */
public interface RecycleCountEndpoint {

    /**
     * 返回该服务所辖维度的回收站计数（key -> count）
     */
    R<Map<String, Object>> count();
}
