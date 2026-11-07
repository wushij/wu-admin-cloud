package com.wu.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.ai.entity.AiKnowledgeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiKnowledgeMapper extends BaseMapper<AiKnowledgeDO> {

    /**
     * 全文检索（ngram），仅返回启用条目，按相关度 + 权重排序取前 limit 条
     * <p>BOOLEAN MODE 对中文短词更稳；MyBatis #{} 绑定 MATCH 表达式安全。</p>
     */
    @Select("""
            SELECT * FROM sys_ai_knowledge
            WHERE deleted = 0 AND status = 1
              AND MATCH(title, keywords, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY MATCH(title, keywords, content) AGAINST(#{query} IN NATURAL LANGUAGE MODE) DESC,
                     sort DESC, id ASC
            LIMIT #{limit}
            """)
    List<AiKnowledgeDO> searchFullText(@Param("query") String query, @Param("limit") int limit);

    /**
     * 知识库统计：总数、启用数、启用条目的正文字符数，避免把全部正文加载到 JVM 求和。
     */
    @Select("""
            SELECT COUNT(*) AS total,
                   COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS activeCount,
                   COALESCE(SUM(CASE WHEN status = 1 THEN CHAR_LENGTH(content) ELSE 0 END), 0) AS totalWords
            FROM sys_ai_knowledge
            WHERE deleted = 0
            """)
    java.util.Map<String, Object> selectStats();
}
