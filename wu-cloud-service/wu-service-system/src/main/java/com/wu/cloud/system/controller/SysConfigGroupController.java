package com.wu.cloud.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.wu.cloud.api.file.dto.StorageConfigDTO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.api.log.Log;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.security.annotation.RequiresPermissions;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.service.SysConfigGroupService;
import com.wu.cloud.system.vo.config.ConfigTestEmailReqVO;
import com.wu.cloud.system.vo.config.ConfigTestPaymentReqVO;
import com.wu.cloud.system.vo.config.ConfigTestRespVO;
import com.wu.cloud.system.vo.config.ConfigTestSmsReqVO;
import com.wu.cloud.system.vo.config.StorageConfigVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置分组 REST 控制器。
 * B 队修复：前缀由 /api/system/config 统一改为 /api/system/config-group，
 * 与前端 wu-admin-web / wu-admin-uniapp 的调用路径完全对齐（见 config/index.ts）。
 * 原有 /get、/create、/update、/delete 等非前端主流程接口保留为兼容入口。
 */
@Slf4j
@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/system/config-group")
public class SysConfigGroupController {

    /** 存储引擎配置与上传限制统一存放在 file 分组（与单体 SystemConfigHelper.GROUP_FILE 一致） */
    private static final String GROUP_FILE = "file";

    /** 敏感字段：写入时留空则保留原值，展示时只回传「是否已配置」 */
    private static final List<String> SENSITIVE_KEYS = List.of("minioSecretKey", "cosSecretKey", "ossAccessKeySecret");

    /** 仅用于展示/计算的派生字段，不写入配置 JSON */
    private static final List<String> DERIVED_KEYS = List.of("minioSecretKeyConfigured", "cosSecretKeyConfigured",
            "ossAccessKeySecretConfigured", "activeType", "activeBucket", "healthy");

    @Resource
    private SysConfigGroupService sysConfigGroupService;
    @Resource
    private RemoteFileService remoteFileService;

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
    @RequiresPermissions("system:config:update")
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

    @RequiresPermissions("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public R<Boolean> createConfigGroup(@RequestBody SysConfigGroupDO groupDO) {
        sysConfigGroupService.createConfigGroup(groupDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/update")
    public R<Boolean> updateConfigGroup(@RequestBody SysConfigGroupDO groupDO) {
        sysConfigGroupService.updateConfigGroup(groupDO);
        return R.ok(true);
    }

    @RequiresPermissions("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.DELETE)
    @DeleteMapping("/delete")
    public R<Boolean> deleteConfigGroup(@RequestParam("id") Long id) {
        sysConfigGroupService.deleteConfigGroup(id);
        return R.ok(true);
    }

    // ===== 存储引擎配置（存放在 file 分组） =====

    /**
     * 查询存储引擎配置（密钥不回传明文，仅给出是否已配置）
     */
    @RequiresPermissions("system:config:query")
    @GetMapping("/storage")
    public R<StorageConfigVO> getStorageConfig() {
        Map<String, Object> config = sysConfigGroupService.getConfigMap(GROUP_FILE);
        StorageConfigVO vo = new StorageConfigVO();
        vo.setType(str(config.get("type"), "local"));
        vo.setLocalPath(str(config.get("localPath"), ""));
        vo.setLocalDomain(str(config.get("localDomain"), ""));

        vo.setMinioDomain(str(config.get("minioDomain"), ""));
        vo.setMinioEndpoint(str(config.get("minioEndpoint"), ""));
        vo.setMinioAccessKey(str(config.get("minioAccessKey"), ""));
        vo.setMinioBucket(str(config.get("minioBucket"), ""));
        vo.setMinioSecretKeyConfigured(StrUtil.isNotBlank(str(config.get("minioSecretKey"), "")));

        vo.setCosRegion(str(config.get("cosRegion"), ""));
        vo.setCosBucket(str(config.get("cosBucket"), ""));
        vo.setCosSecretId(str(config.get("cosSecretId"), ""));
        vo.setCosDomain(str(config.get("cosDomain"), ""));
        vo.setCosSecretKeyConfigured(StrUtil.isNotBlank(str(config.get("cosSecretKey"), "")));

        vo.setOssEndpoint(str(config.get("ossEndpoint"), ""));
        vo.setOssBucket(str(config.get("ossBucket"), ""));
        vo.setOssAccessKeyId(str(config.get("ossAccessKeyId"), ""));
        vo.setOssDomain(str(config.get("ossDomain"), ""));
        vo.setOssAccessKeySecretConfigured(StrUtil.isNotBlank(str(config.get("ossAccessKeySecret"), "")));

        vo.setMaxSizeMb(intOf(config.get("maxSizeMb"), 50));
        vo.setAllowedExtensions(str(config.get("allowedExtensions"), ""));

        vo.setActiveType(vo.getType());
        vo.setActiveBucket(activeBucketOf(vo));
        vo.setHealthy(isStorageHealthy(vo, config));
        return R.ok(vo);
    }

    /**
     * 保存存储引擎配置：敏感字段留空时保留原值，写入后通知文件服务热重载客户端
     */
    @RequiresPermissions("system:config:update")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/storage")
    public R<Boolean> updateStorageConfig(@RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessException("存储配置不能为空");
        }
        Map<String, Object> merged = new LinkedHashMap<>(sysConfigGroupService.getConfigMap(GROUP_FILE));
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (DERIVED_KEYS.contains(key) || value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (SENSITIVE_KEYS.contains(key)) {
                // 密钥留空表示不修改，保留原值
                if (StrUtil.isNotBlank(text)) {
                    merged.put(key, text);
                }
                continue;
            }
            merged.put(key, text);
        }
        SysConfigGroupDO groupDO = new SysConfigGroupDO();
        groupDO.setGroupCode(GROUP_FILE);
        groupDO.setConfigValue(JSONUtil.toJsonStr(merged));
        sysConfigGroupService.updateConfigGroup(groupDO);

        // 通知文件服务清空对象存储客户端缓存，使新密钥/桶名立即生效
        try {
            remoteFileService.reloadStorage();
        } catch (Exception e) {
            log.warn("[存储配置] 文件服务热重载失败，配置已保存但需下次重启生效: {}", e.getMessage());
        }
        return R.ok(true);
    }

    /**
     * 存储引擎连通性测试：由文件服务用给定配置实际探测
     */
    @RequiresPermissions("system:config:update")
    @PostMapping("/test-storage")
    public R<Boolean> testStorage(@RequestBody Map<String, Object> body) {
        Map<String, Object> config = new LinkedHashMap<>(sysConfigGroupService.getConfigMap(GROUP_FILE));
        if (body != null) {
            body.forEach((k, v) -> {
                if (!DERIVED_KEYS.contains(k) && v != null) {
                    config.put(k, String.valueOf(v));
                }
            });
        }
        StorageConfigDTO dto = new StorageConfigDTO();
        dto.setType(str(config.get("type"), "local"));
        dto.setLocalPath(str(config.get("localPath"), ""));
        dto.setLocalDomain(str(config.get("localDomain"), ""));
        dto.setMinioDomain(str(config.get("minioDomain"), ""));
        dto.setMinioEndpoint(str(config.get("minioEndpoint"), ""));
        dto.setMinioAccessKey(str(config.get("minioAccessKey"), ""));
        dto.setMinioSecretKey(str(config.get("minioSecretKey"), ""));
        dto.setMinioBucket(str(config.get("minioBucket"), ""));
        dto.setCosRegion(str(config.get("cosRegion"), ""));
        dto.setCosBucket(str(config.get("cosBucket"), ""));
        dto.setCosSecretId(str(config.get("cosSecretId"), ""));
        dto.setCosSecretKey(str(config.get("cosSecretKey"), ""));
        dto.setCosDomain(str(config.get("cosDomain"), ""));
        dto.setOssEndpoint(str(config.get("ossEndpoint"), ""));
        dto.setOssBucket(str(config.get("ossBucket"), ""));
        dto.setOssAccessKeyId(str(config.get("ossAccessKeyId"), ""));
        dto.setOssAccessKeySecret(str(config.get("ossAccessKeySecret"), ""));
        dto.setOssDomain(str(config.get("ossDomain"), ""));

        R<Map<String, Object>> resp = remoteFileService.testStorage(dto);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            return R.fail(resp == null ? "文件服务无响应" : resp.getMsg());
        }
        Object success = resp.getData().get("success");
        Object message = resp.getData().get("message");
        if (Boolean.TRUE.equals(success)) {
            return R.ok(true);
        }
        return R.fail(message == null ? "存储连通性测试失败" : String.valueOf(message));
    }

    private String str(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private Integer intOf(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String activeBucketOf(StorageConfigVO vo) {
        String type = StrUtil.blankToDefault(vo.getType(), "local").toLowerCase();
        return switch (type) {
            case "oss" -> vo.getOssBucket();
            case "cos" -> vo.getCosBucket();
            case "minio" -> vo.getMinioBucket();
            default -> "";
        };
    }

    /** 当前类型所需的关键参数是否齐全 */
    private boolean isStorageHealthy(StorageConfigVO vo, Map<String, Object> config) {
        String type = StrUtil.blankToDefault(vo.getType(), "local").toLowerCase();
        return switch (type) {
            case "minio" -> StrUtil.isNotBlank(vo.getMinioEndpoint())
                    && StrUtil.isNotBlank(vo.getMinioAccessKey())
                    && StrUtil.isNotBlank(str(config.get("minioSecretKey"), ""));
            case "cos" -> StrUtil.isNotBlank(vo.getCosRegion())
                    && StrUtil.isNotBlank(vo.getCosSecretId())
                    && StrUtil.isNotBlank(str(config.get("cosSecretKey"), ""));
            case "oss" -> StrUtil.isNotBlank(vo.getOssEndpoint())
                    && StrUtil.isNotBlank(vo.getOssAccessKeyId())
                    && StrUtil.isNotBlank(str(config.get("ossAccessKeySecret"), ""));
            default -> true;
        };
    }

    /**
     * 支付配置连通性测试
     */
    @RequiresPermissions("system:config:update")
    @Log(title = "支付配置测试", businessType = Log.BusinessType.OTHER)
    @Operation(summary = "创建测试支付订单")
    @PostMapping("/test-payment")
    public R<ConfigTestRespVO> testPayment(@RequestBody ConfigTestPaymentReqVO request) {
        return R.ok(sysConfigGroupService.testPayment(request));
    }

    /**
     * 短信配置连通性测试：向指定手机号发送真实测试短信
     */
    @RequiresPermissions("system:config:update")
    @Log(title = "短信配置测试", businessType = Log.BusinessType.OTHER)
    @Operation(summary = "测试发送短信")
    @PostMapping("/test-sms")
    public R<ConfigTestRespVO> testSms(@RequestBody ConfigTestSmsReqVO request) {
        return R.ok(sysConfigGroupService.testSms(request));
    }

    /**
     * 邮件配置连通性测试：向指定邮箱发送真实测试邮件
     */
    @RequiresPermissions("system:config:update")
    @Log(title = "邮件配置测试", businessType = Log.BusinessType.OTHER)
    @Operation(summary = "测试发送邮件")
    @PostMapping("/test-email")
    public R<ConfigTestRespVO> testEmail(@RequestBody ConfigTestEmailReqVO request) {
        return R.ok(sysConfigGroupService.testEmail(request));
    }
}
