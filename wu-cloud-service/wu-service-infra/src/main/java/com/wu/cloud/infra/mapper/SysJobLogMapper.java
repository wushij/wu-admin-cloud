package com.wu.cloud.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.cloud.infra.entity.SysJobLogDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 定时任务执行日志 Mapper（SQL 层分页）
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLogDO> {

    /**
     * 近 7 天每日执行统计
     */
    @Select("SELECT DATE_FORMAT(start_time, '%Y-%m-%d') AS exec_date, " +
            "SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS success_count, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS fail_count " +
            "FROM sys_job_log " +
            "WHERE deleted = 0 AND start_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE_FORMAT(start_time, '%Y-%m-%d') " +
            "ORDER BY exec_date")
    List<Map<String, Object>> selectDailyStats();

    /**
     * 分页查询未删除执行日志
     */
    @Select({
            "<script>",
            "SELECT * FROM sys_job_log",
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
            "ORDER BY start_time DESC",
            "LIMIT #{offset}, #{limit}",
            "</script>"
    })
    List<SysJobLogDO> selectPage(@Param("jobName") String jobName,
                                 @Param("jobGroup") String jobGroup,
                                 @Param("status") Integer status,
                                 @Param("offset") long offset,
                                 @Param("limit") long limit);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_job_log",
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
     * 恢复回收站日志（绕过 @TableLogic 的 deleted=0 过滤，按 id 直接置 0）
     */
    @Update("UPDATE sys_job_log SET deleted = 0, update_time = NOW() WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

    /**
     * 彻底删除回收站日志（物理删除，绕过 @TableLogic）
     */
    @Delete("DELETE FROM sys_job_log WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    /**
     * 分页查询回收站执行日志
     */
    @Select({
            "<script>",
            "SELECT * FROM sys_job_log",
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
    List<SysJobLogDO> selectDeletedPage(@Param("jobName") String jobName,
                                        @Param("jobGroup") String jobGroup,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM sys_job_log WHERE deleted = 1",
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
