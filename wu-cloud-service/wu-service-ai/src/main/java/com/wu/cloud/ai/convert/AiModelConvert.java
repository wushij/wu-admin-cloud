package com.wu.cloud.ai.convert;

import cn.hutool.core.util.StrUtil;
import com.wu.cloud.ai.dto.AiModelRespVO;
import com.wu.cloud.ai.dto.AiModelSaveReqVO;
import com.wu.cloud.ai.entity.AiModelDO;

/**
 * AI 模型配置 DO ↔ VO 手写转换器
 */
public final class AiModelConvert {

    private AiModelConvert() {
    }

    /** DO → RespVO，apiKey 转掩码，绝不泄露密文 */
    public static AiModelRespVO convertModel(AiModelDO model, String plainApiKey) {
        if (model == null) {
            return null;
        }
        AiModelRespVO vo = new AiModelRespVO();
        vo.setId(model.getId());
        vo.setName(model.getName());
        vo.setProvider(model.getProvider());
        vo.setModelName(model.getModelName());
        vo.setBaseUrl(model.getBaseUrl());
        vo.setHasApiKey(StrUtil.isNotBlank(model.getApiKey()));
        vo.setApiKeyMasked(maskApiKey(plainApiKey));
        vo.setTemperature(model.getTemperature());
        vo.setMaxTokens(model.getMaxTokens());
        vo.setSystemPrompt(model.getSystemPrompt());
        vo.setIsDefault(model.getIsDefault());
        vo.setStatus(model.getStatus());
        vo.setRemark(model.getRemark());
        vo.setCreateTime(model.getCreateTime());
        vo.setUpdateTime(model.getUpdateTime());
        return vo;
    }

    /** SaveReqVO → DO（apiKey 由 Service 层加密后单独设置） */
    public static AiModelDO convertSave(AiModelSaveReqVO reqVO) {
        if (reqVO == null) {
            return null;
        }
        AiModelDO model = new AiModelDO();
        model.setId(reqVO.getId());
        model.setName(reqVO.getName());
        model.setProvider(reqVO.getProvider());
        model.setModelName(reqVO.getModelName());
        model.setBaseUrl(reqVO.getBaseUrl());
        model.setTemperature(reqVO.getTemperature());
        model.setMaxTokens(reqVO.getMaxTokens());
        model.setSystemPrompt(reqVO.getSystemPrompt());
        model.setIsDefault(reqVO.getIsDefault());
        model.setStatus(reqVO.getStatus());
        model.setRemark(reqVO.getRemark());
        return model;
    }

    /** API Key 掩码：保留前 4 后 4 位，中间以 *** 代替 */
    public static String maskApiKey(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return "";
        }
        if (plainKey.length() <= 8) {
            return plainKey.charAt(0) + "***";
        }
        return plainKey.substring(0, 4) + "***" + plainKey.substring(plainKey.length() - 4);
    }
}
