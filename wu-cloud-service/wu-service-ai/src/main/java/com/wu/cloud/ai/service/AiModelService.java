package com.wu.cloud.ai.service;

import com.wu.cloud.ai.dto.AiModelListModelsReqVO;
import com.wu.cloud.ai.dto.AiModelPageReqVO;
import com.wu.cloud.ai.dto.AiModelRespVO;
import com.wu.cloud.ai.dto.AiModelSaveReqVO;
import com.wu.cloud.ai.dto.AiModelTestReqVO;
import com.wu.cloud.ai.entity.AiModelDO;
import com.wu.cloud.common.core.domain.PageResult;

import java.util.List;

/**
 * AI 模型供应商配置管理
 */
public interface AiModelService {

    PageResult<AiModelRespVO> page(AiModelPageReqVO reqVO);

    /** 启用中的模型列表（供对话面板模型选择器使用） */
    List<AiModelRespVO> listEnabled();

    AiModelRespVO detail(Long id);

    void create(AiModelSaveReqVO reqVO);

    void update(AiModelSaveReqVO reqVO);

    void delete(Long id);

    /** 设为全局默认模型（唯一性由本方法保证） */
    void setDefault(Long id);

    /** 连通性测试，返回往返延迟毫秒数 */
    long test(AiModelTestReqVO reqVO);

    /** 拉取供应商可用模型列表，用于配置表单的「获取模型」 */
    List<String> listModels(AiModelListModelsReqVO reqVO);

    /** 获取可用模型：modelId 为空时取默认模型，均校验启用状态 */
    AiModelDO getAvailableModel(Long modelId);

    /** 解密模型的 API Key 明文 */
    String decryptApiKey(AiModelDO model);
}
