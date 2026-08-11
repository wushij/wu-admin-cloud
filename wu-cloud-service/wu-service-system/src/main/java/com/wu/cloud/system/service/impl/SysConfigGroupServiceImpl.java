package com.wu.cloud.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.common.mail.MailResult;
import com.wu.cloud.common.mail.MailSender;
import com.wu.cloud.common.sms.SmsProperties;
import com.wu.cloud.common.sms.SmsResult;
import com.wu.cloud.common.sms.SmsSender;
import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.mapper.SysConfigGroupMapper;
import com.wu.cloud.system.service.SysConfigGroupService;
import com.wu.cloud.system.vo.config.ConfigTestEmailReqVO;
import com.wu.cloud.system.vo.config.ConfigTestPaymentReqVO;
import com.wu.cloud.system.vo.config.ConfigTestRespVO;
import com.wu.cloud.system.vo.config.ConfigTestSmsReqVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 系统配置分组服务实现，含短信/邮件/支付渠道连通性测试
 */
@Slf4j
@Service
public class SysConfigGroupServiceImpl implements SysConfigGroupService {

    /** 手机号校验规则 */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /** 邮箱校验规则 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 支持的支付类型 */
    private static final Set<String> SUPPORTED_PAY_TYPES = Set.of("wechat", "alipay");

    /** 短信配置分组编码 */
    private static final String GROUP_SMS = "sms";

    /** 支付配置分组编码 */
    private static final String GROUP_PAYMENT = "payment";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private SysConfigGroupMapper sysConfigGroupMapper;

    @Resource
    private SmsSender smsSender;

    @Resource
    private MailSender mailSender;

    @Resource
    private SmsProperties smsProperties;

    /**
     * 支付服务为独立微服务，可能尚未部署，使用 ObjectProvider 做可选注入
     */
    @Resource
    private ObjectProvider<com.wu.cloud.api.pay.feign.RemotePayService> remotePayServiceProvider;

    @Override
    public List<SysConfigGroupDO> getConfigGroupList(String groupCode, String groupName) {
        LambdaQueryWrapper<SysConfigGroupDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(groupCode)) {
            wrapper.like(SysConfigGroupDO::getGroupCode, groupCode);
        }
        if (StringUtils.hasText(groupName)) {
            wrapper.like(SysConfigGroupDO::getGroupName, groupName);
        }
        wrapper.orderByDesc(SysConfigGroupDO::getUpdateTime);
        return sysConfigGroupMapper.selectList(wrapper);
    }

    @Override
    public SysConfigGroupDO getConfigGroupByCode(String groupCode) {
        if (!StringUtils.hasText(groupCode)) {
            return null;
        }
        return sysConfigGroupMapper.selectOne(new LambdaQueryWrapper<SysConfigGroupDO>()
                .eq(SysConfigGroupDO::getGroupCode, groupCode)
                .last("LIMIT 1"));
    }

    @Override
    public Map<String, Object> getConfigMap(String groupCode) {
        SysConfigGroupDO group = getConfigGroupByCode(groupCode);
        if (group == null || !StringUtils.hasText(group.getConfigValue())) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(
                    group.getConfigValue(), new TypeReference<Map<String, Object>>() {
                    });
            return map == null ? Collections.emptyMap() : map;
        } catch (Exception e) {
            log.warn("[系统配置] 分组 {} 的 configValue 不是合法 JSON，已按空配置处理", groupCode);
            return Collections.emptyMap();
        }
    }

    @Override
    public void createConfigGroup(SysConfigGroupDO groupDO) {
        if (groupDO == null || !StringUtils.hasText(groupDO.getGroupCode())) {
            throw new BusinessException("分组编码不能为空");
        }
        if (getConfigGroupByCode(groupDO.getGroupCode()) != null) {
            throw new BusinessException("分组编码已存在：" + groupDO.getGroupCode());
        }
        sysConfigGroupMapper.insert(groupDO);
    }

    @Override
    public void updateConfigGroup(SysConfigGroupDO groupDO) {
        if (groupDO == null) {
            throw new BusinessException("参数不能为空");
        }
        // 兼容前端仅传 groupCode 的场景：按编码回填主键后再更新
        if (groupDO.getId() == null) {
            SysConfigGroupDO exists = getConfigGroupByCode(groupDO.getGroupCode());
            if (exists == null) {
                throw new BusinessException("配置分组不存在：" + groupDO.getGroupCode());
            }
            groupDO.setId(exists.getId());
        }
        sysConfigGroupMapper.updateById(groupDO);
    }

    @Override
    public void deleteConfigGroup(Long id) {
        if (id == null) {
            throw new BusinessException("主键不能为空");
        }
        sysConfigGroupMapper.deleteById(id);
    }

    @Override
    public ConfigTestRespVO testPayment(ConfigTestPaymentReqVO request) {
        String type = request == null ? null : trim(request.getType());
        if (!StringUtils.hasText(type)) {
            throw new BusinessException("支付类型不能为空");
        }
        if (!SUPPORTED_PAY_TYPES.contains(type.toLowerCase())) {
            throw new BusinessException("不支持的支付类型：" + type);
        }

        Map<String, Object> config = getConfigMap(GROUP_PAYMENT);
        if (config.isEmpty()) {
            return ConfigTestRespVO.fail(type, "尚未配置支付参数，请先在【支付配置】中保存配置");
        }

        // 先做本地静态校验：必填项缺失时无需调用支付服务即可给出明确提示
        List<String> missing = checkPaymentRequiredKeys(type.toLowerCase(), config);
        if (!missing.isEmpty()) {
            return ConfigTestRespVO.fail(type, "支付参数不完整，缺少：" + String.join("、", missing));
        }

        // 支付服务当前仅提供订单查询能力，待其补齐测试下单接口后在此发起真实调用
        if (remotePayServiceProvider.getIfAvailable() == null) {
            return ConfigTestRespVO.fail(type, "配置项完整，但支付服务未接入，无法发起真实测试订单")
                    .put("configComplete", true);
        }
        return ConfigTestRespVO.fail(type, "配置项完整，但支付服务暂未提供测试下单接口")
                .put("configComplete", true);
    }

    /**
     * 校验支付渠道必填配置项
     *
     * @param type   支付类型（小写）
     * @param config 配置分组内容
     * @return 缺失的配置项名称列表，全部齐全时返回空列表
     */
    private List<String> checkPaymentRequiredKeys(String type, Map<String, Object> config) {
        List<String> required = "wechat".equals(type)
                ? List.of("appId", "mchId", "apiKey")
                : List.of("appId", "privateKey", "publicKey");
        List<String> missing = new ArrayList<>();
        for (String key : required) {
            Object value = config.get(key);
            if (value == null || !StringUtils.hasText(value.toString())) {
                missing.add(key);
            }
        }
        return missing;
    }

    @Override
    public ConfigTestRespVO testSms(ConfigTestSmsReqVO request) {
        String phone = request == null ? null : trim(request.getPhone());
        if (!StringUtils.hasText(phone) || !MOBILE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("请输入正确的手机号");
        }

        String templateId = StringUtils.hasText(request.getTemplateCode())
                ? trim(request.getTemplateCode())
                : resolveSmsTemplateId();
        if (!StringUtils.hasText(templateId)) {
            return ConfigTestRespVO.fail(smsSender.channel(),
                    "未配置短信模板，请先在【短信配置】中设置验证码模板 ID");
        }

        // 随机 6 位测试验证码，避免固定值被外部利用
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        Map<String, String> params = new HashMap<>(2);
        params.put("code", code);

        long start = System.currentTimeMillis();
        try {
            SmsResult result = smsSender.send(phone, templateId, params);
            long cost = System.currentTimeMillis() - start;
            if (result != null && result.isSuccess()) {
                ConfigTestRespVO vo = ConfigTestRespVO.success(smsSender.channel(), "测试短信已发送，请注意查收");
                vo.setCostMillis(cost);
                return vo.put("bizId", result.getBizId()).put("templateId", templateId);
            }
            String detail = result == null ? "渠道无响应" : result.getMessage();
            ConfigTestRespVO vo = ConfigTestRespVO.fail(smsSender.channel(),
                    "短信发送失败：" + (StringUtils.hasText(detail) ? detail : "请检查签名、模板 ID 与密钥是否正确"));
            vo.setCostMillis(cost);
            return vo;
        } catch (Exception e) {
            log.error("[配置测试] 短信发送异常 phone={}", maskMobile(phone), e);
            ConfigTestRespVO vo = ConfigTestRespVO.fail(smsSender.channel(),
                    "短信发送异常：" + rootMessage(e));
            vo.setCostMillis(System.currentTimeMillis() - start);
            return vo;
        }
    }

    @Override
    public ConfigTestRespVO testEmail(ConfigTestEmailReqVO request) {
        String toEmail = request == null ? null : trim(request.getToEmail());
        if (!StringUtils.hasText(toEmail) || !EMAIL_PATTERN.matcher(toEmail).matches()) {
            throw new BusinessException("请输入正确的接收邮箱");
        }

        String time = LocalDateTime.now().format(TIME_FORMATTER);
        String subject = "【系统配置】邮件通道测试";
        String content = "这是一封来自后台管理系统的测试邮件。\n\n"
                + "若您收到此邮件，说明邮件通道配置正确。\n"
                + "发送时间：" + time + "\n";

        long start = System.currentTimeMillis();
        try {
            MailResult result = mailSender.send(toEmail, subject, content);
            long cost = System.currentTimeMillis() - start;
            if (result != null && result.isSuccess()) {
                ConfigTestRespVO vo = ConfigTestRespVO.success(mailSender.channel(), "测试邮件已发送，请注意查收");
                vo.setCostMillis(cost);
                return vo.put("toEmail", toEmail);
            }
            String detail = result == null ? "渠道无响应" : result.getMessage();
            ConfigTestRespVO vo = ConfigTestRespVO.fail(mailSender.channel(),
                    "邮件发送失败：" + (StringUtils.hasText(detail) ? detail : "请检查 SMTP 主机、端口与授权码"));
            vo.setCostMillis(cost);
            return vo;
        } catch (Exception e) {
            log.error("[配置测试] 邮件发送异常 toEmail={}", toEmail, e);
            ConfigTestRespVO vo = ConfigTestRespVO.fail(mailSender.channel(),
                    "邮件发送异常：" + rootMessage(e));
            vo.setCostMillis(System.currentTimeMillis() - start);
            return vo;
        }
    }

    /**
     * 模板 ID 取值优先级：数据库配置分组 > application.yml
     */
    private String resolveSmsTemplateId() {
        Object fromDb = getConfigMap(GROUP_SMS).get("verifyCodeTemplateId");
        if (fromDb != null && StringUtils.hasText(fromDb.toString())) {
            return fromDb.toString().trim();
        }
        return smsProperties.getVerifyCodeTemplateId();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return StringUtils.hasText(cause.getMessage()) ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
