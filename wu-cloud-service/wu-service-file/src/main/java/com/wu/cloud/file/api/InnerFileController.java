package com.wu.cloud.file.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.file.dto.StorageConfigDTO;
import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.file.config.StorageConfig;
import com.wu.cloud.file.config.StorageRoutingService;
import com.wu.cloud.file.entity.SysFileDO;
import com.wu.cloud.file.service.SysFileService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件服务内部 RPC 端点。
 * <p>契约对齐：返回值统一使用 {@code SysFileDTO}，与 wu-cloud-api
 * 中 {@code RemoteFileService} 声明一致，避免 DO/DTO 字段不一致。</p>
 */
@Hidden
@RestController
@RequestMapping("/inner/file")
public class InnerFileController {

    @Resource
    private SysFileService sysFileService;

    @Resource
    private StorageRoutingService storageRoutingService;

    @Operation(summary = "获取统计信息")
    @GetMapping("/count")
    public R<Long> countFiles() {
        return R.ok(sysFileService.countForGroupSidebar(null, null, null));
    }

    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<SysFileDTO> getFileById(@PathVariable("id") Long id) {
        return R.ok(toDTO(sysFileService.getFileById(id)));
    }

    @Operation(summary = "上传工单附件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFileDTO> uploadFile(@RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "type", required = false) String type) {
        return R.ok(toDTO(sysFileService.uploadFile(file, type)));
    }

    /**
     * 存储引擎连通性测试：用给定配置探测，不影响当前生效引擎。
     *
     * @return {success: Boolean, message: String}，message 为 null 表示连通正常
     */
    @Operation(summary = "连通性测试")
    @PostMapping("/storage/test")
    public R<Map<String, Object>> testStorage(@RequestBody StorageConfigDTO config) {
        Map<String, Object> result = new LinkedHashMap<>(4);
        if (config == null) {
            result.put("success", false);
            result.put("message", "存储配置不能为空");
            return R.ok(result);
        }
        String detail = storageRoutingService.testConnectionDetail(toStorageConfig(config));
        result.put("success", detail == null);
        result.put("message", detail);
        return R.ok(result);
    }

    /**
     * 热重载存储客户端缓存：配置变更后使新密钥/桶名立即生效
     */
    @Operation(summary = "新增")
    @PostMapping("/storage/reload")
    public R<Boolean> reloadStorage() {
        storageRoutingService.reload();
        return R.ok(true);
    }

    private StorageConfig toStorageConfig(StorageConfigDTO dto) {
        return StorageConfig.builder()
                .type(dto.getType())
                .localPath(dto.getLocalPath())
                .localDomain(dto.getLocalDomain())
                .minioDomain(dto.getMinioDomain())
                .minioEndpoint(dto.getMinioEndpoint())
                .minioAccessKey(dto.getMinioAccessKey())
                .minioSecretKey(dto.getMinioSecretKey())
                .minioBucket(dto.getMinioBucket())
                .cosRegion(dto.getCosRegion())
                .cosBucket(dto.getCosBucket())
                .cosSecretId(dto.getCosSecretId())
                .cosSecretKey(dto.getCosSecretKey())
                .cosDomain(dto.getCosDomain())
                .ossEndpoint(dto.getOssEndpoint())
                .ossBucket(dto.getOssBucket())
                .ossAccessKeyId(dto.getOssAccessKeyId())
                .ossAccessKeySecret(dto.getOssAccessKeySecret())
                .ossDomain(dto.getOssDomain())
                .build();
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
