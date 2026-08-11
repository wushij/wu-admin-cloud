package com.wu.cloud.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wu.cloud.api.system.dto.LoginUserDTO;
import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.api.system.dto.UserProfileDTO;
import com.wu.cloud.common.core.domain.PageQuery;
import com.wu.cloud.common.core.domain.PageResult;
import com.wu.cloud.common.exception.BusinessException;
import com.wu.cloud.system.convert.SysUserConvert;
import com.wu.cloud.system.dto.SysUserPageDTO;
import com.wu.cloud.system.entity.SysDeptDO;
import com.wu.cloud.system.entity.SysLoginLogDO;
import com.wu.cloud.system.entity.SysPostDO;
import com.wu.cloud.system.entity.SysRoleDO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.entity.SysUserPostDO;
import com.wu.cloud.system.entity.SysUserRoleDO;
import com.wu.cloud.system.mapper.DeptMapper;
import com.wu.cloud.system.mapper.PostMapper;
import com.wu.cloud.system.mapper.RoleMapper;
import com.wu.cloud.system.mapper.SysLoginLogMapper;
import com.wu.cloud.system.mapper.SysUserMapper;
import com.wu.cloud.system.mapper.SysUserPostMapper;
import com.wu.cloud.system.mapper.SysUserRoleMapper;
import com.wu.cloud.system.service.DeptService;
import com.wu.cloud.system.service.SysConfigGroupService;
import com.wu.cloud.system.service.SysRecycleService;
import com.wu.cloud.system.service.SysUserService;
import com.wu.cloud.system.support.LoginLockSupport;
import com.wu.cloud.system.vo.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final int LIST_ALL_MAX = 2000;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_PENDING_AUDIT = 2;
    private static final int STATUS_AUDIT_REJECTED = 3;
    private static final String CONFIG_GROUP_REGISTER = "register";

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private SysUserRoleMapper sysUserRoleMapper;
    @Resource
    private SysUserPostMapper sysUserPostMapper;
    @Resource
    private SysLoginLogMapper sysLoginLogMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private DeptService deptService;
    @Resource
    private SysConfigGroupService sysConfigGroupService;
    @Resource
    private SysRecycleService sysRecycleService;
    @Resource
    private LoginLockSupport loginLockSupport;

    @Override
    public List<SysUserDO> listAll() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUserDO>()
                .select(SysUserDO::getId, SysUserDO::getUsername, SysUserDO::getNickname,
                        SysUserDO::getMobile, SysUserDO::getStatus, SysUserDO::getDeptId)
                .orderByAsc(SysUserDO::getId)
                .last("LIMIT " + LIST_ALL_MAX));
    }

    @Override
    public PageResult<SysUserDO> pageUsers(SysUserPageDTO pageDTO) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(pageDTO.getUsername())) {
            wrapper.like(SysUserDO::getUsername, pageDTO.getUsername().trim());
        }
        if (StringUtils.hasText(pageDTO.getNickname())) {
            wrapper.like(SysUserDO::getNickname, pageDTO.getNickname().trim());
        }
        if (StringUtils.hasText(pageDTO.getMobile())) {
            wrapper.like(SysUserDO::getMobile, pageDTO.getMobile().trim());
        }
        if (Boolean.TRUE.equals(pageDTO.getLoginLocked())) {
            Set<String> locked = loginLockSupport.listLockedUsernames();
            if (locked.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(SysUserDO::getUsername, locked);
        }
        if (pageDTO.getStatus() != null) {
            wrapper.eq(SysUserDO::getStatus, pageDTO.getStatus());
        } else {
            // 管理列表默认隐藏待审核/驳回，与单体一致
            wrapper.in(SysUserDO::getStatus, STATUS_DISABLED, STATUS_ENABLED);
        }
        if (pageDTO.getDeptId() != null) {
            List<Long> deptIds = deptService.listSelfAndDescendantIds(pageDTO.getDeptId());
            if (deptIds.isEmpty()) {
                wrapper.eq(SysUserDO::getDeptId, -1L);
            } else {
                wrapper.in(SysUserDO::getDeptId, deptIds);
            }
        }
        if (pageDTO.getPostId() != null) {
            List<Long> userIds = sysUserPostMapper.selectUserIdsByPostId(pageDTO.getPostId());
            if (userIds == null || userIds.isEmpty()) {
                wrapper.eq(SysUserDO::getId, -1L);
            } else {
                wrapper.in(SysUserDO::getId, userIds);
            }
        }
        wrapper.orderByDesc(SysUserDO::getCreateTime);

        int pageNo = pageDTO.getPageNo() != null ? pageDTO.getPageNo() : 1;
        int pageSize = pageDTO.getPageSize() != null ? pageDTO.getPageSize() : 10;
        Page<SysUserDO> page = sysUserMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<SysUserDO> users = page.getRecords();
        fillUserDisplayFields(users);
        fillLoginLockFields(users);
        return PageResult.of(users, page.getTotal());
    }

    @Override
    public Page<SysUserDO> getUserPage(PageQuery pageQuery, String username, String mobile,
                                       Integer status, Long deptId) {
        SysUserPageDTO dto = new SysUserPageDTO();
        dto.setPageNo(pageQuery.getPageNo());
        dto.setPageSize(pageQuery.getPageSize());
        dto.setUsername(username);
        dto.setMobile(mobile);
        dto.setStatus(status);
        dto.setDeptId(deptId);
        PageResult<SysUserDO> result = pageUsers(dto);
        Page<SysUserDO> page = new Page<>(dto.getPageNo(), dto.getPageSize(), result.getTotal());
        page.setRecords(result.getList());
        return page;
    }

    @Override
    public SysUserDO getUserById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    public SysUserDO getSysUserById(Long id) {
        return getUserById(id);
    }

    @Override
    public SysUserDO getDetail(Long id) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user != null) {
            fillUserDisplayFields(Collections.singletonList(user));
            fillLoginLockFields(Collections.singletonList(user));
        }
        return user;
    }

    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        SysUserDO user = getDetail(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserProfileDTO profile = new UserProfileDTO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setMobile(user.getMobile());
        profile.setEmail(user.getEmail());
        profile.setAvatar(user.getAvatar());
        profile.setStatus(user.getStatus());
        profile.setSex(user.getSex());
        profile.setDeptId(user.getDeptId());
        profile.setDeptName(user.getDeptName());
        profile.setRemark(user.getRemark());
        profile.setCreateTime(user.getCreateTime());
        profile.setUpdateTime(user.getUpdateTime());
        profile.setMinPasswordLength(resolveMinPasswordLength());

        List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(userId);
        profile.setRoleIds(roleIds == null ? Collections.emptyList() : roleIds);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysRoleDO> roles = roleMapper.selectByIds(roleIds);
            if (roles != null && !roles.isEmpty()) {
                profile.setRoleNames(roles.stream().map(SysRoleDO::getName).toList());
                profile.setRoleCodes(roles.stream().map(SysRoleDO::getCode).toList());
            } else {
                profile.setRoleNames(Collections.emptyList());
                profile.setRoleCodes(Collections.emptyList());
            }
        } else {
            profile.setRoleNames(Collections.emptyList());
            profile.setRoleCodes(Collections.emptyList());
        }

        List<SysUserPostDO> postLinks = sysUserPostMapper.selectByUserIds(Collections.singleton(userId));
        List<Long> postIds = postLinks == null ? Collections.emptyList()
                : postLinks.stream().map(SysUserPostDO::getPostId).toList();
        profile.setPostIds(postIds);
        if (!postIds.isEmpty()) {
            List<SysPostDO> posts = postMapper.selectByIds(postIds);
            if (posts != null && !posts.isEmpty()) {
                profile.setPostNames(posts.stream().map(SysPostDO::getPostName).toList());
            } else {
                profile.setPostNames(Collections.emptyList());
            }
        } else {
            profile.setPostNames(Collections.emptyList());
        }

        SysLoginLogDO lastLogin = sysLoginLogMapper.selectOne(new LambdaQueryWrapper<SysLoginLogDO>()
                .and(w -> w.eq(SysLoginLogDO::getUserId, userId)
                        .or()
                        .eq(SysLoginLogDO::getUsername, user.getUsername()))
                .eq(SysLoginLogDO::getStatus, 0)
                .orderByDesc(SysLoginLogDO::getLoginTime)
                .last("LIMIT 1"));
        if (lastLogin != null) {
            profile.setLastLoginTime(lastLogin.getLoginTime());
            profile.setLastLoginIp(lastLogin.getIp());
            profile.setLastLoginLocation(lastLogin.getAddress());
        }
        return profile;
    }

    private int resolveMinPasswordLength() {
        try {
            Map<String, Object> config = sysConfigGroupService.getConfigMap(CONFIG_GROUP_REGISTER);
            Object minLen = config.get("minPasswordLength");
            if (minLen instanceof Number number) {
                return number.intValue();
            }
            if (minLen != null) {
                return Integer.parseInt(minLen.toString());
            }
        } catch (Exception ignored) {
            // 配置缺失时使用默认值
        }
        return 6;
    }

    @Override
    public SysUserDTO getUserInfoByUsername(String username) {
        SysUserDO userDO = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username));
        return SysUserConvert.convertToDTO(userDO);
    }

    @Override
    public LoginUserDTO getLoginUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        SysUserDO userDO = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username));
        return buildLoginUser(userDO);
    }

    @Override
    public LoginUserDTO getLoginUserByMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        SysUserDO userDO = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getMobile, mobile));
        return buildLoginUser(userDO);
    }

    @Override
    public LoginUserDTO getLoginUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        SysUserDO userDO = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getEmail, email));
        return buildLoginUser(userDO);
    }

    private LoginUserDTO buildLoginUser(SysUserDO userDO) {
        if (userDO == null) {
            return null;
        }
        LoginUserDTO dto = new LoginUserDTO();
        dto.setUserId(userDO.getId());
        dto.setUsername(userDO.getUsername());
        dto.setNickname(userDO.getNickname());
        dto.setPassword(userDO.getPassword());
        dto.setStatus(userDO.getStatus());
        dto.setMobile(userDO.getMobile());
        dto.setEmail(userDO.getEmail());
        dto.setAvatar(userDO.getAvatar());
        dto.setSex(userDO.getSex());
        dto.setDeptId(userDO.getDeptId());
        dto.setRemark(userDO.getRemark());
        dto.setRoles(new HashSet<>(sysUserRoleMapper.selectRoleCodesByUserId(userDO.getId())));
        dto.setPermissions(new HashSet<>(sysUserRoleMapper.selectPermissionsByUserId(userDO.getId())));
        return dto;
    }

    @Override
    public Long registerUser(String username, String encodedPassword, String nickname,
                             String mobile, String email) {
        Long existing = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username));
        if (existing != null && existing > 0) {
            throw new BusinessException("账号已存在");
        }
        if (StringUtils.hasText(mobile)) {
            Long mobileCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                    .eq(SysUserDO::getMobile, mobile));
            if (mobileCount != null && mobileCount > 0) {
                throw new BusinessException("手机号已被注册");
            }
        }
        if (StringUtils.hasText(email)) {
            Long emailCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                    .eq(SysUserDO::getEmail, email));
            if (emailCount != null && emailCount > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }
        SysUserDO userDO = new SysUserDO();
        userDO.setUsername(username);
        userDO.setPassword(encodedPassword);
        userDO.setNickname(!StringUtils.hasText(nickname) ? username : nickname);
        userDO.setMobile(mobile);
        userDO.setEmail(email);
        userDO.setSex(0);
        userDO.setStatus(isRegisterAuditEnabled() ? STATUS_PENDING_AUDIT : STATUS_ENABLED);
        sysUserMapper.insert(userDO);
        return userDO.getId();
    }

    @Override
    public void auditUser(Long id, String action) {
        if (id == null || !StringUtils.hasText(action)) {
            throw new BusinessException("审核参数不完整");
        }
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != STATUS_PENDING_AUDIT) {
            throw new BusinessException("该用户不处于待审核状态，无法审核");
        }
        if ("APPROVE".equalsIgnoreCase(action)) {
            user.setStatus(STATUS_ENABLED);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            user.setStatus(STATUS_AUDIT_REJECTED);
        } else {
            throw new BusinessException("审核动作不合法，仅支持 APPROVE/REJECT");
        }
        sysUserMapper.updateById(user);
    }

    private boolean isRegisterAuditEnabled() {
        try {
            Map<String, Object> config = sysConfigGroupService.getConfigMap(CONFIG_GROUP_REGISTER);
            Object needAudit = config.get("needAudit");
            return needAudit instanceof Boolean && (Boolean) needAudit;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Boolean updateProfile(Long userId, String nickname, Integer sex, String remark) {
        SysUserDO userDO = requireUser(userId);
        if (StringUtils.hasText(nickname)) {
            userDO.setNickname(nickname);
        }
        if (sex != null) {
            userDO.setSex(sex);
        }
        if (remark != null) {
            userDO.setRemark(remark);
        }
        sysUserMapper.updateById(userDO);
        return true;
    }

    @Override
    public Boolean resetPasswordDirectly(Long userId, String encodedPassword) {
        SysUserDO userDO = requireUser(userId);
        userDO.setPassword(encodedPassword);
        sysUserMapper.updateById(userDO);
        return true;
    }

    @Override
    public Boolean bindMobile(Long userId, String mobile) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getMobile, mobile)
                .ne(SysUserDO::getId, userId));
        if (count != null && count > 0) {
            throw new BusinessException("该手机号已被其他账号绑定");
        }
        SysUserDO userDO = requireUser(userId);
        userDO.setMobile(mobile);
        sysUserMapper.updateById(userDO);
        return true;
    }

    @Override
    public Boolean bindEmail(Long userId, String email) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getEmail, email)
                .ne(SysUserDO::getId, userId));
        if (count != null && count > 0) {
            throw new BusinessException("该邮箱已被其他账号绑定");
        }
        SysUserDO userDO = requireUser(userId);
        userDO.setEmail(email);
        sysUserMapper.updateById(userDO);
        return true;
    }

    @Override
    public Boolean updateAvatar(Long userId, String avatar) {
        SysUserDO userDO = requireUser(userId);
        userDO.setAvatar(avatar);
        sysUserMapper.updateById(userDO);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateReqVO reqVO) {
        validateUsername(reqVO.getUsername());
        if (!StringUtils.hasText(reqVO.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (!StringUtils.hasText(reqVO.getNickname())) {
            throw new BusinessException("昵称不能为空");
        }
        Long existing = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, reqVO.getUsername()));
        if (existing != null && existing > 0) {
            throw new BusinessException("账号已存在");
        }
        SysUserDO userDO = new SysUserDO();
        userDO.setUsername(reqVO.getUsername().trim());
        userDO.setNickname(reqVO.getNickname().trim());
        userDO.setPassword(BCrypt.hashpw(reqVO.getPassword(), BCrypt.gensalt()));
        userDO.setEmail(reqVO.getEmail());
        userDO.setMobile(reqVO.getMobile());
        userDO.setSex(reqVO.getSex());
        userDO.setAvatar(reqVO.getAvatar());
        userDO.setStatus(reqVO.getStatus() != null ? reqVO.getStatus() : STATUS_ENABLED);
        userDO.setDeptId(reqVO.getDeptId());
        userDO.setRemark(reqVO.getRemark());
        sysUserMapper.insert(userDO);
        if (reqVO.getRoleId() != null) {
            SysUserRoleReqVO roleReq = new SysUserRoleReqVO();
            roleReq.setUserId(userDO.getId());
            roleReq.setRoleIds(Collections.singletonList(reqVO.getRoleId()));
            assignUserRoles(roleReq);
        }
        saveUserPosts(userDO.getId(), reqVO.getPostIds());
        return userDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UserUpdateReqVO reqVO) {
        SysUserDO userDO = requireUser(reqVO.getId());
        userDO.setNickname(reqVO.getNickname());
        userDO.setEmail(reqVO.getEmail());
        userDO.setMobile(reqVO.getMobile());
        userDO.setSex(reqVO.getSex());
        userDO.setAvatar(reqVO.getAvatar());
        if (reqVO.getStatus() != null) {
            userDO.setStatus(reqVO.getStatus());
        }
        userDO.setDeptId(reqVO.getDeptId());
        userDO.setRemark(reqVO.getRemark());
        sysUserMapper.updateById(userDO);
        if (reqVO.getRoleId() != null) {
            SysUserRoleReqVO roleReq = new SysUserRoleReqVO();
            roleReq.setUserId(userDO.getId());
            roleReq.setRoleIds(Collections.singletonList(reqVO.getRoleId()));
            assignUserRoles(roleReq);
        }
        if (reqVO.getPostIds() != null) {
            saveUserPosts(userDO.getId(), reqVO.getPostIds());
        }
        return true;
    }

    @Override
    public void updateUser(SysUserDO userDO) {
        sysUserMapper.updateById(userDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        requireUser(id);
        sysUserPostMapper.deleteByUserId(id);
        sysUserMapper.deleteById(id);
        return true;
    }

    @Override
    public void batchDeleteUser(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            deleteUser(id);
        }
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        SysUserDO userDO = requireUser(id);
        userDO.setStatus(status);
        sysUserMapper.updateById(userDO);
    }

    @Override
    public void assignRoles(SysUserRoleReqVO reqVO) {
        assignUserRoles(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserRoles(SysUserRoleReqVO reqVO) {
        if (reqVO.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        sysUserRoleMapper.deleteByUserId(reqVO.getUserId());
        List<Long> roleIds = reqVO.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<SysUserRoleDO> records = new ArrayList<>(roleIds.size());
        for (Long roleId : roleIds) {
            SysUserRoleDO ur = new SysUserRoleDO();
            ur.setUserId(reqVO.getUserId());
            ur.setRoleId(roleId);
            records.add(ur);
        }
        sysUserRoleMapper.insertBatch(records);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public void resetPassword(UserResetPwdReqVO reqVO) {
        resetUserPassword(reqVO);
    }

    @Override
    public void resetUserPassword(UserResetPwdReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String raw = reqVO.getNewPassword();
        if (userId == null || !StringUtils.hasText(raw)) {
            throw new BusinessException("重置密码参数不完整");
        }
        resetPassword(userId, raw);
    }

    @Override
    public void resetPassword(Long id, String rawPassword) {
        SysUserDO userDO = requireUser(id);
        userDO.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
        sysUserMapper.updateById(userDO);
    }

    @Override
    public void unlockLogin(Long id) {
        SysUserDO user = requireUser(id);
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException("用户名为空，无法解除锁定");
        }
        loginLockSupport.unlockUser(user.getUsername());
    }

    @Override
    public void kickOut(Long userId) {
        requireUser(userId);
        try {
            StpUtil.kickout(userId);
        } catch (Exception ignored) {
            // 用户可能本无在线
        }
        try {
            StpUtil.logout(userId);
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Override
    public PageResult<SysUserDO> recyclePage(PageQuery pageQuery, String username, String mobile,
                                             Integer status, Long deptId) {
        Page<SysUserDO> page = sysRecycleService.getUserRecyclePage(pageQuery, username, mobile, status, deptId);
        List<SysUserDO> users = page.getRecords();
        fillUserDisplayFields(users);
        return PageResult.of(users, page.getTotal());
    }

    @Override
    public void restore(Long id) {
        if (!sysRecycleService.restoreUser(id)) {
            throw new BusinessException(404, "回收站用户不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        sysUserRoleMapper.deleteByUserId(id);
        sysUserPostMapper.deleteByUserId(id);
        if (!sysRecycleService.purgeUser(id)) {
            throw new BusinessException(404, "回收站用户不存在");
        }
    }

    private SysUserDO requireUser(Long id) {
        SysUserDO user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username) || !username.matches("^[a-zA-Z0-9_]{4,12}$")) {
            throw new BusinessException("用户名须为 4-12 位字母、数字或下划线");
        }
    }

    private void saveUserPosts(Long userId, List<Long> postIds) {
        sysUserPostMapper.deleteByUserId(userId);
        if (postIds == null || postIds.isEmpty()) {
            return;
        }
        List<SysUserPostDO> list = new ArrayList<>(postIds.size());
        for (Long postId : postIds) {
            if (postId == null) {
                continue;
            }
            SysUserPostDO up = new SysUserPostDO();
            up.setUserId(userId);
            up.setPostId(postId);
            list.add(up);
        }
        if (!list.isEmpty()) {
            sysUserPostMapper.insertBatch(list);
        }
    }

    private void fillUserDisplayFields(List<SysUserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream()
                .map(SysUserDO::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = Collections.emptyMap();
        if (!deptIds.isEmpty()) {
            List<SysDeptDO> depts = deptMapper.selectByIds(deptIds);
            if (depts != null) {
                deptNameMap = depts.stream()
                        .collect(Collectors.toMap(SysDeptDO::getId, SysDeptDO::getName, (a, b) -> a));
            }
        }
        Set<Long> userIds = users.stream().map(SysUserDO::getId).collect(Collectors.toSet());
        List<SysUserPostDO> links = sysUserPostMapper.selectByUserIds(userIds);
        Map<Long, List<Long>> userPostMap = links.stream()
                .collect(Collectors.groupingBy(SysUserPostDO::getUserId,
                        Collectors.mapping(SysUserPostDO::getPostId, Collectors.toList())));
        Set<Long> postIds = links.stream().map(SysUserPostDO::getPostId).collect(Collectors.toSet());
        Map<Long, String> postNameMap = Collections.emptyMap();
        if (!postIds.isEmpty()) {
            List<SysPostDO> posts = postMapper.selectByIds(postIds);
            if (posts != null) {
                postNameMap = posts.stream()
                        .collect(Collectors.toMap(SysPostDO::getId, SysPostDO::getPostName, (a, b) -> a));
            }
        }
        Map<Long, String> finalDeptNameMap = deptNameMap;
        Map<Long, String> finalPostNameMap = postNameMap;
        for (SysUserDO user : users) {
            if (user.getDeptId() != null) {
                user.setDeptName(finalDeptNameMap.get(user.getDeptId()));
            }
            List<Long> roleIds = sysUserRoleMapper.selectRoleIdsByUserId(user.getId());
            user.setRoleIds(roleIds == null ? Collections.emptySet() : new HashSet<>(roleIds));
            List<Long> pids = userPostMap.getOrDefault(user.getId(), Collections.emptyList());
            user.setPostIds(pids);
            if (!pids.isEmpty()) {
                user.setPostNames(pids.stream()
                        .map(finalPostNameMap::get)
                        .filter(n -> n != null)
                        .collect(Collectors.joining("、")));
            }
        }
    }

    private void fillLoginLockFields(List<SysUserDO> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        List<String> usernames = users.stream()
                .map(SysUserDO::getUsername)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        Map<String, LoginLockSupport.LockStatus> statusMap = loginLockSupport.getUserLockStatusMap(usernames);
        for (SysUserDO user : users) {
            if (!StringUtils.hasText(user.getUsername())) {
                continue;
            }
            LoginLockSupport.LockStatus status = statusMap.get(user.getUsername().trim());
            if (status == null) {
                user.setLoginLocked(false);
                user.setLoginLockRemainSeconds(0L);
                user.setLoginFailCount(0);
            } else {
                user.setLoginLocked(status.isLocked());
                user.setLoginLockRemainSeconds(status.getRemainSeconds());
                user.setLoginFailCount(status.getFailCount());
            }
            user.setLoginIpLocked(false);
            user.setLoginIpLockRemainSeconds(0L);
            user.setLoginIpFailCount(0);
        }
    }
}
