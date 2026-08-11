package com.wu.cloud.admin.service.impl;

import com.wu.cloud.admin.service.DashboardService;
import com.wu.cloud.admin.vo.DashboardStatsVO;
import com.wu.cloud.api.infra.feign.RemoteOnlineUserService;
import com.wu.cloud.api.system.dto.RecentLoginDTO;
import com.wu.cloud.api.system.feign.RemoteStatsService;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.context.UserContextHolder;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 工作台聚合服务实现：并行聚合系统、工单等微服务的统计数据。
 *
 * <p>任一下游服务不可用时进行降级（返回默认值），保证工作台整体仍能渲染。</p>
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 单个下游调用的超时时间，避免慢服务拖垮整个工作台 */
    private static final long RPC_TIMEOUT_SECONDS = 3L;

    /** 最近登录展示条数 */
    private static final int RECENT_LOGIN_LIMIT = 8;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RemoteStatsService remoteStatsService;
    private final RemoteTicketService remoteTicketService;
    private final RemoteOnlineUserService remoteOnlineUserService;

    public DashboardServiceImpl(RemoteStatsService remoteStatsService,
                                RemoteTicketService remoteTicketService,
                                RemoteOnlineUserService remoteOnlineUserService) {
        this.remoteStatsService = remoteStatsService;
        this.remoteTicketService = remoteTicketService;
        this.remoteOnlineUserService = remoteOnlineUserService;
    }

    @Override
    public DashboardStatsVO getDashboardStats() {
        Long userId = UserContextHolder.getUserId();

        // 三路并行发起 RPC，整体耗时取决于最慢的一路而非累加
        CompletableFuture<Map<String, Object>> statsFuture =
                CompletableFuture.supplyAsync(() -> callOrDefault(
                        () -> remoteStatsService.getDashboardStats(), Collections.emptyMap(), "系统统计"));

        CompletableFuture<List<RecentLoginDTO>> loginsFuture =
                CompletableFuture.supplyAsync(() -> callOrDefault(
                        () -> remoteStatsService.getRecentLogins(RECENT_LOGIN_LIMIT),
                        Collections.emptyList(), "最近登录"));

        CompletableFuture<Long> unreadFuture =
                CompletableFuture.supplyAsync(() -> userId == null ? 0L : callOrDefault(
                        () -> remoteTicketService.getNoticeUnreadCount(userId), 0L, "未读通知"));

        CompletableFuture<TicketDashboardDTO> ticketStatsFuture =
                CompletableFuture.supplyAsync(() -> callOrDefault(
                        () -> remoteTicketService.getDashboardStats(), new TicketDashboardDTO(), "工单统计"));

        CompletableFuture<Integer> onlineFuture =
                CompletableFuture.supplyAsync(() -> callOrDefault(
                        () -> remoteOnlineUserService.countOnlineUsers(), 0, "在线用户"));

        if (userId != null) {
            try {
                remoteOnlineUserService.touchLastAccess(userId);
            } catch (Exception e) {
                log.debug("[工作台] 在线心跳失败 userId={}", userId);
            }
        }

        // 并行发起后一次性等待，避免逐个 join 叠加超时（原先最坏约 15 秒）
        try {
            CompletableFuture.allOf(statsFuture, loginsFuture, unreadFuture, ticketStatsFuture, onlineFuture)
                    .get(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("[工作台] 聚合子任务超时或异常，已降级", e);
        }

        DashboardStatsVO vo = new DashboardStatsVO();

        Map<String, Object> stats = getNowOrDefault(statsFuture, Collections.emptyMap());
        applySiteFromStats(vo, stats);
        vo.setUserCount(toLong(stats.get("userCount")));
        vo.setRoleCount(toLong(stats.get("roleCount")));
        vo.setMenuCount(toLong(stats.get("menuCount")));
        vo.setDeptCount(toLong(stats.get("deptCount")));
        vo.setPostCount(toLong(stats.get("postCount")));
        vo.setTodayLoginSuccess(toLong(stats.get("todayLoginSuccess")));
        vo.setTodayLoginFail(toLong(stats.get("todayLoginFail")));
        vo.setTodayVisits(toLong(stats.get("todayVisits")));
        vo.setYesterdayVisits(toLong(stats.get("yesterdayVisits")));
        vo.setVisitCount(vo.getTodayVisits() + vo.getYesterdayVisits());
        vo.setOnlineCount((long) getNowOrDefault(onlineFuture, 0));

        vo.setRecentLogins(convertRecentLogins(getNowOrDefault(loginsFuture, Collections.emptyList())));
        vo.setChatUnreadCount(getNowOrDefault(unreadFuture, 0L));

        // 工单/审批统计，工单服务不可用时降级为 0
        TicketDashboardDTO ticketStats = getNowOrDefault(ticketStatsFuture, new TicketDashboardDTO());
        vo.setTicketOpenCount(ticketStats == null ? 0L : ticketStats.getTicketOpenCount());
        vo.setTicketOverdueCount(ticketStats == null ? 0L : ticketStats.getTicketOverdueCount());
        vo.setApprovalPendingCount(ticketStats == null ? 0L : ticketStats.getApprovalPendingCount());
        return vo;
    }

    @Override
    public List<DashboardStatsVO.RecentLoginVO> getRecentLogins(Integer limit) {
        int size = (limit == null || limit <= 0) ? RECENT_LOGIN_LIMIT : limit;
        List<RecentLoginDTO> logins = callOrDefault(
                () -> remoteStatsService.getRecentLogins(size), Collections.emptyList(), "最近登录");
        return convertRecentLogins(logins);
    }

    @Override
    public void recordVisit() {
        try {
            remoteStatsService.recordVisit();
        } catch (Exception e) {
            log.warn("[工作台] 访问埋点上报失败", e);
        }
    }

    private void applySiteFromStats(DashboardStatsVO vo, Map<String, Object> stats) {
        Object name = stats.get("platformName");
        Object subtitle = stats.get("platformSubtitle");
        vo.setPlatformName(name != null && !String.valueOf(name).isBlank()
                ? String.valueOf(name) : "Admin Platform");
        vo.setPlatformSubtitle(subtitle != null && !String.valueOf(subtitle).isBlank()
                ? String.valueOf(subtitle) : "统一运维 · 高效管控");
    }

    private List<DashboardStatsVO.RecentLoginVO> convertRecentLogins(List<RecentLoginDTO> logins) {
        if (logins == null || logins.isEmpty()) {
            return Collections.emptyList();
        }
        List<DashboardStatsVO.RecentLoginVO> result = new ArrayList<>(logins.size());
        for (RecentLoginDTO dto : logins) {
            DashboardStatsVO.RecentLoginVO item = new DashboardStatsVO.RecentLoginVO();
            item.setUserId(dto.getUserId());
            item.setUsername(dto.getUsername());
            item.setNickname(dto.getNickname());
            item.setAvatar(dto.getAvatar());
            item.setIpaddr(dto.getIpaddr());
            item.setLoginLocation(dto.getLoginLocation());
            item.setBrowser(dto.getBrowser());
            item.setOs(dto.getOs());
            item.setStatus(dto.getStatus());
            item.setLoginTime(dto.getLoginTime() == null ? null : dto.getLoginTime().format(TIME_FORMATTER));
            result.add(item);
        }
        return result;
    }

    /**
     * 执行远程调用，异常或响应为空时返回默认值
     */
    private <T> T callOrDefault(Supplier<R<T>> supplier, T defaultValue, String scene) {
        try {
            R<T> response = supplier.get();
            if (response == null || response.getData() == null) {
                return defaultValue;
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("[工作台] {} 数据获取失败，已降级为默认值", scene, e);
            return defaultValue;
        }
    }

    /**
     * allOf 等待结束后立即取结果，未完成则返回默认值
     */
    private <T> T getNowOrDefault(CompletableFuture<T> future, T defaultValue) {
        T value = future.getNow(null);
        return value == null ? defaultValue : value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
