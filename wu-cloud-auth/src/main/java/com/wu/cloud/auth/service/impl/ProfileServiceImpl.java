package com.wu.cloud.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.wu.cloud.api.file.dto.SysFileDTO;
import com.wu.cloud.api.file.feign.RemoteFileService;
import com.wu.cloud.api.system.dto.LoginLogDTO;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.api.system.dto.UserProfileUpdateDTO;
import com.wu.cloud.api.system.feign.RemoteLoginLogService;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.dto.BindEmailDTO;
import com.wu.cloud.auth.dto.BindMobileDTO;
import com.wu.cloud.auth.dto.ChangePasswordDTO;
import com.wu.cloud.auth.dto.PasswordEmailResetDTO;
import com.wu.cloud.auth.dto.PasswordSmsResetDTO;
import com.wu.cloud.auth.dto.ProfileUpdateDTO;
import com.wu.cloud.auth.service.ProfileService;
import com.wu.cloud.auth.service.VerifyCodeService;
import com.wu.cloud.auth.support.AuthUtils;
import com.wu.cloud.auth.vo.ProfileVO;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

/**
 * 个人中心服务实现
 */
@Slf4j
@Service
public class ProfileServiceImpl implements ProfileService {

    /** 头像允许的扩展名 */
    private static final List<String> AVATAR_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    /** 头像大小上限 5MB */
    private static final long AVATAR_MAX_SIZE = 5L * 1024 * 1024;

    @Resource
    private RemoteUserService remoteUserService;

    @Resource
    private RemoteLoginLogService remoteLoginLogService;

    @Resource
    private RemoteFileService remoteFileService;

    @Resource
    private VerifyCodeService verifyCodeService;

    @Resource
    private AuthProperties authProperties;

    @Override
    public ProfileVO getProfile() {
        Long userId = currentUserId();
        R<UserProfileDTO> result;
        try {
            result = remoteUserService.getUserProfile(userId);
        } catch (Exception e) {
            log.error("[个人中心] 查询完整资料失败 userId={}", userId, e);
            throw new BusinessException("用户服务暂不可用，请稍后重试");
        }
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(result == null || result.getMsg() == null
                    ? "用户资料查询失败" : result.getMsg());
        }
        UserProfileDTO profile = result.getData();

        ProfileVO vo = new ProfileVO();
        vo.setUserId(profile.getUserId());
        vo.setUsername(profile.getUsername());
        vo.setNickname(profile.getNickname());
        // 与单体 buildProfileMap 一致：返回完整手机号/邮箱，前端自行脱敏展示
        vo.setMobile(profile.getMobile());
        vo.setEmail(profile.getEmail());
        vo.setAvatar(profile.getAvatar());
        vo.setStatus(profile.getStatus());
        vo.setSex(profile.getSex());
        vo.setDeptId(profile.getDeptId());
        vo.setDeptName(profile.getDeptName());
        vo.setRemark(profile.getRemark());
        vo.setRoleIds(profile.getRoleIds());
        vo.setRoleNames(profile.getRoleNames());
        vo.setRoleCodes(profile.getRoleCodes());
        vo.setPostIds(profile.getPostIds());
        vo.setPostNames(profile.getPostNames());
        vo.setCreateTime(profile.getCreateTime());
        vo.setUpdateTime(profile.getUpdateTime());
        vo.setLastLoginTime(profile.getLastLoginTime());
        vo.setLastLoginIp(profile.getLastLoginIp());
        vo.setLastLoginLocation(profile.getLastLoginLocation());
        vo.setMinPasswordLength(profile.getMinPasswordLength());

        R<LoginUserDTO> loginUser = remoteUserService.getUserInfoByUsername(profile.getUsername());
        if (loginUser != null && loginUser.getData() != null) {
            vo.setRoles(loginUser.getData().getRoles());
            vo.setPermissions(loginUser.getData().getPermissions());
        }
        return vo;
    }

    @Override
    public void updateProfile(ProfileUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException("参数不能为空");
        }
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setUserId(currentUserId());
        dto.setNickname(updateDTO.getNickname());
        dto.setSex(updateDTO.getSex());
        dto.setRemark(updateDTO.getRemark());

        assertSuccess(remoteUserService.updateProfile(dto), "个人资料更新失败");
    }

    @Override
    public void changePassword(ChangePasswordDTO changeDTO) {
        if (changeDTO == null || !StringUtils.hasText(changeDTO.getOldPassword())) {
            throw new BusinessException("请输入原密码");
        }
        AuthUtils.validatePassword(changeDTO.getNewPassword(),
                changeDTO.getConfirmPassword(), authProperties.getPasswordMinLength());

        LoginUserDTO user = currentUser();
        if (!AuthUtils.matchPassword(changeDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (AuthUtils.matchPassword(changeDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        assertSuccess(remoteUserService.resetPassword(user.getUserId(),
                AuthUtils.encryptPassword(changeDTO.getNewPassword())), "密码修改失败");

        // 密码变更后强制重新登录
        StpUtil.logout();
    }

    @Override
    public void sendMobileBindCode(String mobile) {
        if (!AuthUtils.isMobile(mobile)) {
            throw new BusinessException("手机号格式不正确");
        }
        R<LoginUserDTO> exists = remoteUserService.getUserByMobile(mobile);
        if (exists != null && exists.getData() != null
                && !exists.getData().getUserId().equals(currentUserId())) {
            throw new BusinessException("该手机号已被其他账号绑定");
        }
        verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_BIND_MOBILE, mobile);
    }

    @Override
    public void bindMobile(BindMobileDTO bindDTO) {
        if (bindDTO == null || !AuthUtils.isMobile(bindDTO.getMobile())) {
            throw new BusinessException("手机号格式不正确");
        }
        verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_BIND_MOBILE,
                bindDTO.getMobile(), bindDTO.getCode());

        R<LoginUserDTO> exists = remoteUserService.getUserByMobile(bindDTO.getMobile());
        if (exists != null && exists.getData() != null
                && !exists.getData().getUserId().equals(currentUserId())) {
            throw new BusinessException("该手机号已被其他账号绑定");
        }

        assertSuccess(remoteUserService.bindMobile(currentUserId(), bindDTO.getMobile()), "手机号绑定失败");
    }

    @Override
    public void sendEmailBindCode(String email) {
        if (!AuthUtils.isEmail(email)) {
            throw new BusinessException("邮箱格式不正确");
        }
        R<LoginUserDTO> exists = remoteUserService.getUserByEmail(email);
        if (exists != null && exists.getData() != null
                && !exists.getData().getUserId().equals(currentUserId())) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        verifyCodeService.sendEmailCode(VerifyCodeService.SCENE_BIND_EMAIL, email);
    }

    @Override
    public void bindEmail(BindEmailDTO bindDTO) {
        if (bindDTO == null || !AuthUtils.isEmail(bindDTO.getEmail())) {
            throw new BusinessException("邮箱格式不正确");
        }
        verifyCodeService.verifyEmailCode(VerifyCodeService.SCENE_BIND_EMAIL,
                bindDTO.getEmail(), bindDTO.getCode());

        R<LoginUserDTO> exists = remoteUserService.getUserByEmail(bindDTO.getEmail());
        if (exists != null && exists.getData() != null
                && !exists.getData().getUserId().equals(currentUserId())) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }

        assertSuccess(remoteUserService.bindEmail(currentUserId(), bindDTO.getEmail()), "邮箱绑定失败");
    }

    @Override
    public String sendPasswordSmsCode() {
        LoginUserDTO user = currentUser();
        if (!StringUtils.hasText(user.getMobile())) {
            throw new BusinessException("当前账号未绑定手机号");
        }
        verifyCodeService.sendSmsCode(VerifyCodeService.SCENE_RESET_PASSWORD_SMS, user.getMobile());
        return AuthUtils.maskMobile(user.getMobile());
    }

    @Override
    public void resetPasswordBySms(PasswordSmsResetDTO resetDTO) {
        if (resetDTO == null) {
            throw new BusinessException("参数不能为空");
        }
        AuthUtils.validatePassword(resetDTO.getNewPassword(),
                resetDTO.getConfirmPassword(), authProperties.getPasswordMinLength());

        LoginUserDTO user = currentUser();
        if (!StringUtils.hasText(user.getMobile())) {
            throw new BusinessException("当前账号未绑定手机号");
        }
        verifyCodeService.verifySmsCode(VerifyCodeService.SCENE_RESET_PASSWORD_SMS,
                user.getMobile(), resetDTO.getCode());

        assertSuccess(remoteUserService.resetPassword(user.getUserId(),
                AuthUtils.encryptPassword(resetDTO.getNewPassword())), "密码重置失败");
        StpUtil.logout();
    }

    @Override
    public String sendPasswordEmailCode() {
        LoginUserDTO user = currentUser();
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱");
        }
        verifyCodeService.sendEmailCode(VerifyCodeService.SCENE_RESET_PASSWORD_EMAIL, user.getEmail());
        return AuthUtils.maskEmail(user.getEmail());
    }

    @Override
    public void resetPasswordByEmail(PasswordEmailResetDTO resetDTO) {
        if (resetDTO == null) {
            throw new BusinessException("参数不能为空");
        }
        AuthUtils.validatePassword(resetDTO.getNewPassword(),
                resetDTO.getConfirmPassword(), authProperties.getPasswordMinLength());

        LoginUserDTO user = currentUser();
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱");
        }
        verifyCodeService.verifyEmailCode(VerifyCodeService.SCENE_RESET_PASSWORD_EMAIL,
                user.getEmail(), resetDTO.getCode());

        assertSuccess(remoteUserService.resetPassword(user.getUserId(),
                AuthUtils.encryptPassword(resetDTO.getNewPassword())), "密码重置失败");
        StpUtil.logout();
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的头像");
        }
        if (file.getSize() > AVATAR_MAX_SIZE) {
            throw new BusinessException("头像大小不能超过 5MB");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new BusinessException("头像仅支持 jpg / jpeg / png / gif / webp 格式");
        }

        R<SysFileDTO> result;
        try {
            result = remoteFileService.uploadFile(file, "avatar");
        } catch (Exception e) {
            log.error("[个人中心] 头像上传失败", e);
            throw new BusinessException("文件服务暂不可用，请稍后重试");
        }
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new BusinessException(result == null ? "头像上传失败" : result.getMsg());
        }

        String url = result.getData().getUrl();
        assertSuccess(remoteUserService.updateAvatar(currentUserId(), url), "头像更新失败");
        return url;
    }

    @Override
    public PageResult<LoginLogDTO> getMyLoginLogs(Integer pageNo, Integer pageSize) {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        String username = currentUser().getUsername();
        try {
            R<PageResult<LoginLogDTO>> result = remoteLoginLogService.pageByUsername(username, no, size);
            if (result == null || result.getData() == null) {
                return PageResult.empty();
            }
            return result.getData();
        } catch (Exception e) {
            log.error("[个人中心] 登录记录查询失败 username={}", username, e);
            return PageResult.empty();
        }
    }

    /**
     * 获取当前登录用户，未登录时抛出异常
     */
    private LoginUserDTO currentUser() {
        Long userId = currentUserId();
        R<com.wu.cloud.api.system.dto.SysUserDTO> basic;
        try {
            basic = remoteUserService.getUserById(userId);
        } catch (Exception e) {
            log.error("[个人中心] 查询用户失败 userId={}", userId, e);
            throw new BusinessException("用户服务暂不可用，请稍后重试");
        }
        if (basic == null || basic.getData() == null) {
            throw new BusinessException("用户不存在");
        }
        // 通过账号再查一次可获得密码与权限，用于密码校验等场景
        R<LoginUserDTO> full = remoteUserService.getUserInfoByUsername(basic.getData().getUsername());
        if (full == null || full.getData() == null) {
            throw new BusinessException("用户不存在");
        }
        return full.getData();
    }

    private Long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
    }

    private void assertSuccess(R<Boolean> result, String defaultMessage) {
        if (result == null || !result.isSuccess() || !Boolean.TRUE.equals(result.getData())) {
            throw new BusinessException(result == null || result.getMsg() == null
                    ? defaultMessage : result.getMsg());
        }
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
