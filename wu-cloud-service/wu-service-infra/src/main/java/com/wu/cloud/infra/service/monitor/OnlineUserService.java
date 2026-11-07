package com.wu.cloud.infra.service.monitor;

import com.wu.cloud.api.infra.dto.OnlineUserVO;
import com.wu.cloud.api.infra.dto.RecordLoginSessionDTO;

import java.util.List;

/**
 * 在线用户服务（与既有规范对齐 OnlineUserService）
 */
public interface OnlineUserService {

    void recordLoginSession(RecordLoginSessionDTO dto);

    void touchLastAccess(Long userId);

    int countOnlineUsers();

    List<OnlineUserVO> listOnlineUsers();

    void forceLogout(Long userId);
}
