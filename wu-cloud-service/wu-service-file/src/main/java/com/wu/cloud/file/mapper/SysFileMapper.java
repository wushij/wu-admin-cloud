package com.wu.cloud.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.file.entity.SysFileDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFileDO> {

    IPage<SysFileDO> selectDeletedPage(Page<?> page, @Param("originalName") String originalName);

    SysFileDO selectDeletedById(@Param("id") Long id);

    int restoreById(@Param("id") Long id);

    int deletePhysicalById(@Param("id") Long id);

    List<Long> selectExpiredRecycleIds(@Param("cutoff") LocalDateTime cutoff);
}
