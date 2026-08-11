package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.SysJobDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 定时任务 Mapper。
 * 注意：当前服务未注册 PaginationInnerInterceptor，selectPage 不会真正分页，
 * 因此分页查询在 SQL 层用 LIMIT 完成。
 */
@Mapper
public interface SysJobMapper extends BaseMapper<SysJobDO> {

    /**
     * 分页查询未删除任务（SQL 层分页）
     */
    @Select({
            "<script>",
            "SELECT * FROM sys_job",
            "WHERE deleted = 0",
            "<if test='jobName != null and jobName != \"\"'>",
            "  AND job_name LIKE CONCAT('%', #{jobName}, '%')",
            "</if>",
            "<if test='jobGroup != null and jobGroup != \"\"'>",
            "  AND job_group = #{jobGroup}",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "ORDER BY id DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<SysJobDO> selectPage(@Param("jobName") String jobName,
                              @Param("jobGroup") String jobGroup,
                              @Param("status") Integer status,
                              @Param("offset") long offset,
                              @Param("limit") long limit);

    /**
     * 统计未删除任务总数
     */
    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_job",
            "WHERE deleted = 0",
            "<if test='jobName != null and jobName != \"\"'>",
            "  AND job_name LIKE CONCAT('%', #{jobName}, '%')",
            "</if>",
            "<if test='jobGroup != null and jobGroup != \"\"'>",
            "  AND job_group = #{jobGroup}",
            "</if>",
            "<if test='status != null'>",
            "  AND status = #{status}",
            "</if>",
            "</script>"
    })
    long selectPageCount(@Param("jobName") String jobName,
                         @Param("jobGroup") String jobGroup,
                         @Param("status") Integer status);

    /**
     * 恢复回收站任务（绕过 @TableLogic 的 deleted=0 过滤，按 id 直接置 0）
     */
    @Update("UPDATE sys_job SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    /**
     * 彻底删除回收站任务（物理删除，绕过 @TableLogic）
     */
    @Delete("DELETE FROM sys_job WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 分页查询已删除（回收站）任务
     */
    @Select({
            "<script>",
            "SELECT * FROM sys_job",
            "WHERE deleted = 1",
            "<if test='jobName != null and jobName != \"\"'>",
            "  AND job_name LIKE CONCAT('%', #{jobName}, '%')",
            "</if>",
            "<if test='jobGroup != null and jobGroup != \"\"'>",
            "  AND job_group = #{jobGroup}",
            "</if>",
            "ORDER BY update_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<SysJobDO> selectDeletedPage(@Param("jobName") String jobName,
                                     @Param("jobGroup") String jobGroup,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_job WHERE deleted = 1",
            "<if test='jobName != null and jobName != \"\"'>",
            "  AND job_name LIKE CONCAT('%', #{jobName}, '%')",
            "</if>",
            "<if test='jobGroup != null and jobGroup != \"\"'>",
            "  AND job_group = #{jobGroup}",
            "</if>",
            "</script>"
    })
    long selectDeletedCount(@Param("jobName") String jobName,
                            @Param("jobGroup") String jobGroup);
}
