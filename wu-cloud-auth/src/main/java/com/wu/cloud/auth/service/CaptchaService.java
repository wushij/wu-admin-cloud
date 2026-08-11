package com.wu.cloud.auth.service;

import com.wu.cloud.auth.vo.CaptchaVO;
import com.wu.cloud.auth.vo.SliderCaptchaVO;

/**
 * 验证码服务：图形验证码与滑块验证码
 */
public interface CaptchaService {

    /**
     * 生成图形验证码
     */
    CaptchaVO generateImageCaptcha();

    /**
     * 校验图形验证码，校验后立即失效
     *
     * @param uuid 验证码标识
     * @param code 用户输入
     */
    void verifyImageCaptcha(String uuid, String code);

    /**
     * 生成滑块验证码挑战
     */
    SliderCaptchaVO generateSliderChallenge();

    /**
     * 校验滑块位移，通过后返回可用于登录的凭证
     *
     * @param token 挑战标识
     * @param x     用户拖动的横向位移
     * @return 通过凭证
     */
    String verifySlider(String token, Integer x);

    /**
     * 校验滑块通过凭证，校验后立即失效
     */
    void verifySliderTicket(String ticket);
}
