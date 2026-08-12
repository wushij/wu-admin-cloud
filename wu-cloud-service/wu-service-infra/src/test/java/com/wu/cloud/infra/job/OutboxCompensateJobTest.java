package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wu.cloud.infra.entity.OutboxDO;
import com.wu.cloud.infra.mapper.OutboxMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OutboxCompensateJob 单元测试")
class OutboxCompensateJobTest {

    @Mock
    private OutboxMapper outboxMapper;
    @Mock
    private DistributedJobLock jobLock;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OutboxCompensateJob job;

    @BeforeAll
    static void initMp() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OutboxDO.class);
    }

    @Test
    @DisplayName("未抢到锁直接跳过")
    void skipWhenLockHeld() {
        when(jobLock.tryLock("outboxCompensate")).thenReturn(false);
        job.compensate();
        verifyNoInteractions(outboxMapper, restTemplate);
        verify(jobLock, never()).unlock(anyString());
    }

    @Test
    @DisplayName("无待补偿消息仍释放锁")
    void emptyPending_unlocks() {
        when(jobLock.tryLock("outboxCompensate")).thenReturn(true);
        when(outboxMapper.selectList(any())).thenReturn(Collections.emptyList());
        job.compensate();
        verify(jobLock).unlock("outboxCompensate");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("HTTP 2xx 标记 SUCCESS")
    void success_marksSuccess() {
        when(jobLock.tryLock("outboxCompensate")).thenReturn(true);
        OutboxDO msg = pending(1L, 0);
        when(outboxMapper.selectList(any())).thenReturn(List.of(msg));
        when(restTemplate.postForEntity(eq("http://wu-service-order/inner/order/notify"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        job.compensate();

        ArgumentCaptor<OutboxDO> captor = ArgumentCaptor.forClass(OutboxDO.class);
        verify(outboxMapper).updateById(captor.capture());
        assertEquals("SUCCESS", captor.getValue().getStatus());
        verify(jobLock).unlock("outboxCompensate");
    }

    @Test
    @DisplayName("失败未达上限指数退避")
    void fail_schedulesRetry() {
        when(jobLock.tryLock("outboxCompensate")).thenReturn(true);
        OutboxDO msg = pending(2L, 0);
        when(outboxMapper.selectList(any())).thenReturn(List.of(msg));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("down"));

        job.compensate();

        ArgumentCaptor<OutboxDO> captor = ArgumentCaptor.forClass(OutboxDO.class);
        verify(outboxMapper).updateById(captor.capture());
        OutboxDO updated = captor.getValue();
        assertEquals(1, updated.getRetryCount());
        assertEquals("PENDING", updated.getStatus());
        assertNotNull(updated.getNextRetryTime());
    }

    @Test
    @DisplayName("达到最大重试标记 DEAD")
    void fail_marksDead() {
        when(jobLock.tryLock("outboxCompensate")).thenReturn(true);
        OutboxDO msg = pending(3L, 4);
        when(outboxMapper.selectList(any())).thenReturn(List.of(msg));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("down"));

        job.compensate();

        ArgumentCaptor<OutboxDO> captor = ArgumentCaptor.forClass(OutboxDO.class);
        verify(outboxMapper).updateById(captor.capture());
        assertEquals("DEAD", captor.getValue().getStatus());
        assertEquals(5, captor.getValue().getRetryCount());
    }

    private static OutboxDO pending(Long id, int retry) {
        OutboxDO msg = new OutboxDO();
        msg.setId(id);
        msg.setBizKey("order:" + id);
        msg.setTargetService("wu-service-order");
        msg.setTargetApi("/inner/order/notify");
        msg.setPayload("{}");
        msg.setStatus("PENDING");
        msg.setRetryCount(retry);
        return msg;
    }
}
