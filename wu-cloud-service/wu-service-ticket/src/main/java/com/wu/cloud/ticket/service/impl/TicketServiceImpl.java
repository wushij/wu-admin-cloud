package com.wu.cloud.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.entity.TicketAttachmentDO;
import com.wu.cloud.ticket.entity.TicketCommentDO;
import com.wu.cloud.ticket.entity.TicketDO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import com.wu.cloud.ticket.mapper.NoticeMapper;
import com.wu.cloud.ticket.mapper.TicketAttachmentMapper;
import com.wu.cloud.ticket.mapper.TicketCommentMapper;
import com.wu.cloud.ticket.mapper.TicketMapper;
import com.wu.cloud.ticket.service.TicketService;
import com.wu.cloud.ticket.service.UserNameResolver;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * 工单 Service 实现
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketDO> implements TicketService {

    @Resource
    private UserNameResolver userNameResolver;
    @Resource
    private TicketCommentMapper commentMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private TicketAttachmentMapper attachmentMapper;

    private String generateTicketNo() {
        return "TK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @Override
    public PageResult<TicketDO> page(PageParam pageParam, String title, String status, String priority, Long assigneeUserId) {
        Page<TicketDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        LambdaQueryWrapper<TicketDO> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            wrapper.like(TicketDO::getTitle, title);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(TicketDO::getStatus, status);
        }
        if (priority != null && !priority.isBlank()) {
            wrapper.eq(TicketDO::getPriority, priority);
        }
        if (assigneeUserId != null) {
            wrapper.eq(TicketDO::getAssigneeUserId, assigneeUserId);
        }
        // 数据隔离：非超管仅可见本人创建或被分配的工单
        Long currentUserId = UserContextHolder.getUserId();
        if (!PermissionChecker.isSuperAdmin() && currentUserId != null) {
            wrapper.and(w -> w.eq(TicketDO::getCreatorUserId, currentUserId)
                    .or().eq(TicketDO::getAssigneeUserId, currentUserId));
        }
        wrapper.orderByDesc(TicketDO::getUpdateTime);
        Page<TicketDO> result = page(page, wrapper);
        userNameResolver.fillNames(result.getRecords(), TicketDO::getCreatorUserId, TicketDO::setCreatorName);
        userNameResolver.fillNames(result.getRecords(), TicketDO::getAssigneeUserId, TicketDO::setAssigneeName);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public TicketDO getDetail(Long id) {
        TicketDO ticket = getById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        ticket.setCreatorName(userNameResolver.resolve(ticket.getCreatorUserId()));
        ticket.setAssigneeName(userNameResolver.resolve(ticket.getAssigneeUserId()));
        return ticket;
    }

    /**
     * 按主键查询工单（内部 RPC 用，委托给 ServiceImpl.getById）
     * <p>
     * ServiceImpl.getById 参数为 Serializable，与接口声明的 Long 不完全匹配，
     * 需显式桥接，否则编译报"未覆盖抽象方法"。
     */
    @Override
    public TicketDO getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TicketDO ticket) {
        Long userId = UserContextHolder.getUserId();
        ticket.setCreatorUserId(userId);
        ticket.setCreator(userId != null ? userId.toString() : null);
        ticket.setTicketNo(generateTicketNo());
        if (ticket.getStatus() == null || ticket.getStatus().isBlank()) {
            ticket.setStatus("OPEN");
        }
        save(ticket);
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TicketDO ticket) {
        ticket.setCreatorUserId(null);
        ticket.setCreator(null);
        updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long id, String status) {
        TicketDO ticket = getById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        if (!Arrays.asList("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(status)) {
            throw new BusinessException("非法状态");
        }
        // 权限校验：非超管仅可流转本人创建或被分配的工单
        Long currentUserId = UserContextHolder.getUserId();
        if (!PermissionChecker.isSuperAdmin() && currentUserId != null) {
            boolean isCreator = currentUserId.equals(ticket.getCreatorUserId());
            boolean isAssignee = currentUserId.equals(ticket.getAssigneeUserId());
            if (!isCreator && !isAssignee) {
                throw new BusinessException(403, "无权流转他人工单");
            }
        }
        LambdaUpdateWrapper<TicketDO> update = new LambdaUpdateWrapper<>();
        update.eq(TicketDO::getId, id).set(TicketDO::getStatus, status);
        if ("CLOSED".equals(status)) {
            update.set(TicketDO::getClosedTime, LocalDateTime.now());
        }
        update(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long assigneeUserId) {
        update(null, new LambdaUpdateWrapper<TicketDO>()
                .eq(TicketDO::getId, id)
                .set(TicketDO::getAssigneeUserId, assigneeUserId)
                .set(TicketDO::getStatus, "IN_PROGRESS"));
        NoticeDO notice = new NoticeDO();
        notice.setUserId(assigneeUserId);
        notice.setTitle("工单分配通知");
        notice.setContent("您有一条新分配的工单，请及时处理");
        notice.setBizType("TICKET");
        notice.setBizId(id);
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TicketDO ticket = getById(id);
        if (ticket == null) {
            return;
        }
        // 权限校验：非超管仅可删除本人创建的工单
        Long currentUserId = UserContextHolder.getUserId();
        if (!PermissionChecker.isSuperAdmin() && currentUserId != null
                && !currentUserId.equals(ticket.getCreatorUserId())) {
            throw new BusinessException(403, "无权删除他人工单");
        }
        // 级联清理：评论 + 附件（逻辑删除主表，物理清理关联数据避免孤儿记录）
        commentMapper.delete(new LambdaQueryWrapper<TicketCommentDO>().eq(TicketCommentDO::getTicketId, id));
        attachmentMapper.delete(new LambdaQueryWrapper<TicketAttachmentDO>().eq(TicketAttachmentDO::getTicketId, id));
        removeById(id);
    }

    @Override
    public void deleteBatch(Long[] ids) {
        if (ids != null) {
            Arrays.stream(ids).forEach(this::delete);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
        commentMapper.delete(new LambdaQueryWrapper<TicketCommentDO>().eq(TicketCommentDO::getTicketId, id));
    }

    @Override
    public PageResult<TicketDO> recyclePage(PageParam pageParam, String title, String status, String priority) {
        Page<TicketDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<TicketDO> result = (Page<TicketDO>) baseMapper.selectDeletedPage(page, title, status, priority);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(Long ticketId, String content) {
        TicketDO ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        Long userId = UserContextHolder.getUserId();
        TicketCommentDO comment = new TicketCommentDO();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCreator(userId != null ? userId.toString() : null);
        commentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    public List<TicketCommentDO> listComments(Long ticketId) {
        List<TicketCommentDO> list = commentMapper.selectList(
                new LambdaQueryWrapper<TicketCommentDO>().eq(TicketCommentDO::getTicketId, ticketId)
                        .orderByAsc(TicketCommentDO::getCreateTime));
        userNameResolver.fillNames(list, TicketCommentDO::getUserId, TicketCommentDO::setUsername);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpiredRecycleBin() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        baseMapper.selectExpiredRecycleIds(cutoff).forEach(this::deletePermanent);
    }

    @Override
    public List<Map<String, Object>> getAssigneeOptions() {
        // 微服务简化：从最近工单提取处理人列表去重，名称通过 UserNameResolver 解析
        List<TicketDO> recent = baseMapper.selectList(
                new LambdaQueryWrapper<TicketDO>()
                        .isNotNull(TicketDO::getAssigneeUserId)
                        .orderByDesc(TicketDO::getCreateTime)
                        .last("LIMIT 200"));
        Map<Long, Map<String, Object>> dedup = new LinkedHashMap<>();
        for (TicketDO t : recent) {
            Long uid = t.getAssigneeUserId();
            if (uid == null || dedup.containsKey(uid)) {
                continue;
            }
            Map<String, Object> opt = new HashMap<>();
            opt.put("userId", uid);
            opt.put("nickname", userNameResolver.resolve(uid));
            dedup.put(uid, opt);
        }
        return new ArrayList<>(dedup.values());
    }

    @Override
    public List<TicketAttachmentDO> listAttachments(Long ticketId) {
        List<TicketAttachmentDO> list = attachmentMapper.selectList(
                new LambdaQueryWrapper<TicketAttachmentDO>()
                        .eq(TicketAttachmentDO::getTicketId, ticketId)
                        .orderByDesc(TicketAttachmentDO::getCreateTime));
        userNameResolver.fillNames(list, TicketAttachmentDO::getUploaderUserId, TicketAttachmentDO::setUploaderName);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadAttachment(Long ticketId, MultipartFile file) throws IOException {
        TicketDO ticket = getById(ticketId);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "附件不能为空");
        }
        String originFileName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename() : "file";
        String safeName = UUID.randomUUID().toString().replace("-", "") + "_" + originFileName;
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "data", "uploads", "ticket");
        Files.createDirectories(uploadDir);
        Path targetPath = uploadDir.resolve(safeName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        Long userId = UserContextHolder.getUserId();
        TicketAttachmentDO attachment = new TicketAttachmentDO();
        attachment.setTicketId(ticketId);
        attachment.setUploaderUserId(userId);
        attachment.setFileName(originFileName);
        attachment.setFilePath(targetPath.toString());
        attachment.setFileSize(file.getSize());
        attachment.setCreator(userId != null ? userId.toString() : null);
        attachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    public TicketAttachmentDO getAttachmentForDownload(Long id) {
        return attachmentMapper.selectById(id);
    }
}
