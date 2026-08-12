package com.wu.cloud.file.controller;

import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.service.SysFileService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务内部 RPC 端点。
 * <p>契约对齐：返回值统一使用 {@code SysFileDTO}，与 wu-cloud-api
 * 中 {@code RemoteFileService} 声明一致，避免 DO/DTO 字段不一致。</p>
 */
@RestController
@RequestMapping("/inner/file")
public class InnerFileController {

    @Resource
    private SysFileService sysFileService;

    @GetMapping("/count")
    public R<Long> countFiles() {
        return R.ok(sysFileService.countForGroupSidebar(null, null, null));
    }

    @GetMapping("/{id}")
    public R<SysFileDTO> getFileById(@PathVariable("id") Long id) {
        return R.ok(toDTO(sysFileService.getFileById(id)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFileDTO> uploadFile(@RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "type", required = false) String type) {
        return R.ok(toDTO(sysFileService.uploadFile(file, type)));
    }

    private SysFileDTO toDTO(SysFileDO file) {
        if (file == null) {
            return null;
        }
        SysFileDTO dto = new SysFileDTO();
        dto.setId(file.getId());
        dto.setOriginalName(file.getOriginalName());
        dto.setFileName(file.getFileName());
        dto.setUrl(file.getUrl());
        dto.setFileSize(file.getFileSize());
        dto.setFileType(file.getFileType());
        dto.setCreateTime(file.getCreateTime());
        return dto;
    }
}
