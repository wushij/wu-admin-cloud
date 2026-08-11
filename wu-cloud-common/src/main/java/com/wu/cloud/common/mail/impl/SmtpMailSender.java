package com.wu.cloud.common.mail.impl;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.wu.cloud.common.mail.MailProperties;
import com.wu.cloud.common.mail.MailResult;
import com.wu.cloud.common.mail.MailSender;
import lombok.extern.slf4j.Slf4j;

/**
 * SMTP 邮件渠道实现（基于 Hutool MailUtil）
 */
@Slf4j
public class SmtpMailSender implements MailSender {

    private final MailProperties properties;

    public SmtpMailSender(MailProperties properties) {
        this.properties = properties;
    }

    @Override
    public String channel() {
        return "smtp";
    }

    @Override
    public MailResult send(String to, String subject, String content) {
        return doSend(to, subject, content, false);
    }

    @Override
    public MailResult sendHtml(String to, String subject, String html) {
        return doSend(to, subject, html, true);
    }

    private MailResult doSend(String to, String subject, String content, boolean isHtml) {
        try {
            MailUtil.send(buildAccount(), to, subject, content, isHtml);
            return MailResult.success();
        } catch (Exception e) {
            log.error("[SMTP邮件] 发送失败 收件人={} 主题={}", to, subject, e);
            return MailResult.fail(e.getMessage());
        }
    }

    private MailAccount buildAccount() {
        MailAccount account = new MailAccount();
        account.setHost(properties.getHost());
        account.setPort(properties.getPort());
        account.setAuth(true);
        account.setFrom(properties.getFromName() + " <" + properties.getUsername() + ">");
        account.setUser(properties.getUsername());
        account.setPass(properties.getPassword());
        account.setSslEnable(properties.isSslEnable());
        account.setTimeout(10000);
        account.setConnectionTimeout(10000);
        return account;
    }
}
