package com.wu.cloud.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.system.dto.SysUserPageDTO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.vo.*;

import java.util.List;

/**
 * 用户服务接口（对外契约与既有规范对齐 + wu-admin-web）
 */
public interface SysUserService {

    /** 下拉列表（上限 2000） */
    List<SysUserDO> listAll();

    PageResult<SysUserDO> pageUsers(SysUserPageDTO pageDTO);

    /** 兼容旧调用 */
    Page<SysUserDO> getUserPage(PageQuery pageQuery, String username, String mobile, Integer status, Long deptId);

    SysUserDO getUserById(Long id);

    SysUserDO getSysUserById(Long id);

    SysUserDO getDetail(Long id);

    UserProfileDTO getUserProfile(Long userId);

    SysUserDTO getUserInfoByUsername(String username);

    LoginUserDTO getLoginUserByUsername(String username);

    LoginUserDTO getLoginUserByMobile(String mobile);

    LoginUserDTO getLoginUserByEmail(String email);

    Long registerUser(String username, String encodedPassword, String nickname,
                      String mobile, String email);

    Boolean updateProfile(Long userId, String nickname, Integer sex, String remark);

    Boolean resetPasswordDirectly(Long userId, String encodedPassword);

    Boolean bindMobile(Long userId, String mobile);

    Boolean bindEmail(Long userId, String email);

    Boolean updateAvatar(Long userId, String avatar);

    Long createUser(UserCreateReqVO createReqVO);

    Boolean updateUser(UserUpdateReqVO updateReqVO);

    void updateUser(SysUserDO userDO);

    Boolean deleteUser(Long id);

    void batchDeleteUser(List<Long> ids);

    void updateUserStatus(Long id, Integer status);

    void assignRoles(SysUserRoleReqVO reqVO);

    void assignUserRoles(SysUserRoleReqVO reqVO);

    List<Long> getUserRoleIds(Long userId);

    void resetPassword(UserResetPwdReqVO reqVO);

    void resetUserPassword(UserResetPwdReqVO reqVO);

    void resetPassword(Long id, String rawPassword);

    void unlockLogin(Long id);

    void kickOut(Long userId);

    void auditUser(Long id, String action);

    PageResult<SysUserDO> recyclePage(PageQuery pageQuery, String username, String mobile,
                                      Integer status, Long deptId);

    void restore(Long id);

    void deletePermanent(Long id);
}
