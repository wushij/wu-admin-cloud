package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SecurityEventDO;
import com.wu.cloud.system.service.security.AntiBotRiskService;
import com.wu.cloud.system.service.security.RiskLevel;
import com.wu.cloud.system.service.security.RiskSubjectPage;
import com.wu.cloud.system.service.security.SecurityEventService;
import com.wu.cloud.system.vo.RiskSubjectVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全审计与行为风控 Controller。
 *
 * <p>风控是本服务的唯一属主：计分、封禁与事件落库都在 system 服务完成，
 * 网关侧只读取同一份 Redis 状态做请求拦截，不重复实现计分逻辑。</p>
 */
@Tag(name = "安全审计与行为风控")
@RestController
@RequestMapping("/api/system/security")
public class SecurityRiskController {

    @Resource
    private SecurityEventService securityEventService;

    @Resource
    private AntiBotRiskService antiBotRiskService;

    /**
     * 获取安全拦截事件列表
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "获取安全拦截事件列表")
    @GetMapping("/events")
    public R<PageResult<SecurityEventDO>> listEvents(PageQuery pageQuery,
                                                    @RequestParam(value = "ipaddr", required = false) String ipaddr,
                                                    @RequestParam(value = "eventType", required = false) String eventType,
                                                    @RequestParam(value = "action", required = false) String action) {
        Page<SecurityEventDO> page = securityEventService.getPage(
                pageNoOf(pageQuery), pageSizeOf(pageQuery), ipaddr, eventType, action);
        return R.ok(PageResult.of(page.getRecords(), page.getTotal()));
    }

    /**
     * 获取安全拦截大盘统计数据
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "获取安全拦截大盘统计数据")
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        return R.ok(securityEventService.getDashboardStats());
    }

    /**
     * 获取当前处于封禁中的风险名单
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "获取当前处于封禁中的风险名单")
    @GetMapping("/risk-list")
    public R<List<RiskSubjectVO>> getRiskList() {
        return R.ok(antiBotRiskService.getActiveRiskList());
    }

    /**
     * 风险主体分页列表（含未封禁的高风险主体）
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "风险主体分页列表（含未封禁的高风险主体）")
    @GetMapping("/risk-subjects")
    public R<PageResult<RiskSubjectVO>> listRiskSubjects(PageQuery pageQuery,
                                                         @RequestParam(value = "keyword", required = false) String keyword,
                                                         @RequestParam(value = "level", required = false) Integer level,
                                                         @RequestParam(value = "blocked", required = false) Boolean blocked) {
        RiskSubjectPage page = antiBotRiskService.getRiskSubjectPage(
                pageNoOf(pageQuery), pageSizeOf(pageQuery), keyword, level, blocked);
        return R.ok(PageResult.of(page.records(), page.total()));
    }

    /**
     * 查询风险主体最近的安全事件
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "查询风险主体最近的安全事件")
    @GetMapping("/risk-subject/events")
    public R<List<SecurityEventDO>> recentEvents(@RequestParam("principal") String principal,
                                                 @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return R.ok(antiBotRiskService.getRecentEvents(principal, limit == null ? 20 : limit));
    }

    /**
     * 获取风险主体当前分数与等级
     */
    @RequiresPermissions("system:security:query")
    @Operation(summary = "获取风险主体当前分数与等级")
    @GetMapping("/risk-subject/score")
    public R<Map<String, Object>> riskScore(@RequestParam("principal") String principal) {
        int score = antiBotRiskService.getRiskScore(principal);
        RiskLevel level = antiBotRiskService.getRiskLevel(principal);
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("principal", principal);
        data.put("score", score);
        data.put("level", level.getCode());
        data.put("levelText", level.getText());
        return R.ok(data);
    }

    /**
     * 一键解除主体封禁
     *
     * <p>{@code resetRisk} 缺省为 true：解封同时清零风险分；
     * 显式传 false 则保留风险分，便于只解封不重置的运营场景。</p>
     */
    @RequiresPermissions("system:security:unblock")
    @Log(title = "安全风控", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "一键解除主体封禁")
    @PostMapping("/unblock")
    public R<Boolean> unblock(@RequestParam("principal") String principal,
                               @RequestParam(value = "resetRisk", defaultValue = "true") Boolean resetRisk) {
        antiBotRiskService.unblockPrincipal(principal, resetRisk == null || resetRisk);
        return R.ok(true);
    }

    /**
     * 手动封禁主体
     */
    @RequiresPermissions("system:security:block")
    @Log(title = "安全风控", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "手动封禁主体")
    @PostMapping("/block")
    public R<Boolean> block(@RequestParam("principal") String principal,
                             @RequestParam(value = "durationSeconds", defaultValue = "3600") Long durationSeconds,
                             @RequestParam(value = "reason", defaultValue = "管理员手动封禁") String reason) {
        antiBotRiskService.blockPrincipal(principal, durationSeconds == null ? 3600L : durationSeconds, reason);
        return R.ok(true);
    }

    /**
     * 删除单条安全事件
     */
    @RequiresPermissions("system:security:delete")
    @Log(title = "安全风控", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除安全事件")
    @DeleteMapping("/events/{id}")
    public R<Boolean> delete(@PathVariable("id") Long id) {
        securityEventService.delete(id);
        return R.ok(true);
    }

    /**
     * 清空安全事件日志
     */
    @RequiresPermissions("system:security:clear")
    @Log(title = "安全风控", businessType = Log.BusinessType.DELETE, isSaveRequestData = false)
    @Operation(summary = "清空安全事件日志")
    @DeleteMapping("/events/clear")
    public R<Boolean> clear() {
        securityEventService.clear();
        return R.ok(true);
    }

    /** 默认页码，缺省为第 1 页 */
    private int pageNoOf(PageQuery pageQuery) {
        return pageQuery == null || pageQuery.getPageNo() == null || pageQuery.getPageNo() < 1
                ? 1 : pageQuery.getPageNo();
    }

    /** 默认每页条数，缺省为 10 */
    private int pageSizeOf(PageQuery pageQuery) {
        return pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1
                ? 10 : pageQuery.getPageSize();
    }
}
