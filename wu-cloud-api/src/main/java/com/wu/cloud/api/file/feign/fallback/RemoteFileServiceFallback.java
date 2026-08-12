package com.wu.cloud.api.file.feign.fallback;

import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 远程文件服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteFileServiceFallback implements FallbackFactory<RemoteFileService> {

    @Override
    public RemoteFileService create(Throwable cause) {
        log.error("[Feign降级] wu-service-file 文件服务调用失败", cause);
        return new RemoteFileService() {
            @Override
            public R<SysFileDTO> getFileById(Long id) {
                return R.fail("文件服务暂不可用");
            }

            @Override
            public R<Long> countFiles() {
                return R.ok(0L);
            }

            @Override
            public R<SysFileDTO> uploadFile(MultipartFile file, String type) {
                return R.fail("文件服务暂不可用，上传失败");
            }
        };
    }
}
