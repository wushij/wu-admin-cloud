package com.wu.cloud.auth.service;

import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.auth.dto.BindEmailDTO;
import com.wu.cloud.auth.dto.BindMobileDTO;
import com.wu.cloud.auth.dto.ChangePasswordDTO;
import com.wu.cloud.auth.dto.PasswordEmailResetDTO;
import com.wu.cloud.auth.dto.PasswordSmsResetDTO;
import com.wu.cloud.auth.dto.ProfileUpdateDTO;
import com.wu.cloud.auth.vo.ProfileVO;
import com.wu.cloud.common.core.domain.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心服务
 */
public interface ProfileService {

    /**
     * 获取当前登录用户的个人信息
     */
    ProfileVO getProfile();

    /**
     * 更新个人资料
     */
    void updateProfile(ProfileUpdateDTO updateDTO);

    /**
     * 使用原密码修改密码
     */
    void changePassword(ChangePasswordDTO changeDTO);

    /**
     * 发送绑定手机号验证码
     */
    void sendMobileBindCode(String mobile);

    /**
     * 绑定手机号
     */
    void bindMobile(BindMobileDTO bindDTO);

    /**
     * 发送绑定邮箱验证码
     */
    void sendEmailBindCode(String email);

    /**
     * 绑定邮箱
     */
    void bindEmail(BindEmailDTO bindDTO);

    /**
     * 向当前账号绑定手机号发送重置密码验证码
     *
     * @return 脱敏手机号
     */
    String sendPasswordSmsCode();

    /**
     * 通过短信验证码重置密码
     */
    void resetPasswordBySms(PasswordSmsResetDTO resetDTO);

    /**
     * 向当前账号绑定邮箱发送重置密码验证码
     *
     * @return 脱敏邮箱
     */
    String sendPasswordEmailCode();

    /**
     * 通过邮箱验证码重置密码
     */
    void resetPasswordByEmail(PasswordEmailResetDTO resetDTO);

    /**
     * 上传头像
     *
     * @return 头像访问地址
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 分页查询我的登录记录
     */
    PageResult<LoginLogDTO> getMyLoginLogs(Integer pageNo, Integer pageSize);
}
