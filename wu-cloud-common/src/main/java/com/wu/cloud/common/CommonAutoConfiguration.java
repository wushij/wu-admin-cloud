package com.wu.cloud.common;

import com.wu.cloud.common.mail.MailProperties;
import com.wu.cloud.common.mail.MailSender;
import com.wu.cloud.common.mail.impl.MockMailSender;
import com.wu.cloud.common.mail.impl.SmtpMailSender;
import com.wu.cloud.common.sms.SmsProperties;
import com.wu.cloud.common.sms.SmsSender;
import com.wu.cloud.common.sms.impl.MockSmsSender;
import com.wu.cloud.common.security.aspect.PermissionAspect;
import com.wu.cloud.common.web.GlobalExceptionHandler;
import com.wu.cloud.common.xss.XssFilter;
import com.wu.cloud.common.xss.XssProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * 公共模块自动配置
 * <p>
 * 引入 wu-cloud-common 的 Web 微服务会自动获得全局异常处理、XSS 过滤、
 * 短信与邮件发送能力，无需各模块重复编写。
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties({SmsProperties.class, MailProperties.class, XssProperties.class})
public class CommonAutoConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication(type = org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 权限校验切面，需类路径存在 AspectJ
     */
    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    @ConditionalOnMissingBean(PermissionAspect.class)
    public PermissionAspect permissionAspect() {
        return new PermissionAspect();
    }

    /**
     * XSS 过滤器，可通过 wu.xss.enabled=false 关闭
     */
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    @ConditionalOnProperty(prefix = "wu.xss", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<XssFilter> xssFilterRegistration(XssProperties properties) {
        FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new XssFilter(properties.getExcludeUrls()));
        registration.addUrlPatterns("/*");
        registration.setName("xssFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registration;
    }

    /**
     * 默认短信渠道，未配置真实渠道时使用 Mock
     */
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender smsSender() {
        return new MockSmsSender();
    }

    /**
     * 邮件渠道：wu.mail.enabled=true 时启用 SMTP，否则使用 Mock
     */
    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public MailSender mailSender(MailProperties properties) {
        if (properties.isEnabled() && properties.getHost() != null && !properties.getHost().isBlank()) {
            return new SmtpMailSender(properties);
        }
        return new MockMailSender();
    }
}
