package com.wu.cloud.api.file.feign;

import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.fallback.RemoteFileServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 远程文件服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteFileService", value = "wu-service-file", path = "/inner/file", fallbackFactory = RemoteFileServiceFallback.class)
public interface RemoteFileService {

    @GetMapping("/{id}")
    R<SysFileDTO> getFileById(@PathVariable("id") Long id);

    /** 工作台文件数量（对齐单体 countForGroupSidebar） */
    @GetMapping("/count")
    R<Long> countFiles();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<SysFileDTO> uploadFile(@RequestPart("file") MultipartFile file,
                            @RequestParam(value = "type", required = false) String type);
}
