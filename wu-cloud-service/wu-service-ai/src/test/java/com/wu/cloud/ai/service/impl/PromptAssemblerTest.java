package com.wu.cloud.ai.service.impl;

import com.wu.cloud.ai.util.AiPromptTemplates;
import com.wu.cloud.common.config.DynamicConfigHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PromptAssembler 单元测试")
class PromptAssemblerTest {

    @Mock
    private DynamicConfigHelper dynamicConfigHelper;
    @Mock
    private KnowledgeRetriever knowledgeRetriever;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PromptAssembler promptAssembler;

    @Test
    @DisplayName("assemble：命中缓存时注入人设/用户上下文/模型提示词/防护")
    void assemble_fromCache_includesCoreBlocks() {
        when(dynamicConfigHelper.getAiAnswerScope()).thenReturn("all");
        when(dynamicConfigHelper.getAiGlobalKnowledge()).thenReturn("");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("ai:user_context:v2:1")).thenReturn(
                "{\"ctx\":\"当前用户：张三 (zhangsan)\",\"menu\":\"\",\"menuTop\":\"\"}");
        when(knowledgeRetriever.retrieveReferenceBlock("你好")).thenReturn("");

        String prompt = promptAssembler.assemble(1L, "MODEL_SYS", "你好");

        assertTrue(prompt.contains("AI wu助手"));
        assertTrue(prompt.contains("当前用户：张三 (zhangsan)"));
        assertTrue(prompt.contains("当前使用 PC 端"));
        assertTrue(prompt.contains("MODEL_SYS"));
        assertTrue(prompt.contains("安全声明"));
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("assemble：focus 范围追加边界规则")
    void assemble_focusScope_appendsRule() {
        when(dynamicConfigHelper.getAiAnswerScope()).thenReturn("focus");
        when(dynamicConfigHelper.getAiGlobalKnowledge()).thenReturn("");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(
                "{\"ctx\":\"当前用户：张三 (zhangsan)\",\"menu\":\"\",\"menuTop\":\"\"}");
        when(knowledgeRetriever.retrieveReferenceBlock(any())).thenReturn("");

        String prompt = promptAssembler.assemble(1L, null, "pc", "q");
        assertTrue(prompt.contains("优先回答与本系统相关"));
        assertTrue(prompt.contains(AiPromptTemplates.PERSONA.substring(0, 10)));
    }

    @Test
    @DisplayName("assemble：全局知识注入到 project_knowledge")
    void assemble_injectsGlobalKnowledge() {
        when(dynamicConfigHelper.getAiAnswerScope()).thenReturn("all");
        when(dynamicConfigHelper.getAiGlobalKnowledge()).thenReturn("本系统支持工单与审批");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(
                "{\"ctx\":\"当前用户：李四 (lisi)\",\"menu\":\"\",\"menuTop\":\"\"}");
        when(knowledgeRetriever.retrieveReferenceBlock(any())).thenReturn("");

        String prompt = promptAssembler.assemble(2L, "", "hello");
        assertTrue(prompt.contains("<project_knowledge>"));
        assertTrue(prompt.contains("本系统支持工单与审批"));
    }
}
