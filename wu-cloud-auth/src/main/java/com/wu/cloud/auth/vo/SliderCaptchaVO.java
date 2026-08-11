package com.wu.cloud.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 滑块验证码挑战响应
 */
@Data
public class SliderCaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 挑战标识，校验时回传 */
    private String token;

    /** 带缺口的背景图 Base64 */
    private String backgroundImage;

    /** 滑块拼图 Base64 */
    private String sliderImage;

    /** 背景图宽度 */
    private Integer backgroundWidth;

    /** 背景图高度 */
    private Integer backgroundHeight;

    /** 滑块边长 */
    private Integer sliderSize;

    /** 滑块纵向位置 */
    private Integer sliderY;
}
