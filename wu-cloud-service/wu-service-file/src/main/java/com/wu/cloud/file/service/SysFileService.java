package com.wu.cloud.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.file.dto.FilePageDTO;
import com.wu.cloud.file.entity.SysFileDO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SysFileService {

    Page<SysFileDO> getFilePage(FilePageDTO pageDTO);

    SysFileDO getFileById(Long id);

    SysFileDO uploadFile(MultipartFile file);

    SysFileDO uploadFile(MultipartFile file, Long groupId);

    /** 按业务类型上传（如 avatar），路径与 URL 与单体对齐 */
    SysFileDO uploadFile(MultipartFile file, String type);

    void deleteFile(Long id);

    void batchDeleteFile(List<Long> ids);

    byte[] readFile(Long id);

    PageResult<SysFileDO> pageByGroup(Integer pageNo, Integer pageSize, Long groupId,
                                      Boolean ungrouped, String fileCategory, String originalName);

    long countForGroupSidebar(Long groupId, Boolean ungrouped, String fileCategory);

    Map<String, Object> uploadPolicy();

    void moveToGroup(List<Long> fileIds, Long groupId);

    void rename(Long id, String newName);

    String readText(Long id);

    PageResult<SysFileDO> recyclePage(PageQuery pageQuery, String originalName);

    void restore(Long id);

    void deletePermanent(Long id);

    Resource openFileResource(Long id) throws IOException;
}
