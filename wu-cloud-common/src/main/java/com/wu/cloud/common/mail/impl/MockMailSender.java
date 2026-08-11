package com.wu.cloud.common.mail.impl;

import com.wu.cloud.common.mail.MailResult;
import com.wu.cloud.common.mail.MailSender;
import lombok.extern.slf4j.Slf4j;

/**
 * Mock 邮件渠道：仅打印日志，用于开发与测试环境
 */
@Slf4j
public class MockMailSender implements MailSender {

    @Override
    public String channel() {
        return "mock";
    }

    @Override
    public MailResult send(String to, String subject, String content) {
        log.info("[Mock邮件] 收件人={} 主题={} 正文={}", to, subject, content);
        return MailResult.success();
    }

    @Override
    public MailResult sendHtml(String to, String subject, String html) {
        log.info("[Mock邮件-HTML] 收件人={} 主题={}", to, subject);
        return MailResult.success();
    }
}
