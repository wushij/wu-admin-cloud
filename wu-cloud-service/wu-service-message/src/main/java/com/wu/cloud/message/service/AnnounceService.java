package com.wu.cloud.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.message.entity.AnnounceDO;
import com.wu.cloud.message.entity.AnnounceSendLogDO;
import com.wu.cloud.message.vo.AnnounceMyVO;

import java.util.List;

public interface AnnounceService {

    Page<AnnounceDO> page(int pageNo, int pageSize, String title, Integer noticeType, Integer status);

    Page<AnnounceMyVO> myPage(Long userId, int pageNo, int pageSize, Integer isRead);

    AnnounceDO getById(Long id);

    void create(AnnounceDO entity);

    void update(AnnounceDO entity);

    void delete(Long id);

    PageResult<AnnounceDO> recyclePage(int pageNo, int pageSize, String title);

    void restore(Long id);

    void deletePermanent(Long id);

    void publish(Long id);

    void markRead(Long userId, Long announceId);

    void markAllRead(Long userId);

    long unreadCount(Long userId);

    List<AnnounceSendLogDO> sendLogs(Long announceId);

    String resolvePublisherName(AnnounceDO announce);

    String resolvePublisherAvatar(AnnounceDO announce);
}
