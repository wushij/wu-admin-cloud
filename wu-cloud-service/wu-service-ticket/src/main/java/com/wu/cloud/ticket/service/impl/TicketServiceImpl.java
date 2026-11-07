package com.wu.cloud.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.PermissionChecker;
import com.wu.cloud.ticket.entity.NoticeDO;
import com.wu.cloud.ticket.entity.TicketAttachmentDO;
import com.wu.cloud.ticket.entity.TicketCommentDO;
import com.wu.cloud.ticket.entity.TicketDO;
import cn.hutool.core.util.StrUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 工单 Service 实现（MyBatis-Plus 3.5.18 已移除 extension.service.ServiceImpl，
 * 直接注入 Mapper 承载数据访问；跨域数据经 OpenFeign 契约获取）
 */
@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Resource
    private TicketMapper ticketMapper;
    @Resource
    private UserNameResolver userNameResolver;
    @Resource
    private TicketCommentMapper commentMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private TicketAttachmentMapper attachmentMapper;
    @Resource
    private RemoteUserService remoteUserService;
    @Resource
    private RemoteFileService remoteFileService;

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
        Page<TicketDO> result = ticketMapper.selectPage(page, wrapper);
        userNameResolver.fillNames(result.getRecords(), TicketDO::getCreatorUserId, TicketDO::setCreatorName);
        userNameResolver.fillNames(result.getRecords(), TicketDO::getAssigneeUserId, TicketDO::setAssigneeName);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public TicketDO getDetail(Long id) {
        TicketDO ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(404, "工单不存在");
        }
        ticket.setCreatorName(userNameResolver.resolve(ticket.getCreatorUserId()));
        ticket.setAssigneeName(userNameResolver.resolve(ticket.getAssigneeUserId()));
        return ticket;
    }

    @Override
    public TicketDO getById(Long id) {
        return ticketMapper.selectById(id);
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
        ticketMapper.insert(ticket);
        return ticket.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TicketDO ticket) {
        ticket.setCreatorUserId(null);
        ticket.setCreator(null);
        ticketMapper.updateById(ticket);
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
        ticketMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long id, Long assigneeUserId) {
        ticketMapper.update(null, new LambdaUpdateWrapper<TicketDO>()
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
        noticeMapper.delete(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getBizType, "TICKET")
                .eq(NoticeDO::getBizId, id));
        ticketMapper.deleteById(id);
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
        int rows = ticketMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
        // 恢复后重新生成通知，让相关用户在通知列表中重新看到该工单
        TicketDO ticket = getById(id);
        if (ticket != null && ticket.getAssigneeUserId() != null) {
            NoticeDO notice = new NoticeDO();
            notice.setUserId(ticket.getAssigneeUserId());
            notice.setTitle("工单恢复通知");
            notice.setContent("工单「" + (ticket.getTitle() != null ? ticket.getTitle() : ticket.getTicketNo()) + "」已从回收站恢复");
            notice.setBizType("TICKET");
            notice.setBizId(id);
            notice.setReadStatus(0);
            noticeMapper.insert(notice);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = ticketMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站工单不存在");
        }
        commentMapper.delete(new LambdaQueryWrapper<TicketCommentDO>().eq(TicketCommentDO::getTicketId, id));
        // 彻底删除时，同步物理删除相关通知，避免用户点击通知时提示「工单不存在」
        noticeMapper.delete(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getBizType, "TICKET")
                .eq(NoticeDO::getBizId, id));
    }

    @Override
    public PageResult<TicketDO> recyclePage(PageParam pageParam, String title, String status, String priority) {
        Page<TicketDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<TicketDO> result = (Page<TicketDO>) ticketMapper.selectDeletedPage(page, title, status, priority);
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
        ticketMapper.selectExpiredRecycleIds(cutoff).forEach(this::deletePermanent);
    }

    @Override
    public List<Map<String, Object>> getAssigneeOptions() {
        // 启用状态用户全量候选（经用户服务 RPC），新入职员工也能被指派；排除当前登录用户
        Long currentUserId = UserContextHolder.getUserId();
        R<List<SysUserDTO>> resp = remoteUserService.listActiveUsers(currentUserId);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            throw new BusinessException("用户服务暂不可用，处理人候选列表获取失败，请稍后重试");
        }
        List<Map<String, Object>> options = new ArrayList<>(resp.getData().size());
        for (SysUserDTO user : resp.getData()) {
            if (user == null || user.getUserId() == null) {
                continue;
            }
            Map<String, Object> opt = new LinkedHashMap<>();
            opt.put("userId", user.getUserId());
            opt.put("username", user.getUsername());
            opt.put("nickname", user.getNickname());
            options.add(opt);
        }
        return options;
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

        // 统一经文件服务存储（受存储配额管控，多实例/容器化部署下均可访问），本服务不落本地磁盘
        R<SysFileDTO> resp = remoteFileService.uploadFile(file, "ticket");
        if (resp == null || !resp.isSuccess() || resp.getData() == null
                || StrUtil.isBlank(resp.getData().getUrl())) {
            log.warn("[Ticket] 附件上传文件服务失败: ticketId={}, resp={}",
                    ticketId, resp == null ? "null" : resp.getMsg());
            throw new BusinessException("文件服务暂不可用，附件上传失败，请稍后重试");
        }
        SysFileDTO uploaded = resp.getData();

        Long userId = UserContextHolder.getUserId();
        TicketAttachmentDO attachment = new TicketAttachmentDO();
        attachment.setTicketId(ticketId);
        attachment.setUploaderUserId(userId);
        attachment.setFileName(originFileName);
        attachment.setFilePath(uploaded.getUrl());
        attachment.setFileSize(uploaded.getFileSize() != null ? uploaded.getFileSize() : file.getSize());
        attachment.setCreator(userId != null ? userId.toString() : null);
        attachmentMapper.insert(attachment);
        return attachment.getId();
    }

    @Override
    public TicketAttachmentDO getAttachmentForDownload(Long id) {
        return attachmentMapper.selectById(id);
    }
}
