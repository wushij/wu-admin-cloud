package com.wu.cloud.system.convert;

import com.wu.cloud.api.system.dto.SysUserDTO;
import com.wu.cloud.system.entity.SysUserDO;
import com.wu.cloud.system.vo.UserRespVO;

/**
 * 系统用户 对象转换类
 */
public class SysUserConvert {

    public static SysUserDTO convertToDTO(SysUserDO userDO) {
        if (userDO == null) {
            return null;
        }
        SysUserDTO dto = new SysUserDTO();
        dto.setUserId(userDO.getId());
        dto.setUsername(userDO.getUsername());
        dto.setRealName(userDO.getNickname());
        dto.setEmail(userDO.getEmail());
        dto.setPhone(userDO.getMobile());
        dto.setAvatar(userDO.getAvatar());
        dto.setStatus(String.valueOf(userDO.getStatus()));
        dto.setDeptId(userDO.getDeptId());
        dto.setDeptName(userDO.getDeptName());
        return dto;
    }

    public static UserRespVO convertToVO(SysUserDO userDO) {
        if (userDO == null) {
            return null;
        }
        UserRespVO vo = new UserRespVO();
        vo.setId(userDO.getId());
        vo.setUsername(userDO.getUsername());
        vo.setNickname(userDO.getNickname());
        vo.setMobile(userDO.getMobile());
        vo.setEmail(userDO.getEmail());
        vo.setAvatar(userDO.getAvatar());
        vo.setStatus(userDO.getStatus());
        vo.setDeptId(userDO.getDeptId());
        vo.setCreateTime(userDO.getCreateTime());
        return vo;
    }
}
