package com.wu.cloud.ticket.service;

import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.PageResult;
import com.wu.cloud.ticket.entity.TicketAttachmentDO;
import com.wu.cloud.ticket.entity.TicketCommentDO;
import com.wu.cloud.ticket.entity.TicketDO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 工单 Service
 */
public interface TicketService {

    PageResult<TicketDO> page(PageParam pageParam, String title, String status, String priority, Long assigneeUserId);

    TicketDO getDetail(Long id);

    /**
     * 按主键查询工单（内部 RPC 用，不填充用户名、不抛 404）
     * 实现由 {@code ServiceImpl.getById} 提供
     */
    TicketDO getById(Long id);

    Long create(TicketDO ticket);

    void update(TicketDO ticket);

    void changeStatus(Long id, String status);

    void assign(Long id, Long assigneeUserId);

    void delete(Long id);

    void deleteBatch(Long[] ids);

    void restore(Long id);

    void deletePermanent(Long id);

    PageResult<TicketDO> recyclePage(PageParam pageParam, String title, String status, String priority);

    Long addComment(Long ticketId, String content);

    List<TicketCommentDO> listComments(Long ticketId);

    void purgeExpiredRecycleBin();

    /** B1: 工单处理人下拉选项（微服务简化：从最近工单提取处理人列表） */
    List<Map<String, Object>> getAssigneeOptions();

    /** B5: 附件列表 */
    List<TicketAttachmentDO> listAttachments(Long ticketId);

    /** B5: 上传附件（存到 data/uploads/ticket 目录，UUID 重命名） */
    Long uploadAttachment(Long ticketId, MultipartFile file) throws IOException;

    /** B5: 获取附件用于下载 */
    TicketAttachmentDO getAttachmentForDownload(Long id);
}
