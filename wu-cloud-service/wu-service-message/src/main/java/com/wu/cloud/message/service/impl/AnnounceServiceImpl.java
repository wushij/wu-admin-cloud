package com.wu.cloud.message.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.message.entity.AnnounceDO;
import com.wu.cloud.message.entity.AnnounceSendLogDO;
import com.wu.cloud.message.entity.UserAnnounceDO;
import com.wu.cloud.message.mapper.AnnounceMapper;
import com.wu.cloud.message.mapper.AnnounceSendLogMapper;
import com.wu.cloud.message.mapper.UserAnnounceMapper;
import com.wu.cloud.message.service.AnnounceService;
import com.wu.cloud.message.vo.AnnounceMyVO;
import com.wu.cloud.message.websocket.MessageWebSocketHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AnnounceServiceImpl implements AnnounceService {

    @Resource
    private AnnounceMapper announceMapper;
    @Resource
    private UserAnnounceMapper userAnnounceMapper;
    @Resource
    private AnnounceSendLogMapper sendLogMapper;
    @Lazy
    @Resource
    private RemoteUserService remoteUserService;
    @Resource
    private MessageWebSocketHandler webSocketHandler;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Page<AnnounceDO> page(int pageNo, int pageSize, String title, Integer noticeType, Integer status) {
        LambdaQueryWrapper<AnnounceDO> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(title)) {
            w.like(AnnounceDO::getTitle, title);
        }
        if (noticeType != null) {
            w.eq(AnnounceDO::getNoticeType, noticeType);
        }
        if (status != null) {
            w.eq(AnnounceDO::getStatus, status);
        }
        w.orderByDesc(AnnounceDO::getCreateTime);
        return announceMapper.selectPage(new Page<>(pageNo, pageSize), w);
    }

    @Override
    public Page<AnnounceMyVO> myPage(Long userId, int pageNo, int pageSize, Integer isRead) {
        LambdaQueryWrapper<UserAnnounceDO> uw = new LambdaQueryWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId);
        if (isRead != null) {
            uw.eq(UserAnnounceDO::getIsRead, isRead);
        }
        uw.orderByDesc(UserAnnounceDO::getCreateTime);
        Page<UserAnnounceDO> uaPage = userAnnounceMapper.selectPage(new Page<>(pageNo, pageSize), uw);
        List<AnnounceMyVO> records = new ArrayList<>();
        for (UserAnnounceDO ua : uaPage.getRecords()) {
            AnnounceDO a = announceMapper.selectById(ua.getAnnounceId());
            if (a != null && a.getStatus() != null && a.getStatus() == 1) {
                AnnounceMyVO vo = new AnnounceMyVO();
                vo.setId(a.getId());
                vo.setTitle(a.getTitle());
                vo.setContent(a.getContent());
                vo.setNoticeType(a.getNoticeType());
                vo.setStatus(a.getStatus());
                vo.setCreateName(resolvePublisherName(a));
                vo.setCreateAvatar(resolvePublisherAvatar(a));
                vo.setCreateTime(a.getCreateTime());
                vo.setIsRead(ua.getIsRead());
                vo.setReadTime(ua.getReadTime());
                records.add(vo);
            }
        }
        Page<AnnounceMyVO> result = new Page<>(pageNo, pageSize, uaPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public AnnounceDO getById(Long id) {
        return announceMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AnnounceDO entity) {
        Long uid = StpUtil.getLoginIdAsLong();
        entity.setCreateBy(uid);
        SysUserDTO user = getUserById(uid);
        if (user != null) {
            entity.setCreateName(user.getNickname());
        }
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (!StringUtils.hasText(entity.getChannels())) {
            entity.setChannels("[\"station\"]");
        }
        announceMapper.insert(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            publish(entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AnnounceDO entity) {
        AnnounceDO oldAnnounce = announceMapper.selectById(entity.getId());
        if (oldAnnounce == null) {
            throw new BusinessException(404, "通知不存在");
        }
        announceMapper.updateById(entity);
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            publish(entity.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (announceMapper.selectById(id) == null) {
            throw new BusinessException(404, "通知不存在");
        }
        announceMapper.deleteById(id);
    }

    @Override
    public PageResult<AnnounceDO> recyclePage(int pageNo, int pageSize, String title) {
        Page<AnnounceDO> page = new Page<>(pageNo, pageSize);
        var deletedPage = announceMapper.selectDeletedPage(page, title);
        return new PageResult<>(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = announceMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站通知不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = announceMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站通知不存在");
        }
        userAnnounceMapper.deletePhysicalByAnnounceId(id);
        sendLogMapper.deletePhysicalByAnnounceId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        AnnounceDO announce = announceMapper.selectById(id);
        if (announce == null) {
            throw new BusinessException(404, "通知不存在");
        }
        announce.setStatus(1);
        announceMapper.updateById(announce);
        List<Long> userIds = resolveTargetUserIds(announce);
        Set<Long> existing = new HashSet<>(userAnnounceMapper.selectUserIdsByAnnounceId(id));
        List<UserAnnounceDO> toInsert = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long uid : userIds) {
            if (uid == null || uid <= 0) {
                continue;
            }
            if (!existing.contains(uid)) {
                UserAnnounceDO ua = new UserAnnounceDO();
                ua.setUserId(uid);
                ua.setAnnounceId(id);
                ua.setIsRead(0);
                ua.setCreateTime(now);
                toInsert.add(ua);
            }
        }
        if (!toInsert.isEmpty()) {
            userAnnounceMapper.insertBatch(toInsert);
        }
        AnnounceSendLogDO sendLog = new AnnounceSendLogDO();
        sendLog.setAnnounceId(id);
        sendLog.setChannel("station");
        sendLog.setStatus(1);
        sendLog.setTargetCount(userIds.size());
        sendLog.setSuccessCount(userIds.size());
        sendLog.setSendTime(LocalDateTime.now());
        sendLogMapper.insert(sendLog);

        final String title = announce.getTitle();
        final String content = announce.getContent();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long uid : userIds) {
                    if (uid == null || uid <= 0) {
                        continue;
                    }
                    webSocketHandler.sendNotice(uid, id, title, content);
                }
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long announceId) {
        userAnnounceMapper.update(null, new LambdaUpdateWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId)
                .eq(UserAnnounceDO::getAnnounceId, announceId)
                .set(UserAnnounceDO::getIsRead, 1)
                .set(UserAnnounceDO::getReadTime, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        userAnnounceMapper.update(null, new LambdaUpdateWrapper<UserAnnounceDO>()
                .eq(UserAnnounceDO::getUserId, userId)
                .eq(UserAnnounceDO::getIsRead, 0)
                .set(UserAnnounceDO::getIsRead, 1)
                .set(UserAnnounceDO::getReadTime, LocalDateTime.now()));
    }

    @Override
    public long unreadCount(Long userId) {
        return userAnnounceMapper.countUnread(userId);
    }

    @Override
    public List<AnnounceSendLogDO> sendLogs(Long announceId) {
        return sendLogMapper.selectList(new LambdaQueryWrapper<AnnounceSendLogDO>()
                .eq(AnnounceSendLogDO::getAnnounceId, announceId)
                .orderByDesc(AnnounceSendLogDO::getSendTime));
    }

    private List<Long> resolveTargetUserIds(AnnounceDO announce) {
        Integer targetType = announce.getTargetType() == null ? 3 : announce.getTargetType();
        if (targetType == 3) {
            return parseTargetIds(announce.getTargetIds());
        }
        return parseTargetIds(announce.getTargetIds());
    }

    private List<Long> parseTargetIds(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public String resolvePublisherName(AnnounceDO announce) {
        if (announce == null) {
            return null;
        }
        SysUserDTO publisher = getUserById(announce.getCreateBy());
        if (publisher != null) {
            if (StringUtils.hasText(publisher.getNickname())) {
                return publisher.getNickname();
            }
            if (StringUtils.hasText(publisher.getUsername())) {
                return publisher.getUsername();
            }
        }
        return announce.getCreateName();
    }

    @Override
    public String resolvePublisherAvatar(AnnounceDO announce) {
        if (announce == null) {
            return null;
        }
        SysUserDTO publisher = getUserById(announce.getCreateBy());
        return publisher != null ? publisher.getAvatar() : null;
    }

    private SysUserDTO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            R<SysUserDTO> res = remoteUserService.getUserById(userId);
            if (res != null && res.getCode() == 200) {
                return res.getData();
            }
        } catch (Exception e) {
            log.warn("Feign call RemoteUserService.getUserById failed userId={}", userId);
        }
        return null;
    }
}
