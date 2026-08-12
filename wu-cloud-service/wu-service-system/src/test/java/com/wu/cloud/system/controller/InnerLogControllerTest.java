package com.wu.cloud.system.controller;

import com.wu.cloud.api.system.dto.OperLogDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.system.entity.SysOperLogDO;
import com.wu.cloud.system.mapper.OperLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InnerLogController 操作日志写入")
class InnerLogControllerTest {

    @Mock
    private OperLogMapper operLogMapper;

    @InjectMocks
    private InnerLogController controller;

    @Test
    @DisplayName("saveOperLog：映射 DTO 并写入 sys_oper_log")
    void saveOperLog_insertsSysOperLog() {
        when(operLogMapper.insert(any(SysOperLogDO.class))).thenReturn(1);

        OperLogDTO dto = new OperLogDTO();
        dto.setTitle("系统配置分组");
        dto.setBusinessType(2);
        dto.setMethod("com.wu.cloud.system.controller.SysConfigGroupController.update");
        dto.setRequestMethod("PUT");
        dto.setOperName("admin");
        dto.setOperUrl("/api/system/config-group/site");
        dto.setOperIp("127.0.0.1");
        dto.setStatus(0);
        dto.setCostTime(12L);
        dto.setOperTime(LocalDateTime.of(2026, 8, 12, 10, 0));

        R<Boolean> result = controller.saveOperLog(dto);

        assertTrue(result.isSuccess());
        assertEquals(Boolean.TRUE, result.getData());

        ArgumentCaptor<SysOperLogDO> captor = ArgumentCaptor.forClass(SysOperLogDO.class);
        verify(operLogMapper).insert(captor.capture());
        SysOperLogDO saved = captor.getValue();
        assertEquals("系统配置分组", saved.getTitle());
        assertEquals(2, saved.getBusinessType());
        assertEquals("admin", saved.getOperName());
        assertEquals(0, saved.getStatus());
        assertEquals(12L, saved.getCostTime());
    }
}
