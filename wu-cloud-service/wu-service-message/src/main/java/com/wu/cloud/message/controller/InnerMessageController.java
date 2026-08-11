package com.wu.cloud.message.controller;

import com.wu.cloud.api.message.dto.SysAnnounceDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.message.entity.SysAnnounceDO;
import com.wu.cloud.message.mapper.SysAnnounceMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 消息服务内部 RPC 端点。
 *
 * <p>契约对齐：返回值统一使用 {@code SysAnnounceDTO}，与 wu-cloud-api 中
 * {@code RemoteMessageService} 声明一致。直接返回 DO 会额外序列化出
 * updater / deleted 等内部字段，既有信息外泄风险，也会在调用方开启
 * FAIL_ON_UNKNOWN_PROPERTIES 时反序列化失败。</p>
 */
@RestController
@RequestMapping("/inner/message")
public class InnerMessageController {

    @Resource
    private SysAnnounceMapper sysAnnounceMapper;

    @GetMapping("/announce/{id}")
    public R<SysAnnounceDTO> getAnnounceById(@PathVariable("id") Long id) {
        return R.ok(toDTO(sysAnnounceMapper.selectById(id)));
    }

    private SysAnnounceDTO toDTO(SysAnnounceDO announce) {
        if (announce == null) {
            return null;
        }
        SysAnnounceDTO dto = new SysAnnounceDTO();
        dto.setId(announce.getId());
        dto.setTitle(announce.getTitle());
        dto.setContent(announce.getContent());
        dto.setPriority(announce.getPriority());
        dto.setTargetType(announce.getTargetType());
        dto.setStatus(announce.getStatus());
        dto.setCreateTime(announce.getCreateTime());
        return dto;
    }
}
