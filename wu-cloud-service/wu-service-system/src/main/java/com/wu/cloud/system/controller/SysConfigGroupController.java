package com.wu.cloud.system.controller;

import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.service.SysConfigGroupService;
import com.wu.cloud.system.vo.config.ConfigTestEmailReqVO;
import com.wu.cloud.system.vo.config.ConfigTestPaymentReqVO;
import com.wu.cloud.system.vo.config.ConfigTestRespVO;
import com.wu.cloud.system.vo.config.ConfigTestSmsReqVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置分组 REST 控制器。
 * B 队修复：前缀由 /api/system/config 统一改为 /api/system/config-group，
 * 与前端 wu-admin-web / wu-admin-uniapp 的调用路径完全对齐（见 config/index.ts）。
 * 原有 /get、/create、/update、/delete 等非前端主流程接口保留为兼容入口。
 */
@RestController
@RequestMapping("/api/system/config-group")
public class SysConfigGroupController {

    @Resource
    private SysConfigGroupService sysConfigGroupService;

    @RequiresPermissions("system:config:list")
    @GetMapping("/list")
    public R<List<SysConfigGroupDO>> getConfigGroupList(
            @RequestParam(value = "groupCode", required = false) String groupCode,
            @RequestParam(value = "groupName", required = false) String groupName) {
        return R.ok(sysConfigGroupService.getConfigGroupList(groupCode, groupName));
    }

    /**
     * 兼容入口：GET /api/system/config-group/get?groupCode=
     */
    @RequiresPermissions("system:config:query")
    @GetMapping("/get")
    public R<SysConfigGroupDO> getConfigGroup(@RequestParam("groupCode") String groupCode) {
        return R.ok(sysConfigGroupService.getConfigGroupByCode(groupCode));
    }

    /**
     * B 队修复：GET /api/system/config-group/{groupCode}（前端 useConfigDraft.loadGroup 调用）。
     * 返回包含 configValue 的配置分组 DO，供前端解析。
     */
    @RequiresPermissions("system:config:query")
    @GetMapping("/{groupCode}")
    public R<SysConfigGroupDO> getConfigGroupByPath(@PathVariable("groupCode") String groupCode) {
        return R.ok(sysConfigGroupService.getConfigGroupByCode(groupCode));
    }

    /**
     * B 队修复：PUT /api/system/config-group/{groupCode}（前端 useConfigDraft.save 调用）。
     * 请求体仅含 configValue，由 service 按 groupCode 回填主键后更新（见 updateConfigGroup）。
     */
    @RequiresPermissions("system:config:edit")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/{groupCode}")
    public R<Boolean> updateConfigGroupByPath(@PathVariable("groupCode") String groupCode,
                                              @RequestBody Map<String, Object> body) {
        Object configValue = body.get("configValue");
        SysConfigGroupDO groupDO = new SysConfigGroupDO();
        groupDO.setGroupCode(groupCode);
        groupDO.setConfigValue(configValue != null ? String.valueOf(configValue) : null);
        sysConfigGroupService.updateConfigGroup(groupDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:config:add")
    @Log(title = "系统配置", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Boolean> createConfigGroup(@RequestBody SysConfigGroupDO groupDO) {
        sysConfigGroupService.createConfigGroup(groupDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:config:edit")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Boolean> updateConfigGroup(@RequestBody SysConfigGroupDO groupDO) {
        sysConfigGroupService.updateConfigGroup(groupDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:config:remove")
    @Log(title = "系统配置", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> deleteConfigGroup(@RequestParam("id") Long id) {
        sysConfigGroupService.deleteConfigGroup(id);
        return R.ok(true);
    }

    /**
     * 支付配置连通性测试
     */
    @RequiresPermissions("system:config:edit")
    @Log(title = "支付配置测试", businessType = Log.BusinessType.OTHER)
    @PostMapping("/test-payment")
    public R<ConfigTestRespVO> testPayment(@RequestBody ConfigTestPaymentReqVO request) {
        return R.ok(sysConfigGroupService.testPayment(request));
    }

    /**
     * 短信配置连通性测试：向指定手机号发送真实测试短信
     */
    @RequiresPermissions("system:config:edit")
    @Log(title = "短信配置测试", businessType = Log.BusinessType.OTHER)
    @PostMapping("/test-sms")
    public R<ConfigTestRespVO> testSms(@RequestBody ConfigTestSmsReqVO request) {
        return R.ok(sysConfigGroupService.testSms(request));
    }

    /**
     * 邮件配置连通性测试：向指定邮箱发送真实测试邮件
     */
    @RequiresPermissions("system:config:edit")
    @Log(title = "邮件配置测试", businessType = Log.BusinessType.OTHER)
    @PostMapping("/test-email")
    public R<ConfigTestRespVO> testEmail(@RequestBody ConfigTestEmailReqVO request) {
        return R.ok(sysConfigGroupService.testEmail(request));
    }
}
