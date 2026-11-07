package com.wu.cloud.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import com.wu.cloud.api.system.dto.SecurityEventDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.risk.RiskPrincipal;
import com.wu.cloud.system.service.security.AntiBotRiskService;
import com.wu.cloud.system.service.security.RiskLevel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安全风控内部 RPC 端点（风控属主唯一：wu-service-system）。
 * <p>其他服务的越权、重放、蜜罐等异常行为经此上报，由本服务统一计分、落库并执行封禁；
 * 封禁标记写入 Redis（security:risk:block:*），请求侧可直接读取拦截。</p>
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/inner/security")
public class InnerSecurityController {

    @Resource
    private AntiBotRiskService antiBotRiskService;

    /**
     * 上报一次风险事件
     *
     * @return {score, level, levelText, blocked}
     */
    @Operation(summary = "新增")
    @PostMapping("/event")
    public R<Map<String, Object>> reportEvent(@RequestBody SecurityEventDTO dto) {
        if (dto == null || dto.getEventType() == null || dto.getEventType().isBlank()) {
            return R.fail("事件类型不能为空");
        }
        RiskPrincipal subject = resolveSubject(dto.getPrincipal(), dto.getIp());
        int delta = dto.getDelta() == null ? 0 : dto.getDelta();
        int score = antiBotRiskService.addRisk(subject, delta, dto.getEventType(),
                dto.getAction() == null ? "NOTICE" : dto.getAction(),
                dto.getUri(), dto.getMethod(), dto.getUserAgent(), dto.getReason());
        RiskLevel level = antiBotRiskService.getRiskLevel(subject.key());

        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("score", score);
        data.put("level", level.getCode());
        data.put("levelText", level.getText());
        data.put("blocked", antiBotRiskService.isBlocked(subject));
        return R.ok(data);
    }

    /**
     * 查询主体是否处于封禁中
     */
    @Operation(summary = "校验")
    @GetMapping("/check")
    public R<Boolean> isBlocked(@RequestParam("principal") String principal) {
        RiskPrincipal subject = resolveSubject(principal, null);
        return R.ok(antiBotRiskService.isBlocked(subject));
    }

    /**
     * 主体归一化：优先使用传入主体，缺省时按 IP 构造 ip: 主体
     */
    private RiskPrincipal resolveSubject(String principal, String ip) {
        if (principal != null && !principal.isBlank()) {
            return RiskPrincipal.normalize(principal, ip);
        }
        if (ip != null && !ip.isBlank()) {
            return RiskPrincipal.ofIp(ip);
        }
        return RiskPrincipal.ofIp("unknown");
    }
}
