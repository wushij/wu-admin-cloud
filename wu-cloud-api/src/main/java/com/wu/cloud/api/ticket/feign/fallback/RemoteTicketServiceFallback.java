package com.wu.cloud.api.ticket.feign.fallback;

import com.wu.cloud.api.ticket.dto.TicketDTO;
import com.wu.cloud.api.ticket.dto.TicketDashboardDTO;
import com.wu.cloud.api.ticket.feign.RemoteTicketService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 远程工单服务 Feign 降级工厂（P0-3 修复）
 */
@Slf4j
@Component
public class RemoteTicketServiceFallback implements FallbackFactory<RemoteTicketService> {

    @Override
    public RemoteTicketService create(Throwable cause) {
        log.error("[Feign降级] wu-service-ticket 工单服务调用失败", cause);
        return new RemoteTicketService() {
            @Override
            public R<TicketDTO> getTicketById(Long id) {
                return R.fail("工单服务暂不可用");
            }

            @Override
            public R<Long> getNoticeUnreadCount(Long userId) {
                // 通知未读数降级为 0，避免阻塞消息中心面板加载
                return R.ok(0L);
            }

            @Override
            public R<TicketDashboardDTO> getDashboardStats() {
                return R.fail("工单服务暂不可用");
            }
        };
    }
}
