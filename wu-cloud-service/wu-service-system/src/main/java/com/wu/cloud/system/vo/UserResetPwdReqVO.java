package com.wu.cloud.system.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserResetPwdReqVO implements Serializable {
    private Long id;
    private Long userId;
    private String password;
    private String newPassword;

    public Long getUserId() {
        return userId != null ? userId : id;
    }

    public String getNewPassword() {
        return newPassword != null ? newPassword : password;
    }
}
