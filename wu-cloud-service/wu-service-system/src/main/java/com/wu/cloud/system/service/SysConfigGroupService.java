package com.wu.cloud.system.service;

import com.wu.cloud.system.entity.SysConfigGroupDO;
import com.wu.cloud.system.vo.config.ConfigTestEmailReqVO;
import com.wu.cloud.system.vo.config.ConfigTestPaymentReqVO;
import com.wu.cloud.system.vo.config.ConfigTestRespVO;
import com.wu.cloud.system.vo.config.ConfigTestSmsReqVO;

import java.util.List;
import java.util.Map;

public interface SysConfigGroupService {

    /**
     * 配置分组全量列表
     */
    List<SysConfigGroupDO> getConfigGroupList(String groupCode, String groupName);

    /**
     * 按 groupCode 获取单个配置分组
     */
    SysConfigGroupDO getConfigGroupByCode(String groupCode);

    /**
     * 解析配置分组的 configValue（JSON）为键值对，分组不存在时返回空 Map
     *
     * @param groupCode 分组编码
     * @return 配置项键值对
     */
    Map<String, Object> getConfigMap(String groupCode);

    /**
     * 新建配置分组
     */
    void createConfigGroup(SysConfigGroupDO groupDO);

    /**
     * 更新配置分组（按 groupCode）
     */
    void updateConfigGroup(SysConfigGroupDO groupDO);

    /**
     * 删除配置分组
     */
    void deleteConfigGroup(Long id);

    /**
     * 测试支付配置是否可用：调用支付服务创建一笔测试订单
     *
     * @param request 支付类型
     * @return 测试结果，成功时包含订单号与二维码链接
     */
    ConfigTestRespVO testPayment(ConfigTestPaymentReqVO request);

    /**
     * 测试短信配置是否可用：向指定手机号发送一条测试短信
     *
     * @param request 手机号与模板
     * @return 测试结果
     */
    ConfigTestRespVO testSms(ConfigTestSmsReqVO request);

    /**
     * 测试邮件配置是否可用：向指定邮箱发送一封测试邮件
     *
     * @param request 收件邮箱
     * @return 测试结果
     */
    ConfigTestRespVO testEmail(ConfigTestEmailReqVO request);
}
