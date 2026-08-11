package com.wu.cloud.infra.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.cloud.api.infra.dto.OutboxDTO;
import com.wu.cloud.common.core.domain.R;
import com.wu.cloud.infra.entity.OutboxDO;
import com.wu.cloud.infra.mapper.OutboxMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 本地消息表内部写入接口（P1-4 修复）
 * <p>
 * 供业务服务通过 {@code RemoteOutboxService} Feign 调用，在业务事务提交后写入补偿消息。
 * 路径 /inner/outbox/** 受网关 InnerApiGuardFilter 保护，禁止外部访问。
 * </p>
 */
@RestController
@RequestMapping("/inner/outbox")
public class InnerOutboxController {

    @Resource
    private OutboxMapper outboxMapper;

    /**
     * 写入补偿消息（按 bizKey 幂等）。
     *
     * <p>infra_outbox 上有 {@code UNIQUE KEY uk_biz_key}，同一 bizKey 重复写入会抛
     * DuplicateKeyException。而调用方（如支付回调）本身可能被第三方重复投递，
     * 因此这里先查后插并捕获唯一键冲突，保证「重复写入不报错、也不产生重复补偿」。</p>
     */
    @PostMapping("/save")
    public R<Long> save(@RequestBody OutboxDTO dto) {
        OutboxDO exist = outboxMapper.selectOne(new LambdaQueryWrapper<OutboxDO>()
                .eq(OutboxDO::getBizKey, dto.getBizKey())
                .last("LIMIT 1"));
        if (exist != null) {
            return R.ok(exist.getId());
        }
        OutboxDO outbox = new OutboxDO();
        outbox.setBizKey(dto.getBizKey());
        outbox.setTargetService(dto.getTargetService());
        outbox.setTargetApi(dto.getTargetApi());
        outbox.setPayload(dto.getPayload());
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setNextRetryTime(LocalDateTime.now());
        outbox.setCreateTime(LocalDateTime.now());
        try {
            outboxMapper.insert(outbox);
        } catch (DuplicateKeyException e) {
            // 并发写入：另一线程已插入同一 bizKey，视为成功
            OutboxDO concurrent = outboxMapper.selectOne(new LambdaQueryWrapper<OutboxDO>()
                    .eq(OutboxDO::getBizKey, dto.getBizKey())
                    .last("LIMIT 1"));
            return R.ok(concurrent == null ? null : concurrent.getId());
        }
        return R.ok(outbox.getId());
    }
}
