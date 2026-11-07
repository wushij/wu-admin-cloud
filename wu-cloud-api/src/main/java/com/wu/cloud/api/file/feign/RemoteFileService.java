package com.wu.cloud.api.file.feign;

import com.wu.cloud.api.file.dto.StorageConfigDTO;
import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.fallback.RemoteFileServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 远程文件服务 OpenFeign 接口契约
 */
@FeignClient(contextId = "remoteFileService", value = "wu-service-file", path = "/inner/file", fallbackFactory = RemoteFileServiceFallback.class)
public interface RemoteFileService {

    @GetMapping("/{id}")
    R<SysFileDTO> getFileById(@PathVariable("id") Long id);

    /** 工作台文件数量（与既有规范对齐 countForGroupSidebar） */
    @GetMapping("/count")
    R<Long> countFiles();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<SysFileDTO> uploadFile(@RequestPart("file") MultipartFile file,
                            @RequestParam(value = "type", required = false) String type);

    /**
     * 存储引擎连通性测试：用给定配置探测，不影响当前生效引擎
     *
     * @return {success: Boolean, message: String}，message 为 null 表示连通正常
     */
    @PostMapping("/storage/test")
    R<Map<String, Object>> testStorage(@RequestBody StorageConfigDTO config);

    /**
     * 热重载存储客户端缓存：存储配置变更后调用，使新密钥/桶名立即生效
     */
    @PostMapping("/storage/reload")
    R<Boolean> reloadStorage();
}
