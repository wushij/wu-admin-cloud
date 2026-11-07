package com.wu.cloud.pay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.wu.cloud.common.core.PageParam;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.pay.entity.PayConfigDO;
import com.wu.cloud.pay.service.PayConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 支付配置 Controller
 */
@Tag(name = "支付配置")
@RestController
@RequestMapping("/api/pay/config")
public class PayConfigController {

    @Resource
    private PayConfigService payConfigService;

    /**
     * 分页查询支付配置
     */
    @Operation(summary = "分页查询 API 访问日志")
    @GetMapping("/page")
    public R<PageResult<PayConfigDO>> page(PageParam pageParam,
                                           @RequestParam(required = false) String payType,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) Integer status) {
        return R.ok(payConfigService.page(pageParam, payType, name, status));
    }

    /**
     * 支付配置详情
     */
    @Operation(summary = "下载工单附件")
    @GetMapping("/{id}")
    public R<PayConfigDO> detail(@PathVariable Long id) {
        return R.ok(payConfigService.detail(id));
    }

    /**
     * 新增支付配置
     */
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> create(@RequestBody PayConfigDO config) {
        return R.ok(payConfigService.create(config));
    }

    /**
     * 修改支付配置
     */
    @Operation(summary = "修改")
    @PostMapping("/update")
    public R<Boolean> update(@RequestBody PayConfigDO config) {
        payConfigService.update(config);
        return R.ok(true);
    }

    /**
     * 删除支付配置
     */
    @Operation(summary = "删除")
    @PostMapping("/delete")
    public R<Boolean> delete(@RequestParam Long id) {
        payConfigService.delete(id);
        return R.ok(true);
    }

    /**
     * 获取指定支付类型启用的配置（apiKey 解密返回）
     */
    @Operation(summary = "获取详情")
    @GetMapping("/enabled")
    public R<PayConfigDO> getEnabledConfig(@RequestParam String payType) {
        return R.ok(payConfigService.getEnabledConfig(payType));
    }
}
