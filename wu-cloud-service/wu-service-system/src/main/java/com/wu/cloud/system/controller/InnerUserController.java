package com.wu.cloud.system.controller;

import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.api.system.dto.UserProfileUpdateDTO;
import com.wu.cloud.api.system.dto.UserRegisterDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.convert.SysUserConvert;
import com.wu.cloud.system.entity.SysMenuDO;
import com.wu.cloud.system.service.MenuService;
import com.wu.cloud.system.service.SysUserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户内部 RPC 端点（仅供其他微服务 OpenFeign 调用，由网关拦截外部访问）
 *
 * <p>路径需与 wu-cloud-api 中 RemoteUserService 的契约保持一致。</p>
 */
@RestController
@RequestMapping("/inner/system/user")
public class InnerUserController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private MenuService menuService;

    @GetMapping("/get-by-username")
    public R<LoginUserDTO> getUserInfoByUsername(@RequestParam("username") String username) {
        return R.ok(sysUserService.getLoginUserByUsername(username));
    }

    @GetMapping("/get-by-id")
    public R<SysUserDTO> getUserById(@RequestParam("id") Long id) {
        return R.ok(SysUserConvert.convertToDTO(sysUserService.getSysUserById(id)));
    }

    @GetMapping("/profile")
    public R<UserProfileDTO> getUserProfile(@RequestParam("userId") Long userId) {
        return R.ok(sysUserService.getUserProfile(userId));
    }

    @GetMapping("/get-by-mobile")
    public R<LoginUserDTO> getUserByMobile(@RequestParam("mobile") String mobile) {
        return R.ok(sysUserService.getLoginUserByMobile(mobile));
    }

    @GetMapping("/get-by-email")
    public R<LoginUserDTO> getUserByEmail(@RequestParam("email") String email) {
        return R.ok(sysUserService.getLoginUserByEmail(email));
    }

    @PostMapping("/register")
    public R<Long> registerUser(@RequestBody UserRegisterDTO registerDTO) {
        return R.ok(sysUserService.registerUser(
                registerDTO.getUsername(),
                registerDTO.getPassword(),
                registerDTO.getNickname(),
                registerDTO.getMobile(),
                registerDTO.getEmail()));
    }

    @PostMapping("/update-profile")
    public R<Boolean> updateProfile(@RequestBody UserProfileUpdateDTO updateDTO) {
        return R.ok(sysUserService.updateProfile(
                updateDTO.getUserId(),
                updateDTO.getNickname(),
                updateDTO.getSex(),
                updateDTO.getRemark()));
    }

    @PostMapping("/reset-password")
    public R<Boolean> resetPassword(@RequestParam("userId") Long userId,
                                    @RequestParam("rawPassword") String rawPassword) {
        return R.ok(sysUserService.resetPasswordDirectly(userId, rawPassword));
    }

    @PostMapping("/bind-mobile")
    public R<Boolean> bindMobile(@RequestParam("userId") Long userId,
                                 @RequestParam("mobile") String mobile) {
        return R.ok(sysUserService.bindMobile(userId, mobile));
    }

    @PostMapping("/bind-email")
    public R<Boolean> bindEmail(@RequestParam("userId") Long userId,
                                @RequestParam("email") String email) {
        return R.ok(sysUserService.bindEmail(userId, email));
    }

    @PostMapping("/update-avatar")
    public R<Boolean> updateAvatar(@RequestParam("userId") Long userId,
                                   @RequestParam("avatar") String avatar) {
        return R.ok(sysUserService.updateAvatar(userId, avatar));
    }

    /**
     * 获取用户可见菜单列表（供认证中心 /auth/info 接口使用）
     * 返回全量可见菜单，前端根据角色/权限动态筛选路由，与单体行为一致。
     */
    @GetMapping("/get-menus")
    public R<List<SysMenuDTO>> getMenusByUserId(@RequestParam("userId") Long userId) {
        List<SysMenuDO> allMenus = menuService.getMenuList();
        List<SysMenuDTO> dtoList = allMenus.stream()
                .map(m -> {
                    SysMenuDTO dto = new SysMenuDTO();
                    dto.setId(m.getId());
                    dto.setName(m.getName());
                    dto.setParentId(m.getParentId());
                    dto.setSort(m.getSort());
                    dto.setPath(m.getPath());
                    dto.setComponent(m.getComponent());
                    dto.setType(m.getType());
                    dto.setPermission(m.getPermission());
                    dto.setIcon(m.getIcon());
                    dto.setStatus(m.getStatus());
                    // 单体库无 visible 列，启用菜单默认可见
                    dto.setVisible(m.getVisible() != null ? m.getVisible() : (m.getStatus() != null && m.getStatus() == 1 ? 1 : 0));
                    return dto;
                })
                .collect(Collectors.toList());
        return R.ok(dtoList);

    }
}
