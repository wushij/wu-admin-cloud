package com.wu.cloud.common.config;

/**
 * 配置组加载 SPI：按 groupCode 返回 sys_config_group 的 configValue JSON 字符串。
 * <p>
 * 标准实现链（按 Order 依次尝试，先命中先返回）：
 * <ol>
 *   <li>Redis 缓存加载器（wu-cloud-common，读共享缓存 Hash）</li>
 *   <li>Feign 远程加载器（wu-cloud-api，回源 wu-service-system 内网接口）</li>
 * </ol>
 * 返回 null 或空白表示本加载器未命中，交由下一个加载器处理。
 * </p>
 */
public interface ConfigGroupLoader {

    /**
     * 加载指定配置组的 configValue JSON 字符串
     *
     * @param groupCode 配置组编码
     * @return JSON 字符串；未命中返回 null
     */
    String load(String groupCode);
}
