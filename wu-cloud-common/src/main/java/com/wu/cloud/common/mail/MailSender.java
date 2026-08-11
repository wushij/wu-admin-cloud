package com.wu.cloud.common.mail;

/**
 * 邮件发送统一抽象接口
 */
public interface MailSender {

    /**
     * 渠道标识，如 smtp / mock
     *
     * @return 渠道编码
     */
    String channel();

    /**
     * 发送纯文本邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param content 正文
     * @return 发送结果
     */
    MailResult send(String to, String subject, String content);

    /**
     * 发送 HTML 邮件
     *
     * @param to      收件人
     * @param subject 主题
     * @param html    HTML 正文
     * @return 发送结果
     */
    MailResult sendHtml(String to, String subject, String html);
}
