package com.wu.cloud.auth.service.impl;

import com.wu.cloud.auth.config.AuthProperties;
import com.wu.cloud.auth.service.CaptchaService;
import com.wu.cloud.auth.support.AuthRedisKeys;
import com.wu.cloud.auth.vo.CaptchaVO;
import com.wu.cloud.auth.vo.SliderCaptchaVO;
import com.wu.cloud.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务实现：图形验证码基于 Hutool，滑块基于 Java2D；状态存放于 Redis
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final int BG_WIDTH = 320;
    private static final int BG_HEIGHT = 160;
    private static final int SLIDER_SIZE = 50;

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private AuthProperties authProperties;

    @Resource
    private com.wu.cloud.auth.config.AuthConfigLoader authConfigLoader;

    private boolean isCaptchaEnabled() {
        authConfigLoader.refreshLoginGroup();
        return authConfigLoader.isCaptchaEnabled();
    }

    @Override
    public CaptchaVO generateImageCaptcha() {
        CaptchaVO vo = new CaptchaVO();
        vo.setEnabled(isCaptchaEnabled());
        if (!isCaptchaEnabled()) {
            return vo;
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        cn.hutool.captcha.LineCaptcha lineCaptcha = cn.hutool.captcha.CaptchaUtil.createLineCaptcha(130, 48, 4, 50);
        String code = lineCaptcha.getCode();
        String imageBase64 = lineCaptcha.getImageBase64();

        redisTemplate.opsForValue().set(
                AuthRedisKeys.captcha(uuid),
                code.toLowerCase(),
                java.time.Duration.ofSeconds(authProperties.getCaptchaExpireSeconds()));

        vo.setUuid(uuid);
        vo.setImg(imageBase64);
        return vo;
    }

    @Override
    public void verifyImageCaptcha(String uuid, String code) {
        if (!isCaptchaEnabled()) {
            return;
        }
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
            throw new BusinessException("请输入验证码");
        }
        String key = AuthRedisKeys.captcha(uuid);
        String cached = redisTemplate.opsForValue().get(key);
        // 验证码一次性使用，无论成功失败都删除，避免被暴力枚举
        redisTemplate.delete(key);
        if (cached == null) {
            throw new BusinessException("验证码已过期，请刷新后重试");
        }
        if (!cached.equalsIgnoreCase(code.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    @Override
    public SliderCaptchaVO generateSliderChallenge() {
        // 缺口横坐标需为滑块留出起始与结束余量
        int gapX = ThreadLocalRandom.current().nextInt(SLIDER_SIZE + 20, BG_WIDTH - SLIDER_SIZE - 10);
        int gapY = ThreadLocalRandom.current().nextInt(10, BG_HEIGHT - SLIDER_SIZE - 10);

        BufferedImage background = drawSliderBackground();
        BufferedImage slider = cutSlider(background, gapX, gapY);

        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                AuthRedisKeys.slider(token),
                String.valueOf(gapX),
                java.time.Duration.ofSeconds(authProperties.getCaptchaExpireSeconds()));

        SliderCaptchaVO vo = new SliderCaptchaVO();
        vo.setToken(token);
        vo.setBackgroundImage(toBase64Png(background));
        vo.setSliderImage(toBase64Png(slider));
        vo.setBackgroundWidth(BG_WIDTH);
        vo.setBackgroundHeight(BG_HEIGHT);
        vo.setSliderSize(SLIDER_SIZE);
        vo.setSliderY(gapY);
        return vo;
    }

    @Override
    public String verifySlider(String token, Integer x) {
        if (!StringUtils.hasText(token) || x == null) {
            throw new BusinessException("滑块校验参数不完整");
        }
        String key = AuthRedisKeys.slider(token);
        String cached = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (cached == null) {
            throw new BusinessException("滑块验证已过期，请重新验证");
        }
        int expected = Integer.parseInt(cached);
        if (Math.abs(expected - x) > authProperties.getSliderTolerance()) {
            throw new BusinessException("滑块位置不正确，请重新验证");
        }

        // 校验通过后签发短期票据，供后续登录请求携带
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                AuthRedisKeys.sliderPass(ticket),
                "1",
                java.time.Duration.ofSeconds(authProperties.getCaptchaExpireSeconds()));
        return ticket;
    }

    @Override
    public void verifySliderTicket(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            throw new BusinessException("请先完成滑块验证");
        }
        String key = AuthRedisKeys.sliderPass(ticket);
        Boolean deleted = redisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new BusinessException("滑块验证已失效，请重新验证");
        }
    }

    /**
     * 绘制滑块底图：渐变背景加随机图形，保证缺口有辨识度
     */
    private BufferedImage drawSliderBackground() {
        BufferedImage image = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setPaint(new java.awt.GradientPaint(
                0, 0, randomColor(80, 160),
                BG_WIDTH, BG_HEIGHT, randomColor(80, 160)));
        g.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);

        for (int i = 0; i < 12; i++) {
            g.setColor(randomColor(100, 220));
            int size = 20 + randomInt(50);
            if (i % 2 == 0) {
                g.fillOval(randomInt(BG_WIDTH), randomInt(BG_HEIGHT), size, size);
            } else {
                g.fillRect(randomInt(BG_WIDTH), randomInt(BG_HEIGHT), size, size);
            }
        }

        g.dispose();
        return image;
    }

    /**
     * 从底图抠出滑块，并在底图上留下半透明缺口
     */
    private BufferedImage cutSlider(BufferedImage background, int gapX, int gapY) {
        GeneralPath shape = buildSliderShape(gapX, gapY);

        BufferedImage slider = new BufferedImage(SLIDER_SIZE, SLIDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = slider.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 将拼图形状平移到滑块画布原点后作为裁剪区
        sg.translate(-gapX, -gapY);
        sg.setClip(shape);
        sg.drawImage(background, 0, 0, null);
        sg.dispose();

        Graphics2D bg = background.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.setClip(shape);
        bg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        bg.setColor(new Color(30, 30, 30));
        bg.fillRect(gapX, gapY, SLIDER_SIZE, SLIDER_SIZE);
        bg.dispose();

        return slider;
    }

    /**
     * 构造带凸起的经典拼图形状
     */
    private GeneralPath buildSliderShape(int x, int y) {
        int r = SLIDER_SIZE / 5;
        GeneralPath path = new GeneralPath();
        path.moveTo(x, y);
        path.lineTo(x + SLIDER_SIZE / 2.0 - r, y);
        // 上方凸起
        path.curveTo(x + SLIDER_SIZE / 2.0 - r, y - r * 1.5,
                x + SLIDER_SIZE / 2.0 + r, y - r * 1.5,
                x + SLIDER_SIZE / 2.0 + r, y);
        path.lineTo(x + SLIDER_SIZE, y);
        path.lineTo(x + SLIDER_SIZE, y + SLIDER_SIZE / 2.0 - r);
        // 右侧凸起
        path.curveTo(x + SLIDER_SIZE + r * 1.5, y + SLIDER_SIZE / 2.0 - r,
                x + SLIDER_SIZE + r * 1.5, y + SLIDER_SIZE / 2.0 + r,
                x + SLIDER_SIZE, y + SLIDER_SIZE / 2.0 + r);
        path.lineTo(x + SLIDER_SIZE, y + SLIDER_SIZE);
        path.lineTo(x, y + SLIDER_SIZE);
        path.closePath();
        return path;
    }

    private String toBase64Png(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("[验证码] 图片编码失败", e);
            throw new BusinessException("验证码生成失败，请稍后重试");
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(
                min + ThreadLocalRandom.current().nextInt(range),
                min + ThreadLocalRandom.current().nextInt(range),
                min + ThreadLocalRandom.current().nextInt(range));
    }

    private int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}