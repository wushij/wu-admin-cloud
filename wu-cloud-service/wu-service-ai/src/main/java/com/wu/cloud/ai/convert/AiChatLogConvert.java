package com.wu.cloud.ai.convert;

import com.wu.cloud.ai.entity.AiChatLogDO;
import com.wu.cloud.ai.vo.AiChatLogVO;

public class AiChatLogConvert {

    public static AiChatLogVO convertToVO(AiChatLogDO logDO) {
        if (logDO == null) {
            return null;
        }
        AiChatLogVO vo = new AiChatLogVO();
        vo.setId(logDO.getId());
        vo.setUserId(logDO.getUserId());
        vo.setConversationId(logDO.getConversationId());
        vo.setQuestion(logDO.getQuestion());
        vo.setAnswer(logDO.getAnswer());
        vo.setCreateTime(logDO.getCreateTime());
        return vo;
    }
}
