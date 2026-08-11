package com.wu.cloud.common.mail;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮件发送结果
 */
@Data
public class MailResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否发送成功 */
    private boolean success;

    /** 结果描述 */
    private String message;

    public static MailResult success() {
        MailResult result = new MailResult();
        result.setSuccess(true);
        result.setMessage("发送成功");
        return result;
    }

    public static MailResult fail(String message) {
        MailResult result = new MailResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
