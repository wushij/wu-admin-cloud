package com.wu.cloud.message.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.message.entity.AnnounceDO;
import com.wu.cloud.message.entity.AnnounceSendLogDO;
import com.wu.cloud.message.service.AnnounceService;
import com.wu.cloud.message.vo.AnnounceMyVO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/message/announce")
public class AnnounceController {

    @Resource
    private AnnounceService announceService;
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/page")
    public R<PageResult<AnnounceDO>> page(@RequestParam(defaultValue = "1") int pageNo,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) Integer noticeType,
                                           @RequestParam(required = false) Integer status) {
        Page<AnnounceDO> page = announceService.page(pageNo, pageSize, title, noticeType, status);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/my")
    public R<PageResult<AnnounceMyVO>> my(@RequestParam(defaultValue = "1") int pageNo,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Integer isRead) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<AnnounceMyVO> page = announceService.myPage(userId, pageNo, pageSize, isRead);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/{id}")
    public R<AnnounceRequest> detail(@PathVariable("id") Long id) {
        AnnounceDO entity = announceService.getById(id);
        if (entity == null) {
            throw new BusinessException(404, "通知不存在");
        }
        AnnounceRequest resp = AnnounceRequest.from(entity, objectMapper);
        resp.setCreateTime(entity.getCreateTime());
        resp.setCreateName(announceService.resolvePublisherName(entity));
        resp.setCreateAvatar(announceService.resolvePublisherAvatar(entity));
        return R.ok(resp);
    }

    @PostMapping
    public R<Boolean> create(@RequestBody AnnounceRequest req) {
        announceService.create(req.toEntity(objectMapper));
        return R.ok(true, "发布通知成功");
    }

    @PutMapping
    public R<Boolean> update(@RequestBody AnnounceRequest req) {
        announceService.update(req.toEntity(objectMapper));
        return R.ok(true, "修改通知成功");
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        announceService.delete(id);
        return R.ok(true, "删除通知成功");
    }

    @PostMapping("/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") Long id) {
        announceService.publish(id);
        return R.ok(true, "发布成功");
    }

    @PostMapping("/{id}/read")
    public R<Boolean> read(@PathVariable("id") Long id) {
        announceService.markRead(StpUtil.getLoginIdAsLong(), id);
        return R.ok(true, "已标记为已读");
    }

    @PostMapping("/read-all")
    public R<Boolean> readAll() {
        announceService.markAllRead(StpUtil.getLoginIdAsLong());
        return R.ok(true, "全部已读成功");
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(announceService.unreadCount(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/{id}/send-logs")
    public R<List<AnnounceSendLogDO>> sendLogs(@PathVariable("id") Long id) {
        return R.ok(announceService.sendLogs(id));
    }

    @GetMapping("/recycle/page")
    public R<PageResult<AnnounceDO>> recyclePage(@RequestParam(defaultValue = "1") int pageNo,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String title) {
        return R.ok(announceService.recyclePage(pageNo, pageSize, title));
    }

    @PutMapping("/restore")
    public R<Boolean> restore(@RequestParam("id") Long id) {
        announceService.restore(id);
        return R.ok(true, "恢复通知成功");
    }

    @DeleteMapping("/delete-permanent")
    public R<Boolean> deletePermanent(@RequestParam("id") Long id) {
        announceService.deletePermanent(id);
        return R.ok(true, "彻底删除成功");
    }

    @Data
    public static class AnnounceRequest {
        private Long id;
        private String title;
        private String content;
        private Integer noticeType;
        private List<String> channels;
        private Integer targetType;
        private List<Long> targetIds;
        private Integer status;
        private String createName;
        private LocalDateTime createTime;
        private String createAvatar;

        static AnnounceRequest from(AnnounceDO e, ObjectMapper mapper) {
            AnnounceRequest r = new AnnounceRequest();
            r.setId(e.getId());
            r.setTitle(e.getTitle());
            r.setContent(e.getContent());
            r.setNoticeType(e.getNoticeType());
            r.setTargetType(e.getTargetType());
            r.setStatus(e.getStatus());
            try {
                if (StringUtils.hasText(e.getChannels())) {
                    r.setChannels(mapper.readValue(e.getChannels(), new TypeReference<List<String>>() {}));
                }
                if (StringUtils.hasText(e.getTargetIds())) {
                    r.setTargetIds(mapper.readValue(e.getTargetIds(), new TypeReference<List<Long>>() {}));
                }
            } catch (Exception ex) {
                log.warn("解析通知 channels/targetIds 失败 announceId={}", e.getId(), ex);
            }
            return r;
        }

        AnnounceDO toEntity(ObjectMapper mapper) {
            AnnounceDO e = new AnnounceDO();
            e.setId(id);
            e.setTitle(title);
            e.setContent(content);
            e.setNoticeType(noticeType);
            e.setTargetType(targetType);
            e.setStatus(status);
            try {
                e.setChannels(channels == null ? "[\"station\"]" : mapper.writeValueAsString(channels));
                e.setTargetIds(targetIds == null ? null : mapper.writeValueAsString(targetIds));
            } catch (Exception ex) {
                log.warn("序列化通知 channels/targetIds 失败 id={}", id, ex);
                e.setChannels("[\"station\"]");
            }
            return e;
        }
    }
}
