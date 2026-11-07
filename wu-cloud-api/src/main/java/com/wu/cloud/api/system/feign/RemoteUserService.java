package com.wu.cloud.api.system.feign;

import com.wu.cloud.api.system.dto.AiUserContextDTO;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileUpdateDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.api.system.dto.UserRegisterDTO;
import com.wu.cloud.api.system.feign.fallback.RemoteUserServiceFallback;
import com.wu.cloud.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 远程用户服务 OpenFeign 接口契约
 *
 * <p>路径需与 wu-service-system 的 InnerUserController 保持一致。</p>
 */
@FeignClient(contextId = "remoteUserService", value = "wu-service-system", path = "/inner/system/user", fallbackFactory = RemoteUserServiceFallback.class)
public interface RemoteUserService {

    /**
     * 按账号查询登录用户（含密码、角色、权限）
     */
    @GetMapping("/get-by-username")
    R<LoginUserDTO> getUserInfoByUsername(@RequestParam("username") String username);

    /**
     * 按主键查询用户基础信息
     */
    @GetMapping("/get-by-id")
    R<SysUserDTO> getUserById(@RequestParam("id") Long id);

    /**
     * 个人中心完整资料（部门/角色/岗位/最近登录等）
     */
    @GetMapping("/profile")
    R<UserProfileDTO> getUserProfile(@RequestParam("userId") Long userId);

    /**
     * 按手机号查询登录用户
     */
    @GetMapping("/get-by-mobile")
    R<LoginUserDTO> getUserByMobile(@RequestParam("mobile") String mobile);

    /**
     * 按邮箱查询登录用户
     */
    @GetMapping("/get-by-email")
    R<LoginUserDTO> getUserByEmail(@RequestParam("email") String email);

    /**
     * 注册新用户，返回用户主键
     */
    @PostMapping("/register")
    R<Long> registerUser(@RequestBody UserRegisterDTO registerDTO);

    /**
     * 更新用户个人资料
     */
    @PostMapping("/update-profile")
    R<Boolean> updateProfile(@RequestBody UserProfileUpdateDTO updateDTO);

    /**
     * 重置密码为指定明文（内部完成加密）
     */
    @PostMapping("/reset-password")
    R<Boolean> resetPassword(@RequestParam("userId") Long userId,
                             @RequestParam("rawPassword") String rawPassword);

    /**
     * 绑定手机号
     */
    @PostMapping("/bind-mobile")
    R<Boolean> bindMobile(@RequestParam("userId") Long userId,
                          @RequestParam("mobile") String mobile);

    /**
     * 绑定邮箱
     */
    @PostMapping("/bind-email")
    R<Boolean> bindEmail(@RequestParam("userId") Long userId,
                         @RequestParam("email") String email);

    /**
     * 更新头像地址
     */
    @PostMapping("/update-avatar")
    R<Boolean> updateAvatar(@RequestParam("userId") Long userId,
                            @RequestParam("avatar") String avatar);

    /**
     * 获取指定用户的菜单列表（供 /auth/info 接口使用）
     */
    @GetMapping("/get-menus")
    R<List<SysMenuDTO>> getMenusByUserId(@RequestParam("userId") Long userId);

    /**
     * 启用状态用户列表（工单/审批候选人、聊天联系人等场景），可按需排除指定用户
     */
    @GetMapping("/list-active")
    R<List<SysUserDTO>> listActiveUsers(@RequestParam(value = "excludeUserId", required = false) Long excludeUserId);

    /**
     * 按主键集合批量查询用户基础信息
     */
    @PostMapping("/list-by-ids")
    R<List<SysUserDTO>> listUsersByIds(@RequestBody List<Long> ids);

    /**
     * AI 用户上下文聚合：用户档案 + 部门 + 角色 + 可见菜单，一次调用返回
     */
    @GetMapping("/ai-context")
    R<AiUserContextDTO> getAiContext(@RequestParam("userId") Long userId);
}
