package com.wu.cloud.message.vo;

import org.springframework.util.StringUtils;

public final class ChatMsgType {
    public static final int TEXT = 1;
    public static final int IMAGE = 2;
    public static final int FILE = 3;
    /** 系统消息（如入群/退群提示） */
    public static final int SYSTEM = 4;
    /** 已撤回消息（列表预览显示「已撤回」，不展示原文） */
    public static final int RECALLED = 5;

    private ChatMsgType() {}

    public static String previewLabel(Integer msgType, String rawContent) {
        if (msgType == null || msgType == TEXT) {
            return rawContent != null ? rawContent : "";
        }
        if (msgType == IMAGE) {
            return "[图片]";
        }
        if (msgType == FILE) {
            if (StringUtils.hasText(rawContent)) {
                String trimmed = rawContent.trim();
                int idx = trimmed.lastIndexOf('/');
                if (idx >= 0 && idx < trimmed.length() - 1) {
                    return "[文件] " + trimmed.substring(idx + 1);
                }
            }
            return "[文件]";
        }
        if (msgType == SYSTEM) {
            return "[系统消息]";
        }
        if (msgType == RECALLED) {
            return "消息已撤回";
        }
        return rawContent != null ? rawContent : "";
    }
}
