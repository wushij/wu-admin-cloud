package com.wu.cloud.common.banner;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微服务启动成功炫彩控制台横幅打印器 (StartupBanner)
 * <p>
 * 自动解析服务标识、实际端口、Nacos 注册中心状态、网关路由映射及 Knife4j 接口文档地址，
 * 采用 ANSI 彩色高亮终端展示，支持自动事件监听与 main 方法手动调用双保险（自动幂等去重）。
 * </p>
 */
public class StartupBanner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Set<Integer> PRINTED_CONTEXTS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<String, ServiceMeta> SERVICE_META_MAP = new LinkedHashMap<>();

    static {
        registerMeta("wu-cloud-gateway", "统一微服务网关 (Gateway)", "", "国密 SM2/SM3/SM4 · 防重放签名 · 反爬风控 · 动态网关路由已生效");
        registerMeta("wu-cloud-auth", "统一认证授权中心 (Auth)", "/api/auth", "Sa-Token 统一会话存储 · 多端 SSO 登录 · 验证码风控已生效");
        registerMeta("wu-admin", "聚合服务编排中心 (Admin BFF)", "/api/admin", "BFF 并行编排线程池 (DASHBOARD_EXECUTOR) · 前后端适配已生效");
        registerMeta("wu-service-system", "系统管理微服务 (System)", "/api/system", "Sa-Token 分布式鉴权 · 用户角色菜单 · 字典参数治理已生效");
        registerMeta("wu-service-ticket", "工单与审批微服务 (Ticket)", "/api/ticket", "Sa-Token 分布式鉴权 · 状态机流转 · 工单级联通知已生效");
        registerMeta("wu-service-message", "消息与企业IM微服务 (Message)", "/api/message", "WebSocket 跨实例集群 · 站内信 · 离线消息拉取已生效");
        registerMeta("wu-service-ai", "AI 智能助手微服务 (AI)", "/api/ai", "大模型流式对话 (SSE) · 智能知识库 · 文本生成服务已生效");
        registerMeta("wu-service-pay", "支付中心微服务 (Pay)", "/api/pay", "微信/支付宝全渠道接入 · 本地消息表 · 支付状态机已生效");
        registerMeta("wu-service-order", "订单交易微服务 (Order)", "/api/order", "分布式高并发下单 · 超时关单 · 交易状态机已生效");
        registerMeta("wu-service-file", "文件存储微服务 (File)", "/api/file", "四层语义存储对象 · 本地/MinIO 双引擎 · 统一流式直出已生效");
        registerMeta("wu-service-infra", "基础设施微服务 (Infra)", "/api/infra", "在线代码生成 · 数据监控 · 分布式任务调度已生效");
    }

    private static void registerMeta(String appName, String description, String gatewayPrefix, String features) {
        SERVICE_META_MAP.put(appName, new ServiceMeta(description, gatewayPrefix, features));
    }

    /**
     * 手动打印启动横幅（幂等安全）
     *
     * @param context Spring 容器上下文
     */
    public static void print(ApplicationContext context) {
        print(context, null);
    }

    /**
     * 手动打印启动横幅（指定自定义服务描述，幂等安全）
     *
     * @param context Spring 容器上下文
     * @param customDescription 自定义服务中文描述
     */
    public static void print(ApplicationContext context, String customDescription) {
        if (context == null) {
            return;
        }
        Environment env = context.getEnvironment();
        if (isTestEnvironment(env)) {
            return;
        }
        int contextId = System.identityHashCode(context);
        if (PRINTED_CONTEXTS.add(contextId)) {
            doPrint(env, customDescription);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        print(event.getApplicationContext());
    }

    private static void doPrint(Environment env, String customDescription) {
        String appName = env.getProperty("spring.application.name", "wu-cloud-service");
        String port = env.getProperty("local.server.port");
        if (!StringUtils.hasText(port)) {
            port = env.getProperty("server.port", "8080");
        }

        // 若端口非法或属于无服务端口测试场景，跳过打印
        if ("-1".equals(port) || "0".equals(port)) {
            return;
        }

        String contextPath = env.getProperty("server.servlet.context-path", "");
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            contextPath = "";
        }

        String ip = getLocalIp();

        String[] activeProfiles = env.getActiveProfiles();
        String profile = (activeProfiles != null && activeProfiles.length > 0)
                ? String.join(", ", activeProfiles)
                : "dev";

        ServiceMeta meta = SERVICE_META_MAP.getOrDefault(appName, new ServiceMeta(
                StringUtils.hasText(customDescription) ? customDescription : appName,
                "/api/" + appName.replace("wu-service-", "").replace("wu-cloud-", ""),
                "Sa-Token 分布式鉴权 & Sentinel 熔断保护已生效"
        ));

        String serviceTitle = StringUtils.hasText(customDescription) ? customDescription : meta.description;

        // 注册中心信息
        String nacosServer = env.getProperty("spring.cloud.nacos.discovery.server-addr",
                env.getProperty("spring.cloud.nacos.config.server-addr", "127.0.0.1:8848"));
        String nacosNamespace = env.getProperty("spring.cloud.nacos.discovery.namespace",
                env.getProperty("spring.cloud.nacos.config.namespace", "dev"));
        String nacosGroup = env.getProperty("spring.cloud.nacos.discovery.group",
                env.getProperty("spring.cloud.nacos.config.group", "WU_CLOUD"));

        // 特殊服务的文档和网关路由处理
        boolean isGateway = "wu-cloud-gateway".equalsIgnoreCase(appName);
        String docLine;
        String gatewayLine;

        if (isGateway) {
            docLine = "\u001B[33m  > 聚合文档:\u001B[0m \u001B[34;1mhttp://localhost:%s/api/doc.html\u001B[0m (Knife4j 聚合接口文档)".formatted(port);
            gatewayLine = "\u001B[33m  > 网关映射:\u001B[0m \u001B[1mhttp://localhost:%s/api/**\u001B[0m (全微服务统一调度反代与风控入口)".formatted(port);
        } else {
            docLine = "\u001B[33m  > 接口文档:\u001B[0m \u001B[34;1mhttp://localhost:%s%s/doc.html\u001B[0m (网关聚合: \u001B[34mhttp://localhost:8080/api/doc.html\u001B[0m)".formatted(port, contextPath);
            gatewayLine = "\u001B[33m  > 网关映射:\u001B[0m \u001B[1mhttp://localhost:8080%s/**\u001B[0m".formatted(meta.gatewayPrefix);
        }

        String banner = """
                
                \u001B[32;1m========================================================================================\u001B[0m
                \u001B[36;1m __        __ _   _       _       ____   __  __ ___ _   _                           \u001B[0m
                \u001B[36;1m \\ \\      / /| | | |     / \\     |  _ \\ |  \\/  |_ _| \\ | |   ____ _                 _   \u001B[0m
                \u001B[36;1m  \\ \\ /\\ / / | | | |    / _ \\    | | | || |\\/| || ||  \\| |  / ___| | ___  _   _  __| |  \u001B[0m
                \u001B[36;1m   \\ V  V /  | |_| |   / ___ \\   | |_| || |  | || || |\\  | | |   | |/ _ \\| | | |/ _` |  \u001B[0m
                \u001B[36;1m    \\_/\\_/    \\___/   /_/   \\_\\  |____/ |_|  |_|___|_| \\_|  \\____|_|\\___/ \\__,_|\\__,_|  \u001B[0m
                
                \u001B[32;1m  ★ Wu Admin Cloud · 企业级微服务治理平台 —— [%s] 启动成功！★\u001B[0m
                
                \u001B[33m  > 服务标识:\u001B[0m \u001B[1m%s\u001B[0m (端口: \u001B[1m%s\u001B[0m)
                \u001B[33m  > 本地直连:\u001B[0m \u001B[1mhttp://localhost:%s%s/\u001B[0m
                \u001B[33m  > 局域网络:\u001B[0m \u001B[1mhttp://%s:%s%s/\u001B[0m
                %s
                %s
                \u001B[33m  > 注册中心:\u001B[0m Nacos (%s) [命名空间: \u001B[36m%s\u001B[0m, 分组: \u001B[36m%s\u001B[0m]
                \u001B[33m  > 运行环境:\u001B[0m \u001B[35;1m%s\u001B[0m
                \u001B[33m  > 核心特性:\u001B[0m \u001B[32;1m%s\u001B[0m
                \u001B[32;1m========================================================================================\u001B[0m
                """.formatted(
                serviceTitle,
                appName, port,
                port, contextPath,
                ip, port, contextPath,
                docLine,
                gatewayLine,
                nacosServer, nacosNamespace, nacosGroup,
                profile,
                meta.features
        );

        System.out.println(banner);
    }

    private static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
            return "127.0.0.1";
        }
    }

    private static boolean isTestEnvironment(Environment env) {
        if (env == null) {
            return false;
        }
        for (String profile : env.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        String testProperty = env.getProperty("org.springframework.boot.test.context.SpringBootTest");
        if (StringUtils.hasText(testProperty)) {
            return true;
        }
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith("org.junit.") || className.startsWith("org.testng.")) {
                return true;
            }
        }
        return false;
    }

    private record ServiceMeta(String description, String gatewayPrefix, String features) {
    }
}

