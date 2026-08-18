package com.wu.cloud.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatGroupLogVO {
    private Long id;
    private Long groupId;
    private String actionType;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    private String detail;
    private LocalDateTime createTime;
    /** 前端直接展示的可读日志文本（由 actionType + operatorName + detail 组合） */
    private String content;

    /** 根据各字段组装可读的日志描述 */
    public void buildContent() {
        String actor = operatorName != null ? operatorName : ("用户" + operatorId);
        String target = targetUserName != null ? "「" + targetUserName + "」" : "";
        String desc = detail != null ? detail : "";

        String text;
        switch (actionType != null ? actionType : "") {
            case "CREATE":
                text = actor + " " + desc;
                break;
            case "INVITE":
                text = actor + " 邀请了新成员" + (desc.isBlank() ? "" : "（" + desc + "）");
                break;
            case "REMOVE":
                text = actor + " 移出了成员 " + target;
                break;
            case "QUIT":
                text = actor + " 退出了群聊";
                break;
            case "DISSOLVE":
                text = actor + " 解散了群聊";
                break;
            case "UPDATE":
                text = actor + " 修改了群资料" + (desc.isBlank() ? "" : "（" + desc + "）");
                break;
            case "SET_ADMIN":
                text = actor + " 将 " + target + " 设为管理员";
                break;
            case "REMOVE_ADMIN":
                text = actor + " 取消了 " + target + " 的管理员";
                break;
            case "MUTE":
                text = actor + " 禁言了成员 " + target;
                break;
            case "UNMUTE":
                text = actor + " 解除了成员 " + target + " 的禁言";
                break;
            case "TRANSFER_OWNER":
                text = actor + " 将群主转让给 " + target;
                break;
            default:
                text = desc.isBlank() ? (actor + " 执行了操作") : (actor + "：" + desc);
                break;
        }
        this.content = text;
    }
}
