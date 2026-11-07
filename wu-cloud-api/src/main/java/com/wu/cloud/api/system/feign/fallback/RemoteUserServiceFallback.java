package com.wu.cloud.api.system.feign.fallback;

import com.wu.cloud.api.system.dto.AiUserContextDTO;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysMenuDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileUpdateDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.api.system.dto.UserRegisterDTO;
import com.wu.cloud.api.system.feign.RemoteUserService;
import com.wu.cloud.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 远程用户服务 Feign 降级工厂
 *
 * <p>所有方法返回明确的失败标识，禁止返回 null，避免把"服务不可用"伪装成"数据不存在"。</p>
 */
@Slf4j
@Component
public class RemoteUserServiceFallback implements FallbackFactory<RemoteUserService> {

    @Override
    public RemoteUserService create(Throwable cause) {
        log.error("[Feign降级] wu-service-system 用户服务调用失败", cause);
        return new RemoteUserService() {
            @Override
            public R<LoginUserDTO> getUserInfoByUsername(String username) {
                return R.fail("用户服务暂不可用");
            }

            @Override
            public R<SysUserDTO> getUserById(Long id) {
                return R.fail("用户服务暂不可用");
            }

            @Override
            public R<UserProfileDTO> getUserProfile(Long userId) {
                return R.fail("用户服务暂不可用");
            }

            @Override
            public R<LoginUserDTO> getUserByMobile(String mobile) {
                return R.fail("用户服务暂不可用");
            }

            @Override
            public R<LoginUserDTO> getUserByEmail(String email) {
                return R.fail("用户服务暂不可用");
            }

            @Override
            public R<Long> registerUser(UserRegisterDTO registerDTO) {
                return R.fail("用户服务暂不可用，注册失败");
            }

            @Override
            public R<Boolean> updateProfile(UserProfileUpdateDTO updateDTO) {
                return R.fail("用户服务暂不可用，资料更新失败");
            }

            @Override
            public R<Boolean> resetPassword(Long userId, String rawPassword) {
                return R.fail("用户服务暂不可用，密码重置失败");
            }

            @Override
            public R<Boolean> bindMobile(Long userId, String mobile) {
                return R.fail("用户服务暂不可用，手机号绑定失败");
            }

            @Override
            public R<Boolean> bindEmail(Long userId, String email) {
                return R.fail("用户服务暂不可用，邮箱绑定失败");
            }

            @Override
            public R<Boolean> updateAvatar(Long userId, String avatar) {
                return R.fail("用户服务暂不可用，头像更新失败");
            }

            @Override
            public R<List<SysMenuDTO>> getMenusByUserId(Long userId) {
                return R.fail("用户服务暂不可用，菜单获取失败");
            }

            @Override
            public R<List<SysUserDTO>> listActiveUsers(Long excludeUserId) {
                return R.fail("用户服务暂不可用，用户列表获取失败");
            }

            @Override
            public R<List<SysUserDTO>> listUsersByIds(List<Long> ids) {
                return R.fail("用户服务暂不可用，批量用户查询失败");
            }

            @Override
            public R<AiUserContextDTO> getAiContext(Long userId) {
                return R.fail("用户服务暂不可用，AI 用户上下文获取失败");
            }
        };
    }
}
