package com.wu.cloud.ticket.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.mapper.NoticeMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单/审批通知 REST 控制器（规范路径 /api/notice/**）
 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    @Resource
    private NoticeMapper noticeMapper;

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = noticeMapper.selectCount(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0));
        return R.ok(count != null ? count : 0L);
    }

    @GetMapping("/my-list")
    public R<List<NoticeDO>> myList() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getUserId, userId)
                .orderByDesc(NoticeDO::getCreateTime)));
    }

    @GetMapping("/page")
    public R<PageResult<NoticeDO>> page(@RequestParam(defaultValue = "1") int pageNo,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<NoticeDO> page = noticeMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<NoticeDO>()
                        .eq(NoticeDO::getUserId, userId)
                        .orderByDesc(NoticeDO::getCreateTime));
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal()));
    }

    @GetMapping("/list")
    public R<List<NoticeDO>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(noticeMapper.selectList(new LambdaQueryWrapper<NoticeDO>()
                .eq(userId != null, NoticeDO::getUserId, userId)
                .eq(NoticeDO::getReadStatus, 0)
                .orderByDesc(NoticeDO::getCreateTime)));
    }

    @PutMapping("/read")
    public R<Boolean> readPut(@RequestBody NoticeDO noticeDO) {
        Long id = noticeDO != null ? noticeDO.getId() : null;
        return R.ok(markRead(id));
    }

    @PostMapping("/read")
    public R<Boolean> readPost(@RequestParam Long id) {
        return R.ok(markRead(id));
    }

    /**
     * 标记单条站内消息为已读。
     *
     * <p>必须带上 user_id 条件：站内消息属于用户私有收件箱，
     * 仅按 id 更新会让任意登录用户篡改他人消息的已读状态（越权）。</p>
     */
    private boolean markRead(Long id) {
        if (id == null) {
            return false;
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return noticeMapper.update(null, new LambdaUpdateWrapper<NoticeDO>()
                .eq(NoticeDO::getId, id)
                .eq(NoticeDO::getUserId, userId)
                .set(NoticeDO::getReadStatus, 1)) > 0;
    }

    @PutMapping("/read-all")
    public R<Boolean> readAllPut() {
        return readAllPost();
    }

    @PostMapping("/read-all")
    public R<Boolean> readAllPost() {
        Long userId = StpUtil.getLoginIdAsLong();
        if (userId != null) {
            noticeMapper.update(null, new LambdaUpdateWrapper<NoticeDO>()
                    .eq(NoticeDO::getUserId, userId)
                    .eq(NoticeDO::getReadStatus, 0)
                    .set(NoticeDO::getReadStatus, 1));
        }
        return R.ok(true);
    }

    /**
     * 删除单条站内消息（移动端「删除消息」使用）。
     *
     * <p>同样限定 user_id，避免越权删除他人消息。</p>
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(noticeMapper.delete(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getId, id)
                .eq(NoticeDO::getUserId, userId)) > 0);
    }
}
